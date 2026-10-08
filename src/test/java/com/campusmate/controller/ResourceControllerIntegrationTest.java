package com.campusmate.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campusmate.model.Resource;
import com.campusmate.repository.ResourceRepository;
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
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class ResourceControllerIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private FilterChainProxy springSecurityFilterChain;

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
    void adminCanManageResourcesWhileStudentsCanOnlyViewThem() throws Exception {
        MockHttpSession adminSession = authenticatedSession("ROLE_ADMIN");
        String adminCsrf = csrfToken("/admin/resources/new", adminSession);
        String title = "Resource " + UUID.randomUUID();

        MvcResult unsafeUrlForm = mockMvc.perform(post("/admin/resources")
                        .session(adminSession)
                        .param("_csrf", adminCsrf)
                        .param("title", title)
                        .param("subject", "Mathematics")
                        .param("description", "Must not become an executable link.")
                        .param("url", "javascript:alert(1)"))
                .andExpect(status().isOk())
                .andReturn();
        assertTrue(unsafeUrlForm.getResponse().getContentAsString()
                .contains("Enter a valid HTTP or HTTPS URL."));
        assertTrue(resourceRepository.findAll().stream().noneMatch(resource -> title.equals(resource.getTitle())));

        MvcResult invalidForm = mockMvc.perform(post("/admin/resources")
                        .session(adminSession)
                        .param("_csrf", adminCsrf)
                        .param("title", "")
                        .param("subject", "")
                        .param("description", "")
                        .param("url", ""))
                .andExpect(status().isOk())
                .andReturn();
        String invalidFormHtml = invalidForm.getResponse().getContentAsString();
        assertTrue(invalidFormHtml.contains("Title is required."));
        assertTrue(invalidFormHtml.contains("Subject is required."));
        assertTrue(invalidFormHtml.contains("Description is required."));
        assertTrue(invalidFormHtml.contains("URL is required."));

        mockMvc.perform(post("/admin/resources")
                        .session(adminSession)
                        .param("_csrf", adminCsrf)
                        .param("title", title)
                        .param("subject", "Mathematics")
                        .param("description", "Lecture notes for calculus.")
                        .param("url", "https://example.test/calculus"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/resources"));

        Resource resource = resourceRepository.findAll().stream()
                .filter(candidate -> title.equals(candidate.getTitle()))
                .findFirst()
                .orElseThrow();
        Long resourceId = resource.getId();

        mockMvc.perform(get("/admin/resources/edit/{id}", resourceId).session(adminSession))
                .andExpect(status().isOk());
        mockMvc.perform(post("/admin/resources/update/{id}", resourceId)
                        .session(adminSession)
                        .param("_csrf", adminCsrf)
                        .param("title", title + " Updated")
                        .param("subject", "Advanced Mathematics")
                        .param("description", "Updated lecture notes.")
                        .param("url", "https://example.test/advanced-calculus"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/resources"));

        entityManager.flush();
        entityManager.clear();
        String updatedTitle = title + " Updated";
        assertTrue(resourceRepository.findById(resourceId)
                .map(saved -> updatedTitle.equals(saved.getTitle()))
                .orElse(false));
        MvcResult adminList = mockMvc.perform(get("/admin/resources").session(adminSession))
                .andExpect(status().isOk())
                .andReturn();
        assertTrue(adminList.getResponse().getContentAsString().contains(updatedTitle));

        MockHttpSession studentSession = authenticatedSession("ROLE_STUDENT");
        String studentCsrf = csrfToken("/login", studentSession);
        MvcResult studentPage = mockMvc.perform(get("/resources").session(studentSession))
                .andExpect(status().isOk())
                .andReturn();
        String studentHtml = studentPage.getResponse().getContentAsString();
        assertTrue(studentHtml.contains(updatedTitle));
        assertTrue(studentHtml.contains("target=\"_blank\""));
        assertTrue(studentHtml.contains("rel=\"noopener noreferrer\""));
        assertFalse(studentHtml.contains("Add Resource") || studentHtml.contains("Delete"));

        mockMvc.perform(get("/admin/resources").session(studentSession))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/resources/new").session(studentSession))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/resources/edit/{id}", resourceId).session(studentSession))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/admin/resources")
                        .session(studentSession)
                        .param("_csrf", studentCsrf)
                        .param("title", "Unauthorized")
                        .param("subject", "Mathematics")
                        .param("description", "Must not be created.")
                        .param("url", "https://example.test/unauthorized"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/admin/resources/update/{id}", resourceId)
                        .session(studentSession)
                        .param("_csrf", studentCsrf))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/admin/resources/delete/{id}", resourceId)
                        .session(studentSession)
                        .param("_csrf", studentCsrf))
                .andExpect(status().isForbidden());

        MockHttpSession anonymousSession = new MockHttpSession();
        String anonymousCsrf = csrfToken("/login", anonymousSession);
        for (String route : List.of(
                "/admin/resources",
                "/admin/resources/new",
                "/admin/resources/edit/" + resourceId)) {
            mockMvc.perform(get(route))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/login"));
        }
        for (String route : List.of(
                "/admin/resources",
                "/admin/resources/update/" + resourceId,
                "/admin/resources/delete/" + resourceId)) {
            mockMvc.perform(post(route)
                            .session(anonymousSession)
                            .param("_csrf", anonymousCsrf))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/login"));
        }

        mockMvc.perform(post("/admin/resources/delete/{id}", resourceId)
                        .session(adminSession)
                        .param("_csrf", adminCsrf))
                .andExpect(status().is3xxRedirection());
        entityManager.flush();
        entityManager.clear();
        assertTrue(resourceRepository.findById(resourceId).isEmpty());
    }

    private MockHttpSession authenticatedSession(String role) {
        var authentication = new UsernamePasswordAuthenticationToken(
                "resources-test@example.test",
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
