package com.volunteerportal.app.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import com.volunteerportal.app.dto.EventAttendanceRow;
import com.volunteerportal.app.dto.InitiativeParticipationRow;
import com.volunteerportal.app.dto.VolunteerLeaderboardRow;
import com.volunteerportal.app.model.Event;
import com.volunteerportal.app.model.Initiative;
import com.volunteerportal.app.model.User;
import com.volunteerportal.app.repository.AttendRepository;
import com.volunteerportal.app.repository.EventRepository;
import com.volunteerportal.app.repository.InitiativeRepository;
import com.volunteerportal.app.repository.UserRepository;
import com.volunteerportal.app.repository.VolunteerInitiativeRepository;
import com.volunteerportal.app.repository.VolunteerProfileRepository;

@Service
public class ReportServiceImpl implements ReportService {

    private static final String VOLUNTEER_ROLE = "VOLUNTEER";
    private static final int CHECK_IN = 1;
    private static final int CHECK_OUT = 2;

    private final InitiativeRepository initiativeRepository;
    private final VolunteerInitiativeRepository volunteerInitiativeRepository;
    private final EventRepository eventRepository;
    private final AttendRepository attendRepository;
    private final UserRepository userRepository;
    private final VolunteerProfileRepository volunteerProfileRepository;

    public ReportServiceImpl(InitiativeRepository initiativeRepository,
            VolunteerInitiativeRepository volunteerInitiativeRepository, EventRepository eventRepository,
            AttendRepository attendRepository, UserRepository userRepository,
            VolunteerProfileRepository volunteerProfileRepository) {
        this.initiativeRepository = initiativeRepository;
        this.volunteerInitiativeRepository = volunteerInitiativeRepository;
        this.eventRepository = eventRepository;
        this.attendRepository = attendRepository;
        this.userRepository = userRepository;
        this.volunteerProfileRepository = volunteerProfileRepository;
    }

    @Override
    public List<InitiativeParticipationRow> initiativeParticipationReport() {
        return initiativeRepository.findAll().stream()
                .map(this::toParticipationRow)
                .toList();
    }

    private InitiativeParticipationRow toParticipationRow(Initiative initiative) {
        long approved = volunteerInitiativeRepository.countByInitiativeIdAndEnabledTrue(initiative.getId());
        long rejected = volunteerInitiativeRepository.countByInitiativeIdAndEnabledFalse(initiative.getId());
        long pending = volunteerInitiativeRepository.countByInitiativeIdAndEnabledIsNull(initiative.getId());
        long total = volunteerInitiativeRepository.countByInitiativeId(initiative.getId());

        return new InitiativeParticipationRow(
                initiative.getId(),
                initiative.getName(),
                initiative.getOffice() != null ? initiative.getOffice().getName() : null,
                initiative.getSupervisor() != null ? initiative.getSupervisor().getUsername() : null,
                approved,
                pending,
                rejected,
                total);
    }

    @Override
    public List<EventAttendanceRow> eventAttendanceReport() {
        return eventRepository.findAll().stream()
                .map(this::toAttendanceRow)
                .toList();
    }

    private EventAttendanceRow toAttendanceRow(Event event) {
        long checkIns = attendRepository.countByEventIdAndAttendInOut(event.getId(), CHECK_IN);
        long checkOuts = attendRepository.countByEventIdAndAttendInOut(event.getId(), CHECK_OUT);

        return new EventAttendanceRow(
                event.getId(),
                event.getInitiative() != null ? event.getInitiative().getName() : null,
                event.getName(),
                event.getFromDttm(),
                checkIns,
                checkOuts);
    }

    @Override
    public List<VolunteerLeaderboardRow> volunteerLeaderboard() {
        List<User> volunteers = userRepository.findByRoles_Name(VOLUNTEER_ROLE);

        return volunteers.stream()
                .map(this::toLeaderboardRow)
                .sorted(Comparator.comparingLong(VolunteerLeaderboardRow::points).reversed())
                .toList();
    }

    private VolunteerLeaderboardRow toLeaderboardRow(User volunteer) {
        return volunteerProfileRepository.findByUserId(volunteer.getId())
                .map(profile -> new VolunteerLeaderboardRow(
                        volunteer.getId(),
                        volunteer.getUsername(),
                        profile.getGrade() != null ? profile.getGrade().getName() : "Unranked",
                        profile.getPoints() != null ? profile.getPoints() : 0L))
                .orElseGet(() -> new VolunteerLeaderboardRow(volunteer.getId(), volunteer.getUsername(), "Unranked", 0L));
    }
}
