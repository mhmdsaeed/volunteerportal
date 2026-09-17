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
import com.volunteerportal.app.model.Initiative;
import com.volunteerportal.app.repository.OfficeRepository;
import com.volunteerportal.app.repository.UserRepository;
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

@WebMvcTest(InitiativeController.class)
@Import(SecurityConfig.class)
class InitiativeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InitiativeService initiativeService;

    @MockitoBean
    private OfficeRepository officeRepository;

    @MockitoBean
    private UserRepository userRepository;

    // GlobalModelAttributes (a @ControllerAdvice picked up by the web slice) depends on this.
    @MockitoBean
    private NotificationService notificationService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void list_rendersInitiativesFromService() throws Exception {
        given(initiativeService.findAll()).willReturn(List.of());

        mockMvc.perform(get("/admin/initiatives"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/initiatives/list"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void newForm_showsBlankFormWithReferenceData() throws Exception {
        given(officeRepository.findAll()).willReturn(List.of());
        given(userRepository.findAll()).willReturn(List.of());

        mockMvc.perform(get("/admin/initiatives/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/initiatives/form"))
                .andExpect(model().attributeExists("initiativeForm"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_blankName_rendersFormWithFieldError() throws Exception {
        mockMvc.perform(post("/admin/initiatives").with(csrf())
                        .param("name", "")
                        .param("enabled", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/initiatives/form"))
                .andExpect(model().attributeHasFieldErrors("initiativeForm", "name"));

        verify(initiativeService, never()).create(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_valid_savesAndRedirects() throws Exception {
        mockMvc.perform(post("/admin/initiatives").with(csrf())
                        .param("name", "Beach Cleanup")
                        .param("enabled", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/initiatives"));

        verify(initiativeService).create(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void editForm_populatesFormFromExistingInitiative() throws Exception {
        Initiative initiative = new Initiative();
        initiative.setId(5L);
        initiative.setName("Existing Initiative");
        initiative.setDescription("Some description");
        initiative.setEnabled(true);
        given(initiativeService.findById(5L)).willReturn(initiative);

        mockMvc.perform(get("/admin/initiatives/5/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/initiatives/form"))
                .andExpect(model().attribute("initiativeId", 5L));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_valid_updatesAndRedirects() throws Exception {
        mockMvc.perform(post("/admin/initiatives/5").with(csrf())
                        .param("name", "Updated Name")
                        .param("enabled", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/initiatives"));

        verify(initiativeService).update(eq(5L), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_removesAndRedirects() throws Exception {
        mockMvc.perform(post("/admin/initiatives/5/delete").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/initiatives"));

        verify(initiativeService).delete(5L);
    }
}
