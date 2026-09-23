package com.volunteerportal.app.regression;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import com.volunteerportal.app.model.Event;
import com.volunteerportal.app.model.Initiative;
import com.volunteerportal.app.model.Office;
import com.volunteerportal.app.model.Role;
import com.volunteerportal.app.model.User;
import com.volunteerportal.app.model.VolunteerInitiative;
import com.volunteerportal.app.repository.EventRepository;
import com.volunteerportal.app.repository.InitiativeRepository;
import com.volunteerportal.app.repository.NotificationRepository;
import com.volunteerportal.app.repository.OfficeRepository;
import com.volunteerportal.app.repository.RoleRepository;
import com.volunteerportal.app.repository.UserRepository;
import com.volunteerportal.app.repository.VolunteerInitiativeRepository;
import com.volunteerportal.app.security.UserPrincipal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The join-request flow end to end against the real database and templates: an office
 * coordinator (who is not the initiative's supervisor) sees the pending request, approves it,
 * and the approved volunteer can then see the initiative's events. Uses no test-level
 * transaction, so lazy associations behave as in production (open-in-view is disabled).
 */
@SpringBootTest
@AutoConfigureMockMvc
class JoinRequestFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private OfficeRepository officeRepository;

    @Autowired
    private InitiativeRepository initiativeRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private VolunteerInitiativeRepository volunteerInitiativeRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    private final String suffix = String.valueOf(System.nanoTime());
    private User volunteer;
    private User officeCoordinator;
    private User otherCoordinator;
    private Office office;
    private Initiative initiative;
    private Event event;
    private VolunteerInitiative request;

    private void setUpPendingRequest() {
        volunteer = persistUser("flow_vol_" + suffix, "VOLUNTEER");
        officeCoordinator = persistUser("flow_officecoord_" + suffix, "COORDINATOR");
        otherCoordinator = persistUser("flow_othercoord_" + suffix, "COORDINATOR");

        office = new Office();
        office.setName("Flow Office " + suffix);
        office.setUser(officeCoordinator);
        office = officeRepository.save(office);

        initiative = new Initiative();
        initiative.setName("Flow Initiative " + suffix);
        initiative.setEnabled(true);
        initiative.setOffice(office); // no supervisor: only the office coordinator manages it
        initiative = initiativeRepository.save(initiative);

        event = new Event();
        event.setName("Flow Event " + suffix);
        event.setInitiative(initiative);
        event.setEnabled(true);
        event.setFromDttm(LocalDateTime.now().plusDays(3));
        event = eventRepository.save(event);

        request = new VolunteerInitiative();
        request.setUser(volunteer);
        request.setInitiative(initiative);
        request.setRequestJoinDttm(LocalDateTime.now());
        request = volunteerInitiativeRepository.save(request);
    }

    @AfterEach
    void cleanUp() {
        if (request != null) {
            volunteerInitiativeRepository.deleteById(request.getId());
        }
        if (event != null) {
            eventRepository.delete(event);
        }
        if (initiative != null) {
            initiativeRepository.delete(initiative);
        }
        if (office != null) {
            officeRepository.delete(office);
        }
        for (User u : new User[] { volunteer, officeCoordinator, otherCoordinator }) {
            if (u != null) {
                notificationRepository.deleteAll(notificationRepository.findByUserIdOrderByCreatedDttmDesc(u.getId()));
                userRepository.delete(u);
            }
        }
    }

    @Test
    void officeCoordinator_seesPendingRequest_approvesIt_andVolunteerThenSeesEvents() throws Exception {
        setUpPendingRequest();

        // Before approval the volunteer is not a member: no events shown
        mockMvc.perform(get("/initiatives/{id}", initiative.getId()).with(user(principal(volunteer))))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(event.getName()))));

        mockMvc.perform(get("/coordinator/requests").with(user(principal(officeCoordinator))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(volunteer.getUsername())))
                .andExpect(content().string(containsString(initiative.getName())));

        mockMvc.perform(post("/coordinator/requests/{id}/approve", request.getId())
                        .with(user(principal(officeCoordinator))).with(csrf()))
                .andExpect(redirectedUrl("/coordinator/requests"));

        VolunteerInitiative approved = volunteerInitiativeRepository.findById(request.getId()).orElseThrow();
        assertThat(approved.getEnabled()).isTrue();
        assertThat(approved.getResponseJoinDttm()).isNotNull();
        assertThat(notificationRepository.findByUserIdOrderByCreatedDttmDesc(volunteer.getId()))
                .anySatisfy(n -> assertThat(n.getMessage()).contains("approved"));

        // No longer pending, and the approved member now sees the initiative's events
        mockMvc.perform(get("/coordinator/requests").with(user(principal(officeCoordinator))))
                .andExpect(content().string(not(containsString(volunteer.getUsername()))));
        mockMvc.perform(get("/initiatives/{id}", initiative.getId()).with(user(principal(volunteer))))
                .andExpect(content().string(containsString(event.getName())));
    }

    @Test
    void unrelatedCoordinator_neitherSeesNorApprovesTheRequest() throws Exception {
        setUpPendingRequest();

        mockMvc.perform(get("/coordinator/requests").with(user(principal(otherCoordinator))))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(volunteer.getUsername()))));

        mockMvc.perform(post("/coordinator/requests/{id}/approve", request.getId())
                        .with(user(principal(otherCoordinator))).with(csrf()))
                .andExpect(status().isForbidden());

        assertThat(volunteerInitiativeRepository.findById(request.getId()).orElseThrow().getResponseJoinDttm()).isNull();
    }

    @Test
    void pendingCountBadge_showsForTheOfficeCoordinator() throws Exception {
        setUpPendingRequest();

        assertThat(volunteerInitiativeRepository.countPendingManagedBy(officeCoordinator.getId())).isEqualTo(1);
        assertThat(volunteerInitiativeRepository.countPendingManagedBy(otherCoordinator.getId())).isZero();
        assertThat(initiativeRepository.findManagedBy(officeCoordinator.getId()))
                .extracting(Initiative::getId).containsExactly(initiative.getId());

        mockMvc.perform(get("/coordinator/initiatives").with(user(principal(officeCoordinator))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(initiative.getName())))
                .andExpect(content().string(containsString("text-bg-warning\">1</span>")));
    }

    @Test
    void officeCoordinator_addsAndEditsAnEventForTheirInitiative() throws Exception {
        setUpPendingRequest();
        String newName = "Coordinator Event " + suffix;

        mockMvc.perform(post("/coordinator/initiatives/{id}/events", initiative.getId())
                        .with(user(principal(officeCoordinator))).with(csrf())
                        .param("name", newName)
                        .param("enabled", "true"))
                .andExpect(redirectedUrl("/coordinator/initiatives/" + initiative.getId() + "/events"));

        Event created = eventRepository.findByInitiativeId(initiative.getId()).stream()
                .filter(e -> newName.equals(e.getName())).findFirst().orElseThrow();
        try {
            mockMvc.perform(get("/coordinator/initiatives/{id}/events", initiative.getId()).with(user(principal(officeCoordinator))))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString(newName)));

            // Edit loads the event outside any session and checks it belongs to the initiative
            mockMvc.perform(get("/coordinator/initiatives/{id}/events/{eventId}/edit", initiative.getId(), created.getId())
                            .with(user(principal(officeCoordinator))))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString(newName)));

            mockMvc.perform(post("/coordinator/initiatives/{id}/events", initiative.getId())
                            .with(user(principal(otherCoordinator))).with(csrf())
                            .param("name", "Not allowed"))
                    .andExpect(status().isForbidden());
        } finally {
            eventRepository.delete(created);
        }
    }

    private UserPrincipal principal(User user) {
        return new UserPrincipal(userRepository.findByUsername(user.getUsername()).orElseThrow());
    }

    private User persistUser(String username, String roleName) {
        Role role = roleRepository.findByName(roleName).orElseThrow();
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setPassword("irrelevant-hash");
        user.setEnabled(true);
        user.setRoles(new HashSet<>(Set.of(role)));
        return userRepository.save(user);
    }
}
