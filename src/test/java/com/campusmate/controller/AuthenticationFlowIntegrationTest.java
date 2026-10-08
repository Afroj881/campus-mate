package com.campusmate.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campusmate.model.User;
import com.campusmate.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class AuthenticationFlowIntegrationTest {

    @Autowired private WebApplicationContext webApplicationContext;
    @Autowired private FilterChainProxy springSecurityFilterChain;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EntityManager entityManager;

    @Value("${campusmate.admin.email}")
    private String configuredAdminEmail;

    @Value("${campusmate.admin.password}")
    private String configuredAdminPassword;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilters(springSecurityFilterChain)
                .build();
    }

    @Test
    @Transactional
    void studentRegistrationAndCredentialsWorkAndConfiguredAdminCanLogIn() throws Exception {
        MockHttpSession studentSession = new MockHttpSession();
        String csrf = loginCsrf(studentSession);
        String studentEmail = "auth-audit-" + UUID.randomUUID() + "@example.test";
        String studentPassword = "Student-password-" + UUID.randomUUID();

        mockMvc.perform(post("/register")
                        .session(studentSession)
                        .param("_csrf", csrf)
                        .param("name", "Authentication Audit Student")
                        .param("email", studentEmail)
                        .param("password", studentPassword)
                        .param("department", "Testing")
                        .param("semester", "2")
                        .param("section", "A"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        User registeredStudent = userRepository.findByEmailIgnoreCase(studentEmail).orElseThrow();
        assertEquals(User.Role.STUDENT, registeredStudent.getRole());
        assertEquals("A", registeredStudent.getSection());
        assertTrue(passwordEncoder.matches(studentPassword, registeredStudent.getPassword()));

        MvcResult invalidStudentLogin = mockMvc.perform(post("/login")
                        .session(studentSession)
                        .param("_csrf", csrf)
                        .param("username", studentEmail)
                        .param("password", "incorrect-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"))
                .andReturn();

        MvcResult studentLogin = mockMvc.perform(post("/login")
                        .session(studentSession)
                        .param("_csrf", csrf)
                        .param("username", studentEmail)
                        .param("password", studentPassword))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"))
                .andReturn();

        MockHttpSession authenticatedStudentSession = (MockHttpSession) studentLogin.getRequest().getSession(false);
        assertNotNull(authenticatedStudentSession);
        assertFalse(invalidStudentLogin.getResponse().getContentAsString().contains(studentPassword));

        for (String route : List.of(
                "/", "/dashboard", "/notices", "/events", "/timetable", "/planner",
                "/clubs", "/resources", "/profile", "/search")) {
            mockMvc.perform(get(route).session(authenticatedStudentSession))
                    .andExpect(status().isOk());
        }

        String logoutCsrf = csrfFromDashboard(authenticatedStudentSession);
        mockMvc.perform(post("/logout")
                        .session(authenticatedStudentSession)
                        .param("_csrf", logoutCsrf))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"));
        mockMvc.perform(get("/dashboard").session(authenticatedStudentSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        entityManager.flush();
        entityManager.clear();
        User admin = userRepository.findByEmailIgnoreCase(configuredAdminEmail).orElseThrow();
        assertEquals(User.Role.ADMIN, admin.getRole());

        MockHttpSession adminSession = new MockHttpSession();
        String adminCsrf = loginCsrf(adminSession);
        MvcResult adminLogin = mockMvc.perform(post("/login")
                        .session(adminSession)
                        .param("_csrf", adminCsrf)
                        .param("username", configuredAdminEmail)
                        .param("password", configuredAdminPassword))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard/admin"))
                .andReturn();
        MockHttpSession authenticatedAdminSession = (MockHttpSession) adminLogin.getRequest().getSession(false);
        assertTrue(authenticatedAdminSession != null);
        for (String route : List.of(
                "/dashboard/admin", "/admin/notices", "/admin/events", "/admin/timetable",
                "/admin/clubs", "/admin/resources")) {
            mockMvc.perform(get(route).session(authenticatedAdminSession))
                    .andExpect(status().isOk());
        }
    }

    private String loginCsrf(MockHttpSession session) throws Exception {
        String html = mockMvc.perform(get("/login").session(session))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Matcher matcher = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"").matcher(html);
        assertTrue(matcher.find(), "Expected a login CSRF token.");
        return matcher.group(1);
    }

    private String csrfFromDashboard(MockHttpSession session) throws Exception {
        String html = mockMvc.perform(get("/dashboard").session(session))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Matcher matcher = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"").matcher(html);
        assertTrue(matcher.find(), "Expected a dashboard logout CSRF token.");
        return matcher.group(1);
    }
}
