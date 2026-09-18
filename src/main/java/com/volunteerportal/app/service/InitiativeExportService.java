package com.volunteerportal.app.service;

public interface InitiativeExportService {

    /** CSV of every join request for the initiative: volunteer, status, dates, answer count. */
    String exportVolunteersCsv(Long initiativeId);
}
