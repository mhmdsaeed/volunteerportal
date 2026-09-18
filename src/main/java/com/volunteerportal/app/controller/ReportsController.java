package com.volunteerportal.app.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.volunteerportal.app.service.ReportService;

@Controller
@RequestMapping("/admin/reports")
public class ReportsController {

    private final ReportService reportService;

    public ReportsController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    public String index() {
        return "admin/reports/index";
    }

    @GetMapping("/initiatives")
    public String initiativeParticipation(Model model) {
        model.addAttribute("rows", reportService.initiativeParticipationReport());
        return "admin/reports/initiatives";
    }

    @GetMapping("/attendance")
    public String eventAttendance(Model model) {
        model.addAttribute("rows", reportService.eventAttendanceReport());
        return "admin/reports/attendance";
    }

    @GetMapping("/volunteers")
    public String volunteerLeaderboard(Model model) {
        model.addAttribute("rows", reportService.volunteerLeaderboard());
        return "admin/reports/volunteers";
    }
}
