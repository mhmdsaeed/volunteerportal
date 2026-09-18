package com.volunteerportal.app.dto;

public record InitiativeParticipationRow(
        Long initiativeId,
        String initiativeName,
        String officeName,
        String supervisorUsername,
        long approvedCount,
        long pendingCount,
        long rejectedCount,
        long totalCount) {
}
