package ru.pep.platform.service.internal;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.function.IntPredicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ArchiveBuildAndProbeHelper {

    private static final Logger log = LoggerFactory.getLogger(ArchiveBuildAndProbeHelper.class);

    private static final List<String> FORBIDDEN_FRAGMENTS = List.of(
            "privileged: true",
            "network_mode: host",
            "pid: host",
            "/var/run/docker.sock",
            "host.docker.internal");

    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    public Path createWorkingDirectory(String prefix) throws IOException {
        return Files.createTempDirectory(prefix);
    }

    public void extractArchive(Path archivePath, Path destination) throws IOException {
        String filename = archivePath.getFileName().toString().toLowerCase(Locale.ROOT);
        if (filename.endsWith(".zip")) {
            extractZip(archivePath, destination);
            return;
        }
        if (filename.endsWith(".tar") || filename.endsWith(".tar.gz") || filename.endsWith(".tgz")) {
            runCommand(List.of("tar", "-xf", archivePath.toString(), "-C", destination.toString()), null, Duration.ofMinutes(2));
            return;
        }
        throw new IllegalStateException("Неподдерживаемый формат архива (поддерживаются zip / tar / tar.gz)");
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

    public Path locateBuildContext(Path workDir) throws IOException {
        if (Files.exists(workDir.resolve("Dockerfile"))
                || Files.exists(workDir.resolve("docker-compose.yml"))
                || Files.exists(workDir.resolve("compose.yaml"))) {
            return workDir;
        }
        try (var children = Files.list(workDir)) {
            List<Path> dirs = children.filter(Files::isDirectory).toList();
            if (dirs.size() == 1) {
                Path nested = dirs.get(0);
                if (Files.exists(nested.resolve("Dockerfile"))
                        || Files.exists(nested.resolve("docker-compose.yml"))
                        || Files.exists(nested.resolve("compose.yaml"))) {
                    return nested;
                }
            }
        }
        return workDir;
    }

    public void validateBuildContext(Path workDir) throws IOException {
        try (var paths = Files.walk(workDir)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> {
                        String name = path.getFileName().toString();
                        return name.equals("Dockerfile")
                                || name.equals("docker-compose.yml")
                                || name.equals("compose.yaml");
                    })
                    .forEach(path -> {
                        try {
                            String content = Files.readString(path).toLowerCase(Locale.ROOT);
                            FORBIDDEN_FRAGMENTS.forEach(fragment -> {
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

    public BuildKind detectBuildKind(Path workDir) {
        if (Files.exists(workDir.resolve("docker-compose.yml")) || Files.exists(workDir.resolve("compose.yaml"))) {
            return BuildKind.COMPOSE;
        }
        if (Files.exists(workDir.resolve("Dockerfile"))) {
            return BuildKind.DOCKERFILE;
        }
        return BuildKind.NONE;
    }

    public CommandResult runCommand(List<String> command, Path workingDirectory, Duration timeout) {
        ProcessBuilder builder = new ProcessBuilder(command);
        if (workingDirectory != null) {
            builder.directory(workingDirectory.toFile());
        }
        builder.redirectErrorStream(true);
        try {
            Process process = builder.start();
            boolean finished = process.waitFor(timeout.toSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IllegalStateException("Command timeout: " + summarize(command));
            }
            String output = new String(process.getInputStream().readAllBytes());
            if (process.exitValue() != 0) {
                throw new CommandFailedException(summarize(command), output);
            }
            return new CommandResult(output);
        } catch (IOException exception) {
            throw new IllegalStateException("Не удалось выполнить команду '" + command.get(0) + "': " + exception.getMessage());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Команда прервана: " + summarize(command));
        }
    }

    public CommandResult runCompose(Path workDir, List<String> args, Duration timeout) {
        String composeFile = Files.exists(workDir.resolve("docker-compose.yml")) ? "docker-compose.yml" : "compose.yaml";
        List<String> docker = new ArrayList<>(List.of("docker", "compose", "-f", composeFile));
        docker.addAll(args);
        try {
            return runCommand(docker, workDir, timeout);
        } catch (IllegalStateException firstFailure) {
            List<String> legacy = new ArrayList<>(List.of("docker-compose", "-f", composeFile));
            legacy.addAll(args);
            return runCommand(legacy, workDir, timeout);
        }
    }

    public String resolveComposeService(Path workDir, String requestedService, Duration timeout) {
        if (requestedService != null && !requestedService.isBlank()) {
            return requestedService.trim();
        }
        return runCompose(workDir, List.of("config", "--services"), timeout)
                .output()
                .lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("docker-compose не содержит services"));
    }

    public List<String> listComposeServices(Path workDir, Duration timeout) {
        return runCompose(workDir, List.of("config", "--services"), timeout)
                .output()
                .lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toList();
    }

    /**
     * Возвращает список объявленных в compose-файле портов вида container/protocol для service.
     * Не запускает контейнер, читает только статически из compose config.
     */
    public List<Integer> declaredComposePorts(Path workDir, String service, Duration timeout) {
        try {
            String json = runCompose(workDir, List.of("config", "--format", "json"), timeout).output();
            return parseComposePortsFromJson(json, service);
        } catch (RuntimeException exception) {
            log.debug("Failed to read compose config json for service {}: {}", service, exception.getMessage());
            return List.of();
        }
    }

    private static List<Integer> parseComposePortsFromJson(String json, String service) {
        if (json == null) {
            return List.of();
        }
        String token = "\"" + service + "\"";
        int serviceIdx = json.indexOf(token);
        if (serviceIdx < 0) {
            return List.of();
        }
        int portsIdx = json.indexOf("\"ports\"", serviceIdx);
        int nextServiceIdx = json.indexOf("\"image\"", serviceIdx + token.length());
        if (portsIdx < 0 || (nextServiceIdx > 0 && portsIdx > nextServiceIdx + 200)) {
            return List.of();
        }
        int arrayStart = json.indexOf('[', portsIdx);
        int arrayEnd = json.indexOf(']', arrayStart);
        if (arrayStart < 0 || arrayEnd < 0) {
            return List.of();
        }
        String portsArray = json.substring(arrayStart, arrayEnd + 1);
        List<Integer> result = new ArrayList<>();
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("\"target\"\\s*:\\s*(\\d+)")
                .matcher(portsArray);
        while (matcher.find()) {
            try {
                result.add(Integer.parseInt(matcher.group(1)));
            } catch (NumberFormatException ignored) {
            }
        }
        return result;
    }

    /**
     * Парсит EXPOSE директивы Dockerfile, возвращает первый найденный порт или Optional.empty().
     */
    public java.util.Optional<Integer> detectDockerfileExpose(Path buildContext) {
        Path dockerfile = buildContext.resolve("Dockerfile");
        if (!Files.isRegularFile(dockerfile)) {
            return java.util.Optional.empty();
        }
        try {
            for (String raw : Files.readAllLines(dockerfile)) {
                String line = raw.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                if (line.regionMatches(true, 0, "EXPOSE", 0, 6)) {
                    String[] parts = line.substring(6).trim().split("\\s+");
                    for (String part : parts) {
                        String numeric = part.split("/")[0];
                        try {
                            int port = Integer.parseInt(numeric);
                            if (port > 0 && port <= 65_535) {
                                return java.util.Optional.of(port);
                            }
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
            }
        } catch (IOException exception) {
            log.debug("Failed to read Dockerfile for EXPOSE detection: {}", exception.getMessage());
        }
        return java.util.Optional.empty();
    }

    /**
     * Возвращает размер образа в байтах через `docker image inspect`.
     */
    public long imageSizeBytes(String image, Duration timeout) {
        CommandResult result = runCommand(
                List.of("docker", "image", "inspect", "--format", "{{.Size}}", image),
                null,
                timeout);
        String trimmed = result.output().trim();
        try {
            return Long.parseLong(trimmed);
        } catch (NumberFormatException exception) {
            return -1L;
        }
    }

    public String publishedHostPort(String containerName, Integer applicationPort, Duration timeout) {
        CommandResult result = runCommand(
                List.of("docker", "port", containerName, applicationPort + "/tcp"),
                null,
                timeout);
        String portLine = result.output().lines().findFirst()
                .orElseThrow(() -> new IllegalStateException("Docker не вернул published port"));
        int separator = portLine.lastIndexOf(':');
        if (separator < 0 || separator == portLine.length() - 1) {
            throw new IllegalStateException("Некорректный published port: " + portLine);
        }
        return portLine.substring(separator + 1).trim();
    }

    public void probeHttp(String url, IntPredicate accept, int attempts, Duration interval) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        Instant deadline = Instant.now().plusSeconds(attempts * Math.max(1L, interval.toSeconds()) + 30L);
        RuntimeException lastFailure = null;
        for (int i = 0; i < attempts && Instant.now().isBefore(deadline); i++) {
            try {
                HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
                if (accept.test(response.statusCode())) {
                    return;
                }
                lastFailure = new IllegalStateException("Endpoint " + url + " вернул HTTP " + response.statusCode() + " (ожидался 200)");
            } catch (IOException exception) {
                lastFailure = new IllegalStateException("Endpoint недоступен: " + url + " (" + exception.getMessage() + ")");
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Health check interrupted");
            }
            try {
                Thread.sleep(interval.toMillis());
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Health check interrupted");
            }
        }
        throw lastFailure == null
                ? new IllegalStateException("Endpoint недоступен: " + url)
                : lastFailure;
    }

    public void cleanupContainer(String containerName) {
        try {
            ProcessBuilder builder = new ProcessBuilder("docker", "rm", "-f", containerName);
            builder.redirectErrorStream(true);
            Process process = builder.start();
            process.waitFor(30, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } catch (IOException ignored) {
            log.debug("Failed to remove container {}", containerName);
        }
    }

    public void deleteDirectory(Path directory) {
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

    private String summarize(List<String> command) {
        String joined = String.join(" ", command);
        return joined.length() > 200 ? joined.substring(0, 200) + "..." : joined;
    }

    public enum BuildKind {
        DOCKERFILE,
        COMPOSE,
        NONE
    }

    public record CommandResult(String output) {
    }

    public static final class CommandFailedException extends RuntimeException {
        private final String output;

        public CommandFailedException(String command, String output) {
            super("Команда '" + command + "' завершилась с ошибкой: " + truncate(output));
            this.output = output == null ? "" : output;
        }

        public String getOutput() {
            return output;
        }

        private static String truncate(String value) {
            if (value == null) {
                return "(нет вывода)";
            }
            String trimmed = value.strip();
            if (trimmed.isEmpty()) {
                return "(нет вывода)";
            }
            return trimmed.length() > 600 ? trimmed.substring(trimmed.length() - 600) : trimmed;
        }
    }
}
