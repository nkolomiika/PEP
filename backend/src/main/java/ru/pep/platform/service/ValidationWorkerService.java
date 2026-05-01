package ru.pep.platform.service;

import java.io.IOException;
import java.time.Instant;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import ru.pep.platform.domain.Submission;
import ru.pep.platform.domain.SubmissionSourceType;
import ru.pep.platform.domain.ValidationJob;
import ru.pep.platform.domain.ValidationJobStatus;
import ru.pep.platform.repository.ValidationJobRepository;

@Service
@ConditionalOnProperty(prefix = "pep.validation-worker", name = "enabled", havingValue = "true")
public class ValidationWorkerService {

    private final ValidationJobRepository validationJobs;
    private final HttpClient httpClient;
    private final TransactionTemplate transactions;
    private final String hostAddress;
    private final String localRegistry;
    private final Duration commandTimeout;
    private final Duration healthTimeout;

    public ValidationWorkerService(
            ValidationJobRepository validationJobs,
            TransactionTemplate transactions,
            @Value("${pep.validation-worker.host-address:host.docker.internal}") String hostAddress,
            @Value("${pep.validation-worker.local-registry:localhost:5001}") String localRegistry,
            @Value("${pep.validation-worker.command-timeout-seconds:120}") long commandTimeoutSeconds,
            @Value("${pep.validation-worker.health-timeout-seconds:30}") long healthTimeoutSeconds) {
        this.validationJobs = validationJobs;
        this.transactions = transactions;
        this.hostAddress = hostAddress;
        this.localRegistry = localRegistry;
        this.commandTimeout = Duration.ofSeconds(commandTimeoutSeconds);
        this.healthTimeout = Duration.ofSeconds(healthTimeoutSeconds);
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    @Scheduled(fixedDelayString = "${pep.validation-worker.poll-interval-ms:5000}")
    public void processNextQueuedJob() {
        validationJobs.findFirstByStatusOrderByCreatedAtAsc(ValidationJobStatus.QUEUED)
                .ifPresent(job -> {
                    String containerName = "pep-validation-" + job.getId().toString().substring(0, 8);
                    try {
                        validate(job.getId(), containerName);
                    } catch (RuntimeException exception) {
                        failJob(job.getId(), exception.getMessage());
                    } finally {
                        cleanupContainer(containerName);
                    }
                });
    }

    private void validate(java.util.UUID jobId, String containerName) {
        JobSnapshot snapshot = markPullingImage(jobId);
        String runtimeImageReference = snapshot.imageReference();
        if (snapshot.sourceType() == SubmissionSourceType.ARCHIVE) {
            runtimeImageReference = buildArchiveImage(jobId, snapshot);
        } else {
            runCommand(List.of("docker", "pull", runtimeImageReference));
        }
        JobSnapshot runtimeSnapshot = snapshot.withImageReference(runtimeImageReference);
        scanImage(jobId, runtimeSnapshot);
        scanDependencies(jobId, runtimeSnapshot);

        markStartingContainer(jobId);
        runCommand(List.of(
                "docker",
                "run",
                "-d",
                "--rm",
                "--name",
                containerName,
                "-p",
                runtimeSnapshot.applicationPort().toString(),
                runtimeSnapshot.imageReference()));

        markCheckingPort(jobId);
        String hostPort = publishedHostPort(containerName, runtimeSnapshot.applicationPort());
        checkHttpEndpoint("http://" + hostAddress + ":" + hostPort + "/");

        if (runtimeSnapshot.healthPath() != null && !runtimeSnapshot.healthPath().isBlank()) {
            markCheckingHealth(jobId);
            checkHttpEndpoint("http://" + hostAddress + ":" + hostPort + runtimeSnapshot.healthPath());
        }

        passJob(jobId);
    }

    private String buildArchiveImage(java.util.UUID jobId, JobSnapshot snapshot) {
        if (snapshot.archiveStoragePath() == null || snapshot.archiveStoragePath().isBlank()) {
            throw new IllegalStateException("Archive submission не содержит путь к архиву");
        }
        Path archivePath = Path.of(snapshot.archiveStoragePath()).toAbsolutePath().normalize();
        if (!Files.isRegularFile(archivePath)) {
            throw new IllegalStateException("Архив стенда не найден");
        }
        Path workDir = null;
        try {
            workDir = Files.createTempDirectory("pep-stand-build-");
            extractArchive(archivePath, workDir);
            validateBuildContext(workDir);
            String runtimeImage = localRegistry + "/student-lab-" + jobId.toString().substring(0, 8) + ":latest";
            if (Files.exists(workDir.resolve("docker-compose.yml")) || Files.exists(workDir.resolve("compose.yaml"))) {
                buildComposeImage(workDir, runtimeImage, snapshot.composeService());
            } else if (Files.exists(workDir.resolve("Dockerfile"))) {
                runCommand(List.of("docker", "build", "-t", runtimeImage, "."), workDir);
            } else {
                throw new IllegalStateException("В архиве нужен Dockerfile или docker-compose.yml");
            }
            runCommand(List.of("docker", "push", runtimeImage));
            updateJob(jobId, job -> job.useBuiltImage(runtimeImage));
            return runtimeImage;
        } catch (IOException exception) {
            throw new IllegalStateException("Не удалось подготовить архив стенда");
        } finally {
            deleteDirectory(workDir);
        }
    }

    private void buildComposeImage(Path workDir, String runtimeImage, String requestedService) {
        String composeFile = Files.exists(workDir.resolve("docker-compose.yml")) ? "docker-compose.yml" : "compose.yaml";
        String service = requestedService;
        if (service == null || service.isBlank()) {
            service = runCompose(composeFile, List.of("config", "--services"), workDir)
                    .output()
                    .lines()
                    .filter(line -> !line.isBlank())
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("docker-compose.yml не содержит services"));
        }
        runCompose(composeFile, List.of("build", service), workDir);
        String serviceImage = runCompose(composeFile, List.of("images", "-q", service), workDir)
                .output()
                .lines()
                .filter(line -> !line.isBlank())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Не удалось определить image для compose service"));
        runCommand(List.of("docker", "tag", serviceImage, runtimeImage));
    }

