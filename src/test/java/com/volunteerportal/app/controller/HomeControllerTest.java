package com.volunteerportal.app.controller;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.volunteerportal.app.api.ApiDtos.EventItem;
import com.volunteerportal.app.api.ApiDtos.InitiativeItem;
import com.volunteerportal.app.api.ApiDtos.Me;
import com.volunteerportal.app.api.ApiDtos.NotificationItem;
import com.volunteerportal.app.api.MobileApiService;
import com.volunteerportal.app.config.SecurityConfig;
import com.volunteerportal.app.model.Role;
import com.volunteerportal.app.model.User;
import com.volunteerportal.app.security.UserPrincipal;
import com.volunteerportal.app.service.NotificationService;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(HomeController.class)
@Import(SecurityConfig.class)
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MobileApiService mobileApiService;

    @MockitoBean
    private NotificationService notificationService;

    private UserPrincipal userPrincipal(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setRoles(new HashSet<>(Set.of(new Role("VOLUNTEER"))));
        return new UserPrincipal(user);
    }

    private void givenMe(String grade, long points) {
        given(mobileApiService.me(any())).willReturn(new Me(42L, "vol1", "vol1@example.org", List.of("VOLUNTEER"), grade, points));
    }

    private static EventItem event(Long id, String name, String status) {
        LocalDateTime from = LocalDateTime.of(2026, 10, 11, 8, 0);
        return new EventItem(id, name, 5L, "Coast initiative", from, from.plusHours(4), "https://maps.example.org/beach",
                null, null, false, status);
    }

    @Test
    void home_showsNextEventAsPassAndLaterEvents() throws Exception {
        givenMe("Bronze", 120);
        given(mobileApiService.upcomingEvents(42L)).willReturn(List.of(
                event(1L, "Beach clean-up", "CHECKED_IN"), event(2L, "Food bank shift", "NOT_CHECKED_IN")));
        given(mobileApiService.initiatives(42L)).willReturn(List.of());
        given(mobileApiService.notifications(eq(42L), any())).willReturn(List.of());

        mockMvc.perform(get("/home").with(user(userPrincipal(42L, "vol1"))))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(content().string(containsString("Your next event")))
                .andExpect(content().string(containsString("Beach clean-up")))
                .andExpect(content().string(containsString("Sunday 11 October, 08:00")))
                .andExpect(content().string(containsString("12:00")))
                .andExpect(content().string(containsString("Checked in")))
                .andExpect(content().string(containsString("https://maps.example.org/beach")))
                .andExpect(content().string(containsString("Later events")))
                .andExpect(content().string(containsString("Food bank shift")))
                .andExpect(content().string(containsString("Bronze")))
                .andExpect(content().string(containsString("120")));
    }

    @Test
    void home_withoutEvents_invitesToBrowseInitiatives() throws Exception {
        givenMe(null, 0);
        given(mobileApiService.upcomingEvents(42L)).willReturn(List.of());
        given(mobileApiService.initiatives(42L)).willReturn(List.of());
        given(mobileApiService.notifications(eq(42L), any())).willReturn(List.of());

        mockMvc.perform(get("/home").with(user(userPrincipal(42L, "vol1"))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("You have no upcoming events.")))
                .andExpect(content().string(containsString("href=\"/initiatives?show=notJoined\"")))
                .andExpect(content().string(containsString("No grade yet")))
                .andExpect(content().string(not(containsString("Your next event"))))
                .andExpect(content().string(not(containsString("Later events"))));
    }

    @Test
    void home_listsJoinRequestsByStatusAndUnreadNotificationsOnly() throws Exception {
        givenMe(null, 0);
        given(mobileApiService.upcomingEvents(42L)).willReturn(List.of());
        given(mobileApiService.initiatives(42L)).willReturn(List.of(
                new InitiativeItem(5L, "Coast initiative", null, null, "PENDING"),
                new InitiativeItem(6L, "Food bank", null, null, "APPROVED"),
                new InitiativeItem(7L, "Library helpers", null, null, "REJECTED"),
                new InitiativeItem(8L, "Not joined initiative", null, null, "NONE")));
        given(mobileApiService.notifications(eq(42L), any())).willReturn(List.of(
                new NotificationItem(1L, "Your request was approved.", "/initiatives/6", false, LocalDateTime.now()),
                new NotificationItem(2L, "An old message you already read.", null, true, LocalDateTime.now())));

        mockMvc.perform(get("/home").with(user(userPrincipal(42L, "vol1"))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/initiatives/5\"")))
                .andExpect(content().string(containsString("Pending")))
                .andExpect(content().string(containsString("Approved")))
                .andExpect(content().string(containsString("Not approved")))
                .andExpect(content().string(not(containsString("Not joined initiative"))))
                .andExpect(content().string(containsString("Your request was approved.")))
                .andExpect(content().string(not(containsString("An old message you already read."))));
    }
}
