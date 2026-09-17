package com.volunteerportal.app.init;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.volunteerportal.app.model.Role;
import com.volunteerportal.app.model.User;
import com.volunteerportal.app.repository.RoleRepository;
import com.volunteerportal.app.repository.UserRepository;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private static final String ADMIN_ROLE = "ADMIN";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed-admin.enabled:true}")
    private boolean seedAdminEnabled;

    @Value("${app.seed-admin.username:admin}")
    private String adminUsername;

    @Value("${app.seed-admin.password:admin123}")
    private String adminPassword;

    @Value("${app.seed-admin.email:admin@volunteerportal.local}")
    private String adminEmail;

    public DataInitializer(UserRepository userRepository, RoleRepository roleRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!seedAdminEnabled) {
            return;
        }

        boolean adminExists = userRepository.findByUsername(adminUsername)
                .map(user -> user.getRoles().stream().anyMatch(role -> ADMIN_ROLE.equals(role.getName())))
                .orElse(false);

        if (adminExists) {
            return;
        }

        Role adminRole = roleRepository.findByName(ADMIN_ROLE)
                .orElseThrow(() -> new IllegalStateException(
                        "Role '" + ADMIN_ROLE + "' is missing - check Flyway seed migration"));

        User admin = new User();
        admin.setUsername(adminUsername);
        admin.setEmail(adminEmail);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setRegistrationDttm(LocalDateTime.now());
        admin.setEnabled(true);
        Set<Role> roles = new HashSet<>();
        roles.add(adminRole);
        admin.setRoles(roles);

        userRepository.save(admin);
        log.warn("Seeded default admin user '{}' with a default password - change it immediately.", adminUsername);
    }
}
