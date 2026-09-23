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
import com.volunteerportal.app.model.Event;
import com.volunteerportal.app.model.Initiative;
import com.volunteerportal.app.model.Role;
import com.volunteerportal.app.model.User;
import com.volunteerportal.app.security.UserPrincipal;
import com.volunteerportal.app.service.EventService;
import com.volunteerportal.app.service.InitiativeService;
import com.volunteerportal.app.service.JoinRequestService;
import com.volunteerportal.app.service.NotificationService;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(CoordinatorEventController.class)
@Import(SecurityConfig.class)
class CoordinatorEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService eventService;

    @MockitoBean
    private InitiativeService initiativeService;

    @MockitoBean
    private JoinRequestService joinRequestService;

    // GlobalModelAttributes (a @ControllerAdvice picked up by the web slice) depends on this.
    @MockitoBean
    private NotificationService notificationService;

    private final UserPrincipal coordinator = userPrincipal(42L, "coord", "COORDINATOR");

    @Test
    void list_managingCoordinator_seesTheInitiativesEvents() throws Exception {
        Initiative initiative = managedInitiative(5L);
        given(eventService.findAllForInitiative(5L)).willReturn(List.of(event(9L, initiative)));

        mockMvc.perform(get("/coordinator/initiatives/5/events").with(user(coordinator)))
                .andExpect(status().isOk())
                .andExpect(view().name("coordinator/initiatives/events"))
                .andExpect(content().string(containsString("Kickoff")))
                .andExpect(content().string(containsString("/coordinator/initiatives/5/events/9/edit")));
    }

    @Test
    void newForm_postsBackToTheCoordinatorEventsUrl() throws Exception {
        managedInitiative(5L);

        mockMvc.perform(get("/coordinator/initiatives/5/events/new").with(user(coordinator)))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/initiatives/events/form"))
                .andExpect(content().string(containsString("action=\"/coordinator/initiatives/5/events\"")));
    }

    @Test
    void create_managingCoordinator_createsAndRedirects() throws Exception {
        managedInitiative(5L);

        mockMvc.perform(post("/coordinator/initiatives/5/events").with(user(coordinator)).with(csrf())
                        .param("name", "Beach Day")
                        .param("enabled", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/coordinator/initiatives/5/events"));

        verify(eventService).create(eq(5L), any());
    }

    @Test
    void create_missingName_rendersFormWithFieldError() throws Exception {
        managedInitiative(5L);

        mockMvc.perform(post("/coordinator/initiatives/5/events").with(user(coordinator)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/initiatives/events/form"))
                .andExpect(model().attributeHasFieldErrors("eventForm", "name"));

        verify(eventService, never()).create(any(), any());
    }

    @Test
    void create_coordinatorWhoDoesNotManageTheInitiative_isForbidden() throws Exception {
        given(initiativeService.findById(5L)).willReturn(initiative(5L));
        given(joinRequestService.canManage(any(), anyLong())).willReturn(false);

        mockMvc.perform(post("/coordinator/initiatives/5/events").with(user(coordinator)).with(csrf())
                        .param("name", "Beach Day"))
                .andExpect(status().isForbidden());

        verify(eventService, never()).create(any(), any());
    }

    @Test
    void create_admin_isAllowedForAnyInitiative() throws Exception {
        given(initiativeService.findById(5L)).willReturn(initiative(5L));

        mockMvc.perform(post("/coordinator/initiatives/5/events").with(user(userPrincipal(1L, "admin", "ADMIN"))).with(csrf())
                        .param("name", "Beach Day"))
                .andExpect(redirectedUrl("/coordinator/initiatives/5/events"));

        verify(eventService).create(eq(5L), any());
    }

    @Test
    void list_volunteer_isForbidden() throws Exception {
        mockMvc.perform(get("/coordinator/initiatives/5/events").with(user(userPrincipal(50L, "vol", "VOLUNTEER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void editForm_prefillsTheEvent() throws Exception {
        Initiative initiative = managedInitiative(5L);
        given(eventService.findById(9L)).willReturn(event(9L, initiative));

        mockMvc.perform(get("/coordinator/initiatives/5/events/9/edit").with(user(coordinator)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("eventId", 9L))
                .andExpect(content().string(containsString("action=\"/coordinator/initiatives/5/events/9\"")))
                .andExpect(content().string(containsString("value=\"Kickoff\"")));
    }

    @Test
    void update_managingCoordinator_updatesAndRedirects() throws Exception {
        Initiative initiative = managedInitiative(5L);
        given(eventService.findById(9L)).willReturn(event(9L, initiative));

        mockMvc.perform(post("/coordinator/initiatives/5/events/9").with(user(coordinator)).with(csrf())
                        .param("name", "Renamed"))
                .andExpect(redirectedUrl("/coordinator/initiatives/5/events"));

        verify(eventService).update(eq(9L), any());
    }

    @Test
    void update_eventOfAnotherInitiative_isForbidden() throws Exception {
        managedInitiative(5L);
        given(eventService.findById(9L)).willReturn(event(9L, initiative(6L)));

        mockMvc.perform(post("/coordinator/initiatives/5/events/9").with(user(coordinator)).with(csrf())
                        .param("name", "Hijacked"))
                .andExpect(status().isForbidden());

        verify(eventService, never()).update(any(), any());
    }

    private Initiative managedInitiative(Long id) {
        Initiative initiative = initiative(id);
        given(initiativeService.findById(id)).willReturn(initiative);
        given(joinRequestService.canManage(initiative, 42L)).willReturn(true);
        return initiative;
    }

    private Initiative initiative(Long id) {
        Initiative initiative = new Initiative();
        initiative.setId(id);
        initiative.setName("Initiative " + id);
        return initiative;
    }

    private Event event(Long id, Initiative initiative) {
        Event event = new Event();
        event.setId(id);
        event.setName("Kickoff");
        event.setInitiative(initiative);
        event.setEnabled(true);
        return event;
    }

    private static UserPrincipal userPrincipal(Long id, String username, String roleName) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setRoles(new HashSet<>(Set.of(new Role(roleName))));
        return new UserPrincipal(user);
    }
}
