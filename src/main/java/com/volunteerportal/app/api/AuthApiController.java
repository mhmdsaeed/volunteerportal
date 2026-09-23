package com.volunteerportal.app.api;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.volunteerportal.app.api.ApiDtos.LoginRequest;
import com.volunteerportal.app.api.ApiDtos.LoginResponse;
import com.volunteerportal.app.security.ApiTokenAuthenticationFilter;
import com.volunteerportal.app.service.ApiTokenService;

@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

    private final ApiTokenService apiTokenService;
    private final MobileApiService mobileApiService;

    public AuthApiController(ApiTokenService apiTokenService, MobileApiService mobileApiService) {
        this.apiTokenService = apiTokenService;
        this.mobileApiService = mobileApiService;
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
}
