package com.volunteerportal.app.api;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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

    /**
     * An initiative with my membership and the questions to answer when asking to join. type: YES_NO, ONE_CHOICE,
     * MANY_CHOICES or TEXT; choices are numbered from 1 in that order.
     */
    public record InitiativeDetail(Long id, String name, String description, String office, String membership,
            List<QuestionItem> questions) {
    }

    public record QuestionItem(Long id, String text, String type, List<String> choices) {
    }

    /**
     * answers: question id -> values. YES_NO: "1" (yes) or "0" (no); ONE_CHOICE: one choice number; MANY_CHOICES:
     * any choice numbers; TEXT: the text. Unanswered questions can be left out.
     */
    public record JoinRequest(Map<String, List<String>> answers) {
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
