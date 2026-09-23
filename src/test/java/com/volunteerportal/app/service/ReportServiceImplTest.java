package com.volunteerportal.app.service;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.volunteerportal.app.dto.EventAttendanceRow;
import com.volunteerportal.app.dto.InitiativeParticipationRow;
import com.volunteerportal.app.dto.VolunteerLeaderboardRow;
import com.volunteerportal.app.model.Event;
import com.volunteerportal.app.model.Grade;
import com.volunteerportal.app.model.Initiative;
import com.volunteerportal.app.model.Office;
import com.volunteerportal.app.model.User;
import com.volunteerportal.app.model.VolunteerProfile;
import com.volunteerportal.app.repository.AttendRepository;
import com.volunteerportal.app.repository.EventRepository;
import com.volunteerportal.app.repository.InitiativeRepository;
import com.volunteerportal.app.repository.UserRepository;
import com.volunteerportal.app.repository.VolunteerInitiativeRepository;
import com.volunteerportal.app.repository.VolunteerProfileRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    @Mock
    private InitiativeRepository initiativeRepository;

    @Mock
    private VolunteerInitiativeRepository volunteerInitiativeRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private AttendRepository attendRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private VolunteerProfileRepository volunteerProfileRepository;

    @InjectMocks
    private ReportServiceImpl reportService;

    @Test
    void initiativeParticipationReport_countsRequestsByStatusPerInitiative() {
        Office office = new Office();
        office.setName("Downtown");

        User supervisor = new User();
        supervisor.setUsername("coord1");

        Initiative initiative = new Initiative();
        initiative.setId(10L);
        initiative.setName("Beach Cleanup");
        initiative.setOffice(office);
        initiative.setSupervisor(supervisor);

        given(initiativeRepository.findAll()).willReturn(List.of(initiative));
        given(volunteerInitiativeRepository.countByInitiativeIdAndEnabledTrue(10L)).willReturn(3L);
        given(volunteerInitiativeRepository.countByInitiativeIdAndEnabledFalse(10L)).willReturn(1L);
        given(volunteerInitiativeRepository.countByInitiativeIdAndEnabledIsNull(10L)).willReturn(2L);
        given(volunteerInitiativeRepository.countByInitiativeId(10L)).willReturn(6L);

        List<InitiativeParticipationRow> result = reportService.initiativeParticipationReport();

        assertThat(result).containsExactly(new InitiativeParticipationRow(
                10L, "Beach Cleanup", "Downtown", "coord1", 3L, 2L, 1L, 6L));
    }

    @Test
    void initiativeParticipationReport_missingOfficeAndSupervisor_rendersNulls() {
        Initiative initiative = new Initiative();
        initiative.setId(11L);
        initiative.setName("Food Drive");

        given(initiativeRepository.findAll()).willReturn(List.of(initiative));
        given(volunteerInitiativeRepository.countByInitiativeIdAndEnabledTrue(11L)).willReturn(0L);
        given(volunteerInitiativeRepository.countByInitiativeIdAndEnabledFalse(11L)).willReturn(0L);
        given(volunteerInitiativeRepository.countByInitiativeIdAndEnabledIsNull(11L)).willReturn(0L);
        given(volunteerInitiativeRepository.countByInitiativeId(11L)).willReturn(0L);

        List<InitiativeParticipationRow> result = reportService.initiativeParticipationReport();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).officeName()).isNull();
        assertThat(result.get(0).supervisorUsername()).isNull();
    }

    @Test
    void eventAttendanceReport_countsCheckInsAndCheckOutsPerEvent() {
        Initiative initiative = new Initiative();
        initiative.setName("Beach Cleanup");

        Event event = new Event();
        event.setId(20L);
        event.setName("Saturday Shift");
        event.setInitiative(initiative);

        given(eventRepository.findAll()).willReturn(List.of(event));
        given(attendRepository.countByEventIdAndAttendInOut(20L, 1)).willReturn(5L);
        given(attendRepository.countByEventIdAndAttendInOut(20L, 2)).willReturn(4L);

        List<EventAttendanceRow> result = reportService.eventAttendanceReport();

        assertThat(result).containsExactly(new EventAttendanceRow(
                20L, "Beach Cleanup", "Saturday Shift", event.getFromDttm(), 5L, 4L));
    }

    @Test
    void volunteerLeaderboard_sortsByPointsDescending() {
        User low = new User();
        low.setId(1L);
        low.setUsername("low");

        User high = new User();
        high.setId(2L);
        high.setUsername("high");

        Grade gold = new Grade();
        gold.setName("Gold");

        VolunteerProfile lowProfile = new VolunteerProfile();
        lowProfile.setPoints(10L);

        VolunteerProfile highProfile = new VolunteerProfile();
        highProfile.setPoints(50L);
        highProfile.setGrade(gold);

        given(userRepository.findByRoles_Name("VOLUNTEER")).willReturn(List.of(low, high));
        given(volunteerProfileRepository.findByUserId(1L)).willReturn(Optional.of(lowProfile));
        given(volunteerProfileRepository.findByUserId(2L)).willReturn(Optional.of(highProfile));

        List<VolunteerLeaderboardRow> result = reportService.volunteerLeaderboard();

        assertThat(result).containsExactly(
                new VolunteerLeaderboardRow(2L, "high", "Gold", 50L),
                new VolunteerLeaderboardRow(1L, "low", null, 10L));
    }

    @Test
    void volunteerLeaderboard_noProfileYet_treatedAsUnrankedZeroPoints() {
        User volunteer = new User();
        volunteer.setId(3L);
        volunteer.setUsername("newbie");

        given(userRepository.findByRoles_Name("VOLUNTEER")).willReturn(List.of(volunteer));
        given(volunteerProfileRepository.findByUserId(3L)).willReturn(Optional.empty());

        List<VolunteerLeaderboardRow> result = reportService.volunteerLeaderboard();

        assertThat(result).containsExactly(new VolunteerLeaderboardRow(3L, "newbie", null, 0L));
    }
}
