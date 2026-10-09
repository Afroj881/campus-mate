package com.campusmate.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campusmate.model.Event;
import com.campusmate.model.Notice;
import com.campusmate.model.Task;
import com.campusmate.model.TaskSummary;
import com.campusmate.model.Timetable;
import com.campusmate.model.User;
import com.campusmate.repository.ClubRepository;
import com.campusmate.repository.EventRepository;
import com.campusmate.repository.NoticeRepository;
import com.campusmate.repository.ResourceRepository;
import com.campusmate.repository.TaskRepository;
import com.campusmate.repository.TimetableRepository;
import com.campusmate.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
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
class DashboardIntegrationTest {

    private MockMvc mockMvc;

    @Autowired private WebApplicationContext webApplicationContext;
    @Autowired private FilterChainProxy springSecurityFilterChain;
    @Autowired private UserRepository userRepository;
    @Autowired private TaskRepository taskRepository;
    @Autowired private TimetableRepository timetableRepository;
    @Autowired private NoticeRepository noticeRepository;
    @Autowired private EventRepository eventRepository;
    @Autowired private ClubRepository clubRepository;
    @Autowired private ResourceRepository resourceRepository;
    @Autowired private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilters(springSecurityFilterChain)
                .build();
    }

    @Test
    @Transactional
    void studentDashboardUsesLiveDataAndOnlyTheirTasks() throws Exception {
        User student = userRepository.save(createUser("student", User.Role.STUDENT));
        User otherStudent = userRepository.save(createUser("other", User.Role.STUDENT));

        taskRepository.save(createTask(student, "Dashboard owned pending task", Task.Status.PENDING));
        taskRepository.save(createTask(student, "Dashboard owned completed task", Task.Status.COMPLETED));
        taskRepository.save(createTask(otherStudent, "Dashboard private other task", Task.Status.PENDING));

        Timetable timetable = new Timetable();
        timetable.setDay(DayOfWeek.from(LocalDate.now()));
        timetable.setSubject("Dashboard today subject");
        timetable.setFaculty("Dashboard faculty");
        timetable.setRoom("Dashboard room");
        timetable.setStartTime(LocalTime.of(9, 0));
        timetable.setEndTime(LocalTime.of(10, 0));
        timetableRepository.save(timetable);

        Notice notice = new Notice();
        notice.setTitle("Dashboard latest notice");
        notice.setDescription("Dashboard notice body");
        notice.setCategory("Dashboard category");
        notice.setDate(LocalDate.now());
        noticeRepository.save(notice);

        Event futureEvent = createEvent("Dashboard future event", LocalDate.now().plusDays(1), LocalTime.of(10, 0));
        Event pastTodayEvent = createEvent("Dashboard past event", LocalDate.now(), LocalTime.now().minusHours(1));
        eventRepository.saveAll(List.of(futureEvent, pastTodayEvent));
        entityManager.flush();

        var dashboardResult = mockMvc.perform(get("/dashboard").session(authenticatedSession(student.getEmail(), "ROLE_STUDENT")))
                .andExpect(status().isOk())
            .andReturn();
        String html = dashboardResult.getResponse().getContentAsString();
        List<?> pendingTaskSummaries = (List<?>) dashboardResult.getModelAndView().getModel().get("pendingTasks");

        assertTrue(html.contains("Dashboard today subject"));
        assertTrue(html.contains("Dashboard faculty"));
        assertTrue(html.contains("Dashboard room"));
        assertTrue(html.contains("Dashboard owned pending task"));
        assertFalse(html.contains("Dashboard owned completed task"));
        assertFalse(html.contains("Dashboard private other task"));
        assertTrue(html.contains("Dashboard latest notice"));
        assertTrue(html.contains("Dashboard future event"));
        assertFalse(html.contains("href=\"/assignments\""));
        assertFalse(html.contains("No assignments for your class"));
        assertFalse(html.contains("Dashboard past event"));
        assertFalse(html.contains("unused-test-password"));
        assertTrue(pendingTaskSummaries.stream().allMatch(TaskSummary.class::isInstance));
    }

    @Test
    @Transactional
    void adminDashboardProvidesDatabaseCounts() throws Exception {
        mockMvc.perform(get("/dashboard/admin")
                        .session(authenticatedSession("admin-dashboard@example.test", "ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("studentCount", userRepository.countByRole(User.Role.STUDENT)))
                .andExpect(model().attribute("noticeCount", noticeRepository.count()))
                .andExpect(model().attribute("eventCount", eventRepository.count()))
                .andExpect(model().attribute("clubCount", clubRepository.count()))
                .andExpect(model().attribute("resourceCount", resourceRepository.count()));
    }

    private User createUser(String label, User.Role role) {
        User user = new User();
        user.setName("Dashboard " + label + " " + UUID.randomUUID());
        user.setEmail("dashboard-" + label + "-" + UUID.randomUUID() + "@example.test");
        user.setPassword("unused-test-password");
        user.setRole(role);
        user.setDepartment("Dashboard testing");
        user.setSemester(1);
        return user;
    }

    private Task createTask(User user, String title, Task.Status status) {
        Task task = new Task();
        task.setTitle(title);
        task.setDescription(title);
        task.setType("Assignment");
        task.setDeadline(LocalDate.now().plusDays(1));
        task.setStatus(status);
        task.setUser(user);
        return task;
    }

    private Event createEvent(String title, LocalDate date, LocalTime time) {
        Event event = new Event();
        event.setTitle(title);
        event.setDescription(title);
        event.setDate(date);
        event.setTime(time);
        event.setVenue("Dashboard venue");
        event.setOrganizer("Dashboard organizer");
        return event;
    }

    private MockHttpSession authenticatedSession(String email, String role) {
        var authentication = new UsernamePasswordAuthenticationToken(
                email, "not-used-by-test", List.of(new SimpleGrantedAuthority(role)));
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                new SecurityContextImpl(authentication));
        return session;
    }
}
