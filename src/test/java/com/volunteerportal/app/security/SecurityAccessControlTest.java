package com.volunteerportal.app.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the access-control rules declared in SecurityConfig against the real
 * security filter chain and application context (backed by the reachable dev DB).
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityAccessControlTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void loginPage_isPubliclyAccessible() throws Exception {
        mockMvc.perform(get("/login")).andExpect(status().isOk());
    }

    @Test
    void registerPage_isPubliclyAccessible() throws Exception {
        mockMvc.perform(get("/register")).andExpect(status().isOk());
    }

    @Test
    void adminArea_anonymous_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @WithMockUser(roles = "VOLUNTEER")
    void adminArea_wrongRole_isForbidden() throws Exception {
        mockMvc.perform(get("/admin")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminArea_adminRole_isAccessible() throws Exception {
        mockMvc.perform(get("/admin")).andExpect(status().isOk());
    }
}
