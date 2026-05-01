package ru.pep.platform.api;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import ru.pep.platform.repository.AuditEventRepository;
import ru.pep.platform.repository.AuthLoginThrottleRepository;
import ru.pep.platform.repository.AuthSessionRepository;
import ru.pep.platform.service.AuthSessionCleanupService;
import ru.pep.platform.service.AuthSessionService;

@SpringBootTest(properties = {
        "pep.demo-data.enabled=true",
        "pep.auth.cleanup.revoked-retention-hours=0",
        "pep.auth.cleanup.throttle-retention-hours=0"
})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class SessionAuthenticationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthLoginThrottleRepository throttles;

    @Autowired
    private AuditEventRepository auditEvents;

    @Autowired
    private AuthSessionRepository sessions;

    @Autowired
    private AuthSessionCleanupService sessionCleanup;

    @BeforeEach
    void clearLoginThrottles() {
        throttles.deleteAll();
    }

    @Test
    void authenticatesWithHttpOnlySessionCookieAndRejectsBasicByDefault() throws Exception {
        mockMvc.perform(get("/api/me")
                        .with(httpBasic("student1@pep.local", "student")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "student1@pep.local",
                                  "password": "student"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Security-Policy", containsString("default-src 'none'")))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(header().string("Permissions-Policy", containsString("camera=()")))
                .andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"));

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "student1@pep.local",
                                  "password": "student"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("student1@pep.local"))
                .andExpect(jsonPath("$.displayName").value("Студент 1"))
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(cookie().httpOnly(AuthSessionService.SESSION_COOKIE, true))
                .andReturn();

        Cookie sessionCookie = loginResult.getResponse().getCookie(AuthSessionService.SESSION_COOKIE);

        mockMvc.perform(get("/api/me").cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("STUDENT"));

        mockMvc.perform(post("/api/auth/logout")
                        .with(csrf())
                        .cookie(sessionCookie))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/me").cookie(sessionCookie))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

        assertTrue(auditEvents.findTop100ByOrderByCreatedAtDesc().stream()
                .anyMatch(event -> "AUTH_LOGIN_SUCCESS".equals(event.getAction())));
        assertTrue(auditEvents.findTop100ByOrderByCreatedAtDesc().stream()
                .anyMatch(event -> "AUTH_LOGOUT".equals(event.getAction())));
    }

    @Test
    void rejectsInvalidLoginPayloadBeforeAuthentication() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "not-an-email",
                                  "password": "student"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "student1@pep.local",
                                  "password": "%s"
                                }
                                """.formatted("A".repeat(CoreDtos.LOGIN_PASSWORD_MAX_LENGTH + 1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void throttlesRepeatedFailedLoginAttempts() throws Exception {
        String invalidLogin = """
                {
                  "email": "student1@pep.local",
                  "password": "wrong-password"
                }
                """;

        for (int attempt = 0; attempt < 5; attempt++) {
            mockMvc.perform(post("/api/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidLogin))
                    .andExpect(status().isForbidden());
        }

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "student1@pep.local",
                                  "password": "student"
                                }
                                """))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("AUTH_RATE_LIMITED"));

        assertTrue(auditEvents.findTop100ByOrderByCreatedAtDesc().stream()
                .anyMatch(event -> "AUTH_LOGIN_FAILED".equals(event.getAction())
                        && event.getMetadataJson().contains("\"locked\":true")));
        assertTrue(auditEvents.findTop100ByOrderByCreatedAtDesc().stream()
                .anyMatch(event -> "AUTH_LOGIN_RATE_LIMITED".equals(event.getAction())));
    }

    @Test
    void cleansInactiveLoginThrottles() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "student1@pep.local",
                                  "password": "wrong-password"
                                }
                                """))
                .andExpect(status().isForbidden());

        assertTrue(throttles.count() > 0);
        int deleted = sessionCleanup.cleanupInactiveLoginThrottles();

        assertTrue(deleted >= 1);
        assertTrue(throttles.count() == 0);
    }

    @Test
    void cleansRevokedSessionsWithoutDeletingActiveSessions() throws Exception {
        Cookie firstSession = login("student1@pep.local", "student");
        Cookie secondSession = login("student2@pep.local", "student");

        mockMvc.perform(post("/api/auth/logout")
                        .with(csrf())
                        .cookie(firstSession))
                .andExpect(status().isNoContent());

        int deleted = sessionCleanup.cleanupInactiveSessions();

        assertTrue(deleted >= 1);
        mockMvc.perform(get("/api/me").cookie(firstSession))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
        mockMvc.perform(get("/api/me").cookie(secondSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("student2@pep.local"));
        assertTrue(sessions.count() >= 1);
    }

    private Cookie login(String email, String password) throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        return loginResult.getResponse().getCookie(AuthSessionService.SESSION_COOKIE);
    }
}
