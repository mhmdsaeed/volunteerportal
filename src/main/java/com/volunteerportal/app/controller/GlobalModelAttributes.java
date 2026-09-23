package com.volunteerportal.app.controller;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.volunteerportal.app.security.UserPrincipal;
import com.volunteerportal.app.service.JoinRequestService;
import com.volunteerportal.app.service.NotificationService;

@ControllerAdvice
public class GlobalModelAttributes {

    private final NotificationService notificationService;
    // Optional so @WebMvcTest slices that don't mock it still start; the sidebar badge is then just 0.
    private final ObjectProvider<JoinRequestService> joinRequestService;

    public GlobalModelAttributes(NotificationService notificationService,
            ObjectProvider<JoinRequestService> joinRequestService) {
        this.notificationService = notificationService;
        this.joinRequestService = joinRequestService;
    }

    @ModelAttribute("unreadNotificationCount")
    public long unreadNotificationCount(@AuthenticationPrincipal UserPrincipal principal) {
        return principal == null ? 0 : notificationService.countUnread(principal.getUser().getId());
    }

    /** Pending join requests the current admin/coordinator can decide, for the sidebar badge. */
    @ModelAttribute("pendingJoinRequestCount")
    public long pendingJoinRequestCount(@AuthenticationPrincipal UserPrincipal principal) {
        JoinRequestService service = joinRequestService.getIfAvailable();
        if (principal == null || service == null) {
            return 0;
        }
        if (hasRole(principal, "ROLE_ADMIN")) {
            return service.countPendingRequests(null);
        }
        if (hasRole(principal, "ROLE_COORDINATOR")) {
            return service.countPendingRequests(principal.getUser().getId());
        }
        return 0;
    }

    private boolean hasRole(UserPrincipal principal, String authority) {
        return principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(authority));
    }
}
