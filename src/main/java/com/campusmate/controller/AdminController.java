package com.campusmate.controller;

import com.campusmate.model.Notice;
import org.springframework.security.access.prepost.PreAuthorize;
import com.campusmate.model.Event;
import com.campusmate.model.Club;
import com.campusmate.model.Subject;
import com.campusmate.model.Timetable;
import com.campusmate.model.User;
import com.campusmate.repository.SubjectRepository;
import com.campusmate.repository.TeacherSubjectAssignmentRepository;
import com.campusmate.service.ClubService;
import com.campusmate.service.EventService;
import com.campusmate.service.NoticeService;
import com.campusmate.service.SubjectService;
import com.campusmate.service.TimetableService;
import com.campusmate.service.ResourceService;
import com.campusmate.service.TeacherSubjectAssignmentService;
import com.campusmate.service.UserService;
import jakarta.validation.Valid;
import java.time.DayOfWeek;
import org.springframework.security.core.Authentication;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final NoticeService noticeService;
    private final EventService eventService;
    private final TimetableService timetableService;
    private final ClubService clubService;
    private final ResourceService resourceService;
    private final UserService userService;
    private final SubjectService subjectService;
    private final SubjectRepository subjectRepository;
    private final TeacherSubjectAssignmentRepository teacherSubjectAssignmentRepository;
    private final TeacherSubjectAssignmentService teacherSubjectAssignmentService;

    public AdminController(NoticeService noticeService, EventService eventService,
                           TimetableService timetableService, ClubService clubService,
                           ResourceService resourceService, UserService userService,
                           SubjectService subjectService, SubjectRepository subjectRepository,
                           TeacherSubjectAssignmentRepository teacherSubjectAssignmentRepository,
                           TeacherSubjectAssignmentService teacherSubjectAssignmentService) {
        this.noticeService = noticeService;
        this.eventService = eventService;
        this.timetableService = timetableService;
        this.clubService = clubService;
        this.resourceService = resourceService;
        this.userService = userService;
        this.subjectService = subjectService;
        this.subjectRepository = subjectRepository;
        this.teacherSubjectAssignmentRepository = teacherSubjectAssignmentRepository;
        this.teacherSubjectAssignmentService = teacherSubjectAssignmentService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping({"/admin", "/admin/", "/admin/dashboard"})
    public String adminRoot() {
        return "redirect:/dashboard/admin";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/dashboard/admin")
    public String adminDashboard(Authentication authentication, Model model) {
        model.addAttribute("adminName", authentication.getName());
        model.addAttribute("adminEmail", authentication.getName());
        model.addAttribute("noticeCount", noticeService.countNotices());
        model.addAttribute("eventCount", eventService.countEvents());
        model.addAttribute("studentCount", userService.countStudents());
        model.addAttribute("clubCount", clubService.countClubs());
        model.addAttribute("resourceCount", resourceService.countResources());
        return "admin/dashboard";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/notices")
    public String manageNotices(Model model) {
        model.addAttribute("notices", noticeService.getAllNotices());
        return "admin/notices";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/notices/new")
    public String newNotice(Model model) {
        model.addAttribute("notice", new Notice());
        model.addAttribute("pageHeading", "Add Notice");
        model.addAttribute("noticeAction", "/admin/notices");
        return "admin/notice-form";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/notices")
    public String createNotice(@Valid @ModelAttribute("notice") Notice notice,
                               BindingResult bindingResult,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageHeading", "Add Notice");
            model.addAttribute("noticeAction", "/admin/notices");
            return "admin/notice-form";
        }

        noticeService.createNotice(notice);
        redirectAttributes.addFlashAttribute("successMessage", "Notice created successfully.");
        return "redirect:/admin/notices";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/notices/edit/{id}")
    public String editNotice(@PathVariable Long id, Model model) {
        model.addAttribute("notice", noticeService.getNoticeById(id));
        model.addAttribute("pageHeading", "Edit Notice");
        model.addAttribute("noticeAction", "/admin/notices/update/" + id);
        return "admin/notice-form";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/notices/update/{id}")
    public String updateNotice(@PathVariable Long id,
                               @Valid @ModelAttribute("notice") Notice notice,
                               BindingResult bindingResult,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        notice.setId(id);
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageHeading", "Edit Notice");
            model.addAttribute("noticeAction", "/admin/notices/update/" + id);
            return "admin/notice-form";
        }

        noticeService.updateNotice(id, notice);
        redirectAttributes.addFlashAttribute("successMessage", "Notice updated successfully.");
        return "redirect:/admin/notices";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/notices/delete/{id}")
    public String deleteNotice(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        noticeService.deleteNotice(id);
        redirectAttributes.addFlashAttribute("successMessage", "Notice deleted successfully.");
        return "redirect:/admin/notices";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/events")
    public String manageEvents(Model model) {
        model.addAttribute("events", eventService.getAllEvents());
        return "admin/events";
    }

    @GetMapping("/admin/events/new")
    public String newEvent(Model model) {
        model.addAttribute("event", new Event());
        model.addAttribute("pageHeading", "Add Event");
        model.addAttribute("eventAction", "/admin/events");
        return "admin/event-form";
    }

    @PostMapping("/admin/events")
    public String createEvent(@Valid @ModelAttribute("event") Event event,
                              BindingResult bindingResult,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageHeading", "Add Event");
            model.addAttribute("eventAction", "/admin/events");
            return "admin/event-form";
        }

        eventService.createEvent(event);
        redirectAttributes.addFlashAttribute("successMessage", "Event created successfully.");
        return "redirect:/admin/events";
    }

    @GetMapping("/admin/events/edit/{id}")
    public String editEvent(@PathVariable Long id, Model model) {
        model.addAttribute("event", eventService.getEventById(id));
        model.addAttribute("pageHeading", "Edit Event");
        model.addAttribute("eventAction", "/admin/events/update/" + id);
        return "admin/event-form";
    }

    @PostMapping("/admin/events/update/{id}")
    public String updateEvent(@PathVariable Long id,
                              @Valid @ModelAttribute("event") Event event,
                              BindingResult bindingResult,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageHeading", "Edit Event");
            model.addAttribute("eventAction", "/admin/events/update/" + id);
            return "admin/event-form";
        }

        eventService.updateEvent(id, event);
        redirectAttributes.addFlashAttribute("successMessage", "Event updated successfully.");
        return "redirect:/admin/events";
    }

    @PostMapping("/admin/events/delete/{id}")
    public String deleteEvent(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        eventService.deleteEvent(id);
        redirectAttributes.addFlashAttribute("successMessage", "Event deleted successfully.");
        return "redirect:/admin/events";
    }

    @GetMapping("/admin/timetable")
    public String manageTimetable(Model model) {
        model.addAttribute("timetableEntries", timetableService.getAllTimetableEntries());
        return "admin/timetable";
    }

    @GetMapping("/admin/timetable/new")
    public String newTimetableEntry(Model model) {
        model.addAttribute("timetableEntry", new Timetable());
        setTimetableFormAttributes(model, "Add Timetable Entry", "/admin/timetable");
        return "admin/timetable-form";
    }

    @PostMapping("/admin/timetable")
    public String createTimetableEntry(@Valid @ModelAttribute("timetableEntry") Timetable timetableEntry,
                                       BindingResult bindingResult,
                                       Model model,
                                       RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            setTimetableFormAttributes(model, "Add Timetable Entry", "/admin/timetable");
            return "admin/timetable-form";
        }

        timetableService.createTimetableEntry(timetableEntry);
        redirectAttributes.addFlashAttribute("successMessage", "Timetable entry created successfully.");
        return "redirect:/admin/timetable";
    }

    @GetMapping("/admin/timetable/edit/{id}")
    public String editTimetableEntry(@PathVariable Long id, Model model) {
        model.addAttribute("timetableEntry", timetableService.getTimetableEntryById(id));
        setTimetableFormAttributes(model, "Edit Timetable Entry", "/admin/timetable/update/" + id);
        return "admin/timetable-form";
    }

    @PostMapping("/admin/timetable/update/{id}")
    public String updateTimetableEntry(@PathVariable Long id,
                                       @Valid @ModelAttribute("timetableEntry") Timetable timetableEntry,
                                       BindingResult bindingResult,
                                       Model model,
                                       RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            setTimetableFormAttributes(model, "Edit Timetable Entry", "/admin/timetable/update/" + id);
            return "admin/timetable-form";
        }

        timetableService.updateTimetableEntry(id, timetableEntry);
        redirectAttributes.addFlashAttribute("successMessage", "Timetable entry updated successfully.");
        return "redirect:/admin/timetable";
    }

    @PostMapping("/admin/timetable/delete/{id}")
    public String deleteTimetableEntry(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        timetableService.deleteTimetableEntry(id);
        redirectAttributes.addFlashAttribute("successMessage", "Timetable entry deleted successfully.");
        return "redirect:/admin/timetable";
    }

    @GetMapping("/admin/clubs")
    public String manageClubs(Model model) {
        model.addAttribute("clubs", clubService.getAllClubs());
        return "admin/clubs";
    }

    @GetMapping("/admin/clubs/new")
    public String newClub(Model model) {
        setClubFormAttributes(model, new Club(), "Add Club", "/admin/clubs");
        return "admin/club-form";
    }

    @PostMapping("/admin/clubs")
    public String createClub(@Valid @ModelAttribute("club") Club club,
                             BindingResult bindingResult,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            setClubFormAttributes(model, club, "Add Club", "/admin/clubs");
            return "admin/club-form";
        }

        clubService.createClub(club);
        redirectAttributes.addFlashAttribute("successMessage", "Club created successfully.");
        return "redirect:/admin/clubs";
    }

    @GetMapping("/admin/clubs/edit/{id}")
    public String editClub(@PathVariable Long id, Model model) {
        setClubFormAttributes(model, clubService.getClubById(id), "Edit Club", "/admin/clubs/update/" + id);
        return "admin/club-form";
    }

    @PostMapping("/admin/clubs/update/{id}")
    public String updateClub(@PathVariable Long id,
                             @Valid @ModelAttribute("club") Club club,
                             BindingResult bindingResult,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            setClubFormAttributes(model, club, "Edit Club", "/admin/clubs/update/" + id);
            return "admin/club-form";
        }

        clubService.updateClub(id, club);
        redirectAttributes.addFlashAttribute("successMessage", "Club updated successfully.");
        return "redirect:/admin/clubs";
    }

    @PostMapping("/admin/clubs/delete/{id}")
    public String deleteClub(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        clubService.deleteClub(id);
        redirectAttributes.addFlashAttribute("successMessage", "Club deleted successfully.");
        return "redirect:/admin/clubs";
    }

    @GetMapping("/admin/faculty")
    public String manageFaculty(Model model) {
        model.addAttribute("facultyMembers", userService.findFacultyUsers());
        return "admin/faculty";
    }

    @PostMapping("/admin/faculty")
    public String createFaculty(@RequestParam String name,
                                @RequestParam String email,
                                @RequestParam String password,
                                @RequestParam String department,
                                @RequestParam Integer semester,
                                @RequestParam(required = false) String section,
                                RedirectAttributes redirectAttributes) {
        try {
            userService.createFaculty(name, email, password, department, semester, section);
            redirectAttributes.addFlashAttribute("successMessage", "Faculty account created successfully.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Unable to create faculty account. Check the details and try again.");
        }
        return "redirect:/admin/faculty";
    }

    @GetMapping("/admin/subjects")
    public String manageSubjects(Model model) {
        model.addAttribute("subjects", subjectService.getAllSubjects());
        return "admin/subjects";
    }

    @PostMapping("/admin/subjects")
    public String createSubject(@RequestParam String name,
                               @RequestParam String code,
                               @RequestParam String department,
                               @RequestParam Integer semester,
                               RedirectAttributes redirectAttributes) {
        try {
            Subject subject = new Subject();
            subject.setName(name);
            subject.setCode(code);
            subject.setDepartment(department);
            subject.setSemester(semester);
            subjectService.createSubject(subject);
            redirectAttributes.addFlashAttribute("successMessage", "Subject created successfully.");
        } catch (IllegalArgumentException | SubjectService.DuplicateSubjectException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid or duplicate subject record.");
        }
        return "redirect:/admin/subjects";
    }

    @GetMapping("/admin/teacher-subject-assignments")
    public String manageTeacherAssignments(Model model) {
        model.addAttribute("facultyMembers", userService.findFacultyUsers());
        model.addAttribute("subjects", subjectService.getAllSubjects());
        model.addAttribute("teacherAssignments", teacherSubjectAssignmentRepository.findAllWithFacultyAndSubject());
        return "admin/teacher-subject-assignments";
    }

    @PostMapping("/admin/teacher-subject-assignments")
    public String createTeacherAssignment(@RequestParam Long facultyId,
                                         @RequestParam Long subjectId,
                                         @RequestParam String department,
                                         @RequestParam Integer semester,
                                         @RequestParam String section,
                                         RedirectAttributes redirectAttributes) {
        try {
            User faculty = userService.findById(facultyId)
                    .orElseThrow(() -> new IllegalArgumentException("Faculty not found."));
            Subject subject = subjectRepository.findById(subjectId)
                    .orElseThrow(() -> new IllegalArgumentException("Subject not found."));
            teacherSubjectAssignmentService.createAssignment(faculty, subject, department, semester, section);
            redirectAttributes.addFlashAttribute("successMessage", "Teacher assignment created successfully.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (DataIntegrityViolationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "This faculty assignment already exists.");
        }
        return "redirect:/admin/teacher-subject-assignments";
    }

    private void setClubFormAttributes(Model model, Club club, String pageHeading, String clubAction) {
        model.addAttribute("club", club);
        model.addAttribute("pageHeading", pageHeading);
        model.addAttribute("clubAction", clubAction);
    }

    private void setTimetableFormAttributes(Model model, String pageHeading, String timetableAction) {
        model.addAttribute("days", DayOfWeek.values());
        model.addAttribute("pageHeading", pageHeading);
        model.addAttribute("timetableAction", timetableAction);
    }
}
