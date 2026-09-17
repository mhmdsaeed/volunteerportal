package com.volunteerportal.app.repository;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import com.volunteerportal.app.model.Initiative;
import com.volunteerportal.app.model.Role;
import com.volunteerportal.app.model.User;
import com.volunteerportal.app.model.VolunteerInitiative;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class VolunteerInitiativeRepositoryTest {

    @Autowired
    private VolunteerInitiativeRepository volunteerInitiativeRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findByUserIdAndInitiativeId_returnsMatchingMembership() {
        User user = persistUser("vol1_it", "vol1_it@example.com");
        Initiative initiative = persistInitiative("Initiative A IT");
        persistMembership(user, initiative);

        Optional<VolunteerInitiative> found = volunteerInitiativeRepository
                .findByUserIdAndInitiativeId(user.getId(), initiative.getId());

        assertThat(found).isPresent();
    }

    @Test
    void findByUserId_returnsAllMembershipsForThatUser() {
        User user = persistUser("vol2_it", "vol2_it@example.com");
        Initiative first = persistInitiative("Initiative B IT");
        Initiative second = persistInitiative("Initiative C IT");

        persistMembership(user, first);
        persistMembership(user, second);

        List<VolunteerInitiative> memberships = volunteerInitiativeRepository.findByUserId(user.getId());

        assertThat(memberships).hasSize(2);
    }

    private void persistMembership(User user, Initiative initiative) {
        VolunteerInitiative membership = new VolunteerInitiative();
        membership.setUser(user);
        membership.setInitiative(initiative);
        membership.setRequestJoinDttm(LocalDateTime.now());
        entityManager.persistAndFlush(membership);
    }

    private Initiative persistInitiative(String name) {
        Initiative initiative = new Initiative();
        initiative.setName(name);
        initiative.setEnabled(true);
        return entityManager.persistFlushFind(initiative);
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
