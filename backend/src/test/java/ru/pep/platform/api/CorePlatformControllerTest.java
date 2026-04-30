package ru.pep.platform.api;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class CorePlatformControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void supportsStudentSubmissionAndCuratorReviewFlow() throws Exception {
        MvcResult coursesResult = mockMvc.perform(get("/api/courses")
                        .with(httpBasic("student1@pep.local", "student")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andReturn();

        JsonNode courses = objectMapper.readTree(coursesResult.getResponse().getContentAsString());
        String moduleId = findModuleId(courses, "A03. Injection");

        MvcResult lessonsResult = mockMvc.perform(get("/api/modules/{moduleId}/lessons", moduleId)
                        .with(httpBasic("student1@pep.local", "student")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andReturn();

        String lessonId = objectMapper.readTree(lessonsResult.getResponse().getContentAsString()).get(0).get("id").asText();

        mockMvc.perform(get("/api/lessons/{lessonId}", lessonId)
                        .with(httpBasic("student1@pep.local", "student")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Docker image для сдачи на платформе"));

        mockMvc.perform(post("/api/lessons/{lessonId}/complete", lessonId)
                        .with(httpBasic("student1@pep.local", "student")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lessonId").value(lessonId))
                .andExpect(jsonPath("$.completed").value(true));

        mockMvc.perform(get("/api/modules/{moduleId}/lesson-progress", moduleId)
                        .with(httpBasic("student1@pep.local", "student")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].lessonId").value(lessonId));

        MvcResult submissionResult = mockMvc.perform(post("/api/submissions")
                        .with(httpBasic("student1@pep.local", "student"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "moduleId": "%s",
                                  "imageReference": "localhost:5001/vulnerable-sqli-demo:latest",
                                  "applicationPort": 8080,
                                  "healthPath": "/health"
                                }
                                """.formatted(moduleId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("VALIDATION_QUEUED"))
                .andReturn();

        String submissionId = objectMapper.readTree(submissionResult.getResponse().getContentAsString()).get("id").asText();

        MvcResult submissionsResult = mockMvc.perform(get("/api/submissions")
                        .with(httpBasic("curator@pep.local", "curator")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andReturn();

        objectMapper.readTree(submissionsResult.getResponse().getContentAsString());

        MvcResult validationJobsResult = mockMvc.perform(get("/api/validation-jobs")
                        .with(httpBasic("curator@pep.local", "curator")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andReturn();

        String validationJobId = objectMapper.readTree(validationJobsResult.getResponse().getContentAsString())
                .get(0)
                .get("id")
                .asText();

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

        mockMvc.perform(get("/api/submissions")
                        .with(httpBasic("student2@pep.local", "student")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        MvcResult reportResult = mockMvc.perform(post("/api/reports")
                        .with(httpBasic("student1@pep.local", "student"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "moduleId": "%s",
                                  "submissionId": "%s",
                                  "type": "WHITE_BOX",
                                  "title": "SQL Injection отчет",
                                  "contentMarkdown": "Payload: ' OR '1'='1"
                                }
                                """.formatted(moduleId, submissionId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andReturn();

        String reportId = objectMapper.readTree(reportResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/api/reviews")
                        .with(httpBasic("curator@pep.local", "curator"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reportId": "%s",
                                  "decision": "APPROVED",
                                  "score": 90,
                                  "commentMarkdown": "Уязвимость воспроизводится."
                                }
                                """.formatted(reportId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.decision").value("APPROVED"));

        mockMvc.perform(get("/api/reviews")
                        .with(httpBasic("student1@pep.local", "student")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].reportId").value(reportId))
                .andExpect(jsonPath("$[0].score").value(90))
                .andExpect(jsonPath("$[0].commentMarkdown").value("Уязвимость воспроизводится."));

        mockMvc.perform(get("/api/reviews")
                        .with(httpBasic("student2@pep.local", "student")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(get("/api/modules/{moduleId}/result", moduleId)
                        .with(httpBasic("student1@pep.local", "student")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dockerPassed").value(true))
                .andExpect(jsonPath("$.whiteBoxScore").value(90))
                .andExpect(jsonPath("$.blackBoxScore").value(nullValue()))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        MvcResult labResult = mockMvc.perform(post("/api/labs")
                        .with(httpBasic("admin@pep.local", "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "submissionId": "%s"
                                }
                                """.formatted(submissionId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RUNNING"))
                .andReturn();

        String labId = objectMapper.readTree(labResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/api/modules/{moduleId}/black-box-assignments/distribute", moduleId)
                        .with(httpBasic("admin@pep.local", "admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdAssignments").value(1));

        mockMvc.perform(post("/api/modules/{moduleId}/black-box-assignments/distribute", moduleId)
                        .with(httpBasic("admin@pep.local", "admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdAssignments").value(0));

        MvcResult assignmentResult = mockMvc.perform(get("/api/black-box-assignments/my")
                        .with(httpBasic("student2@pep.local", "student")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].targetLabId").value(labId))
                .andReturn();

        String assignmentId = objectMapper.readTree(assignmentResult.getResponse().getContentAsString()).get(0).get("id").asText();

        MvcResult blackBoxReportResult = mockMvc.perform(post("/api/reports")
                        .with(httpBasic("student2@pep.local", "student"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "moduleId": "%s",
                                  "blackBoxAssignmentId": "%s",
                                  "type": "BLACK_BOX",
                                  "title": "Black box отчет по SQL Injection",
                                  "contentMarkdown": "Payload найден через форму поиска."
                                }
                                """.formatted(moduleId, assignmentId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.blackBoxAssignmentId").value(assignmentId))
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andReturn();

        String blackBoxReportId = objectMapper.readTree(blackBoxReportResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/api/reviews")
                        .with(httpBasic("curator@pep.local", "curator"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reportId": "%s",
                                  "decision": "APPROVED",
                                  "score": 80,
                                  "commentMarkdown": "Black box finding воспроизводится."
                                }
                                """.formatted(blackBoxReportId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.decision").value("APPROVED"));

        mockMvc.perform(get("/api/modules/{moduleId}/result", moduleId)
                        .with(httpBasic("student2@pep.local", "student")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dockerPassed").value(false))
                .andExpect(jsonPath("$.whiteBoxScore").value(nullValue()))
                .andExpect(jsonPath("$.blackBoxScore").value(80))
                .andExpect(jsonPath("$.finalScore").value(nullValue()))
                .andExpect(jsonPath("$.status").value("DOCKER_REQUIRED"));

        mockMvc.perform(get("/api/audit")
                        .with(httpBasic("admin@pep.local", "admin")))
                .andExpect(status().isOk());
    }

    private String findModuleId(JsonNode courses, String moduleTitle) {
        for (JsonNode course : courses) {
            for (JsonNode module : course.get("modules")) {
                if (moduleTitle.equals(module.get("title").asText())) {
                    return module.get("id").asText();
                }
            }
        }
        throw new AssertionError("Module not found: " + moduleTitle);
    }
}
