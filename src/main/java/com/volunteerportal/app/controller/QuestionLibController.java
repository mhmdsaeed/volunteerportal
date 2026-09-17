package com.volunteerportal.app.controller;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.volunteerportal.app.dto.QuestionLibForm;
import com.volunteerportal.app.service.QuestionLibCatService;
import com.volunteerportal.app.service.QuestionLibService;

@Controller
@RequestMapping("/admin/question-library/{categoryId}/questions")
public class QuestionLibController {

    private final QuestionLibService questionLibService;
    private final QuestionLibCatService questionLibCatService;

    public QuestionLibController(QuestionLibService questionLibService, QuestionLibCatService questionLibCatService) {
        this.questionLibService = questionLibService;
        this.questionLibCatService = questionLibCatService;
    }

    @GetMapping
    public String list(@PathVariable Long categoryId, Model model) {
        model.addAttribute("category", questionLibCatService.findById(categoryId));
        model.addAttribute("questions", questionLibService.findAllForCategory(categoryId));
        return "admin/question-library/questions/list";
    }

    @GetMapping("/new")
    public String newForm(@PathVariable Long categoryId, Model model) {
        model.addAttribute("category", questionLibCatService.findById(categoryId));
        model.addAttribute("questionForm", new QuestionLibForm());
        return "admin/question-library/questions/form";
    }

    @PostMapping
    public String create(@PathVariable Long categoryId, @Valid @ModelAttribute("questionForm") QuestionLibForm form,
            BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("category", questionLibCatService.findById(categoryId));
            return "admin/question-library/questions/form";
        }
        questionLibService.create(categoryId, form);
        return "redirect:/admin/question-library/{categoryId}/questions";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long categoryId, @PathVariable Long id, Model model) {
        var question = questionLibService.findById(id);

        QuestionLibForm form = new QuestionLibForm();
        form.setQuestionText(question.getQuestionText());
        form.setQuestionType(question.getQuestionType());
        form.setQuestionChoicesCount(question.getQuestionChoicesCount());
        form.setQuestionChoices(question.getQuestionChoices());

        model.addAttribute("category", questionLibCatService.findById(categoryId));
        model.addAttribute("questionForm", form);
        model.addAttribute("questionId", id);
        return "admin/question-library/questions/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long categoryId, @PathVariable Long id,
            @Valid @ModelAttribute("questionForm") QuestionLibForm form, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("category", questionLibCatService.findById(categoryId));
            model.addAttribute("questionId", id);
            return "admin/question-library/questions/form";
        }
        questionLibService.update(id, form);
        return "redirect:/admin/question-library/{categoryId}/questions";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long categoryId, @PathVariable Long id) {
        questionLibService.delete(id);
        return "redirect:/admin/question-library/{categoryId}/questions";
    }
}
