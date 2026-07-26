package com.campusmate;
import com.campusmate.model.RegistrationForm;
import com.campusmate.service.UserService;
import com.campusmate.service.UserService.DuplicateEmailException;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
@Controller
public class AuthController {
 private final UserService userService;
 public AuthController(UserService service){userService=service;}
 @GetMapping("/register") public String registrationForm(HttpSession session,Model model){
  if(session.getAttribute("userId")!=null)return "redirect:/";
  model.addAttribute("registrationForm",new RegistrationForm()); return "register";
 }
 @PostMapping("/register") public String register(@Valid @ModelAttribute("registrationForm") RegistrationForm form,BindingResult result){
  if(result.hasErrors())return "register";
  try{userService.registerStudent(form);}catch(DuplicateEmailException e){result.rejectValue("email","duplicate","An account with this email already exists.");return "register";}
  return "redirect:/login?registered";
 }
 @GetMapping("/login") public String loginForm(HttpSession session){return session.getAttribute("userId")==null?"login":"redirect:/";}
 @PostMapping("/login") public String login(@RequestParam String email,@RequestParam String password,HttpSession session,Model model){
  if(email.isBlank()||password.isBlank()){model.addAttribute("loginError","Enter your email and password.");model.addAttribute("loginEmail",email);return "login";}
  return userService.authenticate(email,password).map(user->{session.setAttribute("userId",user.getId());session.setAttribute("userName",user.getName());session.setAttribute("userRole",user.getRole().name());return "redirect:/";})
   .orElseGet(()->{model.addAttribute("loginError","Email or password is incorrect.");model.addAttribute("loginEmail",email);return "login";});
 }
}
