package com.campusmate.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.campusmate.model.Club;
import com.campusmate.repository.ClubRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
class ClubServiceIntegrationTest {

    @Autowired
    private ClubService clubService;

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @Transactional
    void persistsAndPerformsClubCrud() {
        Club submittedClub = createClub("Campus Coding Club");
        Club createdClub = clubService.createClub(submittedClub);
        Long id = createdClub.getId();
        assertNotNull(id);

        entityManager.flush();
        entityManager.clear();
        assertEquals("Campus Coding Club", clubService.getClubById(id).getName());

        Club update = createClub("Updated Coding Club");
        assertEquals("Updated Coding Club", clubService.updateClub(id, update).getName());
        entityManager.flush();
        entityManager.clear();
        assertEquals("Updated Coding Club", clubService.getClubById(id).getName());
        assertTrue(clubService.getAllClubs().stream().anyMatch(club -> id.equals(club.getId())));

        clubService.deleteClub(id);
        entityManager.flush();
        entityManager.clear();
        assertFalse(clubRepository.findById(id).isPresent());
    }

    private Club createClub(String name) {
        Club club = new Club();
        club.setName(name);
        club.setDescription("A club used to verify database persistence.");
        club.setFacultyCoordinator("Faculty Coordinator");
        club.setStudentCoordinator("Student Coordinator");
        club.setContact("clubs@example.test");
        return club;
    }
}