package com.volunteerportal.app.service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.volunteerportal.app.model.Role;
import com.volunteerportal.app.model.User;
import com.volunteerportal.app.repository.RoleRepository;
import com.volunteerportal.app.repository.UserRepository;
import com.volunteerportal.app.service.UserAdminService.RoleChangeRejectedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserAdminServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private NotificationService notificationService;

    private UserAdminServiceImpl service;
    private User admin;
    private User volunteer;

    @BeforeEach
    void setUp() {
        service = new UserAdminServiceImpl(userRepository, roleRepository, notificationService);
        admin = user(1L, "admin", "ADMIN");
        volunteer = user(2L, "vol", "VOLUNTEER");
        for (String name : List.of("ADMIN", "COORDINATOR", "VOLUNTEER")) {
            lenient().when(roleRepository.findByName(name)).thenReturn(Optional.of(new Role(name)));
        }
        lenient().when(roleRepository.findByName("SUPERUSER")).thenReturn(Optional.empty());
        lenient().when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        lenient().when(userRepository.findById(2L)).thenReturn(Optional.of(volunteer));
        lenient().when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void makingAVolunteerACoordinator_savesTheRolesAndNotifiesThem() {
        User saved = service.updateRoles(2L, Set.of("VOLUNTEER", "COORDINATOR"), 1L);

        assertThat(names(saved)).containsExactlyInAnyOrder("VOLUNTEER", "COORDINATOR");
        verify(notificationService).notify(volunteer, "notification.rolesChanged", "/home", "COORDINATOR, VOLUNTEER");
    }

    @Test
    void savingTheSameRoles_doesNotNotify() {
        service.updateRoles(2L, Set.of("VOLUNTEER"), 1L);

        verify(notificationService, never()).notify(any(), anyString(), anyString(), any(String[].class));
    }

    @Test
    void noRoles_isRefused() {
        assertRejected(() -> service.updateRoles(2L, Set.of(), 1L), "users.error.noRoles");
        assertRejected(() -> service.updateRoles(2L, null, 1L), "users.error.noRoles");
    }

    @Test
    void unknownRole_isRefused() {
        assertRejected(() -> service.updateRoles(2L, Set.of("SUPERUSER"), 1L), "users.error.unknownRole");
    }

    @Test
    void adminsCannotRemoveTheirOwnAdminRole() {
        assertRejected(() -> service.updateRoles(1L, Set.of("VOLUNTEER"), 1L), "users.error.ownAdmin");
        assertThat(names(admin)).containsExactly("ADMIN");
    }

    @Test
    void theLastEnabledAdmin_keepsTheAdminRole() {
        User otherAdmin = user(3L, "other", "ADMIN");
        otherAdmin.setEnabled(false); // a disabled admin doesn't count
        given(userRepository.findByRoles_Name("ADMIN")).willReturn(List.of(admin, otherAdmin));

        // otherAdmin (disabled) acting, so it isn't the "own role" rule that stops it
        assertRejected(() -> service.updateRoles(1L, Set.of("VOLUNTEER"), 3L), "users.error.lastAdmin");
    }

    @Test
    void withAnotherEnabledAdmin_anAdminRoleCanBeRemoved() {
        User otherAdmin = user(3L, "other", "ADMIN");
        given(userRepository.findByRoles_Name("ADMIN")).willReturn(List.of(admin, otherAdmin));

        User saved = service.updateRoles(1L, Set.of("COORDINATOR"), 3L);

        assertThat(names(saved)).containsExactly("COORDINATOR");
        verify(notificationService).notify(eq(admin), eq("notification.rolesChanged"), eq("/home"), any(String[].class));
    }

    @Test
    void addingAdmin_hasNoRestrictions() {
        User saved = service.updateRoles(2L, Set.of("ADMIN", "VOLUNTEER"), 1L);

        assertThat(names(saved)).containsExactlyInAnyOrder("ADMIN", "VOLUNTEER");
    }

    @Test
    void usersAreListedByUsername_ignoringCase() {
        given(userRepository.findAll()).willReturn(List.of(user(5L, "zed", "VOLUNTEER"), user(6L, "Amy", "VOLUNTEER"), admin));

        assertThat(service.findAllUsers()).extracting(User::getUsername).containsExactly("admin", "Amy", "zed");
    }

    private void assertRejected(Runnable call, String expectedKey) {
        assertThatThrownBy(call::run)
                .isInstanceOf(RoleChangeRejectedException.class)
                .extracting(e -> ((RoleChangeRejectedException) e).getMessageKey())
                .isEqualTo(expectedKey);
        verify(userRepository, never()).save(any());
    }

    private static User user(Long id, String username, String role) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEnabled(true);
        user.setRoles(new HashSet<>(Set.of(new Role(role))));
        return user;
    }

    private static List<String> names(User user) {
        return user.getRoles().stream().map(Role::getName).toList();
    }
}
