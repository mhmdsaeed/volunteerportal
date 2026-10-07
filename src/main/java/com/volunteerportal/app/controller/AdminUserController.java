package com.volunteerportal.app.controller;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.volunteerportal.app.dto.NewPasswordForm;
import com.volunteerportal.app.model.Role;
import com.volunteerportal.app.model.User;
import com.volunteerportal.app.security.UserPrincipal;
import com.volunteerportal.app.service.PasswordService;
import com.volunteerportal.app.service.PasswordService.PasswordRejectedException;
import com.volunteerportal.app.service.UserAdminService;
import com.volunteerportal.app.service.UserAdminService.UserChangeRejectedException;

/**
 * Admin → Manage Users: every account with its roles and status; changing roles, activating and deactivating,
 * and setting a new password for someone who forgot theirs.
 */
@Controller
@RequestMapping("/admin/users")
public class AdminUserController {

    private final UserAdminService userAdminService;
    private final PasswordService passwordService;

    public AdminUserController(UserAdminService userAdminService, PasswordService passwordService) {
        this.userAdminService = userAdminService;
        this.passwordService = passwordService;
    }

    @GetMapping
    public String list(Model model, @AuthenticationPrincipal UserPrincipal principal) {
        model.addAttribute("users", userAdminService.findAllUsers());
        model.addAttribute("currentUserId", principal.getUser().getId());
        return "admin/users/list";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal, Model model) {
        User user = userAdminService.findUser(id);
        addFormModel(model, user, user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()), principal);
        return "admin/users/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @RequestParam(name = "roles", required = false) List<String> roles,
            @AuthenticationPrincipal UserPrincipal principal, Model model, RedirectAttributes redirectAttributes) {
        Set<String> requested = roles == null ? Set.of() : new HashSet<>(roles);
        try {
            User saved = userAdminService.updateRoles(id, requested, principal.getUser().getId());
            redirectAttributes.addFlashAttribute("savedUser", saved.getUsername());
            return "redirect:/admin/users";
        } catch (UserChangeRejectedException e) {
            addFormModel(model, userAdminService.findUser(id), requested, principal);
            model.addAttribute("errorKey", e.getMessageKey());
            return "admin/users/form";
        }
    }

    /** Sets a new password (the admin tells the user); signs the user out of the website and the app. */
    @PostMapping("/{id}/password")
    public String setPassword(@PathVariable Long id, @Valid @ModelAttribute("passwordForm") NewPasswordForm form,
            BindingResult bindingResult, @AuthenticationPrincipal UserPrincipal principal, Model model,
            RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasFieldErrors("confirmPassword") && !form.passwordsMatch()) {
            bindingResult.rejectValue("confirmPassword", "error.password.mismatch", "Passwords do not match");
        }
        if (!bindingResult.hasErrors()) {
            try {
                User saved = passwordService.setPassword(id, form.getNewPassword(), principal.getUser().getId());
                redirectAttributes.addFlashAttribute("passwordSetUser", saved.getUsername());
                return "redirect:/admin/users";
            } catch (PasswordRejectedException e) {
                bindingResult.rejectValue("newPassword", e.getMessageKey());
            }
        }
        User user = userAdminService.findUser(id);
        addFormModel(model, user, user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()), principal);
        return "admin/users/form";
    }

    @PostMapping("/{id}/deactivate")
    public String deactivate(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal,
            RedirectAttributes redirectAttributes) {
        return setEnabled(id, false, principal, redirectAttributes);
    }

    @PostMapping("/{id}/activate")
    public String activate(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal,
            RedirectAttributes redirectAttributes) {
        return setEnabled(id, true, principal, redirectAttributes);
    }

    private String setEnabled(Long id, boolean enabled, UserPrincipal principal, RedirectAttributes redirectAttributes) {
        try {
            User saved = userAdminService.setEnabled(id, enabled, principal.getUser().getId());
            redirectAttributes.addFlashAttribute(enabled ? "activatedUser" : "deactivatedUser", saved.getUsername());
        } catch (UserChangeRejectedException e) {
            redirectAttributes.addFlashAttribute("errorKey", e.getMessageKey());
        }
        return "redirect:/admin/users";
    }

    private void addFormModel(Model model, User user, Set<String> selected, UserPrincipal principal) {
        model.addAttribute("user", user);
        if (!model.containsAttribute("passwordForm")) {
            model.addAttribute("passwordForm", new NewPasswordForm());
        }
        model.addAttribute("allRoles", userAdminService.findAllRoles());
        model.addAttribute("selected", selected);
        model.addAttribute("isSelf", user.getId().equals(principal.getUser().getId()));
    }
}
