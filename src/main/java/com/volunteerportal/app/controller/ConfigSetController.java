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

import com.volunteerportal.app.dto.ConfigSetForm;
import com.volunteerportal.app.service.ConfigSetAdminService;

@Controller
@RequestMapping("/admin/config")
public class ConfigSetController {

    private final ConfigSetAdminService configSetAdminService;

    public ConfigSetController(ConfigSetAdminService configSetAdminService) {
        this.configSetAdminService = configSetAdminService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("configs", configSetAdminService.findAll());
        return "admin/config/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("configForm", new ConfigSetForm());
        return "admin/config/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("configForm") ConfigSetForm form, BindingResult bindingResult) {
        if (configSetAdminService.keyTaken(form.getConfigsetKey(), null)) {
            bindingResult.rejectValue("configsetKey", "error.configKey.taken", "Key already exists");
        }
        if (bindingResult.hasErrors()) {
            return "admin/config/form";
        }
        configSetAdminService.create(form);
        return "redirect:/admin/config";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        var configSet = configSetAdminService.findById(id);

        ConfigSetForm form = new ConfigSetForm();
        form.setConfigsetKey(configSet.getConfigsetKey());
        form.setConfigsetValue(configSet.getConfigsetValue());

        model.addAttribute("configForm", form);
        model.addAttribute("configId", id);
        return "admin/config/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("configForm") ConfigSetForm form,
            BindingResult bindingResult, Model model) {
        if (configSetAdminService.keyTaken(form.getConfigsetKey(), id)) {
            bindingResult.rejectValue("configsetKey", "error.configKey.taken", "Key already exists");
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("configId", id);
            return "admin/config/form";
        }
        configSetAdminService.update(id, form);
        return "redirect:/admin/config";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        configSetAdminService.delete(id);
        return "redirect:/admin/config";
    }
}
