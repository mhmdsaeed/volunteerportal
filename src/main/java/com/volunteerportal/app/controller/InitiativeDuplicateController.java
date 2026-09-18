package com.volunteerportal.app.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.volunteerportal.app.model.Initiative;
import com.volunteerportal.app.service.InitiativeDuplicateService;

@Controller
@RequestMapping("/admin/initiatives")
public class InitiativeDuplicateController {

    private final InitiativeDuplicateService initiativeDuplicateService;

    public InitiativeDuplicateController(InitiativeDuplicateService initiativeDuplicateService) {
        this.initiativeDuplicateService = initiativeDuplicateService;
    }

    @PostMapping("/{id}/duplicate")
    public String duplicate(@PathVariable Long id) {
        Initiative copy = initiativeDuplicateService.duplicate(id);
        return "redirect:/admin/initiatives/" + copy.getId() + "/edit";
    }
}
