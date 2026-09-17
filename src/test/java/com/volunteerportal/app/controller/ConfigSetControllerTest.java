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
import com.volunteerportal.app.model.ConfigSet;
import com.volunteerportal.app.service.ConfigSetAdminService;
import com.volunteerportal.app.service.NotificationService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(ConfigSetController.class)
@Import(SecurityConfig.class)
class ConfigSetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConfigSetAdminService configSetAdminService;

    // GlobalModelAttributes (a @ControllerAdvice picked up by the web slice) depends on this.
    @MockitoBean
    private NotificationService notificationService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void list_rendersConfigsFromService() throws Exception {
        given(configSetAdminService.findAll()).willReturn(List.of());

        mockMvc.perform(get("/admin/config"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/config/list"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_duplicateKey_rendersFormWithFieldError() throws Exception {
        given(configSetAdminService.keyTaken("app.setting", null)).willReturn(true);

        mockMvc.perform(post("/admin/config").with(csrf())
                        .param("configsetKey", "app.setting")
                        .param("configsetValue", "value"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/config/form"))
                .andExpect(model().attributeHasFieldErrors("configForm", "configsetKey"));

        verify(configSetAdminService, never()).create(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_valid_savesAndRedirects() throws Exception {
        given(configSetAdminService.keyTaken("app.new.setting", null)).willReturn(false);

        mockMvc.perform(post("/admin/config").with(csrf())
                        .param("configsetKey", "app.new.setting")
                        .param("configsetValue", "value"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/config"));

        verify(configSetAdminService).create(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void editForm_populatesFormFromExistingEntry() throws Exception {
        ConfigSet configSet = new ConfigSet();
        configSet.setId(3L);
        configSet.setConfigsetKey("app.setting");
        configSet.setConfigsetValue("value");
        given(configSetAdminService.findById(3L)).willReturn(configSet);

        mockMvc.perform(get("/admin/config/3/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/config/form"))
                .andExpect(model().attribute("configId", 3L));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_duplicateKeyFromAnotherEntry_rendersFormWithFieldError() throws Exception {
        given(configSetAdminService.keyTaken("taken.key", 3L)).willReturn(true);

        mockMvc.perform(post("/admin/config/3").with(csrf())
                        .param("configsetKey", "taken.key")
                        .param("configsetValue", "value"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/config/form"))
                .andExpect(model().attributeHasFieldErrors("configForm", "configsetKey"));

        verify(configSetAdminService, never()).update(any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_valid_updatesAndRedirects() throws Exception {
        given(configSetAdminService.keyTaken("app.setting", 3L)).willReturn(false);

        mockMvc.perform(post("/admin/config/3").with(csrf())
                        .param("configsetKey", "app.setting")
                        .param("configsetValue", "new-value"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/config"));

        verify(configSetAdminService).update(eq(3L), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_removesAndRedirects() throws Exception {
        mockMvc.perform(post("/admin/config/3/delete").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/config"));

        verify(configSetAdminService).delete(3L);
    }
}
