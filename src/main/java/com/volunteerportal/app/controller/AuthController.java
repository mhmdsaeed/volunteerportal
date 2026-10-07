package com.volunteerportal.app.controller;

import java.util.Locale;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.volunteerportal.app.dto.ForgotPasswordForm;
import com.volunteerportal.app.dto.NewPasswordForm;
import com.volunteerportal.app.dto.RegistrationForm;
import com.volunteerportal.app.service.PasswordService;
import com.volunteerportal.app.service.PasswordService.PasswordRejectedException;
import com.volunteerportal.app.service.UserService;

@Controller
public class AuthController {

    private final UserService userService;
    private final PasswordService passwordService;

    public AuthController(UserService userService, PasswordService passwordService) {
        this.userService = userService;
        this.passwordService = passwordService;
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
            bindingResult.rejectValue("username", "error.username.taken", "Username is already taken");
        }
        if (userService.emailExists(form.getEmail())) {
            bindingResult.rejectValue("email", "error.email.taken", "Email is already registered");
        }
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "error.password.mismatch", "Passwords do not match");
        }

        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        userService.registerNewUser(form);
        return "redirect:/login?registered";
    }

    /** "Forgot your password?": asks for the account's email. Without mail set up, says to ask an admin. */
    @GetMapping("/forgot-password")
    public String forgotPasswordPage(Model model) {
        model.addAttribute("forgotPasswordForm", new ForgotPasswordForm());
        model.addAttribute("available", passwordService.resetByEmailAvailable());
        return "auth/forgot-password";
    }

    /** Sends a reset link if the email belongs to an active account; the answer is the same either way. */
    @PostMapping("/forgot-password")
    public String forgotPassword(@Valid @ModelAttribute("forgotPasswordForm") ForgotPasswordForm form,
            BindingResult bindingResult, Model model, Locale locale) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("available", passwordService.resetByEmailAvailable());
            return "auth/forgot-password";
        }
        passwordService.requestReset(form.getEmail(), siteAddress(), locale);
        return "redirect:/forgot-password?sent";
    }

    /** The page an emailed link opens: a new password form, or why the link no longer works. */
    @GetMapping("/reset-password")
    public String resetPasswordPage(@RequestParam(name = "token", required = false) String token, Model model) {
        NewPasswordForm form = new NewPasswordForm();
        form.setToken(token);
        model.addAttribute("resetForm", form);
        model.addAttribute("valid", passwordService.findResetUser(token).isPresent());
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String resetPassword(@Valid @ModelAttribute("resetForm") NewPasswordForm form,
            BindingResult bindingResult, Model model) {
        if (!bindingResult.hasFieldErrors("confirmPassword") && !form.passwordsMatch()) {
            bindingResult.rejectValue("confirmPassword", "error.password.mismatch", "Passwords do not match");
        }
        if (!bindingResult.hasErrors()) {
            try {
                passwordService.resetPassword(form.getToken(), form.getNewPassword());
                return "redirect:/login?reset";
            } catch (PasswordRejectedException e) {
                if (!e.getMessageKey().equals("password.error.invalidLink")) {
                    bindingResult.rejectValue("newPassword", e.getMessageKey());
                }
            }
        }
        model.addAttribute("valid", passwordService.findResetUser(form.getToken()).isPresent());
        return "auth/reset-password";
    }

    /**
     * The site's address for the emailed link, from the request. Safe behind Caddy, which only answers for
     * its own domain (and sets X-Forwarded-*), so a forged Host header can't put another site in the link.
     */
    private static String siteAddress() {
        return ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
    }
}
