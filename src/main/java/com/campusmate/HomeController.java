package com.campusmate;

import org.springframework.security.access.prepost.PreAuthorize;
import com.campusmate.model.Task;
import com.campusmate.model.TaskForm;
import com.campusmate.model.TaskSummary;
import com.campusmate.model.User;
import com.campusmate.service.EventService;
import com.campusmate.service.NoticeService;
import com.campusmate.service.TimetableService;
import com.campusmate.service.TaskService;
import com.campusmate.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class HomeController {

    private final NoticeService noticeService;
    private final EventService eventService;
    private final TimetableService timetableService;
    private final TaskService taskService;
    private final UserService userService;

    private static final List<String> TASK_TYPES = List.of("Assignment", "Exam", "Project", "Study", "Other");

    public HomeController(NoticeService noticeService,
                          EventService eventService,
                          TimetableService timetableService,
                          TaskService taskService,
                          UserService userService) {
        this.noticeService = noticeService;
        this.eventService = eventService;
        this.timetableService = timetableService;
        this.taskService = taskService;
        this.userService = userService;
    }

    @GetMapping("/")
    public String home(HttpSession session, Model model) {
        model.addAttribute("userName", session.getAttribute("userName"));
        return "home";
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, HttpSession session, Model model) {
        if (authentication == null || authentication.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_STUDENT"))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied.");
        }
        model.addAttribute("userName", session.getAttribute("userName"));
        model.addAttribute("latestNotices", noticeService.getLatestNotices());
        model.addAttribute("noticeCount", noticeService.countNotices());
        model.addAttribute("upcomingEvents", eventService.getUpcomingDashboardEvents());
        model.addAttribute("upcomingEventCount", eventService.countUpcomingEvents());
        model.addAttribute("todayTimetable", timetableService.getTimetableEntriesForDay(LocalDate.now().getDayOfWeek()));
        User currentStudent = isStudent(authentication) ? getCurrentUser(authentication) : null;
        List<TaskSummary> pendingTasks = currentStudent == null
                ? List.of()
            : taskService.getUpcomingPendingTasksForUser(currentStudent).stream()
                .map(TaskSummary::new)
                .toList();
        model.addAttribute("pendingTasks", pendingTasks);
        model.addAttribute("pendingTaskCount", currentStudent == null
                ? 0
                : taskService.countPendingTasksForUser(currentStudent));
        return "dashboard";
    }

    @GetMapping("/notices")
    public String notices(Model model) {
        model.addAttribute("notices", noticeService.getAllNotices());
        return "notices";
    }

    @org.springframework.web.bind.annotation.GetMapping("/events")
    public String events(Model model) {
        model.addAttribute("events", eventService.getUpcomingEvents());
        return "events";
    }

    @GetMapping("/timetable")
    public String timetable(Model model) {
        model.addAttribute("timetableEntries", timetableService.getAllTimetableEntries());
        return "timetable";
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/planner")
    public String planner(Authentication authentication, Model model) {
        if (authentication == null || authentication.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_STUDENT"))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied.");
        }
        model.addAttribute("userName", authentication.getName());
        model.addAttribute("tasks", taskService.getTasksForUser(getCurrentUser(authentication)).stream()
            .map(TaskSummary::new)
            .toList());
        return "planner";
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/planner/new")
    public String newTask(Authentication authentication, Model model) {
        model.addAttribute("task", new TaskForm());
        setTaskFormAttributes(model, "Add Task", "/planner", authentication.getName());
        return "task-form";
    }

    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/planner")
    public String createTask(Authentication authentication,
                             @Valid @ModelAttribute("task") TaskForm task,
                             BindingResult bindingResult,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            setTaskFormAttributes(model, "Add Task", "/planner", authentication.getName());
            return "task-form";
        }

        taskService.createTask(toTask(task), getCurrentUser(authentication));
        redirectAttributes.addFlashAttribute("successMessage", "Task added successfully.");
        return "redirect:/planner";
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/planner/edit/{id}")
    public String editTask(Authentication authentication, @PathVariable Long id, Model model) {
        model.addAttribute("task", new TaskForm(taskService.getTaskForUser(id, getCurrentUser(authentication))));
        setTaskFormAttributes(model, "Edit Task", "/planner/update/" + id, authentication.getName());
        return "task-form";
    }

    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/planner/update/{id}")
    public String updateTask(Authentication authentication,
                             @PathVariable Long id,
                             @Valid @ModelAttribute("task") TaskForm task,
                             BindingResult bindingResult,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        User currentUser = getCurrentUser(authentication);
        if (bindingResult.hasErrors()) {
            taskService.getTaskForUser(id, currentUser);
            setTaskFormAttributes(model, "Edit Task", "/planner/update/" + id, authentication.getName());
            return "task-form";
        }

        taskService.updateTask(id, toTask(task), currentUser);
        redirectAttributes.addFlashAttribute("successMessage", "Task updated successfully.");
        return "redirect:/planner";
    }

    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/planner/delete/{id}")
    public String deleteTask(Authentication authentication, @PathVariable Long id, RedirectAttributes redirectAttributes) {
        taskService.deleteTask(id, getCurrentUser(authentication));
        redirectAttributes.addFlashAttribute("successMessage", "Task deleted successfully.");
        return "redirect:/planner";
    }

    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/planner/complete/{id}")
    public String completeTask(Authentication authentication, @PathVariable Long id, RedirectAttributes redirectAttributes) {
        taskService.markTaskCompleted(id, getCurrentUser(authentication));
        redirectAttributes.addFlashAttribute("successMessage", "Task marked completed.");
        return "redirect:/planner";
    }

    private User getCurrentUser(Authentication authentication) {
        return userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private boolean isStudent(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_STUDENT"));
    }

    private void setTaskFormAttributes(Model model, String pageHeading, String taskAction, String userName) {
        model.addAttribute("pageHeading", pageHeading);
        model.addAttribute("taskTypes", TASK_TYPES);
        model.addAttribute("taskAction", taskAction);
        model.addAttribute("userName", userName);
    }

    private Task toTask(TaskForm taskForm) {
        Task task = new Task();
        task.setTitle(taskForm.getTitle());
        task.setDescription(taskForm.getDescription());
        task.setType(taskForm.getType());
        task.setDeadline(taskForm.getDeadline());
        return task;
    }
}
