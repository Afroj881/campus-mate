package com.campusmate.controller;

import com.campusmate.service.SearchService;
import com.campusmate.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class SearchController {

    private final SearchService searchService;
    private final UserService userService;

    public SearchController(SearchService searchService, UserService userService) {
        this.searchService = searchService;
        this.userService = userService;
    }

    @GetMapping("/search")
    public String search(@RequestParam(required = false) String query, Authentication authentication, Model model) {
        String searchQuery = query == null ? "" : query.trim();
        model.addAttribute("query", searchQuery);
        model.addAttribute("results", searchService.search(searchQuery,
                userService.findByEmail(authentication.getName()).orElse(null)));
        return "search";
    }
}
