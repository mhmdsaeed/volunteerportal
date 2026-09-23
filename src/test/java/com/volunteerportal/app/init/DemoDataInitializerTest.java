package com.volunteerportal.app.init;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.volunteerportal.app.model.Event;
import com.volunteerportal.app.model.Initiative;
import com.volunteerportal.app.model.Role;
import com.volunteerportal.app.model.User;
import com.volunteerportal.app.model.VolunteerInitiative;
import com.volunteerportal.app.repository.EventRepository;
import com.volunteerportal.app.repository.InitiativeRepository;
import com.volunteerportal.app.repository.RoleRepository;
import com.volunteerportal.app.repository.UserRepository;
import com.volunteerportal.app.repository.VolunteerInitiativeRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DemoDataInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private InitiativeRepository initiativeRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private VolunteerInitiativeRepository volunteerInitiativeRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        lenient().when(roleRepository.findByName(anyString())).thenAnswer(inv -> Optional.of(new Role(inv.getArgument(0))));
        lenient().when(passwordEncoder.encode(anyString())).thenReturn("hash");
        lenient().when(userRepository.save(any(User.class))).thenAnswer(inv -> withId(inv.getArgument(0)));
        lenient().when(initiativeRepository.save(any(Initiative.class))).thenAnswer(inv -> {
            Initiative initiative = inv.getArgument(0);
            initiative.setId(50L);
            return initiative;
        });
    }

    @Test
    void firstRun_createsUsersInitiativeTodaysEventAndMemberships() {
        given(userRepository.findByUsername(anyString())).willReturn(Optional.empty());
        given(initiativeRepository.findFirstByName(DemoDataInitializer.INITIATIVE)).willReturn(Optional.empty());
        given(eventRepository.findFirstByInitiativeIdAndName(anyLong(), anyString())).willReturn(Optional.empty());
        given(volunteerInitiativeRepository.findByUserIdAndInitiativeId(anyLong(), anyLong())).willReturn(Optional.empty());

        initializer(null, null).run();

        ArgumentCaptor<User> users = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(3)).save(users.capture());
        assertThat(users.getAllValues()).extracting(User::getUsername)
                .containsExactlyInAnyOrder("demo_coordinator", "demo_volunteer", "demo_pending");
        assertThat(users.getAllValues()).allSatisfy(u -> assertThat(u.isEnabled()).isTrue());

        ArgumentCaptor<Initiative> initiative = ArgumentCaptor.forClass(Initiative.class);
        verify(initiativeRepository).save(initiative.capture());
        assertThat(initiative.getValue().getSupervisor().getUsername()).isEqualTo("demo_coordinator");
        assertThat(initiative.getValue().getEnabled()).isTrue();

        ArgumentCaptor<Event> event = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(event.capture());
        assertThat(event.getValue().getEnabled()).isTrue();
        assertThat(event.getValue().getFromDttm().toLocalDate()).isEqualTo(LocalDate.now());
        assertThat(event.getValue().getLocLatitude()).isNull();

        ArgumentCaptor<VolunteerInitiative> memberships = ArgumentCaptor.forClass(VolunteerInitiative.class);
        verify(volunteerInitiativeRepository, times(2)).save(memberships.capture());
        assertThat(memberships.getAllValues())
                .extracting(vi -> vi.getUser().getUsername(), VolunteerInitiative::getEnabled)
                .containsExactlyInAnyOrder(
                        tuple("demo_volunteer", true),
                        tuple("demo_pending", null));
    }

    @Test
    void withCoordinates_alsoCreatesALocationCheckEvent() {
        given(userRepository.findByUsername(anyString())).willReturn(Optional.empty());
        given(initiativeRepository.findFirstByName(DemoDataInitializer.INITIATIVE)).willReturn(Optional.empty());
        given(eventRepository.findFirstByInitiativeIdAndName(anyLong(), anyString())).willReturn(Optional.empty());
        given(volunteerInitiativeRepository.findByUserIdAndInitiativeId(anyLong(), anyLong())).willReturn(Optional.empty());

        initializer(24.7, 46.7).run();

        ArgumentCaptor<Event> events = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository, times(2)).save(events.capture());
        assertThat(events.getAllValues()).extracting(Event::getName)
                .containsExactly(DemoDataInitializer.EVENT, DemoDataInitializer.LOCATION_EVENT);
        assertThat(events.getAllValues().get(1).getLocLatitude()).isEqualTo(24.7);
    }

    @Test
    void laterRuns_createNothingNew_butMoveTheEventToToday() {
        given(userRepository.findByUsername(anyString())).willAnswer(inv -> Optional.of(withId(user(inv.getArgument(0)))));
        Initiative existing = new Initiative();
        existing.setId(50L);
        given(initiativeRepository.findFirstByName(DemoDataInitializer.INITIATIVE)).willReturn(Optional.of(existing));
        Event oldEvent = new Event();
        oldEvent.setFromDttm(LocalDate.now().minusDays(30).atStartOfDay());
        given(eventRepository.findFirstByInitiativeIdAndName(50L, DemoDataInitializer.EVENT)).willReturn(Optional.of(oldEvent));
        given(volunteerInitiativeRepository.findByUserIdAndInitiativeId(anyLong(), anyLong()))
                .willReturn(Optional.of(new VolunteerInitiative()));

        initializer(null, null).run();

        verify(userRepository, never()).save(any());
        verify(initiativeRepository, never()).save(any());
        verify(volunteerInitiativeRepository, never()).save(any());
        verify(eventRepository).save(oldEvent);
        assertThat(oldEvent.getFromDttm().toLocalDate()).isEqualTo(LocalDate.now());
    }

    private DemoDataInitializer initializer(Double latitude, Double longitude) {
        return new DemoDataInitializer(userRepository, roleRepository, initiativeRepository, eventRepository,
                volunteerInitiativeRepository, passwordEncoder, "demo12345", latitude, longitude);
    }

    private static long nextId = 100;

    private static User withId(User user) {
        if (user.getId() == null) {
            user.setId(nextId++);
        }
        return user;
    }

    private static User user(String username) {
        User user = new User();
        user.setUsername(username);
        return user;
    }
}
