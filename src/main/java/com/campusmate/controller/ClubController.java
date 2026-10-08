package com.campusmate.controller;

import com.campusmate.service.ClubService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ClubController {

    private final ClubService clubService;

    public ClubController(ClubService clubService) {
        this.clubService = clubService;
    }

    @GetMapping("/clubs")
    public String viewClubs(Model model) {
        model.addAttribute("clubs", clubService.getAllClubs());
        return "clubs";
    }
}