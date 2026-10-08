package com.campusmate.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campusmate.model.Subject;
import com.campusmate.model.TeacherSubjectAssignment;
import com.campusmate.model.User;
import com.campusmate.repository.SubjectRepository;
import com.campusmate.repository.TeacherSubjectAssignmentRepository;
import com.campusmate.repository.UserRepository;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class FacultyRoleStep16IntegrationTest {

    @Autowired private WebApplicationContext webApplicationContext;
    @Autowired private FilterChainProxy springSecurityFilterChain;
    @Autowired private UserRepository userRepository;
    @Autowired private SubjectRepository subjectRepository;
    @Autowired private TeacherSubjectAssignmentRepository teacherSubjectAssignmentRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private TransactionTemplate transactionTemplate;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilters(springSecurityFilterChain)
                .build();
    }

    @Test
    @Transactional
    void studentLoginWithStudentOptionSucceeds() throws Exception {
        User student = createUser("student-login-" + UUID.randomUUID() + "@example.test", User.Role.STUDENT, "Computer Science", 3, "A");

        MockHttpSession session = new MockHttpSession();
        String csrf = loginCsrf(session);

        mockMvc.perform(post("/login")
                        .session(session)
                        .param("_csrf", csrf)
                        .param("username", student.getEmail())
                        .param("password", "StrongPassword123!")
                        .param("loginType", "STUDENT"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        mockMvc.perform(get("/assignments").session(session))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void facultyLoginWithTeacherOptionSucceeds() throws Exception {
        User faculty = createUser("faculty-login-" + UUID.randomUUID() + "@example.test", User.Role.FACULTY, "Computer Science", 3, "A");

        MockHttpSession session = new MockHttpSession();
        String csrf = loginCsrf(session);

        MvcResult result = mockMvc.perform(post("/login")
                        .session(session)
                        .param("_csrf", csrf)
                        .param("username", faculty.getEmail())
                        .param("password", "StrongPassword123!")
                        .param("loginType", "FACULTY"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/faculty/dashboard"))
                .andReturn();

        assertNotNull(result.getRequest().getSession(false));
    }

    @Test
    @Transactional
    void adminLoginWithAdminOptionSucceeds() throws Exception {
        User admin = createUser("admin-login-" + UUID.randomUUID() + "@example.test", User.Role.ADMIN, "Administration", 1, "A");

        MockHttpSession session = new MockHttpSession();
        String csrf = loginCsrf(session);

        mockMvc.perform(post("/login")
                        .session(session)
                        .param("_csrf", csrf)
                        .param("username", admin.getEmail())
                        .param("password", "StrongPassword123!")
                        .param("loginType", "ADMIN"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard/admin"));
    }

    @Test
    @Transactional
    void studentUsingTeacherOptionIsRejected() throws Exception {
        User student = createUser("student-mismatch-" + UUID.randomUUID() + "@example.test", User.Role.STUDENT, "Computer Science", 3, "A");

        MockHttpSession session = new MockHttpSession();
        String csrf = loginCsrf(session);

        MvcResult result = mockMvc.perform(post("/login")
                        .session(session)
                        .param("_csrf", csrf)
                        .param("username", student.getEmail())
                        .param("password", "StrongPassword123!")
                        .param("loginType", "FACULTY"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?errorRole"))
                .andReturn();

        assertNull(result.getRequest().getSession(false));
    }

    @Test
    @Transactional
    void facultyCanOnlyAccessOwnTeachingAssignmentsAndNotAdminManagement() throws Exception {
        User faculty = createUser("faculty-own-assignment-" + UUID.randomUUID() + "@example.test", User.Role.FACULTY, "Computer Science", 3, "A");
        User secondFaculty = createUser("faculty-other-" + UUID.randomUUID() + "@example.test", User.Role.FACULTY, "Computer Science", 3, "B");
        Subject subject = createSubject("Java Programming", "CS301", "Computer Science", 3);
        Subject otherSubject = createSubject("Operating Systems", "CS405", "Computer Science", 5);

        teacherSubjectAssignmentRepository.save(new TeacherSubjectAssignment(faculty, subject, "Computer Science", 3, "A"));
        teacherSubjectAssignmentRepository.save(new TeacherSubjectAssignment(secondFaculty, otherSubject, "Computer Science", 5, "B"));

        MockHttpSession facultySession = loginAs(faculty, "FACULTY");

        MvcResult facultyDashboard = mockMvc.perform(get("/faculty/dashboard").session(facultySession))
                .andExpect(status().isOk())
                .andReturn();
        String dashboardHtml = facultyDashboard.getResponse().getContentAsString();
        assertTrue(dashboardHtml.contains("Assigned Classes"));
        assertTrue(dashboardHtml.contains("Java Programming"));
        assertTrue(dashboardHtml.contains("Semester 3"));
        assertTrue(!dashboardHtml.contains("Create Assignment"));
        assertTrue(!dashboardHtml.contains("Operating Systems"));

        mockMvc.perform(get("/dashboard/admin").session(facultySession))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/faculty/assignments").session(facultySession))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/faculty/assignments/new").session(facultySession))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void adminCanCreateFacultyAndSubjectAndTeachingAssignment() throws Exception {
        MockHttpSession adminSession = loginAs(createUser("admin-manage-" + UUID.randomUUID() + "@example.test", User.Role.ADMIN, "Administration", 1, "A"), "ADMIN");

        String dashboardHtml = mockMvc.perform(get("/dashboard/admin").session(adminSession))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(dashboardHtml.contains("Faculty Management"));
        assertTrue(dashboardHtml.contains("href=\"/admin/faculty\""));
        assertTrue(dashboardHtml.contains("Subjects"));
        assertTrue(dashboardHtml.contains("href=\"/admin/subjects\""));
        assertTrue(dashboardHtml.contains("Teacher/Class Assignments"));
        assertTrue(dashboardHtml.contains("href=\"/admin/teacher-subject-assignments\""));
        assertTrue(dashboardHtml.contains("Academic Calendar"));
        assertTrue(dashboardHtml.contains("href=\"/admin/calendar\""));

        String facultyPage = mockMvc.perform(get("/admin/faculty").session(adminSession))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(facultyPage.contains("Faculty Management"));
        assertTrue(facultyPage.contains("class=\"admin-form\""));
        assertLabelsPrecedeInputs(facultyPage, "faculty-name", "faculty-email", "faculty-password",
                "faculty-department", "faculty-semester", "faculty-section");

        String adminFacultyCsrf = csrfFor(adminSession, "/admin/faculty");
        String facultyEmail = "new-faculty-" + UUID.randomUUID() + "@example.test";
        mockMvc.perform(post("/admin/faculty")
                        .session(adminSession)
                        .param("_csrf", adminFacultyCsrf)
                        .param("name", "New Faculty")
                        .param("email", facultyEmail)
                        .param("password", "StrongPassword123!")
                        .param("department", "Computer Science")
                        .param("semester", "3")
                        .param("section", "A"))
                .andExpect(status().is3xxRedirection());

        User faculty = userRepository.findByEmailIgnoreCase(facultyEmail).orElseThrow();
        assertEquals(User.Role.FACULTY, faculty.getRole());
        assertEquals("Computer Science", faculty.getDepartment());
        assertEquals(3, faculty.getSemester());
        assertEquals("A", faculty.getSection());
        MockHttpSession facultySession = loginAs(faculty, "FACULTY");
        mockMvc.perform(get("/faculty/dashboard").session(facultySession))
                .andExpect(status().isOk());

        User student = createUser("admin-student-" + UUID.randomUUID() + "@example.test", User.Role.STUDENT, "Computer Science", 3, "A");
        MockHttpSession studentSession = loginAs(student, "STUDENT");
        mockMvc.perform(get("/admin/faculty").session(studentSession)).andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/faculty").session(facultySession)).andExpect(status().isForbidden());

        Subject subject = new Subject();
        subject.setName("Artificial Intelligence");
        subject.setCode("CS401");
        subject.setDepartment("Computer Science");
        subject.setSemester(4);
        subjectRepository.save(subject);

        String subjectPage = mockMvc.perform(get("/admin/subjects").session(adminSession))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(subjectPage.contains("class=\"admin-form\""));
        assertLabelsPrecedeInputs(subjectPage, "subject-name", "subject-code", "subject-department", "subject-semester");

        String assignmentPage = mockMvc.perform(get("/admin/teacher-subject-assignments").session(adminSession))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(assignmentPage.contains("class=\"admin-form\""));
        assertLabelsPrecedeInputs(assignmentPage, "assignment-faculty", "assignment-subject",
                "assignment-department", "assignment-semester", "assignment-section");

        String adminTeachingCsrf = csrfFor(adminSession, "/admin/teacher-subject-assignments");
        mockMvc.perform(post("/admin/teacher-subject-assignments")
                        .session(adminSession)
                        .param("_csrf", adminTeachingCsrf)
                        .param("facultyId", String.valueOf(faculty.getId()))
                        .param("subjectId", String.valueOf(subject.getId()))
                        .param("department", "Computer Science")
                        .param("semester", "4")
                        .param("section", "A"))
                .andExpect(status().is3xxRedirection());
        TeacherSubjectAssignment teachingAssignment = teacherSubjectAssignmentRepository.findAll().stream()
                .filter(assignment -> assignment.getFaculty().getId().equals(faculty.getId()))
                .filter(assignment -> assignment.getSubject().getId().equals(subject.getId()))
                .findFirst().orElseThrow();
        assertEquals("Computer Science", teachingAssignment.getDepartment());
        assertEquals(4, teachingAssignment.getSemester());
        assertEquals("A", teachingAssignment.getSection());
    }

    @Test
    void adminAssignmentManagementLoadsAndCreatesOutsideTestTransaction() throws Exception {
        String unique = UUID.randomUUID().toString();
        User admin = createUser("assignment-admin-" + unique + "@example.test", User.Role.ADMIN, "Administration", 1, "A");
        User faculty = createUser("assignment-faculty-" + unique + "@example.test", User.Role.FACULTY, "Computer Science", 3, "A");
        Subject subject = createSubject("Java Programming " + unique.substring(0, 8), "CS" + unique.substring(0, 8), "Computer Science", 3);
        MockHttpSession adminSession = loginAs(admin, "ADMIN");
        Long adminId = admin.getId();
        Long facultyId = faculty.getId();
        Long subjectId = subject.getId();

        try {
            mockMvc.perform(get("/admin/teacher-subject-assignments").session(adminSession))
                    .andExpect(status().isOk())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                            .string(org.hamcrest.Matchers.containsString("Java Programming " + unique.substring(0, 8))))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                            .string(org.hamcrest.Matchers.containsString(faculty.getName())));

            String csrf = csrfFor(adminSession, "/admin/teacher-subject-assignments");
            mockMvc.perform(post("/admin/teacher-subject-assignments")
                            .session(adminSession)
                            .param("_csrf", csrf)
                            .param("facultyId", String.valueOf(facultyId))
                            .param("subjectId", String.valueOf(subjectId))
                            .param("department", "Computer Science")
                            .param("semester", "3")
                            .param("section", "A"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/teacher-subject-assignments"));

            assertEquals(1, teacherSubjectAssignmentRepository.findByFacultyWithSubject(faculty).size());

            mockMvc.perform(get("/admin/teacher-subject-assignments").session(adminSession))
                    .andExpect(status().isOk())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                            .string(org.hamcrest.Matchers.containsString("Java Programming " + unique.substring(0, 8))));

            MockHttpSession facultySession = loginAs(faculty, "FACULTY");
            mockMvc.perform(get("/faculty/dashboard").session(facultySession))
                    .andExpect(status().isOk())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                            .string(org.hamcrest.Matchers.containsString("Java Programming " + unique.substring(0, 8))));
            mockMvc.perform(get("/admin/teacher-subject-assignments").session(facultySession))
                    .andExpect(status().isForbidden());

            User student = createUser("assignment-student-" + unique + "@example.test", User.Role.STUDENT, "Computer Science", 3, "A");
            MockHttpSession studentSession = loginAs(student, "STUDENT");
            mockMvc.perform(get("/admin/teacher-subject-assignments").session(studentSession))
                    .andExpect(status().isForbidden());
        } finally {
            transactionTemplate.executeWithoutResult(status -> {
                teacherSubjectAssignmentRepository.findByFaculty(faculty).forEach(teacherSubjectAssignmentRepository::delete);
                userRepository.findByEmailIgnoreCase("assignment-student-" + unique + "@example.test")
                        .ifPresent(userRepository::delete);
                userRepository.deleteById(facultyId);
                userRepository.deleteById(adminId);
                subjectRepository.deleteById(subjectId);
            });
        }
    }

    @Test
    @Transactional
    void loginPageUsesClickableRoleCardsAndKeepsRegistrationStudentOnly() throws Exception {
        String html = mockMvc.perform(get("/login"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertTrue(html.contains("login-role-card\"><strong>Student</strong>"));
        assertTrue(html.contains("login-role-card\"><strong>Faculty / Teacher</strong>"));
        assertTrue(html.contains("login-role-card\"><strong>Admin</strong>"));
        assertTrue(html.contains("name=\"loginType\" value=\"STUDENT\""));
        assertTrue(html.contains("name=\"loginType\" value=\"FACULTY\""));
        assertTrue(html.contains("name=\"loginType\" value=\"ADMIN\""));
        assertTrue(html.contains("class=\"login-register-option\" href=\"/register\""));
        assertTrue(!html.contains("Student Login") && !html.contains("Admin Login"));
    }

    private User createUser(String email, User.Role role, String department, int semester, String section) {
        User user = new User();
        user.setName(role.name() + " User");
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("StrongPassword123!"));
        user.setRole(role);
        user.setDepartment(department);
        user.setSemester(semester);
        user.setSection(section);
        return userRepository.save(user);
    }

    private Subject createSubject(String name, String code, String department, int semester) {
        Subject subject = new Subject();
        subject.setName(name);
        subject.setCode(code + "-" + UUID.randomUUID().toString().substring(0, 8));
        subject.setDepartment(department);
        subject.setSemester(semester);
        return subjectRepository.save(subject);
    }

    private MockHttpSession loginAs(User user, String loginType) throws Exception {
        MockHttpSession session = new MockHttpSession();
        String csrf = loginCsrf(session);
        mockMvc.perform(post("/login")
                        .session(session)
                        .param("_csrf", csrf)
                        .param("username", user.getEmail())
                        .param("password", "StrongPassword123!")
                        .param("loginType", loginType))
                .andExpect(status().is3xxRedirection());
        return session;
    }

    private String loginCsrf(MockHttpSession session) throws Exception {
        return csrfFor(session, "/login");
    }

    private String csrfFor(MockHttpSession session, String url) throws Exception {
        String html = mockMvc.perform(get(url).session(session)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        Matcher matcher = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"").matcher(html);
        assertTrue(matcher.find(), "Expected a CSRF token at " + url + ".");
        return matcher.group(1);
    }

    private void assertLabelsPrecedeInputs(String html, String... fieldIds) {
        for (String fieldId : fieldIds) {
            int label = html.indexOf("for=\"" + fieldId + "\"");
            int input = html.indexOf("id=\"" + fieldId + "\"");
            assertTrue(label >= 0 && input > label, "Expected the label before input " + fieldId + ".");
        }
    }
}
