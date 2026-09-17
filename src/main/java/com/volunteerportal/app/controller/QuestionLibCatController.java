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

import com.volunteerportal.app.dto.QuestionLibCatForm;
import com.volunteerportal.app.service.QuestionLibCatService;

@Controller
@RequestMapping("/admin/question-library")
public class QuestionLibCatController {

    private final QuestionLibCatService questionLibCatService;

    public QuestionLibCatController(QuestionLibCatService questionLibCatService) {
        this.questionLibCatService = questionLibCatService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("categories", questionLibCatService.findAll());
        return "admin/question-library/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("categoryForm", new QuestionLibCatForm());
        return "admin/question-library/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("categoryForm") QuestionLibCatForm form, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "admin/question-library/form";
        }
        questionLibCatService.create(form);
        return "redirect:/admin/question-library";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        var category = questionLibCatService.findById(id);

        QuestionLibCatForm form = new QuestionLibCatForm();
        form.setName(category.getName());
        form.setDescription(category.getDescription());

        model.addAttribute("categoryForm", form);
        model.addAttribute("categoryId", id);
        return "admin/question-library/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("categoryForm") QuestionLibCatForm form,
            BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("categoryId", id);
            return "admin/question-library/form";
        }
        questionLibCatService.update(id, form);
        return "redirect:/admin/question-library";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        questionLibCatService.delete(id);
        return "redirect:/admin/question-library";
    }
}
