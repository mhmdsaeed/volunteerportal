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
import com.volunteerportal.app.model.Initiative;
import com.volunteerportal.app.model.InitiativeQuestion;
import com.volunteerportal.app.model.QuestionLib;
import com.volunteerportal.app.repository.QuestionLibRepository;
import com.volunteerportal.app.service.InitiativeQuestionService;
import com.volunteerportal.app.service.InitiativeService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.hamcrest.Matchers.containsString;

@WebMvcTest(InitiativeQuestionController.class)
@Import(SecurityConfig.class)
class InitiativeQuestionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InitiativeQuestionService initiativeQuestionService;

    @MockitoBean
    private InitiativeService initiativeService;

    @MockitoBean
    private QuestionLibCatService questionLibCatService;

    @MockitoBean
    private QuestionLibRepository questionLibRepository;

    // GlobalModelAttributes (a @ControllerAdvice picked up by the web slice) depends on this.
    @MockitoBean
    private NotificationService notificationService;

    private Initiative initiative(Long id) {
        Initiative initiative = new Initiative();
        initiative.setId(id);
        initiative.setName("Beach Cleanup");
        return initiative;
    }

    private void stubInitiativeAndLibraryReferenceData() {
        given(initiativeService.findById(5L)).willReturn(initiative(5L));
        given(questionLibCatService.findAll()).willReturn(List.of());
        given(questionLibRepository.findAll()).willReturn(List.of());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void list_rendersQuestionsFromService() throws Exception {
        given(initiativeService.findById(5L)).willReturn(initiative(5L));
        given(initiativeQuestionService.findAllForInitiative(5L)).willReturn(List.of());

        mockMvc.perform(get("/admin/initiatives/5/questions"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/initiatives/questions/list"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void newForm_withoutFromLibrary_showsBlankForm() throws Exception {
        stubInitiativeAndLibraryReferenceData();

        mockMvc.perform(get("/admin/initiatives/5/questions/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/initiatives/questions/form"))
                .andExpect(model().attributeExists("questionForm"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void newForm_withFromLibrary_prefillsFormFromLibraryEntry() throws Exception {
        stubInitiativeAndLibraryReferenceData();

        QuestionLib libraryQuestion = new QuestionLib();
        libraryQuestion.setId(20L);
        libraryQuestion.setQuestionText("Do you like volunteering?");
        libraryQuestion.setQuestionType(1);
        given(questionLibRepository.findById(20L)).willReturn(Optional.of(libraryQuestion));

        mockMvc.perform(get("/admin/initiatives/5/questions/new").param("fromLibrary", "20"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/initiatives/questions/form"))
                .andExpect(content().string(containsString("Do you like volunteering?")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_blankQuestionText_rendersFormWithFieldError() throws Exception {
        stubInitiativeAndLibraryReferenceData();

        mockMvc.perform(post("/admin/initiatives/5/questions").with(csrf())
                        .param("questionText", "")
                        .param("questionTypeId", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/initiatives/questions/form"))
                .andExpect(model().attributeHasFieldErrors("questionForm", "questionText"));

        verify(initiativeQuestionService, never()).create(any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_valid_savesAndRedirects() throws Exception {
        mockMvc.perform(post("/admin/initiatives/5/questions").with(csrf())
                        .param("questionText", "Do you have transportation?")
                        .param("questionTypeId", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/initiatives/5/questions"));

        verify(initiativeQuestionService).create(eq(5L), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void editForm_populatesFormFromExistingQuestion() throws Exception {
        given(initiativeService.findById(5L)).willReturn(initiative(5L));

        InitiativeQuestion question = new InitiativeQuestion();
        question.setId(8L);
        question.setQuestionText("Existing question");
        question.setQuestionTypeId(4);
        given(initiativeQuestionService.findById(8L)).willReturn(question);

        mockMvc.perform(get("/admin/initiatives/5/questions/8/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/initiatives/questions/form"))
                .andExpect(model().attribute("questionId", 8L));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_valid_updatesAndRedirects() throws Exception {
        mockMvc.perform(post("/admin/initiatives/5/questions/8").with(csrf())
                        .param("questionText", "Updated question")
                        .param("questionTypeId", "4"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/initiatives/5/questions"));

        verify(initiativeQuestionService).update(eq(8L), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_removesAndRedirects() throws Exception {
        mockMvc.perform(post("/admin/initiatives/5/questions/8/delete").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/initiatives/5/questions"));

        verify(initiativeQuestionService).delete(8L);
    }
}
