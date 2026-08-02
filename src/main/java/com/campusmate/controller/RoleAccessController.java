package com.campusmate.controller;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
@Controller
public class RoleAccessController {
 @GetMapping("/student-area") public String studentArea(){return "student/access";}
 @GetMapping("/admin-area") public String adminArea(){return "admin/access";}
}
