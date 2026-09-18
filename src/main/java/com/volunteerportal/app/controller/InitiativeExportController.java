package com.volunteerportal.app.controller;

import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.volunteerportal.app.service.InitiativeExportService;

@Controller
@RequestMapping("/admin/initiatives")
public class InitiativeExportController {

    private final InitiativeExportService initiativeExportService;

    public InitiativeExportController(InitiativeExportService initiativeExportService) {
        this.initiativeExportService = initiativeExportService;
    }

    @GetMapping("/{id}/export")
    @ResponseBody
    public ResponseEntity<byte[]> exportVolunteers(@PathVariable Long id) {
        String csv = initiativeExportService.exportVolunteersCsv(id);
        byte[] body = csv.getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"initiative-" + id + "-volunteers.csv\"")
                .body(body);
    }
}
