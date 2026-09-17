package com.volunteerportal.app.service;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.volunteerportal.app.dto.RegistrationForm;
import com.volunteerportal.app.model.Role;
import com.volunteerportal.app.model.User;
import com.volunteerportal.app.repository.RoleRepository;
import com.volunteerportal.app.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void registerNewUser_assignsVolunteerRoleAndEncodesPassword() {
        RegistrationForm form = new RegistrationForm();
        form.setUsername("newuser");
        form.setEmail("new@user.com");
        form.setPassword("plainPassword");

        Role volunteerRole = new Role("VOLUNTEER");
        given(roleRepository.findByName("VOLUNTEER")).willReturn(Optional.of(volunteerRole));
        given(passwordEncoder.encode("plainPassword")).willReturn("encodedPassword");
        given(userRepository.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));

        User saved = userService.registerNewUser(form);

        assertThat(saved.getUsername()).isEqualTo("newuser");
        assertThat(saved.getPassword()).isEqualTo("encodedPassword");
        assertThat(saved.isEnabled()).isTrue();
        assertThat(saved.getRoles()).containsExactly(volunteerRole);
    }

    @Test
    void registerNewUser_missingDefaultRole_throwsIllegalState() {
        given(roleRepository.findByName("VOLUNTEER")).willReturn(Optional.empty());

        RegistrationForm form = new RegistrationForm();
        form.setUsername("x");
        form.setEmail("x@x.com");
        form.setPassword("password123");

        assertThatThrownBy(() -> userService.registerNewUser(form))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("VOLUNTEER");
    }

    @Test
    void usernameExists_delegatesToRepository() {
        given(userRepository.existsByUsername("taken")).willReturn(true);

        assertThat(userService.usernameExists("taken")).isTrue();
    }

    @Test
    void emailExists_delegatesToRepository() {
        given(userRepository.existsByEmail("taken@example.com")).willReturn(true);

        assertThat(userService.emailExists("taken@example.com")).isTrue();
    }
}
