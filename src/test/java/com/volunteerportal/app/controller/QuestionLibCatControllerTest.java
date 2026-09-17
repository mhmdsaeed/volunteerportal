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
import com.volunteerportal.app.model.QuestionLibCat;
import com.volunteerportal.app.service.NotificationService;
import com.volunteerportal.app.service.QuestionLibCatService;

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

@WebMvcTest(QuestionLibCatController.class)
@Import(SecurityConfig.class)
class QuestionLibCatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private QuestionLibCatService questionLibCatService;

    // GlobalModelAttributes (a @ControllerAdvice picked up by the web slice) depends on this.
    @MockitoBean
    private NotificationService notificationService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void list_rendersCategoriesFromService() throws Exception {
        given(questionLibCatService.findAll()).willReturn(List.of());

        mockMvc.perform(get("/admin/question-library"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/question-library/list"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void newForm_showsBlankForm() throws Exception {
        mockMvc.perform(get("/admin/question-library/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/question-library/form"))
                .andExpect(model().attributeExists("categoryForm"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_blankName_rendersFormWithFieldError() throws Exception {
        mockMvc.perform(post("/admin/question-library").with(csrf())
                        .param("name", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/question-library/form"))
                .andExpect(model().attributeHasFieldErrors("categoryForm", "name"));

        verify(questionLibCatService, never()).create(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_valid_savesAndRedirects() throws Exception {
        mockMvc.perform(post("/admin/question-library").with(csrf())
                        .param("name", "Onboarding"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/question-library"));

        verify(questionLibCatService).create(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void editForm_populatesFormFromExistingCategory() throws Exception {
        QuestionLibCat category = new QuestionLibCat();
        category.setId(5L);
        category.setName("Existing Category");
        given(questionLibCatService.findById(5L)).willReturn(category);

        mockMvc.perform(get("/admin/question-library/5/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/question-library/form"))
                .andExpect(model().attribute("categoryId", 5L));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_valid_updatesAndRedirects() throws Exception {
        mockMvc.perform(post("/admin/question-library/5").with(csrf())
                        .param("name", "Updated Category"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/question-library"));

        verify(questionLibCatService).update(eq(5L), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_removesAndRedirects() throws Exception {
        mockMvc.perform(post("/admin/question-library/5/delete").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/question-library"));

        verify(questionLibCatService).delete(5L);
    }
}
