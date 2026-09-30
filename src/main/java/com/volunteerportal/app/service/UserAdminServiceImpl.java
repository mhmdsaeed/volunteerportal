package com.volunteerportal.app.service;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.volunteerportal.app.model.Role;
import com.volunteerportal.app.model.User;
import com.volunteerportal.app.repository.ApiTokenRepository;
import com.volunteerportal.app.repository.RoleRepository;
import com.volunteerportal.app.repository.UserRepository;

@Service
public class UserAdminServiceImpl implements UserAdminService {

    static final String ADMIN = "ADMIN";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final NotificationService notificationService;
    private final ApiTokenRepository apiTokenRepository;

    public UserAdminServiceImpl(UserRepository userRepository, RoleRepository roleRepository,
            NotificationService notificationService, ApiTokenRepository apiTokenRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.notificationService = notificationService;
        this.apiTokenRepository = apiTokenRepository;
    }

    @Override
    public List<User> findAllUsers() {
        return userRepository.findAll().stream()
                .sorted(Comparator.comparing(User::getUsername, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    public User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
    }

    @Override
    public List<Role> findAllRoles() {
        return roleRepository.findAll().stream().sorted(Comparator.comparing(Role::getName)).toList();
    }

    @Override
    @Transactional
    public User updateRoles(Long userId, Set<String> roleNames, Long actingAdminId) {
        User user = findUser(userId);
        Set<String> requested = roleNames == null ? Set.of() : roleNames;
        if (requested.isEmpty()) {
            throw new UserChangeRejectedException("users.error.noRoles");
        }

        Set<Role> roles = new HashSet<>();
        for (String name : requested) {
            roles.add(roleRepository.findByName(name)
                    .orElseThrow(() -> new UserChangeRejectedException("users.error.unknownRole")));
        }

        boolean wasAdmin = hasRole(user, ADMIN);
        boolean staysAdmin = requested.contains(ADMIN);
        if (wasAdmin && !staysAdmin) {
            if (user.getId().equals(actingAdminId)) {
                throw new UserChangeRejectedException("users.error.ownAdmin"); // don't lock yourself out
            }
            if (user.isEnabled() && enabledAdminCount() <= 1) {
                throw new UserChangeRejectedException("users.error.lastAdmin");
            }
        }

        Set<String> before = roleNames(user);
        user.setRoles(roles);
        User saved = userRepository.save(user);

        Set<String> after = roleNames(saved);
        if (!after.equals(before)) {
            notificationService.notify(saved, "notification.rolesChanged", "/home",
                    after.stream().sorted().collect(Collectors.joining(", ")));
        }
        return saved;
    }

    @Override
    @Transactional
    public User setEnabled(Long userId, boolean enabled, Long actingAdminId) {
        User user = findUser(userId);
        if (user.isEnabled() == enabled) {
            return user;
        }
        if (!enabled) {
            if (user.getId().equals(actingAdminId)) {
                throw new UserChangeRejectedException("users.error.ownAccount"); // don't lock yourself out
            }
            if (hasRole(user, ADMIN) && enabledAdminCount() <= 1) {
                throw new UserChangeRejectedException("users.error.lastActiveAdmin");
            }
        }
        user.setEnabled(enabled);
        User saved = userRepository.save(user);
        if (!enabled) {
            // Mobile app logins stop at once; the website session ends on the user's next request
            apiTokenRepository.deleteByUserId(saved.getId());
        }
        return saved;
    }

    private long enabledAdminCount() {
        return userRepository.findByRoles_Name(ADMIN).stream().filter(User::isEnabled).count();
    }

    private static boolean hasRole(User user, String roleName) {
        return user.getRoles().stream().anyMatch(role -> roleName.equals(role.getName()));
    }

    private static Set<String> roleNames(User user) {
        return user.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
    }
}
