package com.volunteerportal.app.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.volunteerportal.app.security.UserPrincipal;

@Controller
public class HomeController {

    @GetMapping("/")
    public String index(@AuthenticationPrincipal UserPrincipal principal) {
        return principal != null ? "redirect:/home" : "redirect:/login";
    }

    @GetMapping("/home")
    public String home(@AuthenticationPrincipal UserPrincipal principal, Model model) {
        model.addAttribute("user", principal.getUser());
        return "home";
    }
}
