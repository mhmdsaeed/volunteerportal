package com.volunteerportal.app.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.volunteerportal.app.security.UserPrincipal;
import com.volunteerportal.app.service.VolunteerInitiativeService;

@Controller
@RequestMapping("/initiatives")
public class VolunteerInitiativeController {

    private final VolunteerInitiativeService volunteerInitiativeService;

    public VolunteerInitiativeController(VolunteerInitiativeService volunteerInitiativeService) {
        this.volunteerInitiativeService = volunteerInitiativeService;
    }

    @GetMapping
    public String list(@AuthenticationPrincipal UserPrincipal principal, Model model) {
        model.addAttribute("initiatives", volunteerInitiativeService.findAvailableInitiatives());
        model.addAttribute("memberships", volunteerInitiativeService.findMembershipsForUser(principal.getUser().getId()));
        return "initiatives/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal, Model model) {
        model.addAttribute("initiative", volunteerInitiativeService.findInitiativeDetail(id));
        model.addAttribute("questions", volunteerInitiativeService.findQuestions(id));
        model.addAttribute("membership",
                volunteerInitiativeService.findMembership(principal.getUser().getId(), id).orElse(null));
        return "initiatives/detail";
    }

    @PostMapping("/{id}/join")
    public String join(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam MultiValueMap<String, String> formParams) {
        volunteerInitiativeService.join(id, principal.getUser(), formParams);
        return "redirect:/initiatives/{id}";
    }
}
