package com.campusmate.service;

import com.campusmate.model.Subject;
import com.campusmate.model.TeacherSubjectAssignment;
import com.campusmate.model.User;
import com.campusmate.repository.TeacherSubjectAssignmentRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TeacherSubjectAssignmentService {

    private final TeacherSubjectAssignmentRepository teacherSubjectAssignmentRepository;

    public TeacherSubjectAssignmentService(TeacherSubjectAssignmentRepository teacherSubjectAssignmentRepository) {
        this.teacherSubjectAssignmentRepository = teacherSubjectAssignmentRepository;
    }

    @Transactional(readOnly = true)
    public List<TeacherSubjectAssignment> getAssignmentsForFaculty(User faculty) {
        return teacherSubjectAssignmentRepository.findByFacultyOrderBySemesterAscSubjectNameAsc(faculty);
    }

    @Transactional(readOnly = true)
    public boolean canFacultyManageClass(User faculty, Subject subject, Integer semester, String section) {
        if (faculty == null || subject == null || semester == null || section == null || section.isBlank()) {
            return false;
        }
        return teacherSubjectAssignmentRepository.existsByFacultyAndSubjectAndSemesterAndSection(faculty, subject, semester, section.trim());
    }

    @Transactional(readOnly = true)
    public Optional<TeacherSubjectAssignment> getAssignment(User faculty, Subject subject, Integer semester, String section) {
        return teacherSubjectAssignmentRepository.findByFacultyAndSubjectAndSemesterAndSection(faculty, subject, semester, section);
    }

    @Transactional
    public TeacherSubjectAssignment createAssignment(User faculty, Subject subject, String department, Integer semester, String section) {
        if (!canFacultyManageClass(faculty, subject, semester, section)) {
            throw new IllegalArgumentException("This faculty member is not assigned to the selected subject and class.");
        }
        TeacherSubjectAssignment assignment = new TeacherSubjectAssignment();
        assignment.setFaculty(faculty);
        assignment.setSubject(subject);
        assignment.setDepartment(department == null ? subject.getDepartment() : department.trim());
        assignment.setSemester(semester);
        assignment.setSection(section == null ? "A" : section.trim().toUpperCase());
        return teacherSubjectAssignmentRepository.save(assignment);
    }

    @Transactional
    public TeacherSubjectAssignment updateAssignment(Long id, User faculty, Subject subject, String department, Integer semester, String section) {
        TeacherSubjectAssignment existing = teacherSubjectAssignmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found."));
        if (!existing.getFaculty().getId().equals(faculty.getId())) {
            throw new IllegalStateException("You cannot modify another faculty member's assignment.");
        }
        if (!canFacultyManageClass(faculty, subject, semester, section)) {
            throw new IllegalArgumentException("This faculty member is not assigned to the selected subject and class.");
        }
        existing.setSubject(subject);
        existing.setDepartment(department == null ? subject.getDepartment() : department.trim());
        existing.setSemester(semester);
        existing.setSection(section == null ? "A" : section.trim().toUpperCase());
        return teacherSubjectAssignmentRepository.save(existing);
    }

    @Transactional
    public void deleteAssignment(Long id, User faculty) {
        TeacherSubjectAssignment assignment = teacherSubjectAssignmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found."));
        if (!assignment.getFaculty().getId().equals(faculty.getId())) {
            throw new IllegalStateException("You cannot delete another faculty member's assignment.");
        }
        teacherSubjectAssignmentRepository.delete(assignment);
    }
}
