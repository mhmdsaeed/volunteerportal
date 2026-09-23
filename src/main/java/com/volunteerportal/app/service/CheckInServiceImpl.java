package com.volunteerportal.app.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.volunteerportal.app.dto.AttendForm;
import com.volunteerportal.app.model.Attend;
import com.volunteerportal.app.model.Event;
import com.volunteerportal.app.model.VolunteerInitiative;
import com.volunteerportal.app.repository.AttendRepository;
import com.volunteerportal.app.repository.EventRepository;
import com.volunteerportal.app.repository.VolunteerInitiativeRepository;

@Service
public class CheckInServiceImpl implements CheckInService {

    static final int CHECK_IN = 1;
    static final int CHECK_OUT = 2;
    static final String NOTE = "QR self check-in";
    private static final double EARTH_RADIUS_METERS = 6_371_000;

    private final EventRepository eventRepository;
    private final VolunteerInitiativeRepository volunteerInitiativeRepository;
    private final AttendRepository attendRepository;
    private final AttendService attendService;
    private final CheckInCodes checkInCodes;
    private final double maxDistanceMeters;

    public CheckInServiceImpl(EventRepository eventRepository,
            VolunteerInitiativeRepository volunteerInitiativeRepository, AttendRepository attendRepository,
            AttendService attendService, CheckInCodes checkInCodes,
            @Value("${app.checkin.max-distance-meters:300}") double maxDistanceMeters) {
        this.eventRepository = eventRepository;
        this.volunteerInitiativeRepository = volunteerInitiativeRepository;
        this.attendRepository = attendRepository;
        this.attendService = attendService;
        this.checkInCodes = checkInCodes;
        this.maxDistanceMeters = maxDistanceMeters;
    }

    @Override
    public Event findEvent(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found: " + eventId));
    }

    @Override
    public Action nextAction(Event event, Long userId) {
        Optional<VolunteerInitiative> membership = approvedMembership(event, userId);
        if (membership.isEmpty()) {
            return Action.NOT_MEMBER;
        }
        List<Attend> records = attendRepository.findByEventIdAndVolunteerInitiativeId(event.getId(), membership.get().getId());
        boolean checkedIn = records.stream().anyMatch(a -> Integer.valueOf(CHECK_IN).equals(a.getAttendInOut()));
        boolean checkedOut = records.stream().anyMatch(a -> Integer.valueOf(CHECK_OUT).equals(a.getAttendInOut()));
        if (!checkedIn) {
            return Action.CHECK_IN;
        }
        return checkedOut ? Action.DONE : Action.CHECK_OUT;
    }

    @Override
    public boolean requiresLocation(Event event) {
        return event.getLocLatitude() != null && event.getLocLongitude() != null;
    }

    @Override
    @Transactional
    public Result checkIn(Long eventId, Long userId, String code, Double latitude, Double longitude) {
        Event event = findEvent(eventId);
        if (!checkInCodes.isValid(eventId, code)) {
            return Result.INVALID_CODE;
        }
        if (!Boolean.TRUE.equals(event.getEnabled())) {
            return Result.EVENT_CLOSED;
        }
        Optional<VolunteerInitiative> membership = approvedMembership(event, userId);
        if (membership.isEmpty()) {
            return Result.NOT_MEMBER;
        }
        if (requiresLocation(event)) {
            if (latitude == null || longitude == null) {
                return Result.LOCATION_REQUIRED;
            }
            if (distanceMeters(latitude, longitude, event.getLocLatitude(), event.getLocLongitude()) > maxDistanceMeters) {
                return Result.TOO_FAR;
            }
        }

        Action action = nextAction(event, userId);
        if (action == Action.DONE) {
            return Result.ALREADY_DONE;
        }

        AttendForm form = new AttendForm();
        form.setVolunteerInitiativeId(membership.get().getId());
        form.setAttendInOut(action == Action.CHECK_IN ? CHECK_IN : CHECK_OUT);
        form.setAttendDttm(LocalDateTime.now());
        form.setNote(NOTE);
        attendService.create(eventId, form);
        return action == Action.CHECK_IN ? Result.CHECKED_IN : Result.CHECKED_OUT;
    }

    private Optional<VolunteerInitiative> approvedMembership(Event event, Long userId) {
        if (event.getInitiative() == null || userId == null) {
            return Optional.empty();
        }
        return volunteerInitiativeRepository.findByUserIdAndInitiativeId(userId, event.getInitiative().getId())
                .filter(vi -> Boolean.TRUE.equals(vi.getEnabled()));
    }

    /** Great-circle (haversine) distance. */
    static double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * EARTH_RADIUS_METERS * Math.asin(Math.sqrt(a));
    }
}