    private CommandResult runCompose(String composeFile, List<String> args, Path workingDirectory) {
        List<String> dockerCompose = new java.util.ArrayList<>(List.of("docker", "compose", "-f", composeFile));
        dockerCompose.addAll(args);
        try {
            return runCommand(dockerCompose, workingDirectory);
        } catch (IllegalStateException firstFailure) {
            List<String> legacyCompose = new java.util.ArrayList<>(List.of("docker-compose", "-f", composeFile));
            legacyCompose.addAll(args);
            return runCommand(legacyCompose, workingDirectory);
        }
    }

    private void extractArchive(Path archivePath, Path destination) throws IOException {
        String filename = archivePath.getFileName().toString().toLowerCase();
        if (filename.endsWith(".zip")) {
            extractZip(archivePath, destination);
            return;
        }
        if (filename.endsWith(".tar") || filename.endsWith(".tar.gz") || filename.endsWith(".tgz")) {
            runCommand(List.of("tar", "-xf", archivePath.toString(), "-C", destination.toString()));
            return;
        }
        throw new IllegalStateException("Неподдерживаемый формат архива");
    }

    private void extractZip(Path archivePath, Path destination) throws IOException {
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(archivePath))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                Path target = destination.resolve(entry.getName()).normalize();
                if (!target.startsWith(destination)) {
                    throw new IllegalStateException("Архив содержит небезопасный путь");
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    Files.copy(zip, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private void validateBuildContext(Path workDir) throws IOException {
        List<String> forbiddenFragments = List.of(
                "privileged: true",
                "network_mode: host",
                "pid: host",
                "/var/run/docker.sock",
                "host.docker.internal");
        try (var paths = Files.walk(workDir)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().equals("docker-compose.yml")
                            || path.getFileName().toString().equals("compose.yaml")
                            || path.getFileName().toString().equals("Dockerfile"))
                    .forEach(path -> {
                        try {
                            String content = Files.readString(path).toLowerCase();
                            forbiddenFragments.forEach(fragment -> {
                                if (content.contains(fragment)) {
                                    throw new IllegalStateException("Архив содержит запрещенную настройку: " + fragment);
                                }
                            });
                        } catch (IOException exception) {
                            throw new IllegalStateException("Не удалось прочитать build descriptor");
                        }
                    });
        }
    }

    private void scanImage(java.util.UUID jobId, JobSnapshot snapshot) {
        try {
            CommandResult result = runCommand(List.of(
                    "docker",
                    "image",
                    "inspect",
                    snapshot.imageReference(),
                    "--format",
                    "User={{.Config.User}}\nExposedPorts={{json .Config.ExposedPorts}}\nSize={{.Size}}"));
            ImageScanResult scan = analyzeImageInspect(snapshot, result.output());
            updateJob(jobId, job -> {
                if (scan.hasWarnings()) {
                    job.markImageScanWarnings(scan.summary(), scan.report());
                } else {
                    job.markImageScanPassed(scan.summary(), scan.report());
                }
            });
        } catch (RuntimeException exception) {
            updateJob(jobId, job -> job.markImageScanFailed("Image scan не выполнен", sanitizeOutput(exception.getMessage())));
        }
    }

    private ImageScanResult analyzeImageInspect(JobSnapshot snapshot, String inspectOutput) {
        boolean rootUser = inspectOutput.lines()
                .filter(line -> line.startsWith("User="))
                .map(line -> line.substring("User=".length()).trim())
                .findFirst()
                .map(user -> user.isBlank() || "root".equals(user) || "0".equals(user))
                .orElse(true);
        boolean exposesApplicationPort = inspectOutput.contains("\"" + snapshot.applicationPort() + "/tcp\"");

        StringBuilder summary = new StringBuilder();
        if (rootUser) {
            summary.append("Image запускается от root; ");
        }
        if (!exposesApplicationPort) {
            summary.append("application port не объявлен в EXPOSE; ");
        }
        if (summary.isEmpty()) {
            summary.append("Базовый image scan не выявил предупреждений.");
        }
        String report = "Baseline image scan\n" + inspectOutput.strip();
        return new ImageScanResult(summary.toString().strip(), report, rootUser || !exposesApplicationPort);
    }

    private void scanDependencies(java.util.UUID jobId, JobSnapshot snapshot) {
        try {
            CommandResult labels = runCommand(List.of(
                    "docker",
                    "image",
                    "inspect",
                    snapshot.imageReference(),
                    "--format",
                    "Labels={{json .Config.Labels}}"));
            CommandResult history = runCommand(List.of(
                    "docker",
                    "image",
                    "history",
                    "--no-trunc",
                    "--format",
                    "{{.CreatedBy}}",
                    snapshot.imageReference()));
            DependencyScanResult scan = analyzeDependencyMetadata(labels.output(), history.output());
            updateJob(jobId, job -> {
                if (scan.hasWarnings()) {
                    job.markDependencyScanWarnings(scan.summary(), scan.report());
                } else {
                    job.markDependencyScanPassed(scan.summary(), scan.report());
                }
            });
        } catch (RuntimeException exception) {
            updateJob(jobId, job -> job.markDependencyScanFailed("Dependency scan не выполнен", sanitizeOutput(exception.getMessage())));
        }
    }

    private DependencyScanResult analyzeDependencyMetadata(String labelsOutput, String historyOutput) {
        boolean hasSbomLabel = labelsOutput.toLowerCase().contains("sbom");
        boolean hasPackageInstallCommands = historyOutput.lines().anyMatch(line -> {
            String normalized = line.toLowerCase();
            return normalized.contains("apt-get install")
                    || normalized.contains("apk add")
                    || normalized.contains("yum install")
                    || normalized.contains("pip install")
                    || normalized.contains("npm install")
                    || normalized.contains("mvn ");
        });

        StringBuilder summary = new StringBuilder();
        if (!hasSbomLabel) {
            summary.append("SBOM/dependency labels не найдены; ");
        }
        if (hasPackageInstallCommands) {
            summary.append("history содержит package install commands; ");
        }
        if (summary.isEmpty()) {
            summary.append("Baseline dependency scan не выявил предупреждений.");
        }
        String report = "Baseline dependency scan\n" + labelsOutput.strip() + "\n\nImage history\n" + historyOutput.strip();
        return new DependencyScanResult(summary.toString().strip(), report, !hasSbomLabel || hasPackageInstallCommands);
    }

    private JobSnapshot markPullingImage(java.util.UUID jobId) {
        return transactions.execute(status -> {
            ValidationJob job = validationJobs.findById(jobId)
                    .orElseThrow(() -> new CorePlatformService.NotFoundException("Validation job не найден"));
            Submission submission = job.getSubmission();
            job.markPullingImage();
            return new JobSnapshot(
                    job.getImageReference(),
                    submission.getSourceType(),
                    submission.getArchiveStoragePath(),
                    submission.getComposeService(),
                    submission.getApplicationPort(),
                    submission.getHealthPath());
        });
    }

    private void markStartingContainer(java.util.UUID jobId) {
        updateJob(jobId, ValidationJob::markStartingContainer);
    }

    private void markCheckingPort(java.util.UUID jobId) {
        updateJob(jobId, ValidationJob::markCheckingPort);
    }

    private void markCheckingHealth(java.util.UUID jobId) {
        updateJob(jobId, ValidationJob::markCheckingHealth);
    }

    private void passJob(java.util.UUID jobId) {
        updateJob(jobId, job -> job.pass("docker://validation/" + job.getId() + "/logs"));
    }

    private void failJob(java.util.UUID jobId, String errorMessage) {
        updateJob(jobId, job -> job.fail(errorMessage == null ? "Technical validation failed" : errorMessage));
    }

    private void updateJob(java.util.UUID jobId, java.util.function.Consumer<ValidationJob> update) {
        transactions.executeWithoutResult(status -> validationJobs.findById(jobId).ifPresent(update));
    }

    private String publishedHostPort(String containerName, Integer applicationPort) {
        CommandResult result = runCommand(List.of("docker", "port", containerName, applicationPort + "/tcp"));
        String portLine = result.output().lines().findFirst()
                .orElseThrow(() -> new IllegalStateException("Docker не вернул published port"));
        int separator = portLine.lastIndexOf(':');
        if (separator < 0 || separator == portLine.length() - 1) {
            throw new IllegalStateException("Некорректный published port: " + portLine);
        }
        return portLine.substring(separator + 1).trim();
    }

    private void checkHttpEndpoint(String url) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(healthTimeout)
                .GET()
                .build();
        Instant deadline = Instant.now().plus(healthTimeout);
        RuntimeException lastFailure = null;
        while (Instant.now().isBefore(deadline)) {
            try {
                HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
                if (response.statusCode() < 500) {
                    return;
                }
                lastFailure = new IllegalStateException("Endpoint вернул HTTP " + response.statusCode() + ": " + url);
            } catch (IOException exception) {
                lastFailure = new IllegalStateException("Endpoint недоступен: " + url);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Health check interrupted");
            }
            sleepBeforeRetry();
        }
        throw lastFailure == null ? new IllegalStateException("Endpoint недоступен: " + url) : lastFailure;
    }

