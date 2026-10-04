package com.volunteerportal.app.controller;

import java.time.LocalDateTime;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.volunteerportal.app.model.Event;
import com.volunteerportal.app.security.UserPrincipal;
import com.volunteerportal.app.service.CheckInCodes;
import com.volunteerportal.app.service.CheckInService;

/**
 * Where a volunteer lands after scanning an event's QR code with their phone: they confirm, and
 * are checked in (or out, if already checked in). Logged-out volunteers log in first and are
 * brought back here.
 */
@Controller
@RequestMapping("/checkin/{eventId}")
public class CheckInController {

    private final CheckInService checkInService;
    private final CheckInCodes checkInCodes;

    public CheckInController(CheckInService checkInService, CheckInCodes checkInCodes) {
        this.checkInService = checkInService;
        this.checkInCodes = checkInCodes;
    }

    @GetMapping
    public String confirm(@PathVariable Long eventId, @RequestParam(required = false) String code,
            @AuthenticationPrincipal UserPrincipal principal, Model model) {
        Event event = checkInService.findEvent(eventId);
        model.addAttribute("event", event);
        model.addAttribute("code", code);
        model.addAttribute("codeValid", checkInCodes.isValid(eventId, code));
        CheckInService.Action action = checkInService.nextAction(event, principal.getUser().getId());
        model.addAttribute("action", action);
        // Check-in not open yet, or already closed: say so (with the time) instead of showing the button
        if (action == CheckInService.Action.CHECK_IN) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime opensAt = checkInService.checkInOpensAt(event);
            LocalDateTime closesAt = checkInService.checkInClosesAt(event);
            CheckInService.Result notOpen = opensAt != null && now.isBefore(opensAt) ? CheckInService.Result.TOO_EARLY
                    : closesAt != null && !now.isBefore(closesAt) ? CheckInService.Result.EVENT_ENDED : null;
            if (notOpen != null) {
                model.addAttribute("notOpen", notOpen.name());
                model.addAttribute("messageTime", checkInService.messageTime(notOpen, event));
            }
        }
        model.addAttribute("requiresLocation", checkInService.requiresLocation(event));
        return "checkin/confirm";
    }

    @PostMapping
    public String checkIn(@PathVariable Long eventId, @RequestParam(required = false) String code,
            @RequestParam(required = false) Double latitude, @RequestParam(required = false) Double longitude,
            @AuthenticationPrincipal UserPrincipal principal, RedirectAttributes redirectAttributes) {
        CheckInService.Result result = checkInService.checkIn(eventId, principal.getUser().getId(), code, latitude, longitude);
        redirectAttributes.addFlashAttribute("result", result.name());
        LocalDateTime messageTime = checkInService.messageTime(result, checkInService.findEvent(eventId));
        if (messageTime != null) {
            redirectAttributes.addFlashAttribute("messageTime", messageTime);
        }
        return "redirect:/checkin/{eventId}/done";
    }

    /** Result page (after a redirect, so refreshing it doesn't check in twice). */
    @GetMapping("/done")
    public String done(@PathVariable Long eventId, Model model) {
        if (!model.containsAttribute("result")) {
            return "redirect:/home";
        }
        model.addAttribute("event", checkInService.findEvent(eventId));
        return "checkin/result";
    }
}
