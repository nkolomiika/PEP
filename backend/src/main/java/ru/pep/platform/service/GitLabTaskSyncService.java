package ru.pep.platform.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.pep.platform.api.CoreDtos;
import ru.pep.platform.domain.PentestTask;
import ru.pep.platform.repository.PentestTaskRepository;

@Service
public class GitLabTaskSyncService {

    private final PentestTaskRepository tasks;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String baseUrl;
    private final String token;
    private final String group;
    private final String defaultBranch;

    public GitLabTaskSyncService(
            PentestTaskRepository tasks,
            ObjectMapper objectMapper,
            @Value("${pep.gitlab.base-url}") String baseUrl,
            @Value("${pep.gitlab.token}") String token,
            @Value("${pep.gitlab.group}") String group,
            @Value("${pep.gitlab.default-branch}") String defaultBranch) {
        this.tasks = tasks;
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.token = token;
        this.group = group;
        this.defaultBranch = defaultBranch;
        this.httpClient = HttpClient.newHttpClient();
    }

    @Transactional
    public CoreDtos.GitLabSyncResponse sync() {
        if (token == null || token.isBlank()) {
            throw new CorePlatformService.ValidationException("Не задан PEP_GITLAB_TOKEN");
        }
        JsonNode projects = getJson("/api/v4/groups/" + encode(group) + "/projects?include_subgroups=true&per_page=100");
        int scanned = 0;
        int synced = 0;
        for (JsonNode project : projects) {
            scanned++;
            Long projectId = project.get("id").asLong();
            String manifest = getText("/api/v4/projects/" + projectId + "/repository/files/" + encode("pep-task.yml") + "/raw?ref=" + encode(defaultBranch));
            if (manifest == null || manifest.isBlank()) {
                continue;
            }
            TaskManifest parsed = parseManifest(manifest);
            String repositoryUrl = project.path("http_url_to_repo").asText(project.path("web_url").asText());
            String projectPath = project.path("path_with_namespace").asText();
            String commitSha = project.path("last_activity_at").asText();
            String hash = sha256(manifest);
            PentestTask task = tasks.findByGitlabProjectId(projectId)
                    .orElseGet(() -> new PentestTask(
                            projectId,
                            projectPath,
                            parsed.title(),
                            parsed.slug(),
                            parsed.category(),
                            parsed.difficulty(),
                            parsed.durationMinutes(),
                            parsed.entrypointPort(),
                            parsed.healthPath(),
                            parsed.composeService(),
                            parsed.descriptionMarkdown(),
                            repositoryUrl,
                            defaultBranch,
                            commitSha,
                            hash));
            task.updateFromManifest(
                    parsed.title(),
                    parsed.slug(),
                    parsed.category(),
                    parsed.difficulty(),
                    parsed.durationMinutes(),
                    parsed.entrypointPort(),
                    parsed.healthPath(),
                    parsed.composeService(),
                    parsed.descriptionMarkdown(),
                    repositoryUrl,
                    defaultBranch,
                    commitSha,
                    hash);
            tasks.save(task);
            synced++;
        }
        return new CoreDtos.GitLabSyncResponse(scanned, synced);
    }

    private JsonNode getJson(String path) {
        try {
            return objectMapper.readTree(getText(path));
        } catch (IOException exception) {
            throw new CorePlatformService.ValidationException("GitLab вернул некорректный JSON");
        }
    }

    private String getText(String path) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + path))
                    .GET();
            if (token != null && !token.isBlank()) {
                builder.header("PRIVATE-TOKEN", token);
            }
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 404) {
                return "";
            }
            if (response.statusCode() >= 300) {
                throw new CorePlatformService.ValidationException("GitLab API вернул HTTP " + response.statusCode());
            }
            return response.body();
        } catch (IOException exception) {
            throw new CorePlatformService.ValidationException("GitLab недоступен");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new CorePlatformService.ValidationException("GitLab sync прерван");
        }
    }

    private TaskManifest parseManifest(String manifest) {
        Map<String, String> values = new LinkedHashMap<>();
        String currentMultilineKey = null;
        StringBuilder multiline = new StringBuilder();
        for (String rawLine : manifest.split("\\R")) {
            if (currentMultilineKey != null) {
                if (rawLine.startsWith("  ")) {
                    multiline.append(rawLine.substring(2)).append('\n');
                    continue;
                }
                values.put(currentMultilineKey, multiline.toString().strip());
                currentMultilineKey = null;
                multiline = new StringBuilder();
            }
            if (rawLine.isBlank() || rawLine.trim().startsWith("#")) {
                continue;
            }
            int separator = rawLine.indexOf(':');
            if (separator < 0) {
                continue;
            }
            String key = rawLine.substring(0, separator).trim();
            String value = rawLine.substring(separator + 1).trim();
            if ("|".equals(value)) {
                currentMultilineKey = key;
            } else {
                values.put(key, stripQuotes(value));
            }
        }
        if (currentMultilineKey != null) {
            values.put(currentMultilineKey, multiline.toString().strip());
        }
        return new TaskManifest(
                required(values, "title"),
                required(values, "slug"),
                required(values, "category"),
                values.getOrDefault("difficulty", "beginner"),
                Integer.parseInt(values.getOrDefault("durationMinutes", "240")),
                Integer.parseInt(values.getOrDefault("entrypointPort", "8080")),
                values.getOrDefault("healthPath", "/health"),
                values.get("composeService"),
                values.getOrDefault("descriptionMarkdown", ""));
    }

    private String required(Map<String, String> values, String key) {
        String value = values.get(key);
        if (value == null || value.isBlank()) {
            throw new CorePlatformService.ValidationException("В pep-task.yml не задано поле " + key);
        }
        return value;
    }

    private String stripQuotes(String value) {
        if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte item : digest) {
                result.append(String.format("%02x", item));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 недоступен", exception);
        }
    }

    private record TaskManifest(
            String title,
            String slug,
            String category,
            String difficulty,
            Integer durationMinutes,
            Integer entrypointPort,
            String healthPath,
            String composeService,
            String descriptionMarkdown) {
    }
}
