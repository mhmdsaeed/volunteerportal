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

import com.volunteerportal.app.dto.OfficeForm;
import com.volunteerportal.app.repository.RoleRepository;
import com.volunteerportal.app.repository.UserRepository;
import com.volunteerportal.app.service.OfficeService;

@Controller
@RequestMapping("/admin/offices")
public class OfficeController {

    private final OfficeService officeService;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    public OfficeController(OfficeService officeService, RoleRepository roleRepository,
            UserRepository userRepository) {
        this.officeService = officeService;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("offices", officeService.findAll());
        return "admin/offices/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("officeForm", new OfficeForm());
        addReferenceData(model);
        return "admin/offices/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("officeForm") OfficeForm form, BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            addReferenceData(model);
            return "admin/offices/form";
        }
        officeService.create(form);
        return "redirect:/admin/offices";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        var office = officeService.findById(id);

        OfficeForm form = new OfficeForm();
        form.setName(office.getName());
        form.setDescription(office.getDescription());
        if (office.getRole() != null) {
            form.setRoleId(office.getRole().getId());
        }
        if (office.getUser() != null) {
            form.setUserId(office.getUser().getId());
        }

        model.addAttribute("officeForm", form);
        model.addAttribute("officeId", id);
        addReferenceData(model);
        return "admin/offices/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("officeForm") OfficeForm form,
            BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("officeId", id);
            addReferenceData(model);
            return "admin/offices/form";
        }
        officeService.update(id, form);
        return "redirect:/admin/offices";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        officeService.delete(id);
        return "redirect:/admin/offices";
    }

    private void addReferenceData(Model model) {
        model.addAttribute("roles", roleRepository.findAll());
        model.addAttribute("users", userRepository.findAll());
    }
}
