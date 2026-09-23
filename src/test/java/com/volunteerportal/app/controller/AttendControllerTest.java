package com.volunteerportal.app.controller;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.volunteerportal.app.config.SecurityConfig;
import com.volunteerportal.app.model.Attend;
import com.volunteerportal.app.model.Event;
import com.volunteerportal.app.model.Initiative;
import com.volunteerportal.app.model.VolunteerInitiative;
import com.volunteerportal.app.repository.VolunteerInitiativeRepository;
import com.volunteerportal.app.service.AttendService;
import com.volunteerportal.app.service.EventService;
import com.volunteerportal.app.service.InitiativeService;
import com.volunteerportal.app.service.NotificationService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(AttendController.class)
@Import(SecurityConfig.class)
class AttendControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AttendService attendService;

    @MockitoBean
    private EventService eventService;

    @MockitoBean
    private InitiativeService initiativeService;

    @MockitoBean
    private VolunteerInitiativeRepository volunteerInitiativeRepository;

    // GlobalModelAttributes (a @ControllerAdvice picked up by the web slice) depends on this.
    @MockitoBean
    private NotificationService notificationService;

    private Initiative initiative(Long id) {
        Initiative initiative = new Initiative();
        initiative.setId(id);
        initiative.setName("Beach Cleanup");
        return initiative;
    }

    private Event event(Long id) {
        Event event = new Event();
        event.setId(id);
        event.setName("Kickoff Event");
        return event;
    }

    private VolunteerInitiative member(Long id, Long initiativeId, Boolean approved) {
        VolunteerInitiative member = new VolunteerInitiative();
        member.setId(id);
        member.setInitiative(initiative(initiativeId));
        member.setEnabled(approved);
        return member;
    }

    private void stubParents() {
        given(initiativeService.findById(5L)).willReturn(initiative(5L));
        given(eventService.findById(9L)).willReturn(event(9L));
        given(volunteerInitiativeRepository.findByInitiativeIdAndEnabledTrue(5L)).willReturn(List.of());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void list_rendersAttendancesFromService() throws Exception {
        stubParents();
        given(attendService.findAllForEvent(9L)).willReturn(List.of());

        mockMvc.perform(get("/admin/initiatives/5/events/9/attendance"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/initiatives/events/attendance/list"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void newForm_showsBlankFormWithReferenceData() throws Exception {
        stubParents();

        mockMvc.perform(get("/admin/initiatives/5/events/9/attendance/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/initiatives/events/attendance/form"))
                .andExpect(model().attributeExists("attendForm"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_missingVolunteer_rendersFormWithFieldError() throws Exception {
        stubParents();

        mockMvc.perform(post("/admin/initiatives/5/events/9/attendance").with(csrf())
                        .param("attendInOut", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/initiatives/events/attendance/form"))
                .andExpect(model().attributeHasFieldErrors("attendForm", "volunteerInitiativeId"));

        verify(attendService, never()).create(any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_valid_savesAndRedirects() throws Exception {
        given(volunteerInitiativeRepository.findById(3L)).willReturn(Optional.of(member(3L, 5L, true)));

        mockMvc.perform(post("/admin/initiatives/5/events/9/attendance").with(csrf())
                        .param("volunteerInitiativeId", "3")
                        .param("attendInOut", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/initiatives/5/events/9/attendance"));

        verify(attendService).create(eq(9L), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_pendingVolunteer_rendersFormWithFieldError() throws Exception {
        stubParents();
        given(volunteerInitiativeRepository.findById(3L)).willReturn(Optional.of(member(3L, 5L, null)));

        mockMvc.perform(post("/admin/initiatives/5/events/9/attendance").with(csrf())
                        .param("volunteerInitiativeId", "3")
                        .param("attendInOut", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/initiatives/events/attendance/form"))
                .andExpect(model().attributeHasFieldErrorCode("attendForm", "volunteerInitiativeId", "error.attend.notApprovedMember"));

        verify(attendService, never()).create(any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_approvedMemberOfAnotherInitiative_rendersFormWithFieldError() throws Exception {
        stubParents();
        given(volunteerInitiativeRepository.findById(3L)).willReturn(Optional.of(member(3L, 6L, true)));

        mockMvc.perform(post("/admin/initiatives/5/events/9/attendance").with(csrf())
                        .param("volunteerInitiativeId", "3")
                        .param("attendInOut", "1"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrorCode("attendForm", "volunteerInitiativeId", "error.attend.notApprovedMember"));

        verify(attendService, never()).create(any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void newForm_listsOnlyApprovedMembers() throws Exception {
        stubParents();
        List<VolunteerInitiative> approved = List.of(member(3L, 5L, true));
        given(volunteerInitiativeRepository.findByInitiativeIdAndEnabledTrue(5L)).willReturn(approved);

        mockMvc.perform(get("/admin/initiatives/5/events/9/attendance/new"))
                .andExpect(model().attribute("volunteerInitiatives", approved));

        verify(volunteerInitiativeRepository, never()).findByInitiativeId(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void editForm_populatesFormFromExistingRecord() throws Exception {
        stubParents();

        Attend attend = new Attend();
        attend.setId(11L);
        attend.setAttendInOut(1);
        given(attendService.findById(11L)).willReturn(attend);

        mockMvc.perform(get("/admin/initiatives/5/events/9/attendance/11/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/initiatives/events/attendance/form"))
                .andExpect(model().attribute("attendId", 11L));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_valid_updatesAndRedirects() throws Exception {
        given(volunteerInitiativeRepository.findById(3L)).willReturn(Optional.of(member(3L, 5L, true)));

        mockMvc.perform(post("/admin/initiatives/5/events/9/attendance/11").with(csrf())
                        .param("volunteerInitiativeId", "3")
                        .param("attendInOut", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/initiatives/5/events/9/attendance"));

        verify(attendService).update(eq(11L), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_removesAndRedirects() throws Exception {
        mockMvc.perform(post("/admin/initiatives/5/events/9/attendance/11/delete").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/initiatives/5/events/9/attendance"));

        verify(attendService).delete(11L);
    }
}
