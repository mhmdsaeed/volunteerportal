package com.volunteerportal.app.controller;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.volunteerportal.app.config.SecurityConfig;
import com.volunteerportal.app.model.Event;
import com.volunteerportal.app.model.Initiative;
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

@WebMvcTest(EventController.class)
@Import(SecurityConfig.class)
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService eventService;

    @MockitoBean
    private InitiativeService initiativeService;

    // GlobalModelAttributes (a @ControllerAdvice picked up by the web slice) depends on this.
    @MockitoBean
    private NotificationService notificationService;

    private Initiative initiative(Long id) {
        Initiative initiative = new Initiative();
        initiative.setId(id);
        initiative.setName("Beach Cleanup");
        return initiative;
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void list_rendersEventsFromService() throws Exception {
        given(initiativeService.findById(5L)).willReturn(initiative(5L));
        given(eventService.findAllForInitiative(5L)).willReturn(List.of());

        mockMvc.perform(get("/admin/initiatives/5/events"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/initiatives/events/list"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void newForm_showsBlankForm() throws Exception {
        given(initiativeService.findById(5L)).willReturn(initiative(5L));

        mockMvc.perform(get("/admin/initiatives/5/events/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/initiatives/events/form"))
                .andExpect(model().attributeExists("eventForm"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_blankName_rendersFormWithFieldError() throws Exception {
        given(initiativeService.findById(5L)).willReturn(initiative(5L));

        mockMvc.perform(post("/admin/initiatives/5/events").with(csrf())
                        .param("name", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/initiatives/events/form"))
                .andExpect(model().attributeHasFieldErrors("eventForm", "name"));

        verify(eventService, never()).create(any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_valid_savesAndRedirects() throws Exception {
        mockMvc.perform(post("/admin/initiatives/5/events").with(csrf())
                        .param("name", "Kickoff Event"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/initiatives/5/events"));

        verify(eventService).create(eq(5L), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void editForm_populatesFormFromExistingEvent() throws Exception {
        given(initiativeService.findById(5L)).willReturn(initiative(5L));

        Event event = new Event();
        event.setId(9L);
        event.setName("Existing Event");
        event.setEnabled(true);
        given(eventService.findById(9L)).willReturn(event);

        mockMvc.perform(get("/admin/initiatives/5/events/9/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/initiatives/events/form"))
                .andExpect(model().attribute("eventId", 9L));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_valid_updatesAndRedirects() throws Exception {
        mockMvc.perform(post("/admin/initiatives/5/events/9").with(csrf())
                        .param("name", "Updated Event"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/initiatives/5/events"));

        verify(eventService).update(eq(9L), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_removesAndRedirects() throws Exception {
        mockMvc.perform(post("/admin/initiatives/5/events/9/delete").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/initiatives/5/events"));

        verify(eventService).delete(9L);
    }
}
