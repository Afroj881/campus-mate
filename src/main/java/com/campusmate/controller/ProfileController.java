package com.campusmate.controller;

import com.campusmate.model.ProfileForm;
import org.springframework.security.access.prepost.PreAuthorize;
import com.campusmate.model.ProfileDetails;
import com.campusmate.model.User;
import com.campusmate.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@PreAuthorize("hasRole('STUDENT')")
public class ProfileController {

    private final UserService userService;

    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/profile")
    public String profile(Authentication authentication, Model model) {
        User user = getCurrentUser(authentication);
        model.addAttribute("profileDetails", new ProfileDetails(user));
        model.addAttribute("profileForm", toProfileForm(user));
        return "profile";
    }

    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/profile")
    public String updateProfile(Authentication authentication,
                                @Valid @ModelAttribute("profileForm") ProfileForm profileForm,
                                BindingResult bindingResult,
                                Model model,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        User currentUser = getCurrentUser(authentication);
        if (bindingResult.hasErrors()) {
            model.addAttribute("profileDetails", new ProfileDetails(currentUser));
            return "profile";
        }

        User updatedUser = userService.updateStudentProfile(authentication.getName(), profileForm);
        session.setAttribute("userName", updatedUser.getName());
        redirectAttributes.addFlashAttribute("successMessage", "Profile updated successfully.");
        return "redirect:/profile";
    }

    private User getCurrentUser(Authentication authentication) {
        return userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private ProfileForm toProfileForm(User user) {
        ProfileForm profileForm = new ProfileForm();
        profileForm.setName(user.getName());
        profileForm.setDepartment(user.getDepartment());
        profileForm.setSemester(user.getSemester());
        profileForm.setSection(user.getSection());
        return profileForm;
    }
}
