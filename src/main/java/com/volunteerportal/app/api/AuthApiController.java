package com.volunteerportal.app.api;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.volunteerportal.app.api.ApiDtos.ForgotPasswordRequest;
import com.volunteerportal.app.api.ApiDtos.LoginRequest;
import com.volunteerportal.app.api.ApiDtos.LoginResponse;
import com.volunteerportal.app.api.ApiDtos.PasswordChangeRequest;
import com.volunteerportal.app.security.ApiTokenAuthenticationFilter;
import com.volunteerportal.app.security.UserPrincipal;
import com.volunteerportal.app.service.ApiTokenService;
import com.volunteerportal.app.service.PasswordService;

@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

    private final ApiTokenService apiTokenService;
    private final MobileApiService mobileApiService;
    private final PasswordService passwordService;

    public AuthApiController(ApiTokenService apiTokenService, MobileApiService mobileApiService,
            PasswordService passwordService) {
        this.apiTokenService = apiTokenService;
        this.mobileApiService = mobileApiService;
        this.passwordService = passwordService;
    }

    /** Username + password in, bearer token out (send it as "Authorization: Bearer ..."). */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        return apiTokenService.login(request.username(), request.password(), request.deviceName())
                .<ResponseEntity<?>>map(issued -> ResponseEntity.ok(
                        new LoginResponse(issued.token(), issued.expiresAt(), mobileApiService.me(issued.user()))))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiDtos.Error("invalid_credentials", "Wrong username or password")));
    }

    /** Revokes the token used for this request. */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        apiTokenService.revoke(ApiTokenAuthenticationFilter.bearerToken(request));
        return ResponseEntity.noContent().build();
    }

    /**
     * Changes the signed-in user's password. This token stays valid; the user's other app logins are revoked
     * and their website sessions end. 400 {@code wrong_password} or {@code password_too_short} if refused.
     */
    @PostMapping("/password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal UserPrincipal principal,
            @RequestBody PasswordChangeRequest request, HttpServletRequest httpRequest) {
        passwordService.changeOwnPassword(principal.getUser().getId(), request.currentPassword(),
                request.newPassword(), ApiTokenAuthenticationFilter.bearerToken(httpRequest));
        return ResponseEntity.noContent().build();
    }

    /**
     * Emails a reset link (for the website's reset page) if the address belongs to an active account. Answers
     * 202 either way, so it can't be used to find out who has an account; 409 {@code reset_unavailable} when
     * the server can't send email (the user should ask an administrator).
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@RequestBody ForgotPasswordRequest request, HttpServletRequest httpRequest) {
        if (!passwordService.resetByEmailAvailable()) {
            throw new ApiConflictException("reset_unavailable", "This server can't send password reset emails");
        }
        passwordService.requestReset(request.email(),
                ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString(), ApiLocale.of(httpRequest));
        return ResponseEntity.accepted().build();
    }
}
