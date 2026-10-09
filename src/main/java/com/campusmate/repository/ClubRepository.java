package com.campusmate.repository;

import com.campusmate.model.Club;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClubRepository extends JpaRepository<Club, Long> {

    List<Club> findAllByOrderByNameAsc();

    List<Club> findAllByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrFacultyCoordinatorContainingIgnoreCaseOrStudentCoordinatorContainingIgnoreCase(
            String name, String description, String facultyCoordinator, String studentCoordinator);
}