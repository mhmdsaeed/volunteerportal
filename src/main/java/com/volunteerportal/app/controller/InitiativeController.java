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

import com.volunteerportal.app.dto.InitiativeForm;
import com.volunteerportal.app.repository.OfficeRepository;
import com.volunteerportal.app.repository.UserRepository;
import com.volunteerportal.app.service.InitiativeService;

@Controller
@RequestMapping("/admin/initiatives")
public class InitiativeController {

    private final InitiativeService initiativeService;
    private final OfficeRepository officeRepository;
    private final UserRepository userRepository;

    public InitiativeController(InitiativeService initiativeService, OfficeRepository officeRepository,
            UserRepository userRepository) {
        this.initiativeService = initiativeService;
        this.officeRepository = officeRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("initiatives", initiativeService.findAll());
        return "admin/initiatives/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("initiativeForm", new InitiativeForm());
        addReferenceData(model);
        return "admin/initiatives/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("initiativeForm") InitiativeForm form, BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            addReferenceData(model);
            return "admin/initiatives/form";
        }
        initiativeService.create(form);
        return "redirect:/admin/initiatives";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        var initiative = initiativeService.findById(id);

        InitiativeForm form = new InitiativeForm();
        form.setName(initiative.getName());
        form.setDescription(initiative.getDescription());
        form.setEnabled(Boolean.TRUE.equals(initiative.getEnabled()));
        if (initiative.getOffice() != null) {
            form.setOfficeId(initiative.getOffice().getId());
        }
        if (initiative.getSupervisor() != null) {
            form.setSupervisorId(initiative.getSupervisor().getId());
        }

        model.addAttribute("initiativeForm", form);
        model.addAttribute("initiativeId", id);
        addReferenceData(model);
        return "admin/initiatives/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("initiativeForm") InitiativeForm form,
            BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("initiativeId", id);
            addReferenceData(model);
            return "admin/initiatives/form";
        }
        initiativeService.update(id, form);
        return "redirect:/admin/initiatives";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        initiativeService.delete(id);
        return "redirect:/admin/initiatives";
    }

    private void addReferenceData(Model model) {
        model.addAttribute("offices", officeRepository.findAll());
        model.addAttribute("users", userRepository.findAll());
    }
}
