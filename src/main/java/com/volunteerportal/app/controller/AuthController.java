package com.volunteerportal.app.controller;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import com.volunteerportal.app.dto.RegistrationForm;
import com.volunteerportal.app.service.UserService;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registrationForm", new RegistrationForm());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registrationForm") RegistrationForm form,
            BindingResult bindingResult) {

        if (userService.usernameExists(form.getUsername())) {
            bindingResult.rejectValue("username", "duplicate", "Username is already taken");
        }
        if (userService.emailExists(form.getEmail())) {
            bindingResult.rejectValue("email", "duplicate", "Email is already registered");
        }
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "mismatch", "Passwords do not match");
        }

        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        userService.registerNewUser(form);
        return "redirect:/login?registered";
    }
}
