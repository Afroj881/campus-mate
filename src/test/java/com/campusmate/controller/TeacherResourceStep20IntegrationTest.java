package com.campusmate.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campusmate.model.Resource;
import com.campusmate.model.Subject;
import com.campusmate.model.TeacherSubjectAssignment;
import com.campusmate.model.User;
import com.campusmate.repository.ResourceRepository;
import com.campusmate.repository.SubjectRepository;
import com.campusmate.repository.TeacherSubjectAssignmentRepository;
import com.campusmate.repository.UserRepository;
import com.campusmate.service.ResourceService;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class TeacherResourceStep20IntegrationTest {
    @Autowired WebApplicationContext context;
    @Autowired FilterChainProxy security;
    @Autowired UserRepository users;
    @Autowired SubjectRepository subjects;
    @Autowired TeacherSubjectAssignmentRepository assignments;
    @Autowired ResourceRepository resources;
    @Autowired PasswordEncoder encoder;
    @Autowired ResourceService resourceService;
    MockMvc mvc;

    @BeforeEach void setup() { mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(security).build(); }

    @Test @Transactional
    void facultyUploadsForOwnedAssignmentAndMatchingStudentsCanDownload() throws Exception {
        String suffix = UUID.randomUUID().toString();
        User faculty = user("faculty-" + suffix + "@example.test", User.Role.FACULTY, "AI", 5, "A");
        faculty.setName("Afroj Ali");
        users.save(faculty);
        User unrelated = user("other-" + suffix + "@example.test", User.Role.STUDENT, "AI", 5, "B");
        User otherDepartment = user("other-department-" + suffix + "@example.test", User.Role.STUDENT, "Information Technology", 5, "A");
        User nullSectionStudent = user("null-section-" + suffix + "@example.test", User.Role.STUDENT, "AI", 5, "A");
        nullSectionStudent.setSection(null);
        users.save(nullSectionStudent);
        Subject subject = new Subject();
        subject.setName("Java Programming");
        subject.setCode("JAVA-" + suffix.substring(0, 8));
        subject.setDepartment("AI");
        subject.setSemester(5);
        subjects.save(subject);
        TeacherSubjectAssignment assignment = assignments.save(new TeacherSubjectAssignment(faculty, subject, "AI", 5, "A"));
        Resource urlResource = new Resource();
        urlResource.setTitle("Java Programming all notes");
        urlResource.setSubject("Java Programming");
        urlResource.setDescription("Existing URL resource");
        urlResource.setUrl("https://docs.oracle.com/en/java/");
        resources.save(urlResource);
        MockHttpSession facultySession = login(faculty, "FACULTY");
        String csrf = csrf(facultySession);
        byte[] pdf = "%PDF-1.7\nCampus Mate notes".getBytes(StandardCharsets.US_ASCII);
        mvc.perform(multipart("/faculty/resources").file(new MockMultipartFile("file", "Unit 1.pdf", "application/pdf", pdf))
                        .session(facultySession).param("_csrf", csrf).param("title", "Java Unit 1 Notes")
                        .param("description", "Complete notes covering the basics of Java Programming, including concepts, syntax, and important examples for Unit 1.").param("subjectAndClass", "Java Programming — AI — Semester 5 — Section A"))
                .andExpect(status().is3xxRedirection());
        Resource note = resources.findAll().stream().filter(r -> "Java Unit 1 Notes".equals(r.getTitle())).findFirst().orElseThrow();
        assertTrue(note.isFileBacked());
        assertEquals(faculty.getId(), note.getUploadedBy().getId());
        assertEquals("application/pdf", note.getContentType());
        assertEquals("Unit 1.pdf", note.getOriginalFilename());
        var path = resourceService.resolveStoredFile(note.getStoredFilename());
        assertTrue(Files.exists(path));

        String studentEmail = "student-" + suffix + "@example.test";
        MockHttpSession registrationSession = new MockHttpSession();
        mvc.perform(post("/register").session(registrationSession).param("_csrf", csrfRegister(registrationSession))
                        .param("name", "Test Student").param("email", studentEmail).param("password", "StrongPassword123!")
                        .param("department", "AI").param("semester", "5").param("section", " A "))
                .andExpect(status().is3xxRedirection());
        User student = users.findByEmailIgnoreCase(studentEmail).orElseThrow();
        assertEquals("AI", student.getDepartment());
        assertEquals(5, student.getSemester());
        assertEquals("A", student.getSection());
        MockHttpSession studentSession = login(student, "STUDENT");
        String studentResources = mvc.perform(get("/resources").session(studentSession)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(studentResources.contains("Java Unit 1 Notes"));
        assertTrue(studentResources.contains("Download Notes"));
        assertTrue(studentResources.contains("/resources/" + note.getId() + "/download"));
        assertTrue(studentResources.contains(urlResource.getTitle()));
        assertTrue(studentResources.contains("Open Resource"));
        assertTrue(studentResources.contains("https://docs.oracle.com/en/java/"));
        assertFalse(studentResources.contains("URL:</strong> <span></span>"));
        mvc.perform(get("/resources/{id}/download", note.getId()).session(studentSession)).andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Content-Type", "application/pdf"));
        MockHttpSession unrelatedSession = login(unrelated, "STUDENT");
        mvc.perform(get("/resources").session(unrelatedSession)).andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Java Unit 1 Notes"))));
        mvc.perform(get("/resources/{id}/download", note.getId()).session(unrelatedSession)).andExpect(status().isForbidden());
        MockHttpSession otherDepartmentSession = login(otherDepartment, "STUDENT");
        mvc.perform(get("/resources").session(otherDepartmentSession)).andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Java Unit 1 Notes"))));
        mvc.perform(get("/resources/{id}/download", note.getId()).session(otherDepartmentSession)).andExpect(status().isForbidden());
        MockHttpSession nullSectionSession = login(nullSectionStudent, "STUDENT");
        mvc.perform(get("/resources").session(nullSectionSession)).andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Java Unit 1 Notes"))));
        mvc.perform(get("/resources/{id}/download", note.getId()).session(nullSectionSession)).andExpect(status().isForbidden());

        User otherFaculty = user("faculty-other-" + suffix + "@example.test", User.Role.FACULTY, "CSE", 3, "A");
        MockHttpSession otherFacultySession = login(otherFaculty, "FACULTY");
        mvc.perform(post("/faculty/resources/{id}/delete", note.getId()).session(otherFacultySession)
                        .param("_csrf", csrf(otherFacultySession)))
                .andExpect(status().isNotFound());
        User admin = user("admin-" + suffix + "@example.test", User.Role.ADMIN, "Admin", 1, "A");
        MockHttpSession adminSession = login(admin, "ADMIN");
        mvc.perform(post("/admin/resources/delete/{id}", note.getId()).session(adminSession)
                        .param("_csrf", csrfLogin(adminSession)))
                .andExpect(status().isNotFound());

        mvc.perform(post("/faculty/resources/{id}/delete", note.getId()).session(facultySession).param("_csrf", csrf))
                .andExpect(status().is3xxRedirection());
        assertTrue(resources.findById(note.getId()).isEmpty());

    }

    @Test @Transactional
    void uploadFormListsOnlyAuthenticatedFacultysExistingTeachingAssignments() throws Exception {
        String suffix = UUID.randomUUID().toString();
        User rahul = user("rahul-" + suffix + "@example.test", User.Role.FACULTY, "Computer Science", 5, "A");
        rahul.setName("Rahul Kumar");
        users.save(rahul);
        User otherFaculty = user("other-faculty-" + suffix + "@example.test", User.Role.FACULTY, "Computer Science", 5, "B");

        Subject java = new Subject();
        java.setName("Java Programming"); java.setCode("JAVA-" + suffix.substring(0, 8));
        java.setDepartment("Computer Science"); java.setSemester(5); subjects.save(java);
        Subject dbms = new Subject();
        dbms.setName("DBMS Other Faculty"); dbms.setCode("DBMS-" + suffix.substring(0, 8));
        dbms.setDepartment("Computer Science"); dbms.setSemester(5); subjects.save(dbms);
        Subject ownDb = new Subject();
        ownDb.setName("Database Management"); ownDb.setCode("OWNDB-" + suffix.substring(0, 8));
        ownDb.setDepartment("Computer Science"); ownDb.setSemester(5); subjects.save(ownDb);
        TeacherSubjectAssignment rahulAssignment = assignments.save(new TeacherSubjectAssignment(rahul, java, "Computer Science", 5, "A"));
        assignments.save(new TeacherSubjectAssignment(rahul, ownDb, "Computer Science", 5, "A"));
        TeacherSubjectAssignment otherAssignment = assignments.save(new TeacherSubjectAssignment(otherFaculty, dbms, "Computer Science", 5, "B"));

        MockHttpSession session = login(rahul, "FACULTY");
        String form = mvc.perform(get("/faculty/resources/new").session(session)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(form.contains("<label for=\"subjectAndClass\">Subject and class</label>"));
        assertTrue(form.contains("<input id=\"subjectAndClass\" type=\"text\""));
        assertFalse(form.contains("<select"));
        assertTrue(form.contains("placeholder=\"Java Programming — Computer Science — Semester 5 — Section A\""));
        assertFalse(form.contains("DBMS Other Faculty"));
        assertFalse(form.contains("value=\"" + otherAssignment.getId() + "\""));

        String csrf = csrf(session);
        MockMultipartFile pdf = new MockMultipartFile("file", "java.pdf", "application/pdf",
                "%PDF-1.7\nJava notes".getBytes(StandardCharsets.US_ASCII));
        mvc.perform(multipart("/faculty/resources").file(pdf).session(session).param("_csrf", csrf)
                        .param("title", "Rejected foreign class").param("description", "Validation test")
                        .param("subjectAndClass", "DBMS Other Faculty — Computer Science — Semester 5 — Section B"))
                .andExpect(status().isOk());
        assertFalse(resources.findAll().stream().anyMatch(r -> "Rejected foreign class".equals(r.getTitle())));

        mvc.perform(multipart("/faculty/resources").file(pdf).session(session).param("_csrf", csrf)
                        .param("title", "Java Programming Notes").param("description", "Java notes")
                        .param("subjectAndClass", "  Java Programming — Computer Science — Semester 5 — Section A  "))
                .andExpect(status().is3xxRedirection());
        assertTrue(resources.findAll().stream().anyMatch(r -> "Java Programming Notes".equals(r.getTitle())));

        mvc.perform(multipart("/faculty/resources").file(pdf).session(session).param("_csrf", csrf)
                        .param("title", "Java Notes With Hyphens").param("description", "Java notes")
                        .param("subjectAndClass", "  JAVA   PROGRAMMING - COMPUTER SCIENCE - SEMESTER 5 - SECTION A  "))
                .andExpect(status().is3xxRedirection());
        mvc.perform(multipart("/faculty/resources").file(pdf).session(session).param("_csrf", csrf)
                        .param("title", "Database Management Notes").param("description", "Database notes")
                        .param("subjectAndClass", "Database Management \u2014 Computer Science - Semester 5 \u2014 Section A"))
                .andExpect(status().is3xxRedirection());
        Resource javaNotes = resources.findAll().stream().filter(r -> "Java Programming Notes".equals(r.getTitle())).findFirst().orElseThrow();
        Resource dbNotes = resources.findAll().stream().filter(r -> "Database Management Notes".equals(r.getTitle())).findFirst().orElseThrow();
        assertEquals("Java Programming", javaNotes.getSubject());
        assertEquals("Database Management", dbNotes.getSubject());
        assertEquals(rahul.getId(), javaNotes.getUploadedBy().getId());
        assertEquals(rahul.getId(), dbNotes.getUploadedBy().getId());
    }

    @Test @Transactional
    void facultyCannotUploadUnassignedClassAndStudentCannotUploadOrManageNotes() throws Exception {
        String suffix = UUID.randomUUID().toString();
        User faculty = user("faculty-" + suffix + "@example.test", User.Role.FACULTY, "CSE", 3, "A");
        User student = user("student-" + suffix + "@example.test", User.Role.STUDENT, "CSE", 3, "A");
        Subject subject = subject(suffix);
        assignments.save(new TeacherSubjectAssignment(faculty, subject, "CSE", 3, "A"));
        MockHttpSession facultySession = login(faculty, "FACULTY");
        mvc.perform(multipart("/faculty/resources").file(new MockMultipartFile("file", "Unit.pdf", "application/pdf", "%PDF-1.7".getBytes(StandardCharsets.US_ASCII)))
                        .session(facultySession).param("_csrf", csrf(facultySession)).param("title", "Invalid class")
                        .param("subjectAndClass", "Unassigned Class — Unknown — Semester 99 — Section Z"))
                .andExpect(status().isOk());
        assertFalse(resources.findAll().stream().anyMatch(r -> "Invalid class".equals(r.getTitle())));
        MockHttpSession studentSession = login(student, "STUDENT");
        mvc.perform(get("/faculty/resources").session(studentSession)).andExpect(status().isForbidden());
        mvc.perform(post("/faculty/resources/1/delete").session(studentSession).param("_csrf", csrfLogin(studentSession)))
                .andExpect(status().isForbidden());
        String adminDashboard = mvc.perform(get("/dashboard/admin").session(
                        login(user("admin-" + suffix + "@example.test", User.Role.ADMIN, "Admin", 1, "A"), "ADMIN")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertFalse(adminDashboard.contains("Upload Notes"));
    }

    @Test @Transactional
    void rejectsUnsupportedEmptyAndOversizedUploads() throws Exception {
        String suffix = UUID.randomUUID().toString();
        User faculty = user("faculty-validation-" + suffix + "@example.test", User.Role.FACULTY, "CSE", 3, "A");
        Subject subject = subject(suffix);
        TeacherSubjectAssignment assignment = assignments.save(new TeacherSubjectAssignment(faculty, subject, "CSE", 3, "A"));
        MockHttpSession session = login(faculty, "FACULTY");
        String csrf = csrf(session);

        mvc.perform(multipart("/faculty/resources").file(new MockMultipartFile("file", "script.html", "text/html", "<script>bad</script>".getBytes(StandardCharsets.UTF_8)))
                        .session(session).param("_csrf", csrf).param("title", "Unsupported").param("subjectAndClass", "Java " + suffix.substring(0, 6) + " — CSE — Semester 3 — Section A"))
                .andExpect(status().isOk());
        mvc.perform(multipart("/faculty/resources").file(new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]))
                        .session(session).param("_csrf", csrf).param("title", "Empty").param("subjectAndClass", "Java " + suffix.substring(0, 6) + " — CSE — Semester 3 — Section A"))
                .andExpect(status().isOk());
        byte[] tooLarge = new byte[10 * 1024 * 1024 + 1];
        System.arraycopy("%PDF-1.7".getBytes(StandardCharsets.US_ASCII), 0, tooLarge, 0, 8);
        mvc.perform(multipart("/faculty/resources").file(new MockMultipartFile("file", "large.pdf", "application/pdf", tooLarge))
                        .session(session).param("_csrf", csrf).param("title", "Too Large").param("subjectAndClass", "Java " + suffix.substring(0, 6) + " — CSE — Semester 3 — Section A"))
                .andExpect(status().isOk());
        assertFalse(resources.findAll().stream().anyMatch(r -> java.util.List.of("Unsupported", "Empty", "Too Large").contains(r.getTitle())));
    }

    private User user(String email, User.Role role, String dept, int semester, String section) {
        User u = new User(); u.setName(role.name()); u.setEmail(email); u.setPassword(encoder.encode("StrongPassword123!"));
        u.setRole(role); u.setDepartment(dept); u.setSemester(semester); u.setSection(section); return users.save(u);
    }
    private Subject subject(String suffix) {
        Subject s = new Subject(); s.setName("Java " + suffix.substring(0, 6)); s.setCode("CS" + suffix.substring(0, 8));
        s.setDepartment("CSE"); s.setSemester(3); return subjects.save(s);
    }
    private MockHttpSession login(User u, String role) throws Exception {
        MockHttpSession session = new MockHttpSession();
        String html = mvc.perform(get("/login").session(session)).andReturn().getResponse().getContentAsString();
        Matcher matcher = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"").matcher(html); assertTrue(matcher.find());
        mvc.perform(post("/login").session(session).param("_csrf", matcher.group(1)).param("username", u.getEmail())
                .param("password", "StrongPassword123!").param("loginType", role)).andExpect(status().is3xxRedirection());
        return session;
    }
    private String csrf(MockHttpSession session) throws Exception {
        String html = mvc.perform(get("/faculty/resources/new").session(session)).andReturn().getResponse().getContentAsString();
        Matcher matcher = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"").matcher(html); assertTrue(matcher.find()); return matcher.group(1);
    }
    private String csrfLogin(MockHttpSession session) throws Exception {
        String html = mvc.perform(get("/login").session(session)).andReturn().getResponse().getContentAsString();
        Matcher matcher = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"").matcher(html); assertTrue(matcher.find()); return matcher.group(1);
    }
    private String csrfRegister(MockHttpSession session) throws Exception {
        String html = mvc.perform(get("/register").session(session)).andReturn().getResponse().getContentAsString();
        Matcher matcher = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"").matcher(html); assertTrue(matcher.find()); return matcher.group(1);
    }
}
