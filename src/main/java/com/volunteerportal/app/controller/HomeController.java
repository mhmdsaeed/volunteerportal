package com.volunteerportal.app.controller;

import java.util.List;
import java.util.Locale;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.volunteerportal.app.api.ApiDtos.EventItem;
import com.volunteerportal.app.api.ApiDtos.InitiativeItem;
import com.volunteerportal.app.api.ApiDtos.NotificationItem;
import com.volunteerportal.app.api.MobileApiService;
import com.volunteerportal.app.security.UserPrincipal;

@Controller
public class HomeController {

    /** How many later events and unread notifications the home page lists. */
    private static final int LIST_LIMIT = 3;

    // The mobile API already gathers exactly what the home page shows (next events with check-in status,
    // memberships, grade/points, translated notifications), so the website reuses it.
    private final MobileApiService mobileApiService;

    public HomeController(MobileApiService mobileApiService) {
        this.mobileApiService = mobileApiService;
    }

    @GetMapping("/")
    public String index(@AuthenticationPrincipal UserPrincipal principal) {
        return principal != null ? "redirect:/home" : "redirect:/login";
    }

    @GetMapping("/home")
    public String home(@AuthenticationPrincipal UserPrincipal principal, Locale locale, Model model) {
        Long userId = principal.getUser().getId();
        List<EventItem> events = mobileApiService.upcomingEvents(userId);
        List<InitiativeItem> requests = mobileApiService.initiatives(userId).stream()
                .filter(i -> !"NONE".equals(i.membership()))
                .toList();
        List<NotificationItem> unread = mobileApiService.notifications(userId, locale).stream()
                .filter(n -> !n.read())
                .limit(LIST_LIMIT)
                .toList();

        model.addAttribute("me", mobileApiService.me(principal.getUser()));
        model.addAttribute("nextEvent", events.isEmpty() ? null : events.get(0));
        model.addAttribute("laterEvents", events.stream().skip(1).limit(LIST_LIMIT).toList());
        model.addAttribute("requests", requests);
        model.addAttribute("unreadNotifications", unread);
        return "home";
    }
}
