package com.volunteerportal.app.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/** Changing your own password on My Profile: the current one, then the new one twice. */
@Getter
@Setter
public class PasswordChangeForm extends NewPasswordForm {

    @NotBlank
    private String currentPassword;
}
