package com.volunteerportal.app.controller;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.volunteerportal.app.config.SecurityConfig;
import com.volunteerportal.app.model.Role;
import com.volunteerportal.app.model.User;
import com.volunteerportal.app.security.UserPrincipal;
import com.volunteerportal.app.service.NotificationService;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(NotificationController.class)
@Import(SecurityConfig.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    private UserPrincipal userPrincipal(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setRoles(new HashSet<>(Set.of(new Role("VOLUNTEER"))));
        return new UserPrincipal(user);
    }

    @Test
    void list_rendersNotificationsForCurrentUser() throws Exception {
        UserPrincipal principal = userPrincipal(42L, "vol1");
        given(notificationService.findForUser(42L)).willReturn(List.of());

        mockMvc.perform(get("/notifications").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(view().name("notifications/list"));

        verify(notificationService).findForUser(42L);
    }

    @Test
    void markRead_delegatesToServiceForCurrentUser() throws Exception {
        UserPrincipal principal = userPrincipal(42L, "vol1");

        mockMvc.perform(post("/notifications/7/read").with(user(principal)).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/notifications"));

        verify(notificationService).markRead(7L, 42L);
    }

    @Test
    void markAllRead_delegatesToServiceForCurrentUser() throws Exception {
        UserPrincipal principal = userPrincipal(42L, "vol1");

        mockMvc.perform(post("/notifications/read-all").with(user(principal)).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/notifications"));

        verify(notificationService).markAllRead(42L);
    }
}
