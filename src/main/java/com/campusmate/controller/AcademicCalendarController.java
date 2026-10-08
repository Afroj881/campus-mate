package com.campusmate.controller;

import com.campusmate.model.AcademicCalendarEntry;
import com.campusmate.model.AcademicCalendarEventType;
import com.campusmate.service.AcademicCalendarService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.Arrays;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AcademicCalendarController {

    private final AcademicCalendarService calendarService;

    public AcademicCalendarController(AcademicCalendarService calendarService) {
        this.calendarService = calendarService;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'STUDENT', 'FACULTY')")
    @GetMapping("/calendar")
    public String calendar(Authentication authentication, Model model) {
        model.addAttribute("calendarEntries", calendarService.getAllEntries());
        model.addAttribute("today", LocalDate.now());
        String role = authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring(5))
                .findFirst().orElse("STUDENT");
        model.addAttribute("dashboardUrl", switch (role) {
            case "ADMIN" -> "/dashboard/admin";
            case "FACULTY" -> "/faculty/dashboard";
            default -> "/dashboard";
        });
        return "calendar";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/calendar")
    public String manageCalendar(Model model) {
        model.addAttribute("calendarEntries", calendarService.getAllEntries());
        return "admin/calendar";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/calendar/new")
    public String newEntry(Model model) {
        setForm(model, new AcademicCalendarEntry(), "Add Calendar Date", "/admin/calendar");
        return "admin/calendar-form";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/calendar")
    public String createEntry(@Valid @ModelAttribute("calendarEntry") AcademicCalendarEntry entry,
                              BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            setForm(model, entry, "Add Calendar Date", "/admin/calendar");
            return "admin/calendar-form";
        }
        calendarService.createEntry(entry);
        redirectAttributes.addFlashAttribute("successMessage", "Academic calendar date added.");
        return "redirect:/admin/calendar";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/calendar/edit/{id}")
    public String editEntry(@PathVariable Long id, Model model) {
        setForm(model, calendarService.getEntry(id), "Edit Calendar Date", "/admin/calendar/update/" + id);
        return "admin/calendar-form";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/calendar/update/{id}")
    public String updateEntry(@PathVariable Long id,
                              @Valid @ModelAttribute("calendarEntry") AcademicCalendarEntry entry,
                              BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        entry.setId(id);
        if (bindingResult.hasErrors()) {
            setForm(model, entry, "Edit Calendar Date", "/admin/calendar/update/" + id);
            return "admin/calendar-form";
        }
        calendarService.updateEntry(id, entry);
        redirectAttributes.addFlashAttribute("successMessage", "Academic calendar date updated.");
        return "redirect:/admin/calendar";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/calendar/delete/{id}")
    public String deleteEntry(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        calendarService.deleteEntry(id);
        redirectAttributes.addFlashAttribute("successMessage", "Academic calendar date deleted.");
        return "redirect:/admin/calendar";
    }

    private void setForm(Model model, AcademicCalendarEntry entry, String heading, String action) {
        model.addAttribute("calendarEntry", entry);
        model.addAttribute("eventTypes", Arrays.asList(AcademicCalendarEventType.values()));
        model.addAttribute("pageHeading", heading);
        model.addAttribute("calendarAction", action);
    }
}