    private void sleepBeforeRetry() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Health check interrupted");
        }
    }

    private CommandResult runCommand(List<String> command) {
        return runCommand(command, null);
    }

    private CommandResult runCommand(List<String> command, Path workingDirectory) {
        ProcessBuilder builder = new ProcessBuilder(command);
        if (workingDirectory != null) {
            builder.directory(workingDirectory.toFile());
        }
        builder.redirectErrorStream(true);
        try {
            Process process = builder.start();
            boolean finished = process.waitFor(commandTimeout.toSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IllegalStateException("Command timeout: " + String.join(" ", command));
            }
            String output = new String(process.getInputStream().readAllBytes());
            if (process.exitValue() != 0) {
                throw new IllegalStateException(sanitizeOutput(output));
            }
            return new CommandResult(output);
        } catch (IOException exception) {
            throw new IllegalStateException("Не удалось выполнить docker command: " + command.get(0));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Docker command interrupted");
        }
    }

    private void deleteDirectory(Path directory) {
        if (directory == null || !Files.exists(directory)) {
            return;
        }
        try (var paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                }
            });
        } catch (IOException ignored) {
        }
    }

    private void cleanupContainer(String containerName) {
        ProcessBuilder builder = new ProcessBuilder("docker", "rm", "-f", containerName);
        builder.redirectErrorStream(true);
        try {
            builder.start().waitFor(30, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        } catch (IOException ignored) {
        }
    }

    private String sanitizeOutput(String output) {
        String trimmed = Optional.ofNullable(output).orElse("").trim();
        if (trimmed.isBlank()) {
            return "Docker command failed";
        }
        return trimmed.length() > 1000 ? trimmed.substring(0, 1000) : trimmed;
    }

    private record CommandResult(String output) {
    }

    private record ImageScanResult(String summary, String report, boolean hasWarnings) {
    }

    private record DependencyScanResult(String summary, String report, boolean hasWarnings) {
    }

    private record JobSnapshot(
            String imageReference,
            SubmissionSourceType sourceType,
            String archiveStoragePath,
            String composeService,
            Integer applicationPort,
            String healthPath) {
        private JobSnapshot withImageReference(String nextImageReference) {
            return new JobSnapshot(
                    nextImageReference,
                    sourceType,
                    archiveStoragePath,
                    composeService,
                    applicationPort,
                    healthPath);
        }
    }
}
