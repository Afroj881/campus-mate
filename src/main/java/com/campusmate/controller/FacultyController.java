package com.campusmate.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import com.campusmate.model.User;
import com.campusmate.repository.TeacherSubjectAssignmentRepository;
import com.campusmate.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ResponseStatusException;

@Controller
@PreAuthorize("hasRole('FACULTY')")
public class FacultyController {

    private final UserService userService;
    private final TeacherSubjectAssignmentRepository teacherSubjectAssignmentRepository;

    public FacultyController(UserService userService,
                            TeacherSubjectAssignmentRepository teacherSubjectAssignmentRepository) {
        this.userService = userService;
        this.teacherSubjectAssignmentRepository = teacherSubjectAssignmentRepository;
    }

    @PreAuthorize("hasRole('FACULTY')")
    @GetMapping({"/faculty", "/faculty/"})
    public String facultyRoot() {
        return "redirect:/faculty/dashboard";
    }

    @PreAuthorize("hasRole('FACULTY')")
    @GetMapping("/faculty/dashboard")
    public String facultyDashboard(Authentication authentication, Model model) {
        User faculty = getCurrentFaculty(authentication);
        model.addAttribute("facultyName", faculty.getName());
        model.addAttribute("department", faculty.getDepartment());
        model.addAttribute("facultyAssignments", teacherSubjectAssignmentRepository.findByFacultyIdWithSubject(faculty.getId()));
        return "faculty/dashboard";
    }

    private User getCurrentFaculty(Authentication authentication) {
        return userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Faculty not found."));
    }
}
