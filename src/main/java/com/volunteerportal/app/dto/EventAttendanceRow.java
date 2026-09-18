package com.volunteerportal.app.dto;

import java.time.LocalDateTime;

public record EventAttendanceRow(
        Long eventId,
        String initiativeName,
        String eventName,
        LocalDateTime fromDttm,
        long checkInCount,
        long checkOutCount) {
}
