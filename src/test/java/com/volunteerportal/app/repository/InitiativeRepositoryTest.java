package com.volunteerportal.app.repository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import com.volunteerportal.app.model.Initiative;
import com.volunteerportal.app.model.Role;
import com.volunteerportal.app.model.User;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class InitiativeRepositoryTest {

    @Autowired
    private InitiativeRepository initiativeRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findByEnabledTrue_onlyReturnsEnabledInitiatives() {
        entityManager.persistFlushFind(newInitiative("Enabled One IT", true, null));
        entityManager.persistFlushFind(newInitiative("Disabled One IT", false, null));

        List<Initiative> result = initiativeRepository.findByEnabledTrue();

        assertThat(result).extracting(Initiative::getName).contains("Enabled One IT");
        assertThat(result).extracting(Initiative::getName).doesNotContain("Disabled One IT");
    }

    @Test
    void findBySupervisorId_returnsOnlyThatSupervisorsInitiatives() {
        User supervisor = persistUser("supervisor1_it", "sup1_it@example.com");
        User otherSupervisor = persistUser("supervisor2_it", "sup2_it@example.com");

        entityManager.persistFlushFind(newInitiative("Supervised IT", true, supervisor));
        entityManager.persistFlushFind(newInitiative("Other IT", true, otherSupervisor));

        List<Initiative> result = initiativeRepository.findBySupervisorId(supervisor.getId());

        assertThat(result).extracting(Initiative::getName).containsExactly("Supervised IT");
    }

    private Initiative newInitiative(String name, boolean enabled, User supervisor) {
        Initiative initiative = new Initiative();
        initiative.setName(name);
        initiative.setEnabled(enabled);
        initiative.setSupervisor(supervisor);
        return initiative;
    }

    private User persistUser(String username, String email) {
        Role role = roleRepository.findByName("VOLUNTEER").orElseThrow();
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("hash");
        user.setEnabled(true);
        user.setRoles(new HashSet<>(Set.of(role)));
        return entityManager.persistFlushFind(user);
    }
}
