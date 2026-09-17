package com.volunteerportal.app.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.volunteerportal.app.security.UserPrincipal;
import com.volunteerportal.app.service.NotificationService;

@ControllerAdvice
public class GlobalModelAttributes {

    private final NotificationService notificationService;

    public GlobalModelAttributes(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @ModelAttribute("unreadNotificationCount")
    public long unreadNotificationCount(@AuthenticationPrincipal UserPrincipal principal) {
        return principal == null ? 0 : notificationService.countUnread(principal.getUser().getId());
    }
}
