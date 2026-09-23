package com.volunteerportal.app.api;

import java.time.LocalDateTime;
import java.util.List;

/** JSON shapes of the mobile app API (see "Mobile app API" in README.md). */
public final class ApiDtos {

    private ApiDtos() {
    }

    public record LoginRequest(String username, String password, String deviceName) {
    }

    public record LoginResponse(String token, LocalDateTime expiresAt, Me user) {
    }

    public record Me(Long id, String username, String email, List<String> roles, String grade, long points) {
    }

    /** membership: NONE, PENDING, APPROVED or REJECTED. */
    public record InitiativeItem(Long id, String name, String description, String office, String membership) {
    }

    /** myStatus: NOT_CHECKED_IN, CHECKED_IN or CHECKED_OUT. */
    public record EventItem(Long id, String name, Long initiativeId, String initiative, LocalDateTime from,
            LocalDateTime to, String locationUrl, Double latitude, Double longitude, boolean requiresLocation,
            String myStatus) {
    }

    /** qr: the text of the scanned QR code (the check-in link). Location is needed for events that have one. */
    public record CheckInRequest(String qr, Double latitude, Double longitude) {
    }

    /** result: CHECKED_IN, CHECKED_OUT, ALREADY_DONE, INVALID_CODE, NOT_MEMBER, EVENT_CLOSED, LOCATION_REQUIRED or TOO_FAR. */
    public record CheckInResponse(String result, boolean success, Long eventId, String event, String message) {
    }

    /** type: CHECK_IN or CHECK_OUT. */
    public record AttendanceItem(Long id, Long eventId, String event, String initiative, String type,
            LocalDateTime time, String note) {
    }

    public record NotificationItem(Long id, String message, String link, boolean read, LocalDateTime createdAt) {
    }

    public record Error(String error, String message) {
    }
}
