package com.volunteerportal.app.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.volunteerportal.app.model.Notification;
import com.volunteerportal.app.security.UserPrincipal;
import com.volunteerportal.app.service.NotificationService;

@Controller
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public String list(@AuthenticationPrincipal UserPrincipal principal, Locale locale, Model model) {
        List<Notification> notifications = notificationService.findForUser(principal.getUser().getId());
        // Each notification's text in the viewer's language, by id
        Map<Long, String> texts = new HashMap<>();
        for (Notification notification : notifications) {
            texts.put(notification.getId(), notificationService.text(notification, locale));
        }
        model.addAttribute("notifications", notifications);
        model.addAttribute("texts", texts);
        return "notifications/list";
    }

    @PostMapping("/{id}/read")
    public String markRead(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        notificationService.markRead(id, principal.getUser().getId());
        return "redirect:/notifications";
    }

    @PostMapping("/read-all")
    public String markAllRead(@AuthenticationPrincipal UserPrincipal principal) {
        notificationService.markAllRead(principal.getUser().getId());
        return "redirect:/notifications";
    }
}
