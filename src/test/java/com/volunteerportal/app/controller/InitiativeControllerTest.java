package com.volunteerportal.app.controller;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.volunteerportal.app.config.SecurityConfig;
import com.volunteerportal.app.model.Initiative;
import com.volunteerportal.app.model.User;
import com.volunteerportal.app.repository.OfficeRepository;
import com.volunteerportal.app.repository.UserRepository;
import com.volunteerportal.app.service.InitiativeService;
import com.volunteerportal.app.service.NotificationService;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(InitiativeController.class)
@Import(SecurityConfig.class)
class InitiativeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InitiativeService initiativeService;

    @MockitoBean
    private OfficeRepository officeRepository;

    @MockitoBean
    private UserRepository userRepository;

    // GlobalModelAttributes (a @ControllerAdvice picked up by the web slice) depends on this.
    @MockitoBean
    private NotificationService notificationService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void list_rendersInitiativesFromService() throws Exception {
        given(initiativeService.findAll()).willReturn(List.of());

        mockMvc.perform(get("/admin/initiatives"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/initiatives/list"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void newForm_offersOnlyActiveAdminsAndCoordinatorsAsSupervisors() throws Exception {
        given(officeRepository.findAll()).willReturn(List.of());
        List<User> eligible = List.of(user(1L, "admin"), user(2L, "coord"));
        given(userRepository.findActiveWithAnyRole(InitiativeController.SUPERVISOR_ROLES)).willReturn(eligible);

        mockMvc.perform(get("/admin/initiatives/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/initiatives/form"))
                .andExpect(model().attributeExists("initiativeForm"))
                .andExpect(model().attribute("users", eligible))
                .andExpect(content().string(containsString("Only active admins and coordinators are listed")));

        verify(userRepository, never()).findAll();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_withASupervisorWhoIsNotAnAdminOrCoordinator_isRefused() throws Exception {
        given(userRepository.findActiveWithAnyRole(InitiativeController.SUPERVISOR_ROLES)).willReturn(List.of(user(2L, "coord")));

        mockMvc.perform(post("/admin/initiatives").with(csrf())
                        .param("name", "Beach Cleanup")
                        .param("supervisorId", "9")) // e.g. a volunteer, posted by hand
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrorCode("initiativeForm", "supervisorId", "error.supervisor.notEligible"));

        verify(initiativeService, never()).create(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_withACoordinatorAsSupervisor_saves() throws Exception {
        given(userRepository.findActiveWithAnyRole(InitiativeController.SUPERVISOR_ROLES)).willReturn(List.of(user(2L, "coord")));

        mockMvc.perform(post("/admin/initiatives").with(csrf())
                        .param("name", "Beach Cleanup")
                        .param("supervisorId", "2"))
                .andExpect(redirectedUrl("/admin/initiatives"));

        verify(initiativeService).create(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void editForm_keepsACurrentSupervisorWhoNoLongerQualifies_marked() throws Exception {
        User former = user(9L, "former_coord");
        given(initiativeService.findById(5L)).willReturn(initiativeSupervisedBy(former));
        given(userRepository.findActiveWithAnyRole(InitiativeController.SUPERVISOR_ROLES)).willReturn(List.of(user(2L, "coord")));
        given(userRepository.findById(9L)).willReturn(Optional.of(former));

        mockMvc.perform(get("/admin/initiatives/5/edit"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("former_coord (no longer an active admin or coordinator)")))
                .andExpect(content().string(containsString("value=\"9\" selected")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_keepingThatSupervisorUnchanged_isAllowed_butSwitchingToThemIsNot() throws Exception {
        given(initiativeService.findById(5L)).willReturn(initiativeSupervisedBy(user(9L, "former_coord")));
        given(userRepository.findActiveWithAnyRole(InitiativeController.SUPERVISOR_ROLES)).willReturn(List.of(user(2L, "coord")));

        mockMvc.perform(post("/admin/initiatives/5").with(csrf()).param("name", "Renamed").param("supervisorId", "9"))
                .andExpect(redirectedUrl("/admin/initiatives"));
        verify(initiativeService).update(eq(5L), any());

        given(initiativeService.findById(6L)).willReturn(initiativeSupervisedBy(user(2L, "coord")));
        given(userRepository.findById(2L)).willReturn(Optional.of(user(2L, "coord")));
        mockMvc.perform(post("/admin/initiatives/6").with(csrf()).param("name", "Other").param("supervisorId", "9"))
                .andExpect(model().attributeHasFieldErrorCode("initiativeForm", "supervisorId", "error.supervisor.notEligible"));
        verify(initiativeService, never()).update(eq(6L), any());
    }

    private static User user(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        return user;
    }

    private static Initiative initiativeSupervisedBy(User supervisor) {
        Initiative initiative = new Initiative();
        initiative.setId(5L);
        initiative.setName("Existing");
        initiative.setSupervisor(supervisor);
        return initiative;
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_blankName_rendersFormWithFieldError() throws Exception {
        mockMvc.perform(post("/admin/initiatives").with(csrf())
                        .param("name", "")
                        .param("enabled", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/initiatives/form"))
                .andExpect(model().attributeHasFieldErrors("initiativeForm", "name"));

        verify(initiativeService, never()).create(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_valid_savesAndRedirects() throws Exception {
        mockMvc.perform(post("/admin/initiatives").with(csrf())
                        .param("name", "Beach Cleanup")
                        .param("enabled", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/initiatives"));

        verify(initiativeService).create(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void editForm_populatesFormFromExistingInitiative() throws Exception {
        Initiative initiative = new Initiative();
        initiative.setId(5L);
        initiative.setName("Existing Initiative");
        initiative.setDescription("Some description");
        initiative.setEnabled(true);
        given(initiativeService.findById(5L)).willReturn(initiative);

        mockMvc.perform(get("/admin/initiatives/5/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/initiatives/form"))
                .andExpect(model().attribute("initiativeId", 5L));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_valid_updatesAndRedirects() throws Exception {
        given(initiativeService.findById(5L)).willReturn(new Initiative());

        mockMvc.perform(post("/admin/initiatives/5").with(csrf())
                        .param("name", "Updated Name")
                        .param("enabled", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/initiatives"));

        verify(initiativeService).update(eq(5L), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_removesAndRedirects() throws Exception {
        mockMvc.perform(post("/admin/initiatives/5/delete").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/initiatives"));

        verify(initiativeService).delete(5L);
    }
}
