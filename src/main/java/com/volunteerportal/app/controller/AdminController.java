package com.volunteerportal.app.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminController {

    @GetMapping("/admin")
    public String dashboard() {
        return "admin/dashboard";
    }

    @GetMapping("/403")
    public String accessDenied() {
        return "error/403";
    }
}
