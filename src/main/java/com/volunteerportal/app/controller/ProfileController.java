package com.volunteerportal.app.controller;

import jakarta.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import com.volunteerportal.app.dto.PasswordChangeForm;
import com.volunteerportal.app.dto.ProfileForm;
import com.volunteerportal.app.model.User;
import com.volunteerportal.app.model.VolunteerProfile;
import com.volunteerportal.app.security.UserPrincipal;
import com.volunteerportal.app.service.PasswordService;
import com.volunteerportal.app.service.PasswordService.PasswordRejectedException;
import com.volunteerportal.app.service.ProfileService;

@Controller
public class ProfileController {

    private final ProfileService profileService;
    private final PasswordService passwordService;

    public ProfileController(ProfileService profileService, PasswordService passwordService) {
        this.profileService = profileService;
        this.passwordService = passwordService;
    }

    @GetMapping("/profile")
    public String view(@AuthenticationPrincipal UserPrincipal principal, Model model) {
        VolunteerProfile profile = profileService.findOrCreate(principal.getUser());
        model.addAttribute("profileForm", formOf(profile));
        model.addAttribute("passwordForm", new PasswordChangeForm());
        model.addAttribute("profile", profile);
        return "profile/view";
    }

    @PostMapping("/profile")
    public String update(@AuthenticationPrincipal UserPrincipal principal,
            @Valid @ModelAttribute("profileForm") ProfileForm form, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("passwordForm", new PasswordChangeForm());
            model.addAttribute("profile", profileService.findOrCreate(principal.getUser()));
            return "profile/view";
        }
        profileService.updateProfile(principal.getUser(), form);
        return "redirect:/profile?saved";
    }

    /** Changes the logged-in user's password; their other sessions and app logins end. */
    @PostMapping("/profile/password")
    public String changePassword(@AuthenticationPrincipal UserPrincipal principal,
            @Valid @ModelAttribute("passwordForm") PasswordChangeForm form, BindingResult bindingResult, Model model) {
        if (!bindingResult.hasFieldErrors("confirmPassword") && !form.passwordsMatch()) {
            bindingResult.rejectValue("confirmPassword", "error.password.mismatch", "Passwords do not match");
        }
        if (!bindingResult.hasErrors()) {
            try {
                User saved = passwordService.changeOwnPassword(principal.getUser().getId(),
                        form.getCurrentPassword(), form.getNewPassword(), null);
                // This session holds the hash it logged in with; update it so StaleSessionFilter keeps it
                principal.getUser().setPassword(saved.getPassword());
                return "redirect:/profile?passwordChanged#password";
            } catch (PasswordRejectedException e) {
                String field = e.getMessageKey().equals("password.error.wrongCurrent") ? "currentPassword" : "newPassword";
                bindingResult.rejectValue(field, e.getMessageKey());
            }
        }
        VolunteerProfile profile = profileService.findOrCreate(principal.getUser());
        model.addAttribute("profileForm", formOf(profile));
        model.addAttribute("profile", profile);
        return "profile/view";
    }

    private static ProfileForm formOf(VolunteerProfile profile) {
        ProfileForm form = new ProfileForm();
        form.setFirstName(profile.getFirstName());
        form.setLastName(profile.getLastName());
        form.setMobile(profile.getMobile());
        form.setCity(profile.getCity());
        form.setAddress(profile.getAddress());
        return form;
    }
}
