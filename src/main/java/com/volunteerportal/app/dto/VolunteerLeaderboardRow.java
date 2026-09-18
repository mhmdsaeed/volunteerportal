package com.volunteerportal.app.dto;

public record VolunteerLeaderboardRow(
        Long userId,
        String username,
        String gradeName,
        long points) {
}
