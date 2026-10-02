package com.volunteerportal.app.init;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.volunteerportal.app.model.Role;
import com.volunteerportal.app.model.User;
import com.volunteerportal.app.repository.RoleRepository;
import com.volunteerportal.app.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private DataInitializer initializer;

    @BeforeEach
    void setUp() {
        initializer = new DataInitializer(userRepository, roleRepository, passwordEncoder);
        ReflectionTestUtils.setField(initializer, "seedAdminEnabled", true);
        ReflectionTestUtils.setField(initializer, "adminUsername", "admin");
        ReflectionTestUtils.setField(initializer, "adminPassword", "initial-pass");
        ReflectionTestUtils.setField(initializer, "adminEmail", "admin@example.org");
    }

    @Test
    void noAdminAccountYet_createsIt() {
        given(userRepository.findByUsername("admin")).willReturn(Optional.empty());
        given(roleRepository.findByName("ADMIN")).willReturn(Optional.of(new Role("ADMIN")));
        given(passwordEncoder.encode("initial-pass")).willReturn("hash");

        initializer.run();

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getUsername()).isEqualTo("admin");
        assertThat(saved.getValue().getPassword()).isEqualTo("hash");
        assertThat(saved.getValue().isEnabled()).isTrue();
    }

    @Test
    void existingAdmin_isLeftAlone() {
        given(userRepository.findByUsername("admin")).willReturn(Optional.of(user("ADMIN")));

        initializer.run();

        verify(userRepository, never()).save(any());
    }

    @Test
    void accountWhoseAdminRoleWasRemoved_isNotCreatedAgain() {
        // Creating it again would break the unique username and stop the app from starting
        given(userRepository.findByUsername("admin")).willReturn(Optional.of(user("VOLUNTEER")));

        initializer.run();

        verify(userRepository, never()).save(any());
    }

    @Test
    void seedingTurnedOff_doesNothing() {
        ReflectionTestUtils.setField(initializer, "seedAdminEnabled", false);

        initializer.run();

        verify(userRepository, never()).findByUsername(any());
        verify(userRepository, never()).save(any());
    }

    private static User user(String role) {
        User user = new User();
        user.setUsername("admin");
        user.setRoles(new HashSet<>(Set.of(new Role(role))));
        return user;
    }
}
