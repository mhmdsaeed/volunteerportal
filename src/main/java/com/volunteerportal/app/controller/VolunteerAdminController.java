package com.volunteerportal.app.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.volunteerportal.app.dto.VolunteerGradeForm;
import com.volunteerportal.app.model.User;
import com.volunteerportal.app.model.VolunteerProfile;
import com.volunteerportal.app.repository.GradeRepository;
import com.volunteerportal.app.service.VolunteerAdminService;

@Controller
@RequestMapping("/admin/volunteers")
public class VolunteerAdminController {

    private final VolunteerAdminService volunteerAdminService;
    private final GradeRepository gradeRepository;

    public VolunteerAdminController(VolunteerAdminService volunteerAdminService, GradeRepository gradeRepository) {
        this.volunteerAdminService = volunteerAdminService;
        this.gradeRepository = gradeRepository;
    }

    @GetMapping
    public String list(Model model) {
        var volunteers = volunteerAdminService.findVolunteers();

        Map<Long, VolunteerProfile> profiles = new LinkedHashMap<>();
        for (User volunteer : volunteers) {
            profiles.put(volunteer.getId(), volunteerAdminService.findProfileForUser(volunteer.getId()).orElse(null));
        }

        model.addAttribute("volunteers", volunteers);
        model.addAttribute("profiles", profiles);
        return "admin/volunteers/list";
    }

    @GetMapping("/{userId}/edit")
    public String editForm(@PathVariable Long userId, Model model) {
        var user = volunteerAdminService.findUserById(userId);
        var profile = volunteerAdminService.findProfileForUser(userId).orElse(null);

        VolunteerGradeForm form = new VolunteerGradeForm();
        if (profile != null) {
            form.setPoints(profile.getPoints() != null ? profile.getPoints() : 0L);
            if (profile.getGrade() != null) {
                form.setGradeId(profile.getGrade().getId());
            }
        } else {
            form.setPoints(0L);
        }

        model.addAttribute("volunteer", user);
        model.addAttribute("gradeForm", form);
        model.addAttribute("grades", gradeRepository.findAll());
        return "admin/volunteers/form";
    }

    @PostMapping("/{userId}")
    public String update(@PathVariable Long userId, @Valid @ModelAttribute("gradeForm") VolunteerGradeForm form,
            BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("volunteer", volunteerAdminService.findUserById(userId));
            model.addAttribute("grades", gradeRepository.findAll());
            return "admin/volunteers/form";
        }
        volunteerAdminService.updateGradeAndPoints(userId, form);
        return "redirect:/admin/volunteers";
    }
}
