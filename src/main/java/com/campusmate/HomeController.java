package com.campusmate;
import com.campusmate.service.NoticeService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
@Controller
public class HomeController {
 private final NoticeService notices;
 public HomeController(NoticeService notices){this.notices=notices;}
 @GetMapping("/") public String home(HttpSession session,Model model){model.addAttribute("userName",session.getAttribute("userName"));return "home";}
 @GetMapping("/notices") public String notices(Model model){model.addAttribute("notices",notices.getAllNotices());return "notices";}
}
