package com.volunteerportal.app.controller;

import org.springframework.security.access.AccessDeniedException;

import com.volunteerportal.app.model.Initiative;
import com.volunteerportal.app.security.UserPrincipal;
import com.volunteerportal.app.service.JoinRequestService;

/** Shared access rule for the /coordinator pages: admins, or whoever {@link JoinRequestService#canManage} allows. */
final class CoordinatorAccess {

    private CoordinatorAccess() {
    }

    static boolean isAdmin(UserPrincipal principal) {
        return principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    /** {@code null} for an admin (sees everything), otherwise the user's id. */
    static Long managerIdFor(UserPrincipal principal) {
        return isAdmin(principal) ? null : principal.getUser().getId();
    }

    static void assertCanManage(Initiative initiative, UserPrincipal principal, JoinRequestService joinRequestService) {
        if (isAdmin(principal)) {
            return;
        }
        if (!joinRequestService.canManage(initiative, principal.getUser().getId())) {
            throw new AccessDeniedException("Not a manager of initiative " + initiative.getId());
        }
    }
}
