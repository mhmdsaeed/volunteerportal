package com.volunteerportal.app.service;

import java.util.List;
import java.util.Set;

import com.volunteerportal.app.model.Role;
import com.volunteerportal.app.model.User;

/** Admin management of user accounts: roles (VOLUNTEER, COORDINATOR, ADMIN) and active/deactivated. */
public interface UserAdminService {

    /** A change the rules don't allow; {@code messageKey} explains why (messages*.properties). */
    class UserChangeRejectedException extends RuntimeException {
        private final String messageKey;

        public UserChangeRejectedException(String messageKey) {
            super(messageKey);
            this.messageKey = messageKey;
        }

        public String getMessageKey() {
            return messageKey;
        }
    }

    /** Every user, by username. */
    List<User> findAllUsers();

    User findUser(Long userId);

    /** The roles an admin can assign, by name. */
    List<Role> findAllRoles();

    /**
     * Replaces the user's roles. Refused (with {@link UserChangeRejectedException}) if no role is
     * given, a role doesn't exist, admins would remove their own ADMIN role, or the last enabled
     * admin would lose it. The user is notified of their new roles.
     */
    User updateRoles(Long userId, Set<String> roleNames, Long actingAdminId);

    /**
     * Activates or deactivates an account. A deactivated user can't log in, their website session ends
     * on their next request, and their mobile app logins are deleted. Refused (with
     * {@link UserChangeRejectedException}) if admins would deactivate themselves or the last active admin.
     */
    User setEnabled(Long userId, boolean enabled, Long actingAdminId);
}
