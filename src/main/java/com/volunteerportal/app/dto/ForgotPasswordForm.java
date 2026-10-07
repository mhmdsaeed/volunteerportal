package com.volunteerportal.app.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/** "Forgot your password?": the email address the reset link goes to. */
@Getter
@Setter
public class ForgotPasswordForm {

    @NotBlank
    @Email
    private String email;
}
