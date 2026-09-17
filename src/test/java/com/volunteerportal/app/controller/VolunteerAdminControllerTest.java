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
import com.volunteerportal.app.model.User;
import com.volunteerportal.app.repository.GradeRepository;
import com.volunteerportal.app.service.NotificationService;
import com.volunteerportal.app.service.VolunteerAdminService;

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

@WebMvcTest(VolunteerAdminController.class)
@Import(SecurityConfig.class)
class VolunteerAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VolunteerAdminService volunteerAdminService;

    @MockitoBean
    private GradeRepository gradeRepository;

    // GlobalModelAttributes (a @ControllerAdvice picked up by the web slice) depends on this.
    @MockitoBean
    private NotificationService notificationService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void list_rendersVolunteersWithTheirProfiles() throws Exception {
        User volunteer = new User();
        volunteer.setId(1L);
        volunteer.setUsername("vol1");
        given(volunteerAdminService.findVolunteers()).willReturn(List.of(volunteer));
        given(volunteerAdminService.findProfileForUser(1L)).willReturn(Optional.empty());

        mockMvc.perform(get("/admin/volunteers"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/volunteers/list"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void editForm_noExistingProfile_defaultsPointsToZero() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setUsername("vol1");
        given(volunteerAdminService.findUserById(1L)).willReturn(user);
        given(volunteerAdminService.findProfileForUser(1L)).willReturn(Optional.empty());
        given(gradeRepository.findAll()).willReturn(List.of());

        mockMvc.perform(get("/admin/volunteers/1/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/volunteers/form"))
                .andExpect(model().attribute("volunteer", user));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_missingPoints_rendersFormWithFieldError() throws Exception {
        User user = new User();
        user.setId(1L);
        given(volunteerAdminService.findUserById(1L)).willReturn(user);
        given(gradeRepository.findAll()).willReturn(List.of());

        mockMvc.perform(post("/admin/volunteers/1").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/volunteers/form"))
                .andExpect(model().attributeHasFieldErrors("gradeForm", "points"));

        verify(volunteerAdminService, never()).updateGradeAndPoints(any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_valid_savesAndRedirects() throws Exception {
        mockMvc.perform(post("/admin/volunteers/1").with(csrf())
                        .param("points", "50"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/volunteers"));

        verify(volunteerAdminService).updateGradeAndPoints(eq(1L), any());
    }
}
