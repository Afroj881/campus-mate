package com.campusmate.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campusmate.model.AcademicCalendarEntry;
import com.campusmate.model.AcademicCalendarEventType;
import com.campusmate.repository.AcademicCalendarRepository;
import com.campusmate.service.AcademicCalendarService;
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
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class AcademicCalendarIntegrationTest {

    @Autowired private WebApplicationContext webApplicationContext;
    @Autowired private FilterChainProxy springSecurityFilterChain;
    @Autowired private AcademicCalendarRepository repository;
    @Autowired private AcademicCalendarService calendarService;
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
    void adminCanViewCreateEditAndDeleteCalendarEntries() throws Exception {
        MockHttpSession admin = authenticatedSession("ROLE_ADMIN");
        String csrf = csrfToken(admin);
        String unique = UUID.randomUUID().toString();
        String title = "Calendar exam " + unique;

        mockMvc.perform(get("/admin/calendar").session(admin))
                .andExpect(status().isOk());
        mockMvc.perform(post("/admin/calendar").session(admin).param("_csrf", csrf)
                        .param("title", title).param("description", "Mid semester examination period.")
                        .param("eventType", "EXAM").param("eventDate", "2026-09-10")
                        .param("endDate", "2026-09-20"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/admin/calendar"));

        AcademicCalendarEntry saved = repository.findAll().stream()
                .filter(entry -> title.equals(entry.getTitle())).findFirst().orElseThrow();
        Long id = saved.getId();
        assertEquals(LocalDate.of(2026, 9, 20), saved.getEndDate());
        assertTrue(saved.getCreatedAt() != null);
        assertTrue(saved.getUpdatedAt() != null);
        MvcResult adminList = mockMvc.perform(get("/admin/calendar").session(admin))
                .andExpect(status().isOk()).andReturn();
        assertTrue(adminList.getResponse().getContentAsString().contains(title));

        mockMvc.perform(get("/admin/calendar/edit/{id}", id).session(admin))
                .andExpect(status().isOk());
        mockMvc.perform(post("/admin/calendar/update/{id}", id).session(admin).param("_csrf", csrf)
                        .param("title", title + " updated").param("description", "Updated examination dates.")
                        .param("eventType", "IMPORTANT_DATE").param("eventDate", "2026-09-11"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/admin/calendar"));
        entityManager.flush();
        entityManager.clear();
        AcademicCalendarEntry updated = repository.findById(id).orElseThrow();
        assertEquals(title + " updated", updated.getTitle());
        assertEquals(AcademicCalendarEventType.IMPORTANT_DATE, updated.getEventType());
        assertEquals(null, updated.getEndDate());

        mockMvc.perform(post("/admin/calendar/delete/{id}", id).session(admin).param("_csrf", csrf))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/admin/calendar"));
        entityManager.flush();
        assertFalse(repository.findById(id).isPresent());
    }

    @Test
    @Transactional
    void studentsAndFacultyCanViewCalendarButCannotManageIt() throws Exception {
        String title = "Read only date " + UUID.randomUUID();
        AcademicCalendarEntry entry = repository.save(validEntry(title, LocalDate.of(2026, 8, 15)));
        Long id = entry.getId();

        for (String role : List.of("ROLE_STUDENT", "ROLE_FACULTY")) {
            MockHttpSession session = authenticatedSession(role);
            String csrf = csrfToken(session);
            mockMvc.perform(get("/calendar").session(session))
                    .andExpect(status().isOk());
            mockMvc.perform(get("/admin/calendar").session(session))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get("/admin/calendar/new").session(session))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get("/admin/calendar/edit/{id}", id).session(session))
                    .andExpect(status().isForbidden());
            mockMvc.perform(post("/admin/calendar").session(session).param("_csrf", csrf)
                            .param("title", "Unauthorized " + role).param("description", "Must not persist.")
                            .param("eventType", "HOLIDAY").param("eventDate", "2026-08-15"))
                    .andExpect(status().isForbidden());
            mockMvc.perform(post("/admin/calendar/update/{id}", id).session(session).param("_csrf", csrf)
                            .param("title", "Unauthorized edit").param("description", "Must not persist.")
                            .param("eventType", "HOLIDAY").param("eventDate", "2026-08-15"))
                    .andExpect(status().isForbidden());
            mockMvc.perform(post("/admin/calendar/delete/{id}", id).session(session).param("_csrf", csrf))
                    .andExpect(status().isForbidden());
        }
        assertTrue(repository.findById(id).isPresent());

        mockMvc.perform(get("/calendar")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        mockMvc.perform(get("/admin/calendar")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
    }

    @Test
    @Transactional
    void requiredFieldsAndEndDateRangeAreValidated() throws Exception {
        MockHttpSession admin = authenticatedSession("ROLE_ADMIN");
        String csrf = csrfToken(admin);
        MvcResult required = mockMvc.perform(post("/admin/calendar").session(admin).param("_csrf", csrf))
                .andExpect(status().isOk()).andReturn();
        String requiredHtml = required.getResponse().getContentAsString();
        assertTrue(requiredHtml.contains("Title is required."));
        assertTrue(requiredHtml.contains("Description is required."));
        assertTrue(requiredHtml.contains("Start date is required."));
        assertTrue(requiredHtml.contains("Event type is required."));

        String title = "Invalid range " + UUID.randomUUID();
        MvcResult invalidRange = mockMvc.perform(post("/admin/calendar").session(admin).param("_csrf", csrf)
                        .param("title", title).param("description", "Invalid date range.")
                        .param("eventType", "EXAM").param("eventDate", "2026-09-20")
                        .param("endDate", "2026-09-10"))
                .andExpect(status().isOk()).andReturn();
        assertTrue(invalidRange.getResponse().getContentAsString()
                .contains("End date cannot be before the start date."));
        assertTrue(repository.findAll().stream().noneMatch(entry -> title.equals(entry.getTitle())));
    }

    @Test
    @Transactional
    void calendarEntriesAreOrderedByDateThenTitle() throws Exception {
        String suffix = UUID.randomUUID().toString();
        AcademicCalendarEntry later = repository.save(validEntry("Later " + suffix, LocalDate.of(2027, 1, 10)));
        AcademicCalendarEntry sameDateZ = repository.save(validEntry("Zulu " + suffix, LocalDate.of(2026, 9, 10)));
        AcademicCalendarEntry earlier = repository.save(validEntry("Earlier " + suffix, LocalDate.of(2026, 8, 15)));
        AcademicCalendarEntry sameDateA = repository.save(validEntry("Alpha " + suffix, LocalDate.of(2026, 9, 10)));

        List<AcademicCalendarEntry> ordered = calendarService.getAllEntries().stream()
                .filter(entry -> entry.getTitle().endsWith(suffix)).toList();
        assertEquals(List.of(earlier.getId(), sameDateA.getId(), sameDateZ.getId(), later.getId()),
                ordered.stream().map(AcademicCalendarEntry::getId).toList());

        MvcResult page = mockMvc.perform(get("/calendar").session(authenticatedSession("ROLE_STUDENT")))
                .andExpect(status().isOk()).andReturn();
        String html = page.getResponse().getContentAsString();
        assertTrue(html.indexOf("Earlier " + suffix) < html.indexOf("Alpha " + suffix));
        assertTrue(html.indexOf("Alpha " + suffix) < html.indexOf("Zulu " + suffix));
        assertTrue(html.indexOf("Zulu " + suffix) < html.indexOf("Later " + suffix));
        assertTrue(html.contains("15 August 2026"));
    }

    private AcademicCalendarEntry validEntry(String title, LocalDate eventDate) {
        AcademicCalendarEntry entry = new AcademicCalendarEntry();
        entry.setTitle(title);
        entry.setDescription("Calendar entry details.");
        entry.setEventDate(eventDate);
        entry.setEventType(AcademicCalendarEventType.IMPORTANT_DATE);
        return entry;
    }

    private MockHttpSession authenticatedSession(String role) {
        var authentication = new UsernamePasswordAuthenticationToken(
                "calendar-test@example.test", "unused", List.of(new SimpleGrantedAuthority(role)));
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
