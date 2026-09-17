package com.volunteerportal.app.controller;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.volunteerportal.app.model.Initiative;
import com.volunteerportal.app.model.VolunteerInitiative;
import com.volunteerportal.app.security.UserPrincipal;
import com.volunteerportal.app.service.InitiativeService;
import com.volunteerportal.app.service.JoinRequestService;

@Controller
@RequestMapping("/coordinator/initiatives")
public class CoordinatorController {

    private final JoinRequestService joinRequestService;
    private final InitiativeService initiativeService;

    public CoordinatorController(JoinRequestService joinRequestService, InitiativeService initiativeService) {
        this.joinRequestService = joinRequestService;
        this.initiativeService = initiativeService;
    }

    @GetMapping
    public String list(@AuthenticationPrincipal UserPrincipal principal, Model model) {
        Long supervisorId = isAdmin(principal) ? null : principal.getUser().getId();
        model.addAttribute("initiatives", joinRequestService.findManagedInitiatives(supervisorId));
        return "coordinator/initiatives/list";
    }

    @GetMapping("/{id}/requests")
    public String requests(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal, Model model) {
        Initiative initiative = initiativeService.findById(id);
        assertCanManage(initiative, principal);

        model.addAttribute("initiative", initiative);
        model.addAttribute("requests", joinRequestService.findRequestsForInitiative(id));
        return "coordinator/initiatives/requests";
    }

    @PostMapping("/{id}/requests/{requestId}/approve")
    public String approve(@PathVariable Long id, @PathVariable Long requestId,
            @AuthenticationPrincipal UserPrincipal principal) {
        Initiative initiative = initiativeService.findById(id);
        assertCanManage(initiative, principal);
        assertBelongsToInitiative(joinRequestService.findById(requestId), initiative);

        joinRequestService.approve(requestId);
        return "redirect:/coordinator/initiatives/{id}/requests";
    }

    @PostMapping("/{id}/requests/{requestId}/reject")
    public String reject(@PathVariable Long id, @PathVariable Long requestId,
            @AuthenticationPrincipal UserPrincipal principal) {
        Initiative initiative = initiativeService.findById(id);
        assertCanManage(initiative, principal);
        assertBelongsToInitiative(joinRequestService.findById(requestId), initiative);

        joinRequestService.reject(requestId);
        return "redirect:/coordinator/initiatives/{id}/requests";
    }

    private boolean isAdmin(UserPrincipal principal) {
        return principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private void assertCanManage(Initiative initiative, UserPrincipal principal) {
        if (isAdmin(principal)) {
            return;
        }
        Long supervisorId = initiative.getSupervisor() != null ? initiative.getSupervisor().getId() : null;
        if (!principal.getUser().getId().equals(supervisorId)) {
            throw new AccessDeniedException("Not the supervisor of initiative " + initiative.getId());
        }
    }

    private void assertBelongsToInitiative(VolunteerInitiative request, Initiative initiative) {
        if (request.getInitiative() == null || !request.getInitiative().getId().equals(initiative.getId())) {
            throw new AccessDeniedException("Join request does not belong to initiative " + initiative.getId());
        }
    }
}
