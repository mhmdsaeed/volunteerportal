package com.volunteerportal.app.service;

import com.volunteerportal.app.model.Event;

/** Volunteer self check-in/out by scanning the event's rotating QR code. */
public interface CheckInService {

    /** What scanning now would do for this volunteer. */
    enum Action {
        CHECK_IN, CHECK_OUT, DONE, NOT_MEMBER
    }

    enum Result {
        CHECKED_IN, CHECKED_OUT, ALREADY_DONE, INVALID_CODE, NOT_MEMBER, EVENT_CLOSED, LOCATION_REQUIRED, TOO_FAR
    }

    Event findEvent(Long eventId);

    Action nextAction(Event event, Long userId);

    /** Whether the event has coordinates, so the volunteer's location must be sent and checked. */
    boolean requiresLocation(Event event);

    /**
     * Records a check-in (or, if already checked in, a check-out) after checking the code, that the
     * event is enabled, that the user is an approved member, and, for events with coordinates, that
     * the given location is close enough.
     */
    Result checkIn(Long eventId, Long userId, String code, Double latitude, Double longitude);
}
