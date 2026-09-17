package com.volunteerportal.app.service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.volunteerportal.app.dto.RegistrationForm;
import com.volunteerportal.app.model.Role;
import com.volunteerportal.app.model.User;
import com.volunteerportal.app.repository.RoleRepository;
import com.volunteerportal.app.repository.UserRepository;

@Service
public class UserServiceImpl implements UserService {

    private static final String DEFAULT_ROLE = "VOLUNTEER";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, RoleRepository roleRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public User registerNewUser(RegistrationForm form) {
        Role defaultRole = roleRepository.findByName(DEFAULT_ROLE)
                .orElseThrow(() -> new IllegalStateException(
                        "Default role '" + DEFAULT_ROLE + "' is missing - check Flyway seed migration"));

        User user = new User();
        user.setUsername(form.getUsername());
        user.setEmail(form.getEmail());
        user.setPassword(passwordEncoder.encode(form.getPassword()));
        user.setRegistrationDttm(LocalDateTime.now());
        user.setEnabled(true);
        user.setRoles(new HashSet<>(Set.of(defaultRole)));

        return userRepository.save(user);
    }

    @Override
    public boolean usernameExists(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }
}
