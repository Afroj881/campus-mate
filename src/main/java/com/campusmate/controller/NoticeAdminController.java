package com.campusmate.controller;
import com.campusmate.model.Notice;
import com.campusmate.service.NoticeService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
@Controller
public class NoticeAdminController {
 private final NoticeService notices;
 public NoticeAdminController(NoticeService notices){this.notices=notices;}
 @GetMapping("/admin/notices") public String list(Model model){model.addAttribute("notices",notices.getAllNotices());return "admin/notices";}
 @GetMapping("/admin/notices/new") public String createForm(Model model){model.addAttribute("notice",new Notice());model.addAttribute("pageHeading","Add Notice");model.addAttribute("noticeAction","/admin/notices");return "admin/notice-form";}
 @PostMapping("/admin/notices") public String create(@Valid @ModelAttribute("notice") Notice notice,BindingResult result,Model model,RedirectAttributes flash){
  if(result.hasErrors()){model.addAttribute("pageHeading","Add Notice");model.addAttribute("noticeAction","/admin/notices");return "admin/notice-form";}
  notices.createNotice(notice);flash.addFlashAttribute("successMessage","Notice created successfully.");return "redirect:/admin/notices";
 }
 @GetMapping("/admin/notices/edit/{id}") public String editForm(@PathVariable Long id,Model model){
  model.addAttribute("notice",notices.getNoticeById(id));model.addAttribute("pageHeading","Edit Notice");model.addAttribute("noticeAction","/admin/notices/update/"+id);return "admin/notice-form";
 }
 @PostMapping("/admin/notices/update/{id}") public String update(@PathVariable Long id,@Valid @ModelAttribute("notice") Notice notice,BindingResult result,Model model,RedirectAttributes flash){
  notice.setId(id);
  if(result.hasErrors()){model.addAttribute("pageHeading","Edit Notice");model.addAttribute("noticeAction","/admin/notices/update/"+id);return "admin/notice-form";}
  notices.updateNotice(id,notice);flash.addFlashAttribute("successMessage","Notice updated successfully.");return "redirect:/admin/notices";
 }
 @PostMapping("/admin/notices/delete/{id}") public String delete(@PathVariable Long id,RedirectAttributes flash){
  notices.deleteNotice(id);flash.addFlashAttribute("successMessage","Notice deleted successfully.");return "redirect:/admin/notices";
 }
}
