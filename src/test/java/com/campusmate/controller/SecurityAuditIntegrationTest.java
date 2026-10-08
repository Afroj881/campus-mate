package com.campusmate.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campusmate.model.Task;
import com.campusmate.model.TaskForm;
import com.campusmate.model.TaskSummary;
import com.campusmate.model.User;
import com.campusmate.repository.TaskRepository;
import com.campusmate.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
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
class SecurityAuditIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private FilterChainProxy springSecurityFilterChain;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskRepository taskRepository;

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
    void publicRegistrationAlwaysCreatesBcryptStudentAndDoesNotEchoPassword() throws Exception {
        MockHttpSession session = new MockHttpSession();
        String csrfToken = csrfToken("/register", session);
        String plainPassword = "Registration-secret-" + UUID.randomUUID();
        String email = "security-registration-" + UUID.randomUUID() + "@example.test";

        MvcResult invalidRegistration = mockMvc.perform(post("/register")
                        .session(session)
                        .param("_csrf", csrfToken)
                        .param("name", "")
                        .param("email", email)
                        .param("password", plainPassword)
                        .param("department", "Security Testing")
                        .param("semester", "1")
                        .param("section", "A"))
                .andExpect(status().isOk())
                .andReturn();
        assertFalse(invalidRegistration.getResponse().getContentAsString().contains(plainPassword));

        MvcResult registration = mockMvc.perform(post("/register")
                        .session(session)
                        .param("_csrf", csrfToken)
                        .param("name", "Security Test Student")
                        .param("email", email)
                        .param("password", plainPassword)
                        .param("department", "Security Testing")
                        .param("semester", "1")
                        .param("section", "A")
                        .param("role", "ADMIN")
                        .param("id", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andReturn();

        User registeredUser = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        assertEquals(User.Role.STUDENT, registeredUser.getRole());
        assertTrue(registeredUser.getPassword().startsWith("$2"));
        assertTrue(passwordEncoder.matches(plainPassword, registeredUser.getPassword()));
        assertFalse(registeredUser.getPassword().equals(plainPassword));
        assertFalse(registration.getResponse().getContentAsString().contains(plainPassword));
        assertFalse(registration.getResponse().getContentAsString().contains(registeredUser.getPassword()));
    }

    @Test
    @Transactional
    void anonymousAndStudentSessionsCannotReachAdminRoutesOrMutations() throws Exception {
        User student = userRepository.save(createStudent("route"));
        MockHttpSession studentSession = authenticatedSession(student.getEmail(), "ROLE_STUDENT");
        String studentCsrf = csrfToken("/login", studentSession);

        for (String route : List.of(
                "/",
                "/dashboard",
                "/notices",
                "/events",
                "/timetable",
                "/planner",
                "/clubs",
                "/resources",
                "/profile",
                "/search",
                "/dashboard/admin",
                "/admin",
                "/admin/notices",
                "/admin/events",
                "/admin/timetable",
                "/admin/clubs",
                "/admin/resources")) {
            mockMvc.perform(get(route))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/login"));
        }

        for (String route : List.of(
                "/admin",
                "/dashboard/admin",
                "/admin/notices",
                "/admin/events",
                "/admin/timetable",
                "/admin/clubs",
                "/admin/resources",
                "/admin/notices/new",
                "/admin/events/new",
                "/admin/timetable/new",
                "/admin/clubs/new",
                "/admin/resources/new")) {
            mockMvc.perform(get(route).session(studentSession))
                    .andExpect(status().isForbidden());
        }

        for (String route : List.of(
                "/admin/notices",
                "/admin/notices/update/1",
                "/admin/notices/delete/1",
                "/admin/events",
                "/admin/events/update/1",
                "/admin/events/delete/1",
                "/admin/timetable",
                "/admin/timetable/update/1",
                "/admin/timetable/delete/1",
                "/admin/clubs",
                "/admin/clubs/update/1",
                "/admin/clubs/delete/1",
                "/admin/resources",
                "/admin/resources/update/1",
                "/admin/resources/delete/1")) {
            mockMvc.perform(post(route)
                            .session(studentSession)
                            .param("_csrf", studentCsrf))
                    .andExpect(status().isForbidden());
        }

        MockHttpSession adminSession = authenticatedSession("security-admin@example.test", "ROLE_ADMIN");
        mockMvc.perform(get("/planner").session(adminSession))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/profile").session(adminSession))
                .andExpect(status().isForbidden());

        MvcResult logout = mockMvc.perform(post("/logout")
                        .session(studentSession)
                        .param("_csrf", studentCsrf))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"))
                .andReturn();
        String setCookie = logout.getResponse().getHeader("Set-Cookie");
        assertTrue(setCookie != null && setCookie.contains("JSESSIONID=")
                && (setCookie.contains("Max-Age=0") || setCookie.contains("JSESSIONID=;")));
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @Transactional
    void studentsCannotViewOrMutateAnotherStudentsTasksByChangingTheId() throws Exception {
        User owner = userRepository.save(createStudent("owner"));
        User otherStudent = userRepository.save(createStudent("other"));
        String ownerPasswordHash = owner.getPassword();
        Task ownedTask = taskRepository.save(createTask(owner, "Owner-only task"));
        Task privateTask = taskRepository.save(createTask(otherStudent, "Private other task"));
        entityManager.flush();

        MockHttpSession ownerSession = authenticatedSession(owner.getEmail(), "ROLE_STUDENT");
        String csrfToken = csrfToken("/login", ownerSession);
        MvcResult plannerPage = mockMvc.perform(get("/planner").session(ownerSession))
                .andExpect(status().isOk())
                .andReturn();
        String plannerHtml = plannerPage.getResponse().getContentAsString();
        List<?> visibleTasks = (List<?>) plannerPage.getModelAndView().getModel().get("tasks");
        assertTrue(plannerHtml.contains("Owner-only task"));
        assertFalse(plannerHtml.contains("Private other task"));
        assertFalse(plannerHtml.contains(ownerPasswordHash));
        assertTrue(visibleTasks.stream().allMatch(TaskSummary.class::isInstance));

        MvcResult taskEditPage = mockMvc.perform(get("/planner/edit/{id}", ownedTask.getId()).session(ownerSession))
                .andExpect(status().isOk())
                .andReturn();
        assertTrue(taskEditPage.getModelAndView().getModel().get("task") instanceof TaskForm);
        assertFalse(taskEditPage.getResponse().getContentAsString().contains(ownerPasswordHash));

        mockMvc.perform(get("/planner/edit/{id}", privateTask.getId()).session(ownerSession))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/planner/update/{id}", privateTask.getId())
                        .session(ownerSession)
                        .param("_csrf", csrfToken)
                        .param("title", "Stolen task")
                        .param("description", "Not allowed")
                        .param("type", "Assignment")
                        .param("deadline", LocalDate.now().plusDays(2).toString()))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/planner/delete/{id}", privateTask.getId())
                        .session(ownerSession)
                        .param("_csrf", csrfToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/planner/complete/{id}", privateTask.getId())
                        .session(ownerSession)
                        .param("_csrf", csrfToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/planner/edit/{id}", Long.MAX_VALUE).session(ownerSession))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/planner/delete/{id}", Long.MAX_VALUE)
                        .session(ownerSession)
                        .param("_csrf", csrfToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/planner/update/{id}", ownedTask.getId())
                        .session(ownerSession)
                        .param("_csrf", csrfToken)
                        .param("title", "Updated owner task")
                        .param("description", "Only allowed fields change")
                        .param("type", "Assignment")
                        .param("deadline", LocalDate.now().plusDays(2).toString())
                        .param("id", privateTask.getId().toString())
                        .param("user.id", otherStudent.getId().toString())
                        .param("status", "COMPLETED"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/planner"));

        entityManager.flush();
        entityManager.clear();
        Task unchangedOwner = taskRepository.findById(ownedTask.getId()).orElseThrow();
        assertEquals("Updated owner task", unchangedOwner.getTitle());
        assertEquals(Task.Status.PENDING, unchangedOwner.getStatus());
        assertEquals(owner.getId(), unchangedOwner.getUser().getId());
        assertEquals("Private other task", taskRepository.findById(privateTask.getId()).orElseThrow().getTitle());
    }

    @Test
    @Transactional
    void invalidAdminFormsAndMissingIdsAreHandledWithoutPersistence() throws Exception {
        MockHttpSession adminSession = authenticatedSession("security-admin@example.test", "ROLE_ADMIN");
        String csrfToken = csrfToken("/login", adminSession);

        for (String route : List.of(
                "/admin/notices",
                "/admin/events",
                "/admin/timetable",
                "/admin/clubs",
                "/admin/resources")) {
            MvcResult invalidForm = mockMvc.perform(post(route)
                            .session(adminSession)
                            .param("_csrf", csrfToken))
                    .andExpect(status().isOk())
                    .andReturn();
            assertTrue(invalidForm.getResponse().getContentAsString()
                    .contains("Please correct the highlighted fields and try again."));
        }

        for (String route : List.of(
                "/admin/notices/edit/" + Long.MAX_VALUE,
                "/admin/events/edit/" + Long.MAX_VALUE,
                "/admin/timetable/edit/" + Long.MAX_VALUE,
                "/admin/clubs/edit/" + Long.MAX_VALUE,
                "/admin/resources/edit/" + Long.MAX_VALUE)) {
            mockMvc.perform(get(route).session(adminSession))
                    .andExpect(status().isNotFound());
        }

        for (String route : List.of(
                "/admin/notices/delete/" + Long.MAX_VALUE,
                "/admin/events/delete/" + Long.MAX_VALUE,
                "/admin/timetable/delete/" + Long.MAX_VALUE,
                "/admin/clubs/delete/" + Long.MAX_VALUE,
                "/admin/resources/delete/" + Long.MAX_VALUE)) {
            mockMvc.perform(post(route)
                            .session(adminSession)
                            .param("_csrf", csrfToken))
                    .andExpect(status().isNotFound());
        }

        MockHttpSession studentSession = authenticatedSession("invalid-task@example.test", "ROLE_STUDENT");
        String studentCsrf = csrfToken("/login", studentSession);
        MvcResult invalidTask = mockMvc.perform(post("/planner")
                        .session(studentSession)
                        .param("_csrf", studentCsrf)
                        .param("title", "")
                        .param("type", "")
                        .param("deadline", ""))
                .andExpect(status().isOk())
                .andReturn();
        String invalidTaskHtml = invalidTask.getResponse().getContentAsString();
        assertTrue(invalidTaskHtml.contains("Title is required."));
        assertTrue(invalidTaskHtml.contains("Type is required."));
        assertTrue(invalidTaskHtml.contains("Deadline is required."));
    }

    @Test
    @Transactional
    void overlongFormValuesAreRejectedBeforePersistence() throws Exception {
        MockHttpSession adminSession = authenticatedSession("security-admin@example.test", "ROLE_ADMIN");
        String adminCsrf = csrfToken("/login", adminSession);
        String overVarcharLimit = "x".repeat(256);
        String overClubLimit = "x".repeat(161);

        MvcResult invalidNotice = mockMvc.perform(post("/admin/notices")
                        .session(adminSession).param("_csrf", adminCsrf)
                        .param("title", overVarcharLimit).param("description", "Notice")
                        .param("category", "General").param("date", LocalDate.now().toString()))
                .andExpect(status().isOk()).andReturn();
        assertTrue(invalidNotice.getResponse().getContentAsString()
                .contains("Title must be 255 characters or fewer."));

        MvcResult invalidEvent = mockMvc.perform(post("/admin/events")
                        .session(adminSession).param("_csrf", adminCsrf)
                        .param("title", overVarcharLimit).param("description", "Event")
                        .param("date", LocalDate.now().plusDays(1).toString()).param("time", "12:00")
                        .param("venue", "Hall").param("organizer", "Campus Mate"))
                .andExpect(status().isOk()).andReturn();
        assertTrue(invalidEvent.getResponse().getContentAsString()
                .contains("Title must be 255 characters or fewer."));

        MvcResult invalidTimetable = mockMvc.perform(post("/admin/timetable")
                        .session(adminSession).param("_csrf", adminCsrf)
                        .param("day", "MONDAY").param("subject", overVarcharLimit)
                        .param("faculty", "Faculty").param("room", "Room")
                        .param("startTime", "09:00").param("endTime", "10:00"))
                .andExpect(status().isOk()).andReturn();
        assertTrue(invalidTimetable.getResponse().getContentAsString()
                .contains("Subject must be 255 characters or fewer."));

        MvcResult invalidClub = mockMvc.perform(post("/admin/clubs")
                        .session(adminSession).param("_csrf", adminCsrf)
                        .param("name", overClubLimit).param("description", "Club")
                        .param("facultyCoordinator", "Faculty").param("studentCoordinator", "Student")
                        .param("contact", "club@example.test"))
                .andExpect(status().isOk()).andReturn();
        assertTrue(invalidClub.getResponse().getContentAsString()
                .contains("Club name must be 160 characters or fewer."));

        MvcResult invalidResource = mockMvc.perform(post("/admin/resources")
                        .session(adminSession).param("_csrf", adminCsrf)
                        .param("title", overClubLimit).param("subject", "Math")
                        .param("description", "Resource").param("url", "https://example.test"))
                .andExpect(status().isOk()).andReturn();
        assertTrue(invalidResource.getResponse().getContentAsString()
                .contains("Title must be 160 characters or fewer."));

        User student = userRepository.save(createStudent("length"));
        MockHttpSession studentSession = authenticatedSession(student.getEmail(), "ROLE_STUDENT");
        String studentCsrf = csrfToken("/login", studentSession);
        MvcResult invalidTask = mockMvc.perform(post("/planner")
                        .session(studentSession).param("_csrf", studentCsrf)
                        .param("title", overVarcharLimit).param("type", "Assignment")
                        .param("deadline", LocalDate.now().plusDays(1).toString()))
                .andExpect(status().isOk()).andReturn();
        assertTrue(invalidTask.getResponse().getContentAsString()
                .contains("Title must be 255 characters or fewer."));

        MvcResult invalidProfile = mockMvc.perform(post("/profile")
                        .session(studentSession).param("_csrf", studentCsrf)
                        .param("name", "x".repeat(121)).param("department", "Testing").param("semester", "1").param("section", "A"))
                .andExpect(status().isOk()).andReturn();
        assertTrue(invalidProfile.getResponse().getContentAsString()
                .contains("Name must be 120 characters or fewer."));

        MockHttpSession registrationSession = new MockHttpSession();
        String registrationCsrf = csrfToken("/register", registrationSession);
        MvcResult invalidRegistration = mockMvc.perform(post("/register")
                        .session(registrationSession).param("_csrf", registrationCsrf)
                        .param("name", "x".repeat(121)).param("email", "length@example.test")
                        .param("password", "valid-password").param("department", "Testing").param("semester", "1").param("section", "A"))
                .andExpect(status().isOk()).andReturn();
        assertTrue(invalidRegistration.getResponse().getContentAsString()
                .contains("Name must be 120 characters or fewer."));
    }

    private User createStudent(String label) {
        User user = new User();
        user.setName("Security Test " + label);
        user.setEmail("security-" + label + "-" + UUID.randomUUID() + "@example.test");
        user.setPassword(passwordEncoder.encode("Security-test-password"));
        user.setRole(User.Role.STUDENT);
        user.setDepartment("Security Testing");
        user.setSemester(1);
        return user;
    }

    private Task createTask(User owner, String title) {
        Task task = new Task();
        task.setTitle(title);
        task.setDescription(title);
        task.setType("Assignment");
        task.setDeadline(LocalDate.now().plusDays(1));
        task.setStatus(Task.Status.PENDING);
        task.setUser(owner);
        return task;
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

    private String csrfToken(String route, MockHttpSession session) throws Exception {
        String html = mockMvc.perform(get(route).session(session))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Matcher matcher = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"").matcher(html);
        assertTrue(matcher.find(), "Expected a rendered CSRF token.");
        return matcher.group(1);
    }
}
