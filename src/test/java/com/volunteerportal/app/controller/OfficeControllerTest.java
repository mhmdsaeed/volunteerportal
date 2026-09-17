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
import com.volunteerportal.app.model.Office;
import com.volunteerportal.app.repository.RoleRepository;
import com.volunteerportal.app.repository.UserRepository;
import com.volunteerportal.app.service.NotificationService;
import com.volunteerportal.app.service.OfficeService;

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

@WebMvcTest(OfficeController.class)
@Import(SecurityConfig.class)
class OfficeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OfficeService officeService;

    @MockitoBean
    private RoleRepository roleRepository;

    @MockitoBean
    private UserRepository userRepository;

    // GlobalModelAttributes (a @ControllerAdvice picked up by the web slice) depends on this.
    @MockitoBean
    private NotificationService notificationService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void list_rendersOfficesFromService() throws Exception {
        given(officeService.findAll()).willReturn(List.of());

        mockMvc.perform(get("/admin/offices"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/offices/list"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void newForm_showsBlankFormWithReferenceData() throws Exception {
        given(roleRepository.findAll()).willReturn(List.of());
        given(userRepository.findAll()).willReturn(List.of());

        mockMvc.perform(get("/admin/offices/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/offices/form"))
                .andExpect(model().attributeExists("officeForm"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_blankName_rendersFormWithFieldError() throws Exception {
        mockMvc.perform(post("/admin/offices").with(csrf())
                        .param("name", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/offices/form"))
                .andExpect(model().attributeHasFieldErrors("officeForm", "name"));

        verify(officeService, never()).create(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_valid_savesAndRedirects() throws Exception {
        mockMvc.perform(post("/admin/offices").with(csrf())
                        .param("name", "Downtown Office"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/offices"));

        verify(officeService).create(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void editForm_populatesFormFromExistingOffice() throws Exception {
        Office office = new Office();
        office.setId(5L);
        office.setName("Existing Office");
        office.setDescription("Some description");
        given(officeService.findById(5L)).willReturn(office);

        mockMvc.perform(get("/admin/offices/5/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/offices/form"))
                .andExpect(model().attribute("officeId", 5L));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_valid_updatesAndRedirects() throws Exception {
        mockMvc.perform(post("/admin/offices/5").with(csrf())
                        .param("name", "Updated Name"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/offices"));

        verify(officeService).update(eq(5L), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_removesAndRedirects() throws Exception {
        mockMvc.perform(post("/admin/offices/5/delete").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/offices"));

        verify(officeService).delete(5L);
    }
}
