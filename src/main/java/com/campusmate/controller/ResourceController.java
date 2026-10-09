package com.campusmate.controller;

import com.campusmate.model.Resource;
import com.campusmate.model.User;
import com.campusmate.service.ResourceService;
import com.campusmate.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ResourceController {

    private final ResourceService resourceService;
    private final UserService userService;

    public ResourceController(ResourceService resourceService, UserService userService) {
        this.resourceService = resourceService;
        this.userService = userService;
    }

    @GetMapping("/admin/resources")
    public String manageResources(Model model) {
        model.addAttribute("resources", resourceService.getAllResources());
        return "admin/resources";
    }

    @GetMapping("/admin/resources/new")
    public String newResource(Model model) {
        setResourceFormAttributes(model, new Resource(), "Add Resource", "/admin/resources");
        return "admin/resource-form";
    }

    @PostMapping("/admin/resources")
    public String createResource(@Valid @ModelAttribute("resource") Resource resource,
                                 BindingResult bindingResult,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        if (resource.getUrl() == null || resource.getUrl().isBlank()) bindingResult.rejectValue("url", "required", "URL is required.");
        if (bindingResult.hasErrors()) {
            setResourceFormAttributes(model, resource, "Add Resource", "/admin/resources");
            return "admin/resource-form";
        }

        resourceService.createResource(resource);
        redirectAttributes.addFlashAttribute("successMessage", "Resource created successfully.");
        return "redirect:/admin/resources";
    }

    @GetMapping("/admin/resources/edit/{id}")
    public String editResource(@PathVariable Long id, Model model) {
        setResourceFormAttributes(model, resourceService.getResourceById(id), "Edit Resource",
                "/admin/resources/update/" + id);
        return "admin/resource-form";
    }

    @PostMapping("/admin/resources/update/{id}")
    public String updateResource(@PathVariable Long id,
                                 @Valid @ModelAttribute("resource") Resource resource,
                                 BindingResult bindingResult,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        resource.setId(id);
        if (resource.getUrl() == null || resource.getUrl().isBlank()) bindingResult.rejectValue("url", "required", "URL is required.");
        if (bindingResult.hasErrors()) {
            setResourceFormAttributes(model, resource, "Edit Resource", "/admin/resources/update/" + id);
            return "admin/resource-form";
        }

        resourceService.updateResource(id, resource);
        redirectAttributes.addFlashAttribute("successMessage", "Resource updated successfully.");
        return "redirect:/admin/resources";
    }

    @PostMapping("/admin/resources/delete/{id}")
    public String deleteResource(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        resourceService.deleteResource(id);
        redirectAttributes.addFlashAttribute("successMessage", "Resource deleted successfully.");
        return "redirect:/admin/resources";
    }

    @GetMapping("/resources")
    public String viewResources(Authentication authentication, Model model) {
        User user = userService.findByEmail(authentication.getName()).orElse(null);
        model.addAttribute("resources", user != null && user.getRole() == User.Role.STUDENT
                ? resourceService.getStudentUrlResources(user) : resourceService.getAllResources());
        model.addAttribute("noteResources", user != null && user.getRole() == User.Role.STUDENT
                ? resourceService.getStudentFiles(user) : List.of());
        return "resources";
    }

    private void setResourceFormAttributes(Model model, Resource resource, String pageHeading, String resourceAction) {
        model.addAttribute("resource", resource);
        model.addAttribute("pageHeading", pageHeading);
        model.addAttribute("resourceAction", resourceAction);
    }
}
