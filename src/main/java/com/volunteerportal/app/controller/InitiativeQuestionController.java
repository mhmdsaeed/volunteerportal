package com.volunteerportal.app.controller;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.volunteerportal.app.dto.InitiativeQuestionForm;
import com.volunteerportal.app.model.QuestionLib;
import com.volunteerportal.app.repository.QuestionLibRepository;
import com.volunteerportal.app.service.InitiativeQuestionService;
import com.volunteerportal.app.service.InitiativeService;
import com.volunteerportal.app.service.QuestionLibCatService;

@Controller
@RequestMapping("/admin/initiatives/{initiativeId}/questions")
public class InitiativeQuestionController {

    private final InitiativeQuestionService initiativeQuestionService;
    private final InitiativeService initiativeService;
    private final QuestionLibCatService questionLibCatService;
    private final QuestionLibRepository questionLibRepository;

    public InitiativeQuestionController(InitiativeQuestionService initiativeQuestionService,
            InitiativeService initiativeService, QuestionLibCatService questionLibCatService,
            QuestionLibRepository questionLibRepository) {
        this.initiativeQuestionService = initiativeQuestionService;
        this.initiativeService = initiativeService;
        this.questionLibCatService = questionLibCatService;
        this.questionLibRepository = questionLibRepository;
    }

    @GetMapping
    public String list(@PathVariable Long initiativeId, Model model) {
        model.addAttribute("initiative", initiativeService.findById(initiativeId));
        model.addAttribute("questions", initiativeQuestionService.findAllForInitiative(initiativeId));
        return "admin/initiatives/questions/list";
    }

    @GetMapping("/new")
    public String newForm(@PathVariable Long initiativeId,
            @RequestParam(required = false) Long fromLibrary, Model model) {
        model.addAttribute("initiative", initiativeService.findById(initiativeId));
        model.addAttribute("questionForm", fromLibrary != null ? formFromLibrary(fromLibrary) : new InitiativeQuestionForm());
        addLibraryReferenceData(model);
        return "admin/initiatives/questions/form";
    }

    @PostMapping
    public String create(@PathVariable Long initiativeId,
            @Valid @ModelAttribute("questionForm") InitiativeQuestionForm form, BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("initiative", initiativeService.findById(initiativeId));
            addLibraryReferenceData(model);
            return "admin/initiatives/questions/form";
        }
        initiativeQuestionService.create(initiativeId, form);
        return "redirect:/admin/initiatives/{initiativeId}/questions";
    }

    private InitiativeQuestionForm formFromLibrary(Long libraryQuestionId) {
        QuestionLib libraryQuestion = questionLibRepository.findById(libraryQuestionId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Question library entry not found: " + libraryQuestionId));

        InitiativeQuestionForm form = new InitiativeQuestionForm();
        form.setQuestionText(libraryQuestion.getQuestionText());
        form.setQuestionTypeId(libraryQuestion.getQuestionType());
        form.setQuestionChoicesCount(libraryQuestion.getQuestionChoicesCount());
        form.setQuestionChoicesText(libraryQuestion.getQuestionChoices());
        return form;
    }

    private void addLibraryReferenceData(Model model) {
        model.addAttribute("libraryCategories", questionLibCatService.findAll());
        model.addAttribute("libraryQuestions", questionLibRepository.findAll());
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long initiativeId, @PathVariable Long id, Model model) {
        var question = initiativeQuestionService.findById(id);

        InitiativeQuestionForm form = new InitiativeQuestionForm();
        form.setQuestionText(question.getQuestionText());
        form.setQuestionTypeId(question.getQuestionTypeId());
        form.setQuestionChoicesCount(question.getQuestionChoicesCount());
        form.setQuestionChoicesText(question.getQuestionChoicesText());

        model.addAttribute("initiative", initiativeService.findById(initiativeId));
        model.addAttribute("questionForm", form);
        model.addAttribute("questionId", id);
        return "admin/initiatives/questions/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long initiativeId, @PathVariable Long id,
            @Valid @ModelAttribute("questionForm") InitiativeQuestionForm form, BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("initiative", initiativeService.findById(initiativeId));
            model.addAttribute("questionId", id);
            return "admin/initiatives/questions/form";
        }
        initiativeQuestionService.update(id, form);
        return "redirect:/admin/initiatives/{initiativeId}/questions";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long initiativeId, @PathVariable Long id) {
        initiativeQuestionService.delete(id);
        return "redirect:/admin/initiatives/{initiativeId}/questions";
    }
}
