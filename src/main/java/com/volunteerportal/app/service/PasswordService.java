package com.volunteerportal.app.service;

import java.util.Locale;
import java.util.Optional;

import com.volunteerportal.app.model.User;

/**
 * Every way a password changes: by its owner (website profile or the app), by an admin, or through an
 * emailed reset link. Each one also signs the user out elsewhere: their app logins are revoked, and their
 * other website sessions end on the next request (StaleSessionFilter notices the new hash).
 */
public interface PasswordService {

    /** The same minimum as at registration. */
    int MIN_LENGTH = 8;

    /** Why a change was refused, as a message code. */
    class PasswordRejectedException extends RuntimeException {
        private final String messageKey;

        public PasswordRejectedException(String messageKey) {
            super(messageKey);
            this.messageKey = messageKey;
        }

        public String getMessageKey() {
            return messageKey;
        }
    }

    /**
     * Changes the user's own password after checking the current one. Revokes their app logins except
     * {@code keepApiToken} (the app's own token when it is the app asking; null from the website).
     *
     * @return the user with the new password hash, for the website to update its own session
     * @throws PasswordRejectedException {@code password.error.wrongCurrent} or {@code password.error.tooShort}
     */
    User changeOwnPassword(Long userId, String currentPassword, String newPassword, String keepApiToken);

    /**
     * An admin sets someone else's password (for a user who forgot theirs and has no email access).
     *
     * @throws PasswordRejectedException {@code password.error.ownAccount} or {@code password.error.tooShort}
     */
    User setPassword(Long userId, String newPassword, Long adminId);

    /** Whether "Forgot your password?" can send links (mail is set up, or links are logged in development). */
    boolean resetByEmailAvailable();

    /**
     * Emails a reset link to the active account with this email, if there is one. Says nothing either way,
     * so the form can't be used to find out who has an account. At most one link every few minutes per user.
     *
     * @param linkBase the site's address, e.g. {@code https://portal.example.org}; the link adds
     *                 {@code /reset-password?token=...}
     */
    void requestReset(String email, String linkBase, Locale locale);

    /** The account a reset link is for, if the link is unused, unexpired and the account active. */
    Optional<User> findResetUser(String token);

    /**
     * Sets a new password from a reset link and uses the link up.
     *
     * @throws PasswordRejectedException {@code password.error.invalidLink} or {@code password.error.tooShort}
     */
    User resetPassword(String token, String newPassword);
}
