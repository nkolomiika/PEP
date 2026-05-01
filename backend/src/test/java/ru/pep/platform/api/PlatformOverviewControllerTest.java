package ru.pep.platform.api;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import ru.pep.platform.config.SessionAuthenticationFilter;
import ru.pep.platform.config.SecurityConfig;
import ru.pep.platform.repository.AppUserRepository;
import ru.pep.platform.service.AuthSessionService;

@WebMvcTest(PlatformOverviewController.class)
@Import({SecurityConfig.class, SessionAuthenticationFilter.class})
class PlatformOverviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AppUserRepository users;

    @MockBean
    private AuthSessionService authSessionService;

    @Test
    void returnsPlatformRoles() throws Exception {
        mockMvc.perform(get("/api/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles", hasItem("СТУДЕНТ")))
                .andExpect(jsonPath("$.learningFlow", hasItem("Black box тестирование чужих работ")));
    }
}
