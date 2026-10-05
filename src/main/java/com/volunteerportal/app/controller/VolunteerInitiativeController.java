package com.volunteerportal.app.controller;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.volunteerportal.app.model.Initiative;
import com.volunteerportal.app.model.VolunteerInitiative;
import com.volunteerportal.app.security.UserPrincipal;
import com.volunteerportal.app.service.MembershipFilter;
import com.volunteerportal.app.service.VolunteerInitiativeService;

@Controller
@RequestMapping("/initiatives")
public class VolunteerInitiativeController {

    private final VolunteerInitiativeService volunteerInitiativeService;

    public VolunteerInitiativeController(VolunteerInitiativeService volunteerInitiativeService) {
        this.volunteerInitiativeService = volunteerInitiativeService;
    }

    @GetMapping
    public String list(@AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(name = "show", required = false) String show, Model model) {
        MembershipFilter filter = MembershipFilter.from(show);
        List<Initiative> initiatives = volunteerInitiativeService.findAvailableInitiatives();
        Map<Long, VolunteerInitiative> memberships =
                volunteerInitiativeService.findMembershipsForUser(principal.getUser().getId());

        Map<MembershipFilter, Long> counts = new EnumMap<>(MembershipFilter.class);
        for (MembershipFilter option : MembershipFilter.values()) {
            counts.put(option, initiatives.stream().filter(i -> option.matches(memberships.get(i.getId()))).count());
        }

        model.addAttribute("initiatives",
                initiatives.stream().filter(i -> filter.matches(memberships.get(i.getId()))).toList());
        model.addAttribute("memberships", memberships);
        model.addAttribute("filter", filter);
        model.addAttribute("filters", MembershipFilter.values());
        model.addAttribute("counts", counts);
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
