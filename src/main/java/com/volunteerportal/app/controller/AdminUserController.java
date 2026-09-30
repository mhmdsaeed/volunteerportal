package com.volunteerportal.app.controller;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.volunteerportal.app.model.Role;
import com.volunteerportal.app.model.User;
import com.volunteerportal.app.security.UserPrincipal;
import com.volunteerportal.app.service.UserAdminService;
import com.volunteerportal.app.service.UserAdminService.RoleChangeRejectedException;

/** Admin → Manage Users: every account with its roles, and changing those roles. */
@Controller
@RequestMapping("/admin/users")
public class AdminUserController {

    private final UserAdminService userAdminService;

    public AdminUserController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("users", userAdminService.findAllUsers());
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
        } catch (RoleChangeRejectedException e) {
            addFormModel(model, userAdminService.findUser(id), requested, principal);
            model.addAttribute("errorKey", e.getMessageKey());
            return "admin/users/form";
        }
    }

    private void addFormModel(Model model, User user, Set<String> selected, UserPrincipal principal) {
        model.addAttribute("user", user);
        model.addAttribute("allRoles", userAdminService.findAllRoles());
        model.addAttribute("selected", selected);
        model.addAttribute("isSelf", user.getId().equals(principal.getUser().getId()));
    }
}
