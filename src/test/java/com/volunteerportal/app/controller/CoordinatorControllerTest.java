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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
    void requests_supervisingCoordinator_isAllowed() throws Exception {
        UserPrincipal coordinator = userPrincipal(42L, "coord", "COORDINATOR");
        Initiative initiative = initiativeSupervisedBy(5L, 42L);
        given(initiativeService.findById(5L)).willReturn(initiative);
        given(joinRequestService.findRequestsForInitiative(5L)).willReturn(List.of());

        mockMvc.perform(get("/coordinator/initiatives/5/requests").with(user(coordinator)))
                .andExpect(status().isOk());
    }

    @Test
    void requests_nonSupervisingCoordinator_isForbidden() throws Exception {
        UserPrincipal coordinator = userPrincipal(42L, "coord", "COORDINATOR");
        Initiative initiative = initiativeSupervisedBy(5L, 999L);
        given(initiativeService.findById(5L)).willReturn(initiative);

        mockMvc.perform(get("/coordinator/initiatives/5/requests").with(user(coordinator)))
                .andExpect(status().isForbidden());
    }

    @Test
    void requests_admin_isAllowedRegardlessOfSupervisor() throws Exception {
        UserPrincipal admin = userPrincipal(1L, "admin", "ADMIN");
        Initiative initiative = initiativeSupervisedBy(5L, 999L);
        given(initiativeService.findById(5L)).willReturn(initiative);
        given(joinRequestService.findRequestsForInitiative(5L)).willReturn(List.of());

        mockMvc.perform(get("/coordinator/initiatives/5/requests").with(user(admin)))
                .andExpect(status().isOk());
    }

    @Test
    void approve_requestBelongsToInitiative_succeedsAndRedirects() throws Exception {
        UserPrincipal coordinator = userPrincipal(42L, "coord", "COORDINATOR");
        Initiative initiative = initiativeSupervisedBy(5L, 42L);
        VolunteerInitiative request = requestFor(7L, initiative);

        given(initiativeService.findById(5L)).willReturn(initiative);
        given(joinRequestService.findById(7L)).willReturn(request);

        mockMvc.perform(post("/coordinator/initiatives/5/requests/7/approve").with(user(coordinator)).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/coordinator/initiatives/5/requests"));

        verify(joinRequestService).approve(7L);
    }

    @Test
    void approve_requestBelongsToDifferentInitiative_isForbidden() throws Exception {
        UserPrincipal coordinator = userPrincipal(42L, "coord", "COORDINATOR");
        Initiative initiative = initiativeSupervisedBy(5L, 42L);
        Initiative otherInitiative = initiativeSupervisedBy(6L, 42L);
        VolunteerInitiative request = requestFor(7L, otherInitiative);

        given(initiativeService.findById(5L)).willReturn(initiative);
        given(joinRequestService.findById(7L)).willReturn(request);

        mockMvc.perform(post("/coordinator/initiatives/5/requests/7/approve").with(user(coordinator)).with(csrf()))
                .andExpect(status().isForbidden());

        verify(joinRequestService, never()).approve(any());
    }

    @Test
    void reject_nonSupervisingCoordinator_isForbidden() throws Exception {
        UserPrincipal coordinator = userPrincipal(42L, "coord", "COORDINATOR");
        Initiative initiative = initiativeSupervisedBy(5L, 999L);
        given(initiativeService.findById(5L)).willReturn(initiative);

        mockMvc.perform(post("/coordinator/initiatives/5/requests/7/reject").with(user(coordinator)).with(csrf()))
                .andExpect(status().isForbidden());

        verify(joinRequestService, never()).reject(any());
    }

    private Initiative initiativeSupervisedBy(Long initiativeId, Long supervisorId) {
        Initiative initiative = new Initiative();
        initiative.setId(initiativeId);
        User supervisor = new User();
        supervisor.setId(supervisorId);
        initiative.setSupervisor(supervisor);
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
