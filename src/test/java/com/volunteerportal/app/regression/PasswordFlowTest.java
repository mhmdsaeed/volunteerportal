package com.volunteerportal.app.regression;

import java.time.Duration;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.volunteerportal.app.model.Role;
import com.volunteerportal.app.model.User;
import com.volunteerportal.app.repository.ApiTokenRepository;
import com.volunteerportal.app.repository.PasswordResetTokenRepository;
import com.volunteerportal.app.repository.RoleRepository;
import com.volunteerportal.app.repository.UserRepository;
import com.volunteerportal.app.repository.VolunteerProfileRepository;
import com.volunteerportal.app.security.UserPrincipal;
import com.volunteerportal.app.service.ApiTokenService;
import com.volunteerportal.app.service.PasswordResetMailer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Changing, setting and resetting passwords against the real database, security and templates: each one
 * signs the user out of their other website sessions and app logins. The mailer is replaced, so the test
 * can read the emailed link.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PasswordFlowTest {

    private static final String OLD = "old-password-1";
    private static final String NEW = "new-password-2";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ApiTokenRepository apiTokenRepository;

    @Autowired
    private PasswordResetTokenRepository resetTokenRepository;

    @Autowired
    private VolunteerProfileRepository volunteerProfileRepository;

    @Autowired
    private ApiTokenService apiTokenService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private PasswordResetMailer mailer;

    private User volunteer;

    @BeforeEach
    void setUp() {
        given(mailer.available()).willReturn(true);
        volunteer = new User();
        volunteer.setUsername("pw_vol_" + System.nanoTime());
        volunteer.setEmail(volunteer.getUsername() + "@example.com");
        volunteer.setPassword(passwordEncoder.encode(OLD));
        volunteer.setEnabled(true);
        volunteer.setRoles(new HashSet<>(Set.<Role>of(roleRepository.findByName("VOLUNTEER").orElseThrow())));
        volunteer = userRepository.save(volunteer);
    }

    @AfterEach
    void cleanUp() {
        apiTokenRepository.deleteByUserId(volunteer.getId());
        resetTokenRepository.deleteByUserId(volunteer.getId());
        volunteerProfileRepository.findByUserId(volunteer.getId()).ifPresent(volunteerProfileRepository::delete);
        userRepository.delete(volunteer);
    }

    @Test
    void changingYourPasswordOnTheWebsite_keepsThisSession_butEndsTheOthersAndTheAppLogins() throws Exception {
        MockHttpSession here = login(OLD);
        MockHttpSession elsewhere = login(OLD);
        String appToken = apiTokenService.issue(volunteer, "phone").token();

        mockMvc.perform(post("/profile/password").session(here).with(csrf())
                        .param("currentPassword", OLD).param("newPassword", NEW).param("confirmPassword", NEW))
                .andExpect(redirectedUrl("/profile?passwordChanged#password"));

        mockMvc.perform(get("/profile").session(here).param("passwordChanged", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Password changed.")));
        mockMvc.perform(get("/home").session(elsewhere)).andExpect(redirectedUrl("/login?passwordChanged"));
        mockMvc.perform(get("/login").param("passwordChanged", ""))
                .andExpect(content().string(containsString("Your password was changed")));
        mockMvc.perform(get("/api/me").header("Authorization", "Bearer " + appToken)).andExpect(status().isUnauthorized());

        mockMvc.perform(post("/login").with(csrf()).param("username", volunteer.getUsername()).param("password", OLD))
                .andExpect(redirectedUrl("/login?error"));
        login(NEW);
    }

    @Test
    void aWrongCurrentPassword_isRefusedOnTheProfilePage() throws Exception {
        mockMvc.perform(post("/profile/password").session(login(OLD)).with(csrf())
                        .param("currentPassword", "not-it").param("newPassword", NEW).param("confirmPassword", NEW))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Your current password is wrong.")));

        assertThat(passwordEncoder.matches(OLD, userRepository.findById(volunteer.getId()).orElseThrow().getPassword())).isTrue();
    }

    @Test
    void anAdminSetsANewPassword_whichEndsTheUsersSessionsAndAppLogins() throws Exception {
        MockHttpSession session = login(OLD);
        String appToken = apiTokenService.issue(volunteer, "phone").token();
        UserPrincipal admin = new UserPrincipal(userRepository.findByUsername("admin").orElseThrow());

        mockMvc.perform(get("/admin/users/{id}/edit", volunteer.getId()).with(user(admin)))
                .andExpect(content().string(containsString("/admin/users/" + volunteer.getId() + "/password")));
        mockMvc.perform(post("/admin/users/{id}/password", volunteer.getId()).with(user(admin)).with(csrf())
                        .param("newPassword", NEW).param("confirmPassword", NEW))
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attribute("passwordSetUser", volunteer.getUsername()));

        mockMvc.perform(get("/home").session(session)).andExpect(redirectedUrl("/login?passwordChanged"));
        mockMvc.perform(get("/api/me").header("Authorization", "Bearer " + appToken)).andExpect(status().isUnauthorized());
        login(NEW);
    }

    @Test
    void forgotPassword_emailsALink_thatSetsANewPasswordOnce() throws Exception {
        MockHttpSession session = login(OLD);

        mockMvc.perform(post("/forgot-password").with(csrf()).param("email", volunteer.getEmail()))
                .andExpect(redirectedUrl("/forgot-password?sent"));
        String link = emailedLink();
        assertThat(link).startsWith("http://localhost/reset-password?token=");
        String token = link.substring(link.indexOf('=') + 1);

        mockMvc.perform(get("/reset-password").param("token", token))
                .andExpect(content().string(containsString("name=\"newPassword\"")));
        mockMvc.perform(post("/reset-password").with(csrf())
                        .param("token", token).param("newPassword", NEW).param("confirmPassword", NEW))
                .andExpect(redirectedUrl("/login?reset"));

        // The link is used up, and the session from before the reset has ended
        mockMvc.perform(get("/reset-password").param("token", token))
                .andExpect(content().string(containsString("expired or was already used")));
        mockMvc.perform(get("/home").session(session)).andExpect(redirectedUrl("/login?passwordChanged"));
        login(NEW);
    }

    @Test
    void theAppChangesThePassword_keepingItsOwnLogin_andEndingTheOthers() throws Exception {
        String thisPhone = apiTokenService.issue(volunteer, "this phone").token();
        String otherPhone = apiTokenService.issue(volunteer, "other phone").token();

        mockMvc.perform(post("/api/auth/password").header("Authorization", "Bearer " + thisPhone)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"not-it\",\"newPassword\":\"" + NEW + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("wrong_password"));
        mockMvc.perform(post("/api/auth/password").header("Authorization", "Bearer " + thisPhone).header("Accept-Language", "ar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"not-it\",\"newPassword\":\"" + NEW + "\"}"))
                .andExpect(jsonPath("$.message").value("كلمة المرور الحالية غير صحيحة."));
        mockMvc.perform(post("/api/auth/password").header("Authorization", "Bearer " + thisPhone)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"" + OLD + "\",\"newPassword\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("password_too_short"));

        mockMvc.perform(post("/api/auth/password").header("Authorization", "Bearer " + thisPhone)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"" + OLD + "\",\"newPassword\":\"" + NEW + "\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/me").header("Authorization", "Bearer " + thisPhone)).andExpect(status().isOk());
        mockMvc.perform(get("/api/me").header("Authorization", "Bearer " + otherPhone)).andExpect(status().isUnauthorized());
        login(NEW);
    }

    @Test
    void theAppAsksForAResetLink_withoutLoggingIn() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password").header("Accept-Language", "ar")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"" + volunteer.getEmail() + "\"}"))
                .andExpect(status().isAccepted());
        verify(mailer).send(eq(volunteer.getUsername()), eq(volunteer.getEmail()), any(), any(), eq(Locale.forLanguageTag("ar")));

        // An unknown email gets the same answer
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"nobody-here@example.com\"}"))
                .andExpect(status().isAccepted());

        given(mailer.available()).willReturn(false);
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"" + volunteer.getEmail() + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("reset_unavailable"));
    }

    private String emailedLink() {
        ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);
        verify(mailer).send(eq(volunteer.getUsername()), eq(volunteer.getEmail()), link.capture(), any(Duration.class),
                any(Locale.class));
        return link.getValue();
    }

    private MockHttpSession login(String password) throws Exception {
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(post("/login").session(session).with(csrf())
                        .param("username", volunteer.getUsername()).param("password", password))
                .andExpect(redirectedUrl("/home"));
        return session;
    }
}
