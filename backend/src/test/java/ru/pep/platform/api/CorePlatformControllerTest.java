package ru.pep.platform.api;

import static org.hamcrest.Matchers.hasSize;
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
                .andExpect(jsonPath("$", hasSize(1)))
                .andReturn();

        JsonNode courses = objectMapper.readTree(coursesResult.getResponse().getContentAsString());
        String moduleId = courses.get(0).get("modules").get(0).get("id").asText();

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

        mockMvc.perform(get("/api/audit")
                        .with(httpBasic("admin@pep.local", "admin")))
                .andExpect(status().isOk());
    }
}
