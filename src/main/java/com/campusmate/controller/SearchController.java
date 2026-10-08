package com.campusmate.controller;

import com.campusmate.service.SearchService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/search")
    public String search(@RequestParam(required = false) String query, Model model) {
        String searchQuery = query == null ? "" : query.trim();
        model.addAttribute("query", searchQuery);
        model.addAttribute("results", searchService.search(searchQuery));
        return "search";
    }
}