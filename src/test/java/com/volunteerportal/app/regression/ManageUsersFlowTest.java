package com.volunteerportal.app.regression;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.volunteerportal.app.model.Role;
import com.volunteerportal.app.model.User;
import com.volunteerportal.app.repository.ApiTokenRepository;
import com.volunteerportal.app.repository.NotificationRepository;
import com.volunteerportal.app.repository.RoleRepository;
import com.volunteerportal.app.repository.UserRepository;
import com.volunteerportal.app.security.UserPrincipal;
import com.volunteerportal.app.service.ApiTokenService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
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

    @Autowired
    private ApiTokenRepository apiTokenRepository;

    @Autowired
    private ApiTokenService apiTokenService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String PASSWORD = "roles-pass-123";

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
        apiTokenRepository.deleteByUserId(volunteer.getId());
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

    @Test
    void deactivating_endsTheUsersWebsiteSession_blocksLogin_andRevokesMobileLogins_untilReactivated() throws Exception {
        volunteer.setPassword(passwordEncoder.encode(PASSWORD));
        volunteer = userRepository.save(volunteer);
        MockHttpSession session = loginOnWebsite();
        mockMvc.perform(get("/home").session(session)).andExpect(status().isOk());
        String token = apiTokenService.issue(volunteer, "test phone").token();

        mockMvc.perform(post("/admin/users/{id}/deactivate", volunteer.getId()).with(user(fresh("admin"))).with(csrf()))
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attribute("deactivatedUser", volunteer.getUsername()));

        // Their open website session ends on the next request...
        mockMvc.perform(get("/home").session(session)).andExpect(redirectedUrl("/login?disabled"));
        assertThat(session.isInvalid()).isTrue();
        mockMvc.perform(get("/login").param("disabled", "")).andExpect(content().string(containsString("Your account has been deactivated")));
        // ...they can't log in again (with the usual message, so it doesn't reveal the account exists)...
        mockMvc.perform(post("/login").with(csrf()).param("username", volunteer.getUsername()).param("password", PASSWORD))
                .andExpect(redirectedUrl("/login?error"));
        // ...and the mobile app's token is gone
        mockMvc.perform(get("/api/me").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
        assertThat(apiTokenRepository.countByUserId(volunteer.getId())).isZero();

        mockMvc.perform(post("/admin/users/{id}/activate", volunteer.getId()).with(user(fresh("admin"))).with(csrf()))
                .andExpect(flash().attribute("activatedUser", volunteer.getUsername()));
        mockMvc.perform(get("/home").session(loginOnWebsite())).andExpect(status().isOk());
    }

    @Test
    void adminCannotDeactivateThemselves_andHasNoButtonToDoIt() throws Exception {
        User admin = userRepository.findByUsername("admin").orElseThrow();

        mockMvc.perform(get("/admin/users").with(user(fresh("admin"))))
                .andExpect(content().string(containsString("/admin/users/" + volunteer.getId() + "/deactivate")))
                .andExpect(content().string(not(containsString("/admin/users/" + admin.getId() + "/deactivate"))));

        mockMvc.perform(post("/admin/users/{id}/deactivate", admin.getId()).with(user(fresh("admin"))).with(csrf()))
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attribute("errorKey", "users.error.ownAccount"));
        assertThat(userRepository.findByUsername("admin").orElseThrow().isEnabled()).isTrue();
    }

    private MockHttpSession loginOnWebsite() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(post("/login").session(session).with(csrf())
                        .param("username", volunteer.getUsername()).param("password", PASSWORD))
                .andExpect(redirectedUrl("/home"));
        return session;
    }

    /** The user as a new login would see them (roles read from the database now). */
    private UserPrincipal fresh(String username) {
        return new UserPrincipal(userRepository.findByUsername(username).orElseThrow());
    }
}
