package com.volunteerportal.app.controller;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.volunteerportal.app.model.Initiative;
import com.volunteerportal.app.model.VolunteerInitiative;
import com.volunteerportal.app.security.UserPrincipal;
import com.volunteerportal.app.service.InitiativeService;
import com.volunteerportal.app.service.JoinRequestService;

/**
 * Join-request review for admins and for coordinators, who manage the initiatives they supervise
 * or whose office they coordinate.
 */
@Controller
@RequestMapping("/coordinator")
public class CoordinatorController {

    private final JoinRequestService joinRequestService;
    private final InitiativeService initiativeService;

    public CoordinatorController(JoinRequestService joinRequestService, InitiativeService initiativeService) {
        this.joinRequestService = joinRequestService;
        this.initiativeService = initiativeService;
    }

    @GetMapping("/initiatives")
    public String list(@AuthenticationPrincipal UserPrincipal principal, Model model) {
        model.addAttribute("initiatives", joinRequestService.findManagedInitiatives(managerIdFor(principal)));
        return "coordinator/initiatives/list";
    }

    @GetMapping("/initiatives/{id}/requests")
    public String requests(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal, Model model) {
        Initiative initiative = initiativeService.findById(id);
        assertCanManage(initiative, principal);

        model.addAttribute("initiative", initiative);
        model.addAttribute("requests", joinRequestService.findRequestsForInitiative(id));
        return "coordinator/initiatives/requests";
    }

    @PostMapping("/initiatives/{id}/requests/{requestId}/approve")
    public String approve(@PathVariable Long id, @PathVariable Long requestId,
            @AuthenticationPrincipal UserPrincipal principal, RedirectAttributes redirectAttributes) {
        Initiative initiative = initiativeService.findById(id);
        assertCanManage(initiative, principal);
        assertBelongsToInitiative(joinRequestService.findById(requestId), initiative);

        decide(requestId, true, redirectAttributes);
        return "redirect:/coordinator/initiatives/{id}/requests";
    }

    @PostMapping("/initiatives/{id}/requests/{requestId}/reject")
    public String reject(@PathVariable Long id, @PathVariable Long requestId,
            @AuthenticationPrincipal UserPrincipal principal, RedirectAttributes redirectAttributes) {
        Initiative initiative = initiativeService.findById(id);
        assertCanManage(initiative, principal);
        assertBelongsToInitiative(joinRequestService.findById(requestId), initiative);

        decide(requestId, false, redirectAttributes);
        return "redirect:/coordinator/initiatives/{id}/requests";
    }

    /** Every pending request across the initiatives the user manages (all of them for an admin). */
    @GetMapping("/requests")
    public String pendingRequests(@AuthenticationPrincipal UserPrincipal principal, Model model) {
        model.addAttribute("requests", joinRequestService.findPendingRequests(managerIdFor(principal)));
        return "coordinator/requests/pending";
    }

    @PostMapping("/requests/{requestId}/approve")
    public String approvePending(@PathVariable Long requestId, @AuthenticationPrincipal UserPrincipal principal,
            RedirectAttributes redirectAttributes) {
        assertCanManage(joinRequestService.findById(requestId).getInitiative(), principal);

        decide(requestId, true, redirectAttributes);
        return "redirect:/coordinator/requests";
    }

    @PostMapping("/requests/{requestId}/reject")
    public String rejectPending(@PathVariable Long requestId, @AuthenticationPrincipal UserPrincipal principal,
            RedirectAttributes redirectAttributes) {
        assertCanManage(joinRequestService.findById(requestId).getInitiative(), principal);

        decide(requestId, false, redirectAttributes);
        return "redirect:/coordinator/requests";
    }

    private void decide(Long requestId, boolean approve, RedirectAttributes redirectAttributes) {
        try {
            if (approve) {
                joinRequestService.approve(requestId);
            } else {
                joinRequestService.reject(requestId);
            }
            redirectAttributes.addFlashAttribute("decision", approve ? "approved" : "rejected");
        } catch (IllegalStateException e) {
            // Someone else (or a double submit) already decided it; show a notice instead of an error page
            redirectAttributes.addFlashAttribute("decision", "alreadyDecided");
        }
    }

    private Long managerIdFor(UserPrincipal principal) {
        return isAdmin(principal) ? null : principal.getUser().getId();
    }

    private boolean isAdmin(UserPrincipal principal) {
        return principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private void assertCanManage(Initiative initiative, UserPrincipal principal) {
        if (isAdmin(principal)) {
            return;
        }
        if (!joinRequestService.canManage(initiative, principal.getUser().getId())) {
            throw new AccessDeniedException("Not a manager of initiative " + initiative.getId());
        }
    }

    private void assertBelongsToInitiative(VolunteerInitiative request, Initiative initiative) {
        if (request.getInitiative() == null || !request.getInitiative().getId().equals(initiative.getId())) {
            throw new AccessDeniedException("Join request does not belong to initiative " + initiative.getId());
        }
    }
}
