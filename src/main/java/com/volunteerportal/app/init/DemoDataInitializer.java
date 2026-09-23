package com.volunteerportal.app.init;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.volunteerportal.app.model.Event;
import com.volunteerportal.app.model.Initiative;
import com.volunteerportal.app.model.User;
import com.volunteerportal.app.model.VolunteerInitiative;
import com.volunteerportal.app.repository.EventRepository;
import com.volunteerportal.app.repository.InitiativeRepository;
import com.volunteerportal.app.repository.RoleRepository;
import com.volunteerportal.app.repository.UserRepository;
import com.volunteerportal.app.repository.VolunteerInitiativeRepository;

/**
 * Development only (the "dev" profile): creates what's needed to try QR check-in end to end -
 * a demo coordinator supervising a demo initiative with events today, an approved demo volunteer
 * and one with a pending request. Safe to run on every startup: it only creates what is missing.
 */
@Component
@Order(2)
@ConditionalOnProperty(name = "app.demo-data.enabled", havingValue = "true")
public class DemoDataInitializer implements CommandLineRunner {

    static final String COORDINATOR = "demo_coordinator";
    static final String VOLUNTEER = "demo_volunteer";
    static final String PENDING_VOLUNTEER = "demo_pending";
    static final String INITIATIVE = "Demo Initiative";
    static final String EVENT = "Demo Event";
    static final String LOCATION_EVENT = "Demo Event (location check)";

    private static final Logger log = LoggerFactory.getLogger(DemoDataInitializer.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final InitiativeRepository initiativeRepository;
    private final EventRepository eventRepository;
    private final VolunteerInitiativeRepository volunteerInitiativeRepository;
    private final PasswordEncoder passwordEncoder;
    private final String password;
    private final Double eventLatitude;
    private final Double eventLongitude;

    public DemoDataInitializer(UserRepository userRepository, RoleRepository roleRepository,
            InitiativeRepository initiativeRepository, EventRepository eventRepository,
            VolunteerInitiativeRepository volunteerInitiativeRepository, PasswordEncoder passwordEncoder,
            @Value("${app.demo-data.password:demo12345}") String password,
            @Value("${app.demo-data.event-latitude:#{null}}") Double eventLatitude,
            @Value("${app.demo-data.event-longitude:#{null}}") Double eventLongitude) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.initiativeRepository = initiativeRepository;
        this.eventRepository = eventRepository;
        this.volunteerInitiativeRepository = volunteerInitiativeRepository;
        this.passwordEncoder = passwordEncoder;
        this.password = password;
        this.eventLatitude = eventLatitude;
        this.eventLongitude = eventLongitude;
    }

    @Override
    @Transactional
    public void run(String... args) {
        User coordinator = user(COORDINATOR, "COORDINATOR");
        User volunteer = user(VOLUNTEER, "VOLUNTEER");
        User pendingVolunteer = user(PENDING_VOLUNTEER, "VOLUNTEER");

        Initiative initiative = initiativeRepository.findFirstByName(INITIATIVE).orElseGet(() -> {
            Initiative created = new Initiative();
            created.setName(INITIATIVE);
            created.setDescription("Created by the dev profile for trying QR check-in.");
            created.setSupervisor(coordinator);
            created.setEnabled(true);
            created.setQuestionCount(0);
            return initiativeRepository.save(created);
        });

        event(initiative, EVENT, null, null);
        if (eventLatitude != null && eventLongitude != null) {
            event(initiative, LOCATION_EVENT, eventLatitude, eventLongitude);
        }

        membership(volunteer, initiative, true);
        membership(pendingVolunteer, initiative, false);

        log.warn("Demo data ready (dev profile): log in as '{}' (coordinator of '{}'), '{}' (approved member) or '{}'"
                + " (pending request), password '{}'. Never enable the dev profile on a real server.",
                COORDINATOR, INITIATIVE, VOLUNTEER, PENDING_VOLUNTEER, password);
    }

    private User user(String username, String roleName) {
        return userRepository.findByUsername(username).orElseGet(() -> {
            User user = new User();
            user.setUsername(username);
            user.setEmail(username + "@demo.volunteerportal.local");
            user.setPassword(passwordEncoder.encode(password));
            user.setRegistrationDttm(LocalDateTime.now());
            user.setEnabled(true);
            user.setRoles(new HashSet<>(Set.of(roleRepository.findByName(roleName)
                    .orElseThrow(() -> new IllegalStateException("Role '" + roleName + "' is missing")))));
            return userRepository.save(user);
        });
    }

    /** An enabled event running all day today, so check-in can be tried whenever the app is started. */
    private void event(Initiative initiative, String name, Double latitude, Double longitude) {
        Optional<Event> existing = eventRepository.findFirstByInitiativeIdAndName(initiative.getId(), name);
        Event event = existing.orElseGet(Event::new);
        event.setName(name);
        event.setInitiative(initiative);
        event.setEnabled(true);
        event.setFromDttm(LocalDate.now().atTime(LocalTime.of(8, 0)));
        event.setToDttm(LocalDate.now().atTime(LocalTime.of(22, 0)));
        event.setLocLatitude(latitude);
        event.setLocLongitude(longitude);
        eventRepository.save(event);
    }

    private void membership(User user, Initiative initiative, boolean approved) {
        if (volunteerInitiativeRepository.findByUserIdAndInitiativeId(user.getId(), initiative.getId()).isPresent()) {
            return;
        }
        VolunteerInitiative membership = new VolunteerInitiative();
        membership.setUser(user);
        membership.setInitiative(initiative);
        membership.setRequestJoinDttm(LocalDateTime.now());
        membership.setAnswerCount(0);
        if (approved) {
            membership.setEnabled(true);
            membership.setResponseJoinDttm(LocalDateTime.now());
        }
        volunteerInitiativeRepository.save(membership);
    }
}
