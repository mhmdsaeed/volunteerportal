package com.volunteerportal.app.controller;

import java.util.Map;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.volunteerportal.app.model.Event;
import com.volunteerportal.app.model.Initiative;
import com.volunteerportal.app.repository.AttendRepository;
import com.volunteerportal.app.security.UserPrincipal;
import com.volunteerportal.app.service.CheckInCodes;
import com.volunteerportal.app.service.CheckInService;
import com.volunteerportal.app.service.EventService;
import com.volunteerportal.app.service.InitiativeService;
import com.volunteerportal.app.service.JoinRequestService;
import com.volunteerportal.app.service.QrCodeRenderer;

/**
 * The coordinator's full-screen check-in page for an event: a QR code (refreshed as the code
 * rotates) that volunteers scan with their phones, plus live check-in/out counts.
 */
@Controller
@RequestMapping("/coordinator/initiatives/{initiativeId}/events/{eventId}/checkin")
public class CoordinatorCheckInController {

    private final InitiativeService initiativeService;
    private final EventService eventService;
    private final JoinRequestService joinRequestService;
    private final CheckInService checkInService;
    private final CheckInCodes checkInCodes;
    private final QrCodeRenderer qrCodeRenderer;
    private final AttendRepository attendRepository;

    public CoordinatorCheckInController(InitiativeService initiativeService, EventService eventService,
            JoinRequestService joinRequestService, CheckInService checkInService, CheckInCodes checkInCodes,
            QrCodeRenderer qrCodeRenderer, AttendRepository attendRepository) {
        this.initiativeService = initiativeService;
        this.eventService = eventService;
        this.joinRequestService = joinRequestService;
        this.checkInService = checkInService;
        this.checkInCodes = checkInCodes;
        this.qrCodeRenderer = qrCodeRenderer;
        this.attendRepository = attendRepository;
    }

    @GetMapping
    public String page(@PathVariable Long initiativeId, @PathVariable Long eventId,
            @AuthenticationPrincipal UserPrincipal principal, Model model) {
        Initiative initiative = managedInitiative(initiativeId, principal);
        Event event = eventOf(initiative, eventId);
        model.addAttribute("initiative", initiative);
        model.addAttribute("event", event);
        model.addAttribute("periodSeconds", checkInCodes.periodSeconds());
        model.addAttribute("requiresLocation", checkInService.requiresLocation(event));
        model.addAttribute("counts", counts(eventId));
        return "coordinator/initiatives/checkin";
    }

    /** The current code as a QR image; the page reloads it every few seconds. */
    @GetMapping(value = "/qr.svg", produces = "image/svg+xml")
    public ResponseEntity<String> qr(@PathVariable Long initiativeId, @PathVariable Long eventId,
            @AuthenticationPrincipal UserPrincipal principal) {
        eventOf(managedInitiative(initiativeId, principal), eventId);
        String url = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/checkin/{eventId}")
                .queryParam("code", checkInCodes.currentCode(eventId))
                .buildAndExpand(eventId)
                .toUriString();
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("image/svg+xml"))
                .cacheControl(CacheControl.noStore())
                .body(qrCodeRenderer.toSvg(url));
    }

    @GetMapping("/counts")
    @ResponseBody
    public Map<String, Long> liveCounts(@PathVariable Long initiativeId, @PathVariable Long eventId,
            @AuthenticationPrincipal UserPrincipal principal) {
        eventOf(managedInitiative(initiativeId, principal), eventId);
        return counts(eventId);
    }

    private Map<String, Long> counts(Long eventId) {
        return Map.of("checkedIn", attendRepository.countByEventIdAndAttendInOut(eventId, 1),
                "checkedOut", attendRepository.countByEventIdAndAttendInOut(eventId, 2));
    }

    private Initiative managedInitiative(Long initiativeId, UserPrincipal principal) {
        Initiative initiative = initiativeService.findById(initiativeId);
        CoordinatorAccess.assertCanManage(initiative, principal, joinRequestService);
        return initiative;
    }

    private Event eventOf(Initiative initiative, Long eventId) {
        Event event = eventService.findById(eventId);
        if (event.getInitiative() == null || !event.getInitiative().getId().equals(initiative.getId())) {
            throw new AccessDeniedException("Event " + eventId + " does not belong to initiative " + initiative.getId());
        }
        return event;
    }
}
