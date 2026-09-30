package com.volunteerportal.app.service;

import java.util.List;
import java.util.Set;

import com.volunteerportal.app.model.Role;
import com.volunteerportal.app.model.User;

/** Admin management of user accounts' roles (VOLUNTEER, COORDINATOR, ADMIN). */
public interface UserAdminService {

    /** A role change that the rules don't allow; {@code messageKey} explains why (messages*.properties). */
    class RoleChangeRejectedException extends RuntimeException {
        private final String messageKey;

        public RoleChangeRejectedException(String messageKey) {
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
     * Replaces the user's roles. Refused (with {@link RoleChangeRejectedException}) if no role is
     * given, a role doesn't exist, admins would remove their own ADMIN role, or the last enabled
     * admin would lose it. The user is notified of their new roles.
     */
    User updateRoles(Long userId, Set<String> roleNames, Long actingAdminId);
}
