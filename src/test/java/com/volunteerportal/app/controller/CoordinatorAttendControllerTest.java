package com.volunteerportal.app.controller;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.volunteerportal.app.config.SecurityConfig;
import com.volunteerportal.app.model.Attend;
import com.volunteerportal.app.model.Event;
import com.volunteerportal.app.model.Initiative;
import com.volunteerportal.app.model.Role;
import com.volunteerportal.app.model.User;
import com.volunteerportal.app.model.VolunteerInitiative;
import com.volunteerportal.app.repository.VolunteerInitiativeRepository;
import com.volunteerportal.app.security.UserPrincipal;
import com.volunteerportal.app.service.AttendService;
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

@WebMvcTest(CoordinatorAttendController.class)
@Import(SecurityConfig.class)
class CoordinatorAttendControllerTest {

    private static final String BASE = "/coordinator/initiatives/5/events/9/attendance";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AttendService attendService;

    @MockitoBean
    private EventService eventService;

    @MockitoBean
    private InitiativeService initiativeService;

    @MockitoBean
    private JoinRequestService joinRequestService;

    @MockitoBean
    private VolunteerInitiativeRepository volunteerInitiativeRepository;

    // GlobalModelAttributes (a @ControllerAdvice picked up by the web slice) depends on this.
    @MockitoBean
    private NotificationService notificationService;

    private final UserPrincipal coordinator = userPrincipal(42L, "coord", "COORDINATOR");

    @Test
    void list_managingCoordinator_showsAttendanceWithCoordinatorLinks() throws Exception {
        Event event = managedEvent();
        given(attendService.findAllForEvent(9L)).willReturn(List.of(attend(11L, event)));

        mockMvc.perform(get(BASE).with(user(coordinator)))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/initiatives/events/attendance/list"))
                .andExpect(content().string(containsString(BASE + "/new")))
                .andExpect(content().string(containsString(BASE + "/11/edit")))
                .andExpect(content().string(containsString(BASE + "/11/delete")))
                .andExpect(content().string(containsString("href=\"/coordinator/initiatives/5/events\"")));
    }

    @Test
    void newForm_listsOnlyApprovedMembersAndPostsToCoordinatorUrl() throws Exception {
        managedEvent();
        List<VolunteerInitiative> approved = List.of(member(3L, 5L, true));
        given(volunteerInitiativeRepository.findByInitiativeIdAndEnabledTrue(5L)).willReturn(approved);

        mockMvc.perform(get(BASE + "/new").with(user(coordinator)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("volunteerInitiatives", approved))
                .andExpect(content().string(containsString("action=\"" + BASE + "\"")));
    }

    @Test
    void create_approvedMember_recordsAndRedirects() throws Exception {
        managedEvent();
        given(volunteerInitiativeRepository.findById(3L)).willReturn(Optional.of(member(3L, 5L, true)));

        mockMvc.perform(post(BASE).with(user(coordinator)).with(csrf())
                        .param("volunteerInitiativeId", "3")
                        .param("attendInOut", "1"))
                .andExpect(redirectedUrl(BASE));

        verify(attendService).create(eq(9L), any());
    }

    @Test
    void create_pendingMember_rendersFormWithFieldError() throws Exception {
        managedEvent();
        given(volunteerInitiativeRepository.findById(3L)).willReturn(Optional.of(member(3L, 5L, null)));

        mockMvc.perform(post(BASE).with(user(coordinator)).with(csrf())
                        .param("volunteerInitiativeId", "3")
                        .param("attendInOut", "1"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrorCode("attendForm", "volunteerInitiativeId", "error.attend.notApprovedMember"));

        verify(attendService, never()).create(any(), any());
    }

    @Test
    void create_coordinatorWhoDoesNotManageTheInitiative_isForbidden() throws Exception {
        given(initiativeService.findById(5L)).willReturn(initiative(5L));
        given(joinRequestService.canManage(any(), anyLong())).willReturn(false);

        mockMvc.perform(post(BASE).with(user(coordinator)).with(csrf())
                        .param("volunteerInitiativeId", "3")
                        .param("attendInOut", "1"))
                .andExpect(status().isForbidden());

        verify(attendService, never()).create(any(), any());
    }

    @Test
    void list_eventOfAnotherInitiative_isForbidden() throws Exception {
        Initiative initiative = managedInitiative();
        given(eventService.findById(9L)).willReturn(event(9L, initiative(6L)));

        mockMvc.perform(get(BASE).with(user(coordinator)))
                .andExpect(status().isForbidden());
    }

    @Test
    void editForm_prefillsTheRecord() throws Exception {
        Event event = managedEvent();
        Attend attend = attend(11L, event);
        attend.setVolunteerInitiative(member(3L, 5L, true));
        given(attendService.findById(11L)).willReturn(attend);

        mockMvc.perform(get(BASE + "/11/edit").with(user(coordinator)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("attendId", 11L))
                .andExpect(content().string(containsString("action=\"" + BASE + "/11\"")));
    }

    @Test
    void update_recordOfAnotherEvent_isForbidden() throws Exception {
        managedEvent();
        given(attendService.findById(11L)).willReturn(attend(11L, event(99L, initiative(5L))));

        mockMvc.perform(post(BASE + "/11").with(user(coordinator)).with(csrf())
                        .param("volunteerInitiativeId", "3")
                        .param("attendInOut", "2"))
                .andExpect(status().isForbidden());

        verify(attendService, never()).update(any(), any());
    }

    @Test
    void delete_ownEventsRecord_deletesAndRedirects() throws Exception {
        Event event = managedEvent();
        given(attendService.findById(11L)).willReturn(attend(11L, event));

        mockMvc.perform(post(BASE + "/11/delete").with(user(coordinator)).with(csrf()))
                .andExpect(redirectedUrl(BASE));

        verify(attendService).delete(11L);
    }

    @Test
    void delete_recordOfAnotherEvent_isForbidden() throws Exception {
        managedEvent();
        given(attendService.findById(11L)).willReturn(attend(11L, event(99L, initiative(5L))));

        mockMvc.perform(post(BASE + "/11/delete").with(user(coordinator)).with(csrf()))
                .andExpect(status().isForbidden());

        verify(attendService, never()).delete(any());
    }

    private Initiative managedInitiative() {
        Initiative initiative = initiative(5L);
        given(initiativeService.findById(5L)).willReturn(initiative);
        given(joinRequestService.canManage(initiative, 42L)).willReturn(true);
        return initiative;
    }

    private Event managedEvent() {
        Event event = event(9L, managedInitiative());
        given(eventService.findById(9L)).willReturn(event);
        return event;
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
        return event;
    }

    private Attend attend(Long id, Event event) {
        Attend attend = new Attend();
        attend.setId(id);
        attend.setEvent(event);
        attend.setAttendInOut(1);
        return attend;
    }

    private VolunteerInitiative member(Long id, Long initiativeId, Boolean approved) {
        VolunteerInitiative member = new VolunteerInitiative();
        member.setId(id);
        member.setInitiative(initiative(initiativeId));
        member.setEnabled(approved);
        return member;
    }

    private static UserPrincipal userPrincipal(Long id, String username, String roleName) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setRoles(new HashSet<>(Set.of(new Role(roleName))));
        return new UserPrincipal(user);
    }
}
