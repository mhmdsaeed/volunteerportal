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
import com.volunteerportal.app.model.QuestionLib;
import com.volunteerportal.app.model.QuestionLibCat;
import com.volunteerportal.app.service.NotificationService;
import com.volunteerportal.app.service.QuestionLibCatService;
import com.volunteerportal.app.service.QuestionLibService;

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

@WebMvcTest(QuestionLibController.class)
@Import(SecurityConfig.class)
class QuestionLibControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private QuestionLibService questionLibService;

    @MockitoBean
    private QuestionLibCatService questionLibCatService;

    // GlobalModelAttributes (a @ControllerAdvice picked up by the web slice) depends on this.
    @MockitoBean
    private NotificationService notificationService;

    private QuestionLibCat category(Long id) {
        QuestionLibCat category = new QuestionLibCat();
        category.setId(id);
        category.setName("Onboarding");
        return category;
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void list_rendersQuestionsFromService() throws Exception {
        given(questionLibCatService.findById(5L)).willReturn(category(5L));
        given(questionLibService.findAllForCategory(5L)).willReturn(List.of());

        mockMvc.perform(get("/admin/question-library/5/questions"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/question-library/questions/list"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void newForm_showsBlankForm() throws Exception {
        given(questionLibCatService.findById(5L)).willReturn(category(5L));

        mockMvc.perform(get("/admin/question-library/5/questions/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/question-library/questions/form"))
                .andExpect(model().attributeExists("questionForm"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_blankQuestionText_rendersFormWithFieldError() throws Exception {
        given(questionLibCatService.findById(5L)).willReturn(category(5L));

        mockMvc.perform(post("/admin/question-library/5/questions").with(csrf())
                        .param("questionText", "")
                        .param("questionType", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/question-library/questions/form"))
                .andExpect(model().attributeHasFieldErrors("questionForm", "questionText"));

        verify(questionLibService, never()).create(any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_valid_savesAndRedirects() throws Exception {
        mockMvc.perform(post("/admin/question-library/5/questions").with(csrf())
                        .param("questionText", "Do you have transportation?")
                        .param("questionType", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/question-library/5/questions"));

        verify(questionLibService).create(eq(5L), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void editForm_populatesFormFromExistingQuestion() throws Exception {
        given(questionLibCatService.findById(5L)).willReturn(category(5L));

        QuestionLib question = new QuestionLib();
        question.setId(8L);
        question.setQuestionText("Existing question");
        question.setQuestionType(4);
        given(questionLibService.findById(8L)).willReturn(question);

        mockMvc.perform(get("/admin/question-library/5/questions/8/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/question-library/questions/form"))
                .andExpect(model().attribute("questionId", 8L));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_valid_updatesAndRedirects() throws Exception {
        mockMvc.perform(post("/admin/question-library/5/questions/8").with(csrf())
                        .param("questionText", "Updated question")
                        .param("questionType", "4"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/question-library/5/questions"));

        verify(questionLibService).update(eq(8L), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_removesAndRedirects() throws Exception {
        mockMvc.perform(post("/admin/question-library/5/questions/8/delete").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/question-library/5/questions"));

        verify(questionLibService).delete(8L);
    }
}
