package com.volunteerportal.app.repository;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import com.volunteerportal.app.model.Role;
import com.volunteerportal.app.model.User;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findByUsername_returnsSavedUser() {
        persistUser("finduser_ut", "finduser_ut@example.com", "VOLUNTEER");

        Optional<User> found = userRepository.findByUsername("finduser_ut");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("finduser_ut@example.com");
    }

    @Test
    void existsByUsernameAndEmail_reflectSavedState() {
        persistUser("existsuser_ut", "exists_ut@example.com", "VOLUNTEER");

        assertThat(userRepository.existsByUsername("existsuser_ut")).isTrue();
        assertThat(userRepository.existsByEmail("exists_ut@example.com")).isTrue();
        assertThat(userRepository.existsByUsername("no_such_user_ut")).isFalse();
    }

    @Test
    void findByRolesName_returnsUsersWithThatRole() {
        persistUser("coordinator_ut", "coordinator_ut@example.com", "COORDINATOR");

        List<User> coordinators = userRepository.findByRoles_Name("COORDINATOR");

        assertThat(coordinators).extracting(User::getUsername).contains("coordinator_ut");
    }

    private User persistUser(String username, String email, String roleName) {
        Role role = roleRepository.findByName(roleName).orElseThrow();
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("irrelevant-hash");
        user.setEnabled(true);
        user.setRoles(new HashSet<>(Set.of(role)));
        return entityManager.persistFlushFind(user);
    }
}
