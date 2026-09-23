package com.volunteerportal.app.api;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.volunteerportal.app.api.ApiDtos.AttendanceItem;
import com.volunteerportal.app.api.ApiDtos.CheckInRequest;
import com.volunteerportal.app.api.ApiDtos.EventItem;
import com.volunteerportal.app.api.ApiDtos.InitiativeItem;
import com.volunteerportal.app.api.ApiDtos.Me;
import com.volunteerportal.app.api.ApiDtos.NotificationItem;
import com.volunteerportal.app.security.UserPrincipal;
import com.volunteerportal.app.service.NotificationService;

/** The signed-in volunteer's data for the mobile app. All endpoints need a bearer token. */
@RestController
@RequestMapping("/api")
public class VolunteerApiController {

    private final MobileApiService mobileApiService;
    private final NotificationService notificationService;

    public VolunteerApiController(MobileApiService mobileApiService, NotificationService notificationService) {
        this.mobileApiService = mobileApiService;
        this.notificationService = notificationService;
    }

    @GetMapping("/me")
    public Me me(@AuthenticationPrincipal UserPrincipal principal) {
        return mobileApiService.me(principal.getUser());
    }

    /** Open initiatives with my membership status. */
    @GetMapping("/initiatives")
    public List<InitiativeItem> initiatives(@AuthenticationPrincipal UserPrincipal principal) {
        return mobileApiService.initiatives(principal.getUser().getId());
    }

    /** Upcoming events of initiatives I'm an approved member of, with my check-in status. */
    @GetMapping("/events")
    public List<EventItem> events(@AuthenticationPrincipal UserPrincipal principal) {
        return mobileApiService.upcomingEvents(principal.getUser().getId());
    }

    /**
     * Check in (or out, if already checked in) with the text of a scanned event QR code. Always 200
     * for a readable code - the outcome, with a message in the phone's language, is in the body.
     */
    @PostMapping("/checkin")
    public ResponseEntity<?> checkIn(@AuthenticationPrincipal UserPrincipal principal,
            @RequestBody CheckInRequest request, HttpServletRequest httpRequest) {
        return MobileApiService.parseQr(request.qr())
                .<ResponseEntity<?>>map(scanned -> ResponseEntity.ok(mobileApiService.checkIn(principal.getUser().getId(),
                        scanned, request.latitude(), request.longitude(), ApiLocale.of(httpRequest))))
                .orElseGet(() -> ResponseEntity.badRequest()
                        .body(new ApiDtos.Error("invalid_qr", "Not an event check-in QR code")));
    }

    /** My attendance history, newest first. */
    @GetMapping("/attendance")
    public List<AttendanceItem> attendance(@AuthenticationPrincipal UserPrincipal principal) {
        return mobileApiService.attendance(principal.getUser().getId());
    }

    /** My notifications, newest first, in the phone's language. */
    @GetMapping("/notifications")
    public List<NotificationItem> notifications(@AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        return mobileApiService.notifications(principal.getUser().getId(), ApiLocale.of(request));
    }

    @PostMapping("/notifications/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        notificationService.markRead(id, principal.getUser().getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/notifications/read-all")
    public ResponseEntity<Void> markAllRead(@AuthenticationPrincipal UserPrincipal principal) {
        notificationService.markAllRead(principal.getUser().getId());
        return ResponseEntity.noContent().build();
    }
}
