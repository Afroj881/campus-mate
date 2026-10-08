package com.campusmate.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campusmate.model.User;
import com.campusmate.model.ProfileDetails;
import com.campusmate.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class ProfileControllerIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private FilterChainProxy springSecurityFilterChain;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilters(springSecurityFilterChain)
                .build();
    }

    @Test
    @Transactional
    void studentCanUpdateOnlyTheirOwnEditableProfileFields() throws Exception {
        String password = "profile-test-password";
        User student = userRepository.save(createStudent("first", password));
        User otherStudent = userRepository.save(createStudent("second", "another-password"));
        String originalEmail = student.getEmail();
        String originalPasswordHash = student.getPassword();
        MockHttpSession studentSession = authenticatedSession(originalEmail, "ROLE_STUDENT");

        MvcResult profilePage = mockMvc.perform(get("/profile").session(studentSession))
                .andExpect(status().isOk())
                .andReturn();
        String profileHtml = profilePage.getResponse().getContentAsString();
        assertTrue(profilePage.getModelAndView().getModel().get("profileDetails") instanceof ProfileDetails);
        assertFalse(profilePage.getModelAndView().getModel().containsKey("profileUser"));
        assertFalse(profileHtml.contains(originalPasswordHash));
        assertTrue(profileHtml.contains(student.getName()));
        assertTrue(profileHtml.contains(originalEmail));
        assertTrue(profileHtml.contains(student.getDepartment()));
        assertTrue(profileHtml.contains(student.getSemester().toString()));
        assertTrue(profileHtml.contains(student.getSection()));
        assertTrue(profileHtml.contains("STUDENT"));
        assertFalse(profileHtml.contains("name=\"email\""));
        assertFalse(profileHtml.contains("name=\"role\""));
        String csrfToken = csrfToken(profileHtml);

        MvcResult invalidForm = mockMvc.perform(post("/profile")
                        .session(studentSession)
                        .param("_csrf", csrfToken)
                        .param("name", "")
                        .param("department", "")
                        .param("semester", "")
                        .param("section", ""))
                .andExpect(status().isOk())
                .andReturn();
        String invalidHtml = invalidForm.getResponse().getContentAsString();
        assertTrue(invalidHtml.contains("Name is required."));
        assertTrue(invalidHtml.contains("Department is required."));
        assertTrue(invalidHtml.contains("Semester is required."));
        assertTrue(invalidHtml.contains("Section is required."));

        mockMvc.perform(post("/profile")
                        .session(studentSession)
                        .param("_csrf", csrfToken)
                        .param("name", "Updated Student")
                        .param("department", "Computer Science")
                        .param("semester", "5")
                        .param("section", "B")
                        .param("id", otherStudent.getId().toString())
                        .param("email", otherStudent.getEmail())
                        .param("role", "ADMIN")
                        .param("password", "attacker-controlled-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"));

        entityManager.flush();
        entityManager.clear();
        User updatedStudent = userRepository.findById(student.getId()).orElseThrow();
        User unchangedStudent = userRepository.findById(otherStudent.getId()).orElseThrow();
        assertEquals("Updated Student", updatedStudent.getName());
        assertEquals("Computer Science", updatedStudent.getDepartment());
        assertEquals(5, updatedStudent.getSemester());
        assertEquals("B", updatedStudent.getSection());
        assertEquals(originalEmail, updatedStudent.getEmail());
        assertEquals(User.Role.STUDENT, updatedStudent.getRole());
        assertEquals(originalPasswordHash, updatedStudent.getPassword());
        assertEquals(otherStudent.getName(), unchangedStudent.getName());
        assertEquals(otherStudent.getEmail(), unchangedStudent.getEmail());
        assertEquals(otherStudent.getDepartment(), unchangedStudent.getDepartment());
        assertEquals(otherStudent.getSemester(), unchangedStudent.getSemester());
        assertEquals(User.Role.STUDENT, unchangedStudent.getRole());
        assertEquals("Updated Student", studentSession.getAttribute("userName"));
        assertTrue(passwordEncoder.matches(password, updatedStudent.getPassword()));

        MockHttpSession otherSession = authenticatedSession(otherStudent.getEmail(), "ROLE_STUDENT");
        MvcResult otherProfile = mockMvc.perform(get("/profile").session(otherSession))
                .andExpect(status().isOk())
                .andReturn();
        assertTrue(otherProfile.getResponse().getContentAsString().contains(otherStudent.getName()));
        assertFalse(otherProfile.getResponse().getContentAsString().contains("Updated Student"));

        MockHttpSession adminSession = authenticatedSession("admin@example.test", "ROLE_ADMIN");
        mockMvc.perform(get("/profile").session(adminSession))
                .andExpect(status().isForbidden());

        MockHttpSession anonymousSession = new MockHttpSession();
        MvcResult loginPage = mockMvc.perform(get("/login").session(anonymousSession))
                .andExpect(status().isOk())
                .andReturn();
        String anonymousCsrfToken = csrfToken(loginPage.getResponse().getContentAsString());
        mockMvc.perform(get("/profile"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
        mockMvc.perform(post("/profile")
                        .session(anonymousSession)
                        .param("_csrf", anonymousCsrfToken))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    private User createStudent(String label, String password) {
        User user = new User();
        user.setName("Profile Test " + label + " " + UUID.randomUUID());
        user.setEmail("profile-" + label + "-" + UUID.randomUUID() + "@example.test");
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(User.Role.STUDENT);
        user.setDepartment("Testing");
        user.setSemester(2);
        user.setSection("A");
        return user;
    }

    private MockHttpSession authenticatedSession(String email, String role) {
        var authentication = new UsernamePasswordAuthenticationToken(
                email,
                "not-used-by-test",
                List.of(new SimpleGrantedAuthority(role)));
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                new SecurityContextImpl(authentication));
        return session;
    }

    private String csrfToken(String html) {
        Matcher matcher = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"").matcher(html);
        assertTrue(matcher.find(), "Expected a rendered CSRF token.");
        return matcher.group(1);
    }
}
