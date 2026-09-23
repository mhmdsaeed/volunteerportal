package com.volunteerportal.app.service;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.volunteerportal.app.dto.AttendForm;
import com.volunteerportal.app.model.Attend;
import com.volunteerportal.app.model.Event;
import com.volunteerportal.app.model.Initiative;
import com.volunteerportal.app.model.VolunteerInitiative;
import com.volunteerportal.app.repository.AttendRepository;
import com.volunteerportal.app.repository.EventRepository;
import com.volunteerportal.app.repository.VolunteerInitiativeRepository;
import com.volunteerportal.app.service.CheckInService.Action;
import com.volunteerportal.app.service.CheckInService.Result;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CheckInServiceImplTest {

    // An event in central Riyadh
    private static final double EVENT_LAT = 24.7136;
    private static final double EVENT_LON = 46.6753;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private VolunteerInitiativeRepository volunteerInitiativeRepository;

    @Mock
    private AttendRepository attendRepository;

    @Mock
    private AttendService attendService;

    @Mock
    private CheckInCodes checkInCodes;

    private CheckInServiceImpl service;
    private Event event;
    private VolunteerInitiative membership;

    @BeforeEach
    void setUp() {
        service = new CheckInServiceImpl(eventRepository, volunteerInitiativeRepository, attendRepository,
                attendService, checkInCodes, 300);

        Initiative initiative = new Initiative();
        initiative.setId(5L);
        event = new Event();
        event.setId(9L);
        event.setInitiative(initiative);
        event.setEnabled(true);

        membership = new VolunteerInitiative();
        membership.setId(3L);
        membership.setEnabled(true);

        lenient().when(eventRepository.findById(9L)).thenReturn(Optional.of(event));
        lenient().when(checkInCodes.isValid(9L, "good")).thenReturn(true);
        lenient().when(volunteerInitiativeRepository.findByUserIdAndInitiativeId(7L, 5L)).thenReturn(Optional.of(membership));
        lenient().when(attendRepository.findByEventIdAndVolunteerInitiativeId(9L, 3L)).thenReturn(List.of());
    }

    @Test
    void firstScan_checksIn() {
        assertThat(service.checkIn(9L, 7L, "good", null, null)).isEqualTo(Result.CHECKED_IN);

        ArgumentCaptor<AttendForm> form = ArgumentCaptor.forClass(AttendForm.class);
        verify(attendService).create(eq(9L), form.capture());
        assertThat(form.getValue().getVolunteerInitiativeId()).isEqualTo(3L);
        assertThat(form.getValue().getAttendInOut()).isEqualTo(1);
        assertThat(form.getValue().getAttendDttm()).isNotNull();
        assertThat(form.getValue().getNote()).isEqualTo(CheckInServiceImpl.NOTE);
    }

    @Test
    void scanAfterCheckIn_checksOut() {
        given(attendRepository.findByEventIdAndVolunteerInitiativeId(9L, 3L)).willReturn(List.of(attend(1)));

        assertThat(service.checkIn(9L, 7L, "good", null, null)).isEqualTo(Result.CHECKED_OUT);

        ArgumentCaptor<AttendForm> form = ArgumentCaptor.forClass(AttendForm.class);
        verify(attendService).create(eq(9L), form.capture());
        assertThat(form.getValue().getAttendInOut()).isEqualTo(2);
    }

    @Test
    void scanAfterCheckOut_isAlreadyDone() {
        given(attendRepository.findByEventIdAndVolunteerInitiativeId(9L, 3L)).willReturn(List.of(attend(1), attend(2)));

        assertThat(service.checkIn(9L, 7L, "good", null, null)).isEqualTo(Result.ALREADY_DONE);
        verify(attendService, never()).create(anyLong(), any());
    }

    @Test
    void invalidCode_isRejectedBeforeAnythingElse() {
        assertThat(service.checkIn(9L, 7L, "expired", null, null)).isEqualTo(Result.INVALID_CODE);
        verify(attendService, never()).create(anyLong(), any());
    }

    @Test
    void disabledEvent_isClosed() {
        event.setEnabled(false);

        assertThat(service.checkIn(9L, 7L, "good", null, null)).isEqualTo(Result.EVENT_CLOSED);
    }

    @Test
    void pendingOrNonMember_cannotCheckIn() {
        membership.setEnabled(null);
        assertThat(service.checkIn(9L, 7L, "good", null, null)).isEqualTo(Result.NOT_MEMBER);

        given(volunteerInitiativeRepository.findByUserIdAndInitiativeId(8L, 5L)).willReturn(Optional.empty());
        assertThat(service.checkIn(9L, 8L, "good", null, null)).isEqualTo(Result.NOT_MEMBER);

        verify(attendService, never()).create(anyLong(), any());
    }

    @Test
    void eventWithCoordinates_requiresTheVolunteersLocation() {
        withCoordinates();

        assertThat(service.checkIn(9L, 7L, "good", null, null)).isEqualTo(Result.LOCATION_REQUIRED);
        verify(attendService, never()).create(anyLong(), any());
    }

    @Test
    void eventWithCoordinates_nearbyVolunteerChecksIn() {
        withCoordinates();

        // about 100 m north
        assertThat(service.checkIn(9L, 7L, "good", EVENT_LAT + 0.0009, EVENT_LON)).isEqualTo(Result.CHECKED_IN);
    }

    @Test
    void eventWithCoordinates_farAwayVolunteerIsTooFar() {
        withCoordinates();

        // about 1 km north
        assertThat(service.checkIn(9L, 7L, "good", EVENT_LAT + 0.009, EVENT_LON)).isEqualTo(Result.TOO_FAR);
        verify(attendService, never()).create(anyLong(), any());
    }

    @Test
    void nextAction_followsTheRecordsSoFar() {
        assertThat(service.nextAction(event, 7L)).isEqualTo(Action.CHECK_IN);

        given(attendRepository.findByEventIdAndVolunteerInitiativeId(9L, 3L)).willReturn(List.of(attend(1)));
        assertThat(service.nextAction(event, 7L)).isEqualTo(Action.CHECK_OUT);

        given(attendRepository.findByEventIdAndVolunteerInitiativeId(9L, 3L)).willReturn(List.of(attend(1), attend(2)));
        assertThat(service.nextAction(event, 7L)).isEqualTo(Action.DONE);

        given(volunteerInitiativeRepository.findByUserIdAndInitiativeId(8L, 5L)).willReturn(Optional.empty());
        assertThat(service.nextAction(event, 8L)).isEqualTo(Action.NOT_MEMBER);
    }

    @Test
    void distanceMeters_matchesKnownDistances() {
        assertThat(CheckInServiceImpl.distanceMeters(EVENT_LAT, EVENT_LON, EVENT_LAT, EVENT_LON)).isZero();
        // 0.009 degrees of latitude is about 1 km
        assertThat(CheckInServiceImpl.distanceMeters(EVENT_LAT, EVENT_LON, EVENT_LAT + 0.009, EVENT_LON)).isCloseTo(1001, within(5.0));
        // Riyadh to Jeddah is about 850 km
        assertThat(CheckInServiceImpl.distanceMeters(EVENT_LAT, EVENT_LON, 21.4858, 39.1925) / 1000).isCloseTo(850, within(15.0));
    }

    private void withCoordinates() {
        event.setLocLatitude(EVENT_LAT);
        event.setLocLongitude(EVENT_LON);
    }

    private Attend attend(int inOut) {
        Attend attend = new Attend();
        attend.setAttendInOut(inOut);
        return attend;
    }
}
