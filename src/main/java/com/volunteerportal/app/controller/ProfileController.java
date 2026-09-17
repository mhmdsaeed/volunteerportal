package com.volunteerportal.app.controller;

import jakarta.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import com.volunteerportal.app.dto.ProfileForm;
import com.volunteerportal.app.model.VolunteerProfile;
import com.volunteerportal.app.security.UserPrincipal;
import com.volunteerportal.app.service.ProfileService;

@Controller
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/profile")
    public String view(@AuthenticationPrincipal UserPrincipal principal, Model model) {
        VolunteerProfile profile = profileService.findOrCreate(principal.getUser());

        ProfileForm form = new ProfileForm();
        form.setFirstName(profile.getFirstName());
        form.setLastName(profile.getLastName());
        form.setMobile(profile.getMobile());
        form.setCity(profile.getCity());
        form.setAddress(profile.getAddress());

        model.addAttribute("profileForm", form);
        model.addAttribute("profile", profile);
        return "profile/view";
    }

    @PostMapping("/profile")
    public String update(@AuthenticationPrincipal UserPrincipal principal,
            @Valid @ModelAttribute("profileForm") ProfileForm form, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("profile", profileService.findOrCreate(principal.getUser()));
            return "profile/view";
        }
        profileService.updateProfile(principal.getUser(), form);
        return "redirect:/profile?saved";
    }
}
