package ru.pep.platform.api;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import ru.pep.platform.repository.AuditEventRepository;

@SpringBootTest(properties = {
        "pep.demo-data.enabled=true",
        "pep.auth.basic-enabled=true",
        "pep.auth.csrf-enabled=false"
})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class CorePlatformSecurityAndValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuditEventRepository auditEvents;

    @Test
    void protectsAuthenticatedApiAndLeavesPublicOverviewOpen() throws Exception {
        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

        mockMvc.perform(get("/api/overview"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/me")
                        .with(httpBasic("student1@pep.local", "student")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("student1@pep.local"))
                .andExpect(jsonPath("$.displayName").value("Студент 1"))
                .andExpect(jsonPath("$.role").value("STUDENT"));
    }

    @Test
    void enforcesRoleGuardsForPrivilegedActions() throws Exception {
        String moduleId = firstSecurityModuleId();

        mockMvc.perform(get("/api/modules/{moduleId}/grades/export", moduleId)
                        .with(httpBasic("student1@pep.local", "student")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        mockMvc.perform(post("/api/labs")
                        .with(httpBasic("curator@pep.local", "curator"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "submissionId": "00000000-0000-0000-0000-000000000001"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        mockMvc.perform(post("/api/lessons/{lessonId}/complete", "00000000-0000-0000-0000-000000000001")
                        .with(httpBasic("admin@pep.local", "admin")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void preventsStudentsFromReadingOrAttachingToOtherStudentsObjects() throws Exception {
        String moduleId = firstSecurityModuleId();
        String submissionId = createSubmission(moduleId);
        String validationJobId = firstValidationJobId("student1@pep.local", "student");

        mockMvc.perform(get("/api/validation-jobs/{jobId}", validationJobId)
                        .with(httpBasic("student2@pep.local", "student")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        MvcResult reportResult = mockMvc.perform(post("/api/reports")
                        .with(httpBasic("student1@pep.local", "student"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "moduleId": "%s",
                                  "submissionId": "%s",
                                  "type": "WHITE_BOX",
                                  "title": "White box evidence",
                                  "contentMarkdown": "Payload and vulnerable code"
                                }
                                """.formatted(moduleId, submissionId)))
                .andExpect(status().isCreated())
                .andReturn();

        String reportId = objectMapper.readTree(reportResult.getResponse().getContentAsString()).get("id").asText();
        MockMultipartFile evidence = new MockMultipartFile(
                "file",
                "evidence.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "secret evidence".getBytes());

        mockMvc.perform(multipart("/api/reports/{reportId}/attachments", reportId)
                        .file(evidence)
                        .with(httpBasic("student2@pep.local", "student")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        mockMvc.perform(multipart("/api/reports/{reportId}/attachments", reportId)
                        .file(evidence)
                        .with(httpBasic("curator@pep.local", "curator")))
                .andExpect(status().isForbidden());

        MvcResult uploadResult = mockMvc.perform(multipart("/api/reports/{reportId}/attachments", reportId)
                        .file(evidence)
                        .with(httpBasic("student1@pep.local", "student")))
                .andExpect(status().isCreated())
                .andReturn();
        String attachmentId = objectMapper.readTree(uploadResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/report-attachments/{attachmentId}", attachmentId)
                        .with(httpBasic("student2@pep.local", "student")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        mockMvc.perform(get("/api/report-attachments/{attachmentId}", attachmentId)
                        .with(httpBasic("student1@pep.local", "student")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("evidence.txt")))
                .andExpect(content().string("secret evidence"));
        mockMvc.perform(get("/api/report-attachments/{attachmentId}", attachmentId)
                        .with(httpBasic("curator@pep.local", "curator")))
                .andExpect(status().isOk())
                .andExpect(content().string("secret evidence"));

        assertAuditEventExists("REPORT_ATTACHMENT_DOWNLOADED");
    }

    @Test
    void rejectsUnsafeReportAttachmentTypes() throws Exception {
        String moduleId = firstSecurityModuleId();
        String submissionId = createSubmission(moduleId);
        MvcResult reportResult = mockMvc.perform(post("/api/reports")
                        .with(httpBasic("student1@pep.local", "student"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "moduleId": "%s",
                                  "submissionId": "%s",
                                  "type": "WHITE_BOX",
                                  "title": "White box evidence",
                                  "contentMarkdown": "Payload and vulnerable code"
                                }
                                """.formatted(moduleId, submissionId)))
                .andExpect(status().isCreated())
                .andReturn();

        String reportId = objectMapper.readTree(reportResult.getResponse().getContentAsString()).get("id").asText();
        MockMultipartFile executable = new MockMultipartFile(
                "file",
                "evidence.exe",
                "application/x-msdownload",
                "not a real executable".getBytes());

        mockMvc.perform(multipart("/api/reports/{reportId}/attachments", reportId)
                        .file(executable)
                        .with(httpBasic("student1@pep.local", "student")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void acceptsArchiveSubmissionAndRejectsUnsupportedArchiveType() throws Exception {
        String moduleId = firstSecurityModuleId();
        MockMultipartFile archive = new MockMultipartFile(
                "archive",
                "stand.zip",
                "application/zip",
                zipProject());

        mockMvc.perform(multipart("/api/submissions/archive")
                        .file(archive)
                        .param("moduleId", moduleId)
                        .param("applicationPort", "8080")
                        .param("healthPath", "health")
                        .param("composeService", "web")
                        .with(httpBasic("student1@pep.local", "student")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sourceType").value("ARCHIVE"))
                .andExpect(jsonPath("$.archiveFilename").value("stand.zip"))
                .andExpect(jsonPath("$.composeService").value("web"))
                .andExpect(jsonPath("$.healthPath").value("/health"))
                .andExpect(jsonPath("$.status").value("VALIDATION_QUEUED"));

        MockMultipartFile unsupported = new MockMultipartFile(
                "archive",
                "stand.exe",
                "application/octet-stream",
                "binary".getBytes());
        mockMvc.perform(multipart("/api/submissions/archive")
                        .file(unsupported)
                        .param("moduleId", moduleId)
                        .param("applicationPort", "8080")
                        .with(httpBasic("student1@pep.local", "student")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsInvalidPayloadsAndMismatchedReportScopes() throws Exception {
        String injectionModuleId = firstSecurityModuleId();
        String accessControlModuleId = securityModuleIdAt(8);
        String submissionId = createSubmission(injectionModuleId);

        mockMvc.perform(post("/api/submissions")
                        .with(httpBasic("student1@pep.local", "student"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "moduleId": "%s",
                                  "imageReference": "localhost:5001/invalid-port:latest",
                                  "applicationPort": 70000,
                                  "healthPath": "/health"
                                }
                                """.formatted(injectionModuleId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        mockMvc.perform(post("/api/reports")
                        .with(httpBasic("student1@pep.local", "student"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "moduleId": "%s",
                                  "submissionId": "%s",
                                  "type": "WHITE_BOX",
                                  "title": "Wrong module report",
                                  "contentMarkdown": "This report points to a submission from another module."
                                }
                                """.formatted(accessControlModuleId, submissionId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        mockMvc.perform(post("/api/reports")
                        .with(httpBasic("student1@pep.local", "student"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "moduleId": "%s",
                                  "type": "BLACK_BOX",
                                  "title": "Missing assignment",
                                  "contentMarkdown": "Black box report without assignment."
                                }
                                """.formatted(injectionModuleId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsOversizedReportAndReviewMarkdown() throws Exception {
        String moduleId = firstSecurityModuleId();
        String submissionId = createSubmission(moduleId);

        mockMvc.perform(post("/api/reports")
                        .with(httpBasic("student1@pep.local", "student"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "moduleId": "%s",
                                  "submissionId": "%s",
                                  "type": "WHITE_BOX",
                                  "title": "Oversized report",
                                  "contentMarkdown": "%s"
                                }
                                """.formatted(moduleId, submissionId, "A".repeat(CoreDtos.REPORT_MARKDOWN_MAX_LENGTH + 1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        MvcResult reportResult = mockMvc.perform(post("/api/reports")
                        .with(httpBasic("student1@pep.local", "student"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "moduleId": "%s",
                                  "submissionId": "%s",
                                  "type": "WHITE_BOX",
                                  "title": "Valid report",
                                  "contentMarkdown": "Payload and vulnerable code"
                                }
                                """.formatted(moduleId, submissionId)))
                .andExpect(status().isCreated())
                .andReturn();

        String reportId = objectMapper.readTree(reportResult.getResponse().getContentAsString()).get("id").asText();
        mockMvc.perform(post("/api/reviews")
                        .with(httpBasic("curator@pep.local", "curator"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reportId": "%s",
                                  "decision": "NEEDS_REVISION",
                                  "score": 40,
                                  "commentMarkdown": "%s"
                                }
                                """.formatted(reportId, "B".repeat(CoreDtos.REVIEW_COMMENT_MAX_LENGTH + 1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void allowsManualValidationOnlyWhileJobIsQueued() throws Exception {
        String moduleId = firstSecurityModuleId();
        createSubmission(moduleId);
        String validationJobId = firstValidationJobId("curator@pep.local", "curator");

        mockMvc.perform(post("/api/validation-jobs/{jobId}/complete", validationJobId)
                        .with(httpBasic("student1@pep.local", "student"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "passed": true,
                                  "logsUri": "memory://validation/student-forbidden.log"
                                }
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/validation-jobs/{jobId}/complete", validationJobId)
                        .with(httpBasic("curator@pep.local", "curator"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "passed": true,
                                  "logsUri": "memory://validation/success.log"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PASSED"));

        mockMvc.perform(post("/api/validation-jobs/{jobId}/complete", validationJobId)
                        .with(httpBasic("curator@pep.local", "curator"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "passed": false,
                                  "errorMessage": "repeat completion must not flip status"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        mockMvc.perform(get("/api/validation-jobs/{jobId}", validationJobId)
                        .with(httpBasic("curator@pep.local", "curator")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PASSED"));
    }

    private String createSubmission(String moduleId) throws Exception {
        MvcResult submissionResult = mockMvc.perform(post("/api/submissions")
                        .with(httpBasic("student1@pep.local", "student"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "moduleId": "%s",
                                  "imageReference": "localhost:5001/vulnerable-sqli-demo:latest",
                                  "applicationPort": 8080,
                                  "healthPath": "health"
                                }
                                """.formatted(moduleId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.healthPath").value("/health"))
                .andReturn();
        return objectMapper.readTree(submissionResult.getResponse().getContentAsString()).get("id").asText();
    }

    private String firstValidationJobId(String email, String password) throws Exception {
        MvcResult validationJobsResult = mockMvc.perform(get("/api/validation-jobs")
                        .with(httpBasic(email, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andReturn();
        return objectMapper.readTree(validationJobsResult.getResponse().getContentAsString()).get(0).get("id").asText();
    }

    private String firstSecurityModuleId() throws Exception {
        return securityModuleIdAt(0);
    }

    private String securityModuleIdAt(int index) throws Exception {
        MvcResult coursesResult = mockMvc.perform(get("/api/courses")
                        .with(httpBasic("student1@pep.local", "student")))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode courses = objectMapper.readTree(coursesResult.getResponse().getContentAsString());
        JsonNode largestCourse = null;
        for (JsonNode course : courses) {
            if (largestCourse == null || course.get("modules").size() > largestCourse.get("modules").size()) {
                largestCourse = course;
            }
        }
        if (largestCourse == null || largestCourse.get("modules").size() <= index) {
            throw new AssertionError("Security module not found at index: " + index);
        }
        return largestCourse.get("modules").get(index).get("id").asText();
    }

    private void assertAuditEventExists(String action) {
        boolean exists = auditEvents.findTop100ByOrderByCreatedAtDesc().stream()
                .anyMatch(event -> action.equals(event.getAction()));
        if (!exists) {
            throw new AssertionError("Audit event not found: " + action);
        }
    }

    private byte[] zipProject() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            zip.putNextEntry(new ZipEntry("Dockerfile"));
            zip.write("""
                    FROM nginx:alpine
                    COPY index.html /usr/share/nginx/html/index.html
                    EXPOSE 8080
                    """.getBytes());
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("index.html"));
            zip.write("ok".getBytes());
            zip.closeEntry();
        }
        return output.toByteArray();
    }
}
