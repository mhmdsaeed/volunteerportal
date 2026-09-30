package com.volunteerportal.app.regression;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import com.volunteerportal.app.model.Role;
import com.volunteerportal.app.model.User;
import com.volunteerportal.app.repository.NotificationRepository;
import com.volunteerportal.app.repository.RoleRepository;
import com.volunteerportal.app.repository.UserRepository;
import com.volunteerportal.app.security.UserPrincipal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Admin → Manage Users against the real database, security and templates. */
@SpringBootTest
@AutoConfigureMockMvc
class ManageUsersFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    private User volunteer;

    @BeforeEach
    void setUp() {
        volunteer = new User();
        volunteer.setUsername("roles_vol_" + System.nanoTime());
        volunteer.setEmail(volunteer.getUsername() + "@example.com");
        volunteer.setPassword("irrelevant-hash");
        volunteer.setEnabled(true);
        volunteer.setRoles(new HashSet<>(Set.<Role>of(roleRepository.findByName("VOLUNTEER").orElseThrow())));
        volunteer = userRepository.save(volunteer);
    }

    @AfterEach
    void cleanUp() {
        notificationRepository.deleteAll(notificationRepository.findByUserIdOrderByCreatedDttmDesc(volunteer.getId()));
        userRepository.delete(volunteer);
    }

    @Test
    void adminMakesAVolunteerACoordinator_whoCanThenUseTheCoordinatorPages() throws Exception {
        mockMvc.perform(get("/admin/users").with(user(fresh("admin"))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(volunteer.getUsername())));
        mockMvc.perform(get("/admin/users/{id}/edit", volunteer.getId()).with(user(fresh("admin"))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("value=\"COORDINATOR\"")));

        // Before: a volunteer can't open the coordinator pages
        mockMvc.perform(get("/coordinator/requests").with(user(fresh(volunteer.getUsername()))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/admin/users/{id}", volunteer.getId()).with(user(fresh("admin"))).with(csrf())
                        .param("roles", "VOLUNTEER", "COORDINATOR"))
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attribute("savedUser", volunteer.getUsername()));

        assertThat(fresh(volunteer.getUsername()).getAuthorities()).extracting(a -> a.getAuthority())
                .containsExactlyInAnyOrder("ROLE_VOLUNTEER", "ROLE_COORDINATOR");
        assertThat(notificationRepository.findByUserIdOrderByCreatedDttmDesc(volunteer.getId()))
                .anySatisfy(n -> assertThat(n.getMessageKey()).isEqualTo("notification.rolesChanged"));

        // After logging in again (a fresh principal), the coordinator pages open
        mockMvc.perform(get("/coordinator/requests").with(user(fresh(volunteer.getUsername()))))
                .andExpect(status().isOk());
    }

    @Test
    void adminCannotRemoveTheirOwnAdminRole_andSeesWhy() throws Exception {
        User admin = userRepository.findByUsername("admin").orElseThrow();

        mockMvc.perform(post("/admin/users/{id}", admin.getId()).with(user(fresh("admin"))).with(csrf())
                        .param("roles", "VOLUNTEER"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("You can&#39;t remove your own Admin role")));

        assertThat(fresh("admin").getAuthorities()).extracting(a -> a.getAuthority()).contains("ROLE_ADMIN");
    }

    @Test
    void choosingNoRole_isRefusedWithAMessage() throws Exception {
        mockMvc.perform(post("/admin/users/{id}", volunteer.getId()).with(user(fresh("admin"))).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Choose at least one role.")));
    }

    @Test
    void nonAdmins_cannotOpenOrUseManageUsers() throws Exception {
        mockMvc.perform(get("/admin/users").with(user(fresh(volunteer.getUsername()))))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/admin/users/{id}", volunteer.getId()).with(user(fresh(volunteer.getUsername()))).with(csrf())
                        .param("roles", "ADMIN"))
                .andExpect(status().isForbidden());

        assertThat(fresh(volunteer.getUsername()).getAuthorities()).extracting(a -> a.getAuthority())
                .containsExactly("ROLE_VOLUNTEER");
    }

    @Test
    void manageUsers_isTranslated() throws Exception {
        mockMvc.perform(get("/admin/users").param("lang", "ar").with(user(fresh("admin"))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("المستخدمون")))
                .andExpect(content().string(containsString("مسؤول")));
    }

    /** The user as a new login would see them (roles read from the database now). */
    private UserPrincipal fresh(String username) {
        return new UserPrincipal(userRepository.findByUsername(username).orElseThrow());
    }
}
