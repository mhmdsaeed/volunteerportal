package com.volunteerportal.app.dto;

/** {@code gradeName} is null for an unranked volunteer; the view renders the localized label. */
public record VolunteerLeaderboardRow(
        Long userId,
        String username,
        String gradeName,
        long points) {
}
