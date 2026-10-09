package com.campusmate.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.campusmate.model.Resource;
import com.campusmate.repository.ResourceRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
class ResourceServiceIntegrationTest {

    @Autowired
    private ResourceService resourceService;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @Transactional
    void persistsAndPerformsResourceCrud() {
        Resource createdResource = resourceService.createResource(createResource("Calculus Notes"));
        Long id = createdResource.getId();
        assertNotNull(id);

        entityManager.flush();
        entityManager.clear();
        assertEquals("Calculus Notes", resourceService.getResourceById(id).getTitle());

        Resource update = createResource("Updated Calculus Notes");
        update.setSubject("Advanced Mathematics");
        assertEquals("Updated Calculus Notes", resourceService.updateResource(id, update).getTitle());
        entityManager.flush();
        entityManager.clear();
        assertEquals("Advanced Mathematics", resourceService.getResourceById(id).getSubject());
        assertTrue(resourceService.getAllResources().stream().anyMatch(resource -> id.equals(resource.getId())));

        resourceService.deleteResource(id);
        entityManager.flush();
        entityManager.clear();
        assertFalse(resourceRepository.findById(id).isPresent());
    }

    private Resource createResource(String title) {
        Resource resource = new Resource();
        resource.setTitle(title);
        resource.setSubject("Mathematics");
        resource.setDescription("A resource used to verify database persistence.");
        resource.setUrl("https://example.test/calculus");
        return resource;
    }
}
