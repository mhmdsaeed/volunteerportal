package com.volunteerportal.app.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.volunteerportal.app.config.SecurityConfig;
import com.volunteerportal.app.service.InitiativeExportService;
import com.volunteerportal.app.service.NotificationService;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InitiativeExportController.class)
@Import(SecurityConfig.class)
class InitiativeExportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InitiativeExportService initiativeExportService;

    // GlobalModelAttributes (a @ControllerAdvice picked up by the web slice) depends on this.
    @MockitoBean
    private NotificationService notificationService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void exportVolunteers_returnsCsvAttachmentFromService() throws Exception {
        String csv = "Username,Email,Status,Requested At,Responded At,Answers Given\r\nvol1,vol1@example.com,Approved,2026-01-01 09:00,2026-01-02 09:00,3\r\n";
        given(initiativeExportService.exportVolunteersCsv(5L)).willReturn(csv);

        mockMvc.perform(get("/admin/initiatives/5/export"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"initiative-5-volunteers.csv\""))
                .andExpect(content().string(csv));
    }
}
