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
import com.volunteerportal.app.model.Grade;
import com.volunteerportal.app.service.GradeService;
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

@WebMvcTest(GradeController.class)
@Import(SecurityConfig.class)
class GradeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GradeService gradeService;

    // GlobalModelAttributes (a @ControllerAdvice picked up by the web slice) depends on this.
    @MockitoBean
    private NotificationService notificationService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void list_rendersGradesFromService() throws Exception {
        given(gradeService.findAll()).willReturn(List.of());

        mockMvc.perform(get("/admin/grades"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/grades/list"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void newForm_showsBlankForm() throws Exception {
        mockMvc.perform(get("/admin/grades/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/grades/form"))
                .andExpect(model().attributeExists("gradeForm"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_blankName_rendersFormWithFieldError() throws Exception {
        mockMvc.perform(post("/admin/grades").with(csrf())
                        .param("name", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/grades/form"))
                .andExpect(model().attributeHasFieldErrors("gradeForm", "name"));

        verify(gradeService, never()).create(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_valid_savesAndRedirects() throws Exception {
        mockMvc.perform(post("/admin/grades").with(csrf())
                        .param("name", "Gold"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/grades"));

        verify(gradeService).create(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void editForm_populatesFormFromExistingGrade() throws Exception {
        Grade grade = new Grade();
        grade.setId(5L);
        grade.setName("Silver");
        given(gradeService.findById(5L)).willReturn(grade);

        mockMvc.perform(get("/admin/grades/5/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/grades/form"))
                .andExpect(model().attribute("gradeId", 5L));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_valid_updatesAndRedirects() throws Exception {
        mockMvc.perform(post("/admin/grades/5").with(csrf())
                        .param("name", "Platinum"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/grades"));

        verify(gradeService).update(eq(5L), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_removesAndRedirects() throws Exception {
        mockMvc.perform(post("/admin/grades/5/delete").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/grades"));

        verify(gradeService).delete(5L);
    }
}
