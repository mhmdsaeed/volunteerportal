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

import com.volunteerportal.app.dto.GradeForm;
import com.volunteerportal.app.service.GradeService;

@Controller
@RequestMapping("/admin/grades")
public class GradeController {

    private final GradeService gradeService;

    public GradeController(GradeService gradeService) {
        this.gradeService = gradeService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("grades", gradeService.findAll());
        return "admin/grades/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("gradeForm", new GradeForm());
        return "admin/grades/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("gradeForm") GradeForm form, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "admin/grades/form";
        }
        gradeService.create(form);
        return "redirect:/admin/grades";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        var grade = gradeService.findById(id);

        GradeForm form = new GradeForm();
        form.setName(grade.getName());

        model.addAttribute("gradeForm", form);
        model.addAttribute("gradeId", id);
        return "admin/grades/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("gradeForm") GradeForm form,
            BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("gradeId", id);
            return "admin/grades/form";
        }
        gradeService.update(id, form);
        return "redirect:/admin/grades";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        gradeService.delete(id);
        return "redirect:/admin/grades";
    }
}
