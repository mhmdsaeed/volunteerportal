package com.volunteerportal.app.controller;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.volunteerportal.app.config.SecurityConfig;
import com.volunteerportal.app.dto.EventAttendanceRow;
import com.volunteerportal.app.dto.InitiativeParticipationRow;
import com.volunteerportal.app.dto.VolunteerLeaderboardRow;
import com.volunteerportal.app.service.NotificationService;
import com.volunteerportal.app.service.ReportService;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(ReportsController.class)
@Import(SecurityConfig.class)
class ReportsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReportService reportService;

    // GlobalModelAttributes (a @ControllerAdvice picked up by the web slice) depends on this.
    @MockitoBean
    private NotificationService notificationService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void index_rendersLandingPage() throws Exception {
        mockMvc.perform(get("/admin/reports"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/reports/index"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void initiativeParticipation_rendersRowsFromService() throws Exception {
        InitiativeParticipationRow row = new InitiativeParticipationRow(1L, "Beach Cleanup", "Downtown", "coord1", 3, 2, 1, 6);
        given(reportService.initiativeParticipationReport()).willReturn(List.of(row));

        mockMvc.perform(get("/admin/reports/initiatives"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/reports/initiatives"))
                .andExpect(model().attribute("rows", List.of(row)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void eventAttendance_rendersRowsFromService() throws Exception {
        EventAttendanceRow row = new EventAttendanceRow(1L, "Beach Cleanup", "Saturday Shift", null, 5, 4);
        given(reportService.eventAttendanceReport()).willReturn(List.of(row));

        mockMvc.perform(get("/admin/reports/attendance"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/reports/attendance"))
                .andExpect(model().attribute("rows", List.of(row)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void volunteerLeaderboard_rendersRowsFromService() throws Exception {
        VolunteerLeaderboardRow row = new VolunteerLeaderboardRow(1L, "high", "Gold", 50);
        given(reportService.volunteerLeaderboard()).willReturn(List.of(row));

        mockMvc.perform(get("/admin/reports/volunteers"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/reports/volunteers"))
                .andExpect(model().attribute("rows", List.of(row)));
    }
}
