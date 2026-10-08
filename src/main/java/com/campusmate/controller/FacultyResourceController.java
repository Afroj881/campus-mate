package com.campusmate.controller;

import com.campusmate.model.Resource;
import com.campusmate.model.User;
import com.campusmate.repository.TeacherSubjectAssignmentRepository;
import com.campusmate.service.ResourceService;
import com.campusmate.service.UserService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.validation.Valid;

@Controller
@PreAuthorize("hasRole('FACULTY')")
public class FacultyResourceController {
    private final ResourceService resourceService;
    private final UserService userService;
    private final TeacherSubjectAssignmentRepository assignmentRepository;

    public FacultyResourceController(ResourceService resourceService, UserService userService,
                                     TeacherSubjectAssignmentRepository assignmentRepository) {
        this.resourceService = resourceService;
        this.userService = userService;
        this.assignmentRepository = assignmentRepository;
    }

    @GetMapping("/faculty/resources")
    public String resources(Authentication authentication, Model model) {
        User faculty = currentFaculty(authentication);
        model.addAttribute("resources", resourceService.getFacultyFiles(faculty));
        return "faculty/resources";
    }

    @GetMapping("/faculty/resources/new")
    public String newResource(Authentication authentication, Model model) {
        User faculty = currentFaculty(authentication);
        model.addAttribute("assignments", assignmentRepository.findByFacultyIdWithSubject(faculty.getId()));
        model.addAttribute("resourceForm", new UploadForm());
        return "faculty/resource-form";
    }

    @PostMapping("/faculty/resources")
    public String upload(Authentication authentication, @Valid @ModelAttribute("resourceForm") UploadForm form,
                         BindingResult bindingResult, @RequestParam(value = "file", required = false) MultipartFile file, Model model,
                         RedirectAttributes redirect) {
        User faculty = currentFaculty(authentication);
        if (file == null || file.isEmpty()) bindingResult.rejectValue("title", "file.required", "Choose a non-empty file.");
        if (bindingResult.hasErrors()) return formPage(faculty, model);
        try {
            resourceService.uploadFileForAssignmentLabel(faculty, form.getSubjectAndClass(), form.getTitle(), form.getDescription(), file);
        } catch (IllegalArgumentException ex) {
            model.addAttribute("uploadError", ex.getMessage());
            return formPage(faculty, model);
        }
        redirect.addFlashAttribute("successMessage", "Notes uploaded successfully.");
        return "redirect:/faculty/resources";
    }

    @PostMapping("/faculty/resources/{id}/delete")
    public String delete(Authentication authentication, @PathVariable Long id, RedirectAttributes redirect) {
        resourceService.deleteFacultyFile(id, currentFaculty(authentication));
        redirect.addFlashAttribute("successMessage", "Notes deleted successfully.");
        return "redirect:/faculty/resources";
    }

    private String formPage(User faculty, Model model) {
        model.addAttribute("assignments", assignmentRepository.findByFacultyIdWithSubject(faculty.getId()));
        if (!model.containsAttribute("resourceForm")) model.addAttribute("resourceForm", new UploadForm());
        return "faculty/resource-form";
    }

    private User currentFaculty(Authentication authentication) {
        return userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Faculty not found."));
    }

    public static class UploadForm {
        @NotBlank(message = "Title is required.")
        @Size(max = 160, message = "Title must be 160 characters or fewer.")
        private String title;
        @Size(max = 2000, message = "Description must be 2,000 characters or fewer.")
        private String description;
        @NotBlank(message = "Subject and class is required.")
        private String subjectAndClass;
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getSubjectAndClass() { return subjectAndClass; }
        public void setSubjectAndClass(String subjectAndClass) { this.subjectAndClass = subjectAndClass == null ? null : subjectAndClass.trim(); }
    }
}
