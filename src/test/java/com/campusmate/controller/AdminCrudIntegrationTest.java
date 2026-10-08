package com.campusmate.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campusmate.model.Club;
import com.campusmate.model.Event;
import com.campusmate.model.Notice;
import com.campusmate.model.Timetable;
import com.campusmate.repository.ClubRepository;
import com.campusmate.repository.EventRepository;
import com.campusmate.repository.NoticeRepository;
import com.campusmate.repository.TimetableRepository;
import jakarta.persistence.EntityManager;
import java.time.DayOfWeek;
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
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class AdminCrudIntegrationTest {

    @Autowired private WebApplicationContext webApplicationContext;
    @Autowired private FilterChainProxy springSecurityFilterChain;
    @Autowired private NoticeRepository noticeRepository;
    @Autowired private EventRepository eventRepository;
    @Autowired private TimetableRepository timetableRepository;
    @Autowired private ClubRepository clubRepository;
    @Autowired private EntityManager entityManager;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilters(springSecurityFilterChain)
                .build();
    }

    @Test
    @Transactional
    void adminCanCreateEditAndDeleteNoticesEventsTimetableAndClubs() throws Exception {
        MockHttpSession adminSession = authenticatedAdminSession();
        String csrf = csrfToken(adminSession);
        String token = UUID.randomUUID().toString();

        String noticeTitle = "Audit notice " + token;
        mockMvc.perform(post("/admin/notices").session(adminSession).param("_csrf", csrf)
                        .param("title", noticeTitle).param("description", "Audit notice details")
                        .param("category", "Audit").param("date", LocalDate.now().toString()))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/admin/notices"));
        Notice notice = noticeRepository.findAllByOrderByDateDescIdDesc().stream()
                .filter(item -> noticeTitle.equals(item.getTitle())).findFirst().orElseThrow();
        mockMvc.perform(post("/admin/notices/update/{id}", notice.getId()).session(adminSession).param("_csrf", csrf)
                        .param("title", noticeTitle + " updated").param("description", "Updated notice details")
                        .param("category", "Audit updated").param("date", LocalDate.now().toString()))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/admin/notices"));
        assertTrue(mockMvc.perform(get("/notices").session(adminSession)).andReturn().getResponse()
                .getContentAsString().contains(noticeTitle + " updated"));
        mockMvc.perform(post("/admin/notices/delete/{id}", notice.getId()).session(adminSession).param("_csrf", csrf))
                .andExpect(status().is3xxRedirection());
        entityManager.flush();
        assertFalse(noticeRepository.findById(notice.getId()).isPresent());

        String eventTitle = "Audit event " + token;
        mockMvc.perform(post("/admin/events").session(adminSession).param("_csrf", csrf)
                        .param("title", eventTitle).param("description", "Audit event details")
                        .param("date", LocalDate.now().plusDays(2).toString()).param("time", "12:00")
                        .param("venue", "Audit hall").param("organizer", "Audit organizer"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/admin/events"));
        Event event = eventRepository.findAllByOrderByDateAscTimeAsc().stream()
                .filter(item -> eventTitle.equals(item.getTitle())).findFirst().orElseThrow();
        mockMvc.perform(post("/admin/events/update/{id}", event.getId()).session(adminSession).param("_csrf", csrf)
                        .param("title", eventTitle + " updated").param("description", "Updated event details")
                        .param("date", LocalDate.now().plusDays(3).toString()).param("time", "13:00")
                        .param("venue", "Updated hall").param("organizer", "Updated organizer"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/admin/events"));
        assertTrue(mockMvc.perform(get("/events").session(adminSession)).andReturn().getResponse()
                .getContentAsString().contains(eventTitle + " updated"));
        mockMvc.perform(post("/admin/events/delete/{id}", event.getId()).session(adminSession).param("_csrf", csrf))
                .andExpect(status().is3xxRedirection());
        entityManager.flush();
        assertFalse(eventRepository.findById(event.getId()).isPresent());

        String subject = "Audit subject " + token;
        mockMvc.perform(post("/admin/timetable").session(adminSession).param("_csrf", csrf)
                        .param("day", DayOfWeek.MONDAY.name()).param("subject", subject)
                        .param("faculty", "Audit faculty").param("room", "Audit room")
                        .param("startTime", "09:00").param("endTime", "10:00"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/admin/timetable"));
        Timetable timetable = timetableRepository.findAllByDayOrderByStartTimeAsc(DayOfWeek.MONDAY).stream()
                .filter(item -> subject.equals(item.getSubject())).findFirst().orElseThrow();
        mockMvc.perform(post("/admin/timetable/update/{id}", timetable.getId()).session(adminSession).param("_csrf", csrf)
                        .param("day", DayOfWeek.TUESDAY.name()).param("subject", subject + " updated")
                        .param("faculty", "Updated faculty").param("room", "Updated room")
                        .param("startTime", "10:00").param("endTime", "11:00"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/admin/timetable"));
        assertTrue(mockMvc.perform(get("/timetable").session(adminSession)).andReturn().getResponse()
                .getContentAsString().contains(subject + " updated"));
        mockMvc.perform(post("/admin/timetable/delete/{id}", timetable.getId()).session(adminSession).param("_csrf", csrf))
                .andExpect(status().is3xxRedirection());
        entityManager.flush();
        assertFalse(timetableRepository.findById(timetable.getId()).isPresent());

        String clubName = "Audit club " + token;
        mockMvc.perform(post("/admin/clubs").session(adminSession).param("_csrf", csrf)
                        .param("name", clubName).param("description", "Audit club details")
                        .param("facultyCoordinator", "Audit faculty").param("studentCoordinator", "Audit student")
                        .param("contact", "audit@example.test"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/admin/clubs"));
        Club club = clubRepository.findAllByOrderByNameAsc().stream()
                .filter(item -> clubName.equals(item.getName())).findFirst().orElseThrow();
        mockMvc.perform(post("/admin/clubs/update/{id}", club.getId()).session(adminSession).param("_csrf", csrf)
                        .param("name", clubName + " updated").param("description", "Updated club details")
                        .param("facultyCoordinator", "Updated faculty").param("studentCoordinator", "Updated student")
                        .param("contact", "updated-audit@example.test"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/admin/clubs"));
        assertTrue(mockMvc.perform(get("/clubs").session(adminSession)).andReturn().getResponse()
                .getContentAsString().contains(clubName + " updated"));
        mockMvc.perform(post("/admin/clubs/delete/{id}", club.getId()).session(adminSession).param("_csrf", csrf))
                .andExpect(status().is3xxRedirection());
        entityManager.flush();
        assertFalse(clubRepository.findById(club.getId()).isPresent());
    }

    private MockHttpSession authenticatedAdminSession() {
        var authentication = new UsernamePasswordAuthenticationToken(
                "functional-audit-admin@example.test", "unused", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                new SecurityContextImpl(authentication));
        return session;
    }

    private String csrfToken(MockHttpSession session) throws Exception {
        String html = mockMvc.perform(get("/login").session(session))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        Matcher matcher = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"").matcher(html);
        assertTrue(matcher.find(), "Expected a CSRF token.");
        return matcher.group(1);
    }
}
