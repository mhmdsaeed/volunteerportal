package com.volunteerportal.app.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.volunteerportal.app.model.VolunteerInitiative;
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
        VolunteerInitiative membership =
                volunteerInitiativeService.findMembership(principal.getUser().getId(), id).orElse(null);
        boolean approvedMember = membership != null && Boolean.TRUE.equals(membership.getEnabled());

        model.addAttribute("initiative", volunteerInitiativeService.findInitiativeDetail(id));
        model.addAttribute("questions", volunteerInitiativeService.findQuestions(id));
        model.addAttribute("membership", membership);
        // Events are only for members: a volunteer can attend once their join request is approved
        model.addAttribute("events", approvedMember ? volunteerInitiativeService.findOpenEvents(id) : List.of());
        return "initiatives/detail";
    }

    @PostMapping("/{id}/join")
    public String join(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam MultiValueMap<String, String> formParams) {
        volunteerInitiativeService.join(id, principal.getUser(), formParams);
        return "redirect:/initiatives/{id}";
    }

    @PostMapping("/{id}/withdraw")
    public String withdraw(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        volunteerInitiativeService.withdraw(id, principal.getUser().getId());
        return "redirect:/initiatives/{id}";
    }
}
