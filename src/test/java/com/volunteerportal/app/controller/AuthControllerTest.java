package com.volunteerportal.app.controller;

import java.util.Locale;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.volunteerportal.app.config.SecurityConfig;
import com.volunteerportal.app.model.User;
import com.volunteerportal.app.service.NotificationService;
import com.volunteerportal.app.service.PasswordService;
import com.volunteerportal.app.service.PasswordService.PasswordRejectedException;
import com.volunteerportal.app.service.UserService;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
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

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private PasswordService passwordService;

    // GlobalModelAttributes (a @ControllerAdvice picked up by the web slice) depends on this.
    @MockitoBean
    private NotificationService notificationService;

    @Test
    void registerPage_showsForm() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"));
    }

    @Test
    void register_duplicateUsername_rendersFormWithFieldError() throws Exception {
        given(userService.usernameExists("taken")).willReturn(true);

        mockMvc.perform(post("/register").with(csrf())
                        .param("username", "taken")
                        .param("email", "a@b.com")
                        .param("password", "password123")
                        .param("confirmPassword", "password123"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeHasFieldErrors("registrationForm", "username"));

        verify(userService, never()).registerNewUser(any());
    }

    @Test
    void register_passwordMismatch_rendersFormWithFieldError() throws Exception {
        mockMvc.perform(post("/register").with(csrf())
                        .param("username", "newuser")
                        .param("email", "a@b.com")
                        .param("password", "password123")
                        .param("confirmPassword", "different123"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("registrationForm", "confirmPassword"));

        verify(userService, never()).registerNewUser(any());
    }

    @Test
    void register_valid_redirectsToLoginAndRegistersUser() throws Exception {
        mockMvc.perform(post("/register").with(csrf())
                        .param("username", "newuser")
                        .param("email", "new@user.com")
                        .param("password", "password123")
                        .param("confirmPassword", "password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered"));

        verify(userService).registerNewUser(any());
    }

    @Test
    void loginPage_linksToForgotPassword() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(content().string(containsString("href=\"/forgot-password\"")));
    }

    @Test
    void loginPage_afterFailedLogin_linksTheMessageToTheUsername() throws Exception {
        // The username field has focus, so a screen reader reads why the login failed with it
        mockMvc.perform(get("/login").param("error", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"loginError\"")))
                .andExpect(content().string(containsString("aria-describedby=\"loginError\"")));
    }

    @Test
    void loginPage_deactivatedAccount_linksThatMessageInstead() throws Exception {
        mockMvc.perform(get("/login").param("disabled", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("aria-describedby=\"loginDisabled\"")));
    }

    @Test
    void loginPage_withoutAMessage_describesNothing() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("aria-describedby=\"login"))));
    }

    @Test
    void forgotPasswordPage_asksForTheEmail_whenMailIsSetUp() throws Exception {
        given(passwordService.resetByEmailAvailable()).willReturn(true);

        mockMvc.perform(get("/forgot-password"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"email\"")))
                .andExpect(content().string(not(containsString("Ask an administrator"))));
    }

    @Test
    void forgotPasswordPage_saysToAskAnAdmin_whenMailIsOff() throws Exception {
        given(passwordService.resetByEmailAvailable()).willReturn(false);

        mockMvc.perform(get("/forgot-password"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ask an administrator")))
                .andExpect(content().string(not(containsString("name=\"email\""))));
    }

    @Test
    void forgotPassword_sendsTheLinkForThisSite_andAnswersTheSameWhateverTheEmail() throws Exception {
        mockMvc.perform(post("/forgot-password").with(csrf()).param("email", "someone@example.com"))
                .andExpect(redirectedUrl("/forgot-password?sent"));

        verify(passwordService).requestReset(eq("someone@example.com"), eq("http://localhost"), any(Locale.class));
    }

    @Test
    void forgotPassword_invalidEmail_showsTheFormAgain() throws Exception {
        given(passwordService.resetByEmailAvailable()).willReturn(true);

        mockMvc.perform(post("/forgot-password").with(csrf()).param("email", "not-an-email"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("forgotPasswordForm", "email"));
        verify(passwordService, never()).requestReset(anyString(), anyString(), any());
    }

    @Test
    void resetPasswordPage_withAValidLink_showsTheForm() throws Exception {
        given(passwordService.findResetUser("good")).willReturn(Optional.of(new User()));

        mockMvc.perform(get("/reset-password").param("token", "good"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"newPassword\"")))
                .andExpect(content().string(containsString("value=\"good\"")));
    }

    @Test
    void resetPasswordPage_withAnUsedOrExpiredLink_offersANewOne() throws Exception {
        given(passwordService.findResetUser("old")).willReturn(Optional.empty());

        mockMvc.perform(get("/reset-password").param("token", "old"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("expired or was already used")))
                .andExpect(content().string(not(containsString("name=\"newPassword\""))));
    }

    @Test
    void resetPassword_setsTheNewPassword_andSendsThemToLogIn() throws Exception {
        mockMvc.perform(post("/reset-password").with(csrf())
                        .param("token", "good").param("newPassword", "brand-new-pass").param("confirmPassword", "brand-new-pass"))
                .andExpect(redirectedUrl("/login?reset"));

        verify(passwordService).resetPassword("good", "brand-new-pass");
    }

    @Test
    void resetPassword_mismatch_showsTheFormAgain() throws Exception {
        given(passwordService.findResetUser("good")).willReturn(Optional.of(new User()));

        mockMvc.perform(post("/reset-password").with(csrf())
                        .param("token", "good").param("newPassword", "brand-new-pass").param("confirmPassword", "other-pass"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("resetForm", "confirmPassword"));
        verify(passwordService, never()).resetPassword(anyString(), anyString());
    }

    @Test
    void resetPassword_linkUsedMeanwhile_saysSo() throws Exception {
        willThrow(new PasswordRejectedException("password.error.invalidLink"))
                .given(passwordService).resetPassword("gone", "brand-new-pass");

        mockMvc.perform(post("/reset-password").with(csrf())
                        .param("token", "gone").param("newPassword", "brand-new-pass").param("confirmPassword", "brand-new-pass"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("expired or was already used")));
    }
}
