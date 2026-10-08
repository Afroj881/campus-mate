package com.campusmate.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campusmate.model.Club;
import com.campusmate.model.Event;
import com.campusmate.model.Notice;
import com.campusmate.model.Resource;
import com.campusmate.model.Task;
import com.campusmate.model.User;
import com.campusmate.repository.ClubRepository;
import com.campusmate.repository.EventRepository;
import com.campusmate.repository.NoticeRepository;
import com.campusmate.repository.ResourceRepository;
import com.campusmate.repository.TaskRepository;
import com.campusmate.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class SearchIntegrationTest {

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
    private NoticeRepository noticeRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private ResourceRepository resourceRepository;

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
    void searchesPublicContentCaseInsensitivelyWithoutExposingPrivateData() throws Exception {
        String token = "MiXeD-" + UUID.randomUUID();
        String privatePassword = "Private-password-" + token;
        User student = userRepository.save(createStudent(token, privatePassword));

        Task privateTask = new Task();
        privateTask.setTitle("Private task " + token);
        privateTask.setDescription("Private task description " + token);
        privateTask.setType("Assignment");
        privateTask.setDeadline(LocalDate.now().plusDays(1));
        privateTask.setStatus(Task.Status.PENDING);
        privateTask.setUser(student);
        taskRepository.save(privateTask);

        Notice notice = new Notice();
        notice.setTitle("Search result notice " + token);
        notice.setDescription("Public notice description");
        notice.setCategory("Category " + token);
        notice.setDate(LocalDate.now());
        noticeRepository.save(notice);

        Event event = new Event();
        event.setTitle("Search result event " + token);
        event.setDescription("Public event description");
        event.setDate(LocalDate.now().plusDays(1));
        event.setTime(LocalTime.of(12, 0));
        event.setVenue("Public venue");
        event.setOrganizer("Organizer " + token);
        eventRepository.save(event);

        Club club = new Club();
        club.setName("Search result club " + token);
        club.setDescription("Public club description");
        club.setFacultyCoordinator("Faculty coordinator");
        club.setStudentCoordinator("Student coordinator " + token);
        club.setContact("club@example.test");
        clubRepository.save(club);

        Resource resource = new Resource();
        resource.setTitle("Search result resource " + token);
        resource.setSubject("Subject " + token);
        resource.setDescription("Public resource description");
        resource.setUrl("https://example.test/resource");
        resourceRepository.save(resource);

        Resource classResource = new Resource();
        classResource.setTitle("Scoped resource " + token);
        classResource.setSubject("Scoped subject " + token);
        classResource.setDescription("Scoped resource description");
        classResource.setUrl("https://example.test/scoped-resource");
        classResource.setDepartment("Computer Science");
        classResource.setSemester(3);
        classResource.setSection("B");
        resourceRepository.save(classResource);

        User matchingStudent = createStudent(token + "-matching", privatePassword);
        matchingStudent.setDepartment("Computer Science");
        matchingStudent.setSection("B");
        userRepository.save(matchingStudent);
        entityManager.flush();

        String html = mockMvc.perform(get("/search")
                        .param("query", token.toLowerCase(Locale.ROOT))
                        .session(authenticatedSession(student.getEmail())))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertTrue(html.contains("Notice"));
        assertTrue(html.contains("Search result notice " + token));
        assertTrue(html.contains("href=\"/notices\""));
        assertTrue(html.contains("Event"));
        assertTrue(html.contains("Search result event " + token));
        assertTrue(html.contains("href=\"/events\""));
        assertTrue(html.contains("Club"));
        assertTrue(html.contains("Search result club " + token));
        assertTrue(html.contains("href=\"/clubs\""));
        assertTrue(html.contains("Resource"));
        assertTrue(html.contains("Search result resource " + token));
        assertTrue(html.contains("href=\"/resources\""));
        assertTrue(html.contains("href=\"https://example.test/resource\""));
        assertTrue(html.contains("target=\"_blank\""));
        assertTrue(html.contains("rel=\"noopener noreferrer\""));
        assertFalse(html.contains("Scoped resource " + token));
        assertFalse(html.contains("Private task " + token));
        assertFalse(html.contains("Private task description " + token));
        assertFalse(html.contains("Private profile " + token));
        assertFalse(html.contains(student.getEmail()));
        assertFalse(html.contains(privatePassword));
        assertFalse(html.contains("/admin/"));

        String emptyQueryHtml = mockMvc.perform(get("/search")
                        .session(authenticatedSession(student.getEmail())))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertTrue(emptyQueryHtml.contains("Please enter a search term."));

        String noResultsHtml = mockMvc.perform(get("/search")
                        .param("query", "No-match-" + UUID.randomUUID())
                        .session(authenticatedSession(student.getEmail())))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertTrue(noResultsHtml.contains("No results found."));

        String matchingClassHtml = mockMvc.perform(get("/search")
                        .param("query", "Scoped resource " + token)
                        .session(authenticatedSession(matchingStudent.getEmail())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(matchingClassHtml.contains("Scoped resource " + token));
        assertTrue(matchingClassHtml.contains("href=\"https://example.test/scoped-resource\""));
    }

    private User createStudent(String token, String password) {
        User user = new User();
        user.setName("Private profile " + token);
        user.setEmail("private-" + token + "@example.test");
        user.setPassword(password);
        user.setRole(User.Role.STUDENT);
        user.setDepartment("Private department " + token);
        user.setSemester(3);
        return user;
    }

    private MockHttpSession authenticatedSession(String email) {
        var authentication = new UsernamePasswordAuthenticationToken(
                email,
                "not-used-by-test",
                List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                new SecurityContextImpl(authentication));
        return session;
    }
}
