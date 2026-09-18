package com.volunteerportal.app.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.volunteerportal.app.config.SecurityConfig;
import com.volunteerportal.app.model.Initiative;
import com.volunteerportal.app.service.InitiativeDuplicateService;
import com.volunteerportal.app.service.NotificationService;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InitiativeDuplicateController.class)
@Import(SecurityConfig.class)
class InitiativeDuplicateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InitiativeDuplicateService initiativeDuplicateService;

    // GlobalModelAttributes (a @ControllerAdvice picked up by the web slice) depends on this.
    @MockitoBean
    private NotificationService notificationService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void duplicate_redirectsToTheNewInitiativesEditPage() throws Exception {
        Initiative copy = new Initiative();
        copy.setId(42L);
        given(initiativeDuplicateService.duplicate(5L)).willReturn(copy);

        mockMvc.perform(post("/admin/initiatives/5/duplicate").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/initiatives/42/edit"));
    }
}
