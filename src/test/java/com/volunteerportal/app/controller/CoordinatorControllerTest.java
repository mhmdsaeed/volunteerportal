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
import com.volunteerportal.app.model.Initiative;
import com.volunteerportal.app.model.Role;
import com.volunteerportal.app.model.User;
import com.volunteerportal.app.model.VolunteerInitiative;
import com.volunteerportal.app.security.UserPrincipal;
import com.volunteerportal.app.service.InitiativeService;
import com.volunteerportal.app.service.JoinRequestService;
import com.volunteerportal.app.service.NotificationService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Who may manage an initiative (supervisor or office coordinator) is decided by
 * {@link JoinRequestService#canManage}, which is unit-tested in JoinRequestServiceImplTest;
 * here it's stubbed so these tests cover how the controller enforces it.
 */
@WebMvcTest(CoordinatorController.class)
@Import(SecurityConfig.class)
class CoordinatorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JoinRequestService joinRequestService;

    @MockitoBean
    private InitiativeService initiativeService;

    // GlobalModelAttributes (a @ControllerAdvice picked up by the web slice) depends on this.
    @MockitoBean
    private NotificationService notificationService;

    @Test
    void list_asCoordinator_filtersByOwnUserId() throws Exception {
        UserPrincipal coordinator = userPrincipal(42L, "coord", "COORDINATOR");
        given(joinRequestService.findManagedInitiatives(42L)).willReturn(List.of());

        mockMvc.perform(get("/coordinator/initiatives").with(user(coordinator)))
                .andExpect(status().isOk());

        verify(joinRequestService).findManagedInitiatives(42L);
    }

    @Test
    void list_asAdmin_passesNullToSeeAllInitiatives() throws Exception {
        UserPrincipal admin = userPrincipal(1L, "admin", "ADMIN");
        given(joinRequestService.findManagedInitiatives(null)).willReturn(List.of());

        mockMvc.perform(get("/coordinator/initiatives").with(user(admin)))
                .andExpect(status().isOk());

        verify(joinRequestService).findManagedInitiatives(null);
    }

    @Test
    void requests_managingCoordinator_isAllowed() throws Exception {
        UserPrincipal coordinator = userPrincipal(42L, "coord", "COORDINATOR");
        Initiative initiative = initiative(5L);
        given(initiativeService.findById(5L)).willReturn(initiative);
        given(joinRequestService.canManage(initiative, 42L)).willReturn(true);
        given(joinRequestService.findRequestsForInitiative(5L)).willReturn(List.of());

        mockMvc.perform(get("/coordinator/initiatives/5/requests").with(user(coordinator)))
                .andExpect(status().isOk());
    }

    @Test
    void requests_nonManagingCoordinator_isForbidden() throws Exception {
        UserPrincipal coordinator = userPrincipal(42L, "coord", "COORDINATOR");
        given(initiativeService.findById(5L)).willReturn(initiative(5L));
        given(joinRequestService.canManage(any(), anyLong())).willReturn(false);

        mockMvc.perform(get("/coordinator/initiatives/5/requests").with(user(coordinator)))
                .andExpect(status().isForbidden());
    }

    @Test
    void requests_admin_isAllowedWithoutManagingTheInitiative() throws Exception {
        UserPrincipal admin = userPrincipal(1L, "admin", "ADMIN");
        given(initiativeService.findById(5L)).willReturn(initiative(5L));
        given(joinRequestService.findRequestsForInitiative(5L)).willReturn(List.of());

        mockMvc.perform(get("/coordinator/initiatives/5/requests").with(user(admin)))
                .andExpect(status().isOk());

        verify(joinRequestService, never()).canManage(any(), anyLong());
    }

    @Test
    void approve_requestBelongsToInitiative_succeedsAndRedirects() throws Exception {
        UserPrincipal coordinator = userPrincipal(42L, "coord", "COORDINATOR");
        Initiative initiative = initiative(5L);
        given(initiativeService.findById(5L)).willReturn(initiative);
        given(joinRequestService.canManage(initiative, 42L)).willReturn(true);
        given(joinRequestService.findById(7L)).willReturn(requestFor(7L, initiative));

        mockMvc.perform(post("/coordinator/initiatives/5/requests/7/approve").with(user(coordinator)).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/coordinator/initiatives/5/requests"))
                .andExpect(flash().attribute("decision", "approved"));

        verify(joinRequestService).approve(7L);
    }

    @Test
    void approve_requestBelongsToDifferentInitiative_isForbidden() throws Exception {
        UserPrincipal coordinator = userPrincipal(42L, "coord", "COORDINATOR");
        Initiative initiative = initiative(5L);
        given(initiativeService.findById(5L)).willReturn(initiative);
        given(joinRequestService.canManage(initiative, 42L)).willReturn(true);
        given(joinRequestService.findById(7L)).willReturn(requestFor(7L, initiative(6L)));

        mockMvc.perform(post("/coordinator/initiatives/5/requests/7/approve").with(user(coordinator)).with(csrf()))
                .andExpect(status().isForbidden());

        verify(joinRequestService, never()).approve(any());
    }

    @Test
    void reject_nonManagingCoordinator_isForbidden() throws Exception {
        UserPrincipal coordinator = userPrincipal(42L, "coord", "COORDINATOR");
        given(initiativeService.findById(5L)).willReturn(initiative(5L));
        given(joinRequestService.canManage(any(), anyLong())).willReturn(false);

        mockMvc.perform(post("/coordinator/initiatives/5/requests/7/reject").with(user(coordinator)).with(csrf()))
                .andExpect(status().isForbidden());

        verify(joinRequestService, never()).reject(any());
    }

    // --- /coordinator/requests: all pending requests in one place ---

    @Test
    void pendingRequests_asCoordinator_showsOnlyTheirPendingRequests() throws Exception {
        UserPrincipal coordinator = userPrincipal(42L, "coord", "COORDINATOR");
        List<VolunteerInitiative> pending = List.of(requestFor(7L, initiative(5L)));
        given(joinRequestService.findPendingRequests(42L)).willReturn(pending);

        mockMvc.perform(get("/coordinator/requests").with(user(coordinator)))
                .andExpect(status().isOk())
                .andExpect(view().name("coordinator/requests/pending"))
                .andExpect(model().attribute("requests", pending));
    }

    @Test
    void pendingRequests_asAdmin_showsEveryPendingRequest() throws Exception {
        UserPrincipal admin = userPrincipal(1L, "admin", "ADMIN");
        given(joinRequestService.findPendingRequests(null)).willReturn(List.of());

        mockMvc.perform(get("/coordinator/requests").with(user(admin)))
                .andExpect(status().isOk());

        verify(joinRequestService).findPendingRequests(null);
    }

    @Test
    void pendingRequests_volunteer_isForbidden() throws Exception {
        UserPrincipal volunteer = userPrincipal(50L, "vol", "VOLUNTEER");

        mockMvc.perform(get("/coordinator/requests").with(user(volunteer)))
                .andExpect(status().isForbidden());
    }

    @Test
    void approvePending_managingCoordinator_approvesAndReturnsToPendingList() throws Exception {
        UserPrincipal coordinator = userPrincipal(42L, "coord", "COORDINATOR");
        Initiative initiative = initiative(5L);
        given(joinRequestService.findById(7L)).willReturn(requestFor(7L, initiative));
        given(joinRequestService.canManage(initiative, 42L)).willReturn(true);

        mockMvc.perform(post("/coordinator/requests/7/approve").with(user(coordinator)).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/coordinator/requests"))
                .andExpect(flash().attribute("decision", "approved"));

        verify(joinRequestService).approve(7L);
    }

    @Test
    void rejectPending_managingCoordinator_rejects() throws Exception {
        UserPrincipal coordinator = userPrincipal(42L, "coord", "COORDINATOR");
        Initiative initiative = initiative(5L);
        given(joinRequestService.findById(7L)).willReturn(requestFor(7L, initiative));
        given(joinRequestService.canManage(initiative, 42L)).willReturn(true);

        mockMvc.perform(post("/coordinator/requests/7/reject").with(user(coordinator)).with(csrf()))
                .andExpect(redirectedUrl("/coordinator/requests"))
                .andExpect(flash().attribute("decision", "rejected"));

        verify(joinRequestService).reject(7L);
    }

    @Test
    void approvePending_nonManagingCoordinator_isForbidden() throws Exception {
        UserPrincipal coordinator = userPrincipal(42L, "coord", "COORDINATOR");
        given(joinRequestService.findById(7L)).willReturn(requestFor(7L, initiative(5L)));
        given(joinRequestService.canManage(any(), anyLong())).willReturn(false);

        mockMvc.perform(post("/coordinator/requests/7/approve").with(user(coordinator)).with(csrf()))
                .andExpect(status().isForbidden());

        verify(joinRequestService, never()).approve(any());
    }

    @Test
    void approvePending_alreadyDecided_showsNoticeInsteadOfError() throws Exception {
        UserPrincipal admin = userPrincipal(1L, "admin", "ADMIN");
        given(joinRequestService.findById(7L)).willReturn(requestFor(7L, initiative(5L)));
        given(joinRequestService.approve(7L)).willThrow(new IllegalStateException("already decided"));

        mockMvc.perform(post("/coordinator/requests/7/approve").with(user(admin)).with(csrf()))
                .andExpect(redirectedUrl("/coordinator/requests"))
                .andExpect(flash().attribute("decision", "alreadyDecided"));
    }

    private Initiative initiative(Long initiativeId) {
        Initiative initiative = new Initiative();
        initiative.setId(initiativeId);
        initiative.setName("Initiative " + initiativeId);
        return initiative;
    }

    private VolunteerInitiative requestFor(Long requestId, Initiative initiative) {
        VolunteerInitiative request = new VolunteerInitiative();
        request.setId(requestId);
        request.setInitiative(initiative);
        return request;
    }

    private UserPrincipal userPrincipal(Long id, String username, String roleName) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setRoles(new HashSet<>(Set.of(new Role(roleName))));
        return new UserPrincipal(user);
    }
}
