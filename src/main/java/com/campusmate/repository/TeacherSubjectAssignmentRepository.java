package com.campusmate.repository;

import com.campusmate.model.Subject;
import com.campusmate.model.TeacherSubjectAssignment;
import com.campusmate.model.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TeacherSubjectAssignmentRepository extends JpaRepository<TeacherSubjectAssignment, Long> {

    @Query("select assignment from TeacherSubjectAssignment assignment "
            + "join fetch assignment.faculty join fetch assignment.subject order by assignment.id")
    List<TeacherSubjectAssignment> findAllWithFacultyAndSubject();

    @Query("select assignment from TeacherSubjectAssignment assignment "
            + "join fetch assignment.subject where assignment.faculty = :faculty "
            + "order by assignment.semester, assignment.subject.name")
    List<TeacherSubjectAssignment> findByFacultyWithSubject(User faculty);

    @Query("select assignment from TeacherSubjectAssignment assignment join fetch assignment.subject "
            + "where assignment.faculty.id = :facultyId order by assignment.semester, assignment.subject.name")
    List<TeacherSubjectAssignment> findByFacultyIdWithSubject(Long facultyId);

    List<TeacherSubjectAssignment> findByFacultyOrderBySemesterAscSubjectNameAsc(User faculty);

    List<TeacherSubjectAssignment> findByFaculty(User faculty);

    Optional<TeacherSubjectAssignment> findByFacultyAndSubjectAndSemesterAndSection(User faculty, Subject subject, Integer semester, String section);

    boolean existsByFacultyAndSubjectAndSemesterAndSection(User faculty, Subject subject, Integer semester, String section);
}
