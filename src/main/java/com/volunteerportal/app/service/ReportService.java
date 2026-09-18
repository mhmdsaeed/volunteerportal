package com.volunteerportal.app.service;

import java.util.List;

import com.volunteerportal.app.dto.EventAttendanceRow;
import com.volunteerportal.app.dto.InitiativeParticipationRow;
import com.volunteerportal.app.dto.VolunteerLeaderboardRow;

public interface ReportService {

    List<InitiativeParticipationRow> initiativeParticipationReport();

    List<EventAttendanceRow> eventAttendanceReport();

    List<VolunteerLeaderboardRow> volunteerLeaderboard();
}
