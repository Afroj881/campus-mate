package com.campusmate.controller;
import com.campusmate.model.User;
import com.campusmate.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
@Controller
public class DashboardController {
 private final UserService users;
 public DashboardController(UserService users){this.users=users;}
 @GetMapping("/dashboard") public String studentDashboard(Authentication auth,Model model){User student=users.findByEmail(auth.getName()).orElseThrow();model.addAttribute("student",student);return "dashboard";}
 @GetMapping("/dashboard/admin") public String adminDashboard(Authentication auth,Model model){model.addAttribute("adminEmail",auth.getName());return "admin/dashboard";}
}
