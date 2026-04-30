package ru.pep.platform.service;

import java.io.IOException;
import java.time.Instant;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import ru.pep.platform.domain.Submission;
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
    private final Duration commandTimeout;
    private final Duration healthTimeout;

    public ValidationWorkerService(
            ValidationJobRepository validationJobs,
            TransactionTemplate transactions,
            @Value("${pep.validation-worker.host-address:host.docker.internal}") String hostAddress,
            @Value("${pep.validation-worker.command-timeout-seconds:120}") long commandTimeoutSeconds,
            @Value("${pep.validation-worker.health-timeout-seconds:30}") long healthTimeoutSeconds) {
        this.validationJobs = validationJobs;
        this.transactions = transactions;
        this.hostAddress = hostAddress;
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
        runCommand(List.of("docker", "pull", snapshot.imageReference()));

        markStartingContainer(jobId);
        runCommand(List.of(
                "docker",
                "run",
                "-d",
                "--rm",
                "--name",
                containerName,
                "-p",
                snapshot.applicationPort().toString(),
                snapshot.imageReference()));

        markCheckingPort(jobId);
        String hostPort = publishedHostPort(containerName, snapshot.applicationPort());
        checkHttpEndpoint("http://" + hostAddress + ":" + hostPort + "/");

        if (snapshot.healthPath() != null && !snapshot.healthPath().isBlank()) {
            markCheckingHealth(jobId);
            checkHttpEndpoint("http://" + hostAddress + ":" + hostPort + snapshot.healthPath());
        }

        passJob(jobId);
    }

    private JobSnapshot markPullingImage(java.util.UUID jobId) {
        return transactions.execute(status -> {
            ValidationJob job = validationJobs.findById(jobId)
                    .orElseThrow(() -> new CorePlatformService.NotFoundException("Validation job не найден"));
            Submission submission = job.getSubmission();
            job.markPullingImage();
            return new JobSnapshot(job.getImageReference(), submission.getApplicationPort(), submission.getHealthPath());
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
        ProcessBuilder builder = new ProcessBuilder(command);
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

    private record JobSnapshot(String imageReference, Integer applicationPort, String healthPath) {
    }
}
