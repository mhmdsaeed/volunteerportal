package com.volunteerportal.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import com.volunteerportal.app.service.PasswordService;

/** A new password typed twice: an admin setting a user's password, or a reset link (which also sends its token). */
@Getter
@Setter
public class NewPasswordForm {

    @NotBlank
    @Size(min = PasswordService.MIN_LENGTH)
    private String newPassword;

    @NotBlank
    private String confirmPassword;

    /** Only for a reset link: the token from the emailed link, sent back in a hidden field. */
    private String token;

    public boolean passwordsMatch() {
        return newPassword != null && newPassword.equals(confirmPassword);
    }
}
