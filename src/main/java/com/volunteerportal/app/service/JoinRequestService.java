package com.volunteerportal.app.service;

import java.util.List;

import com.volunteerportal.app.model.Initiative;
import com.volunteerportal.app.model.VolunteerInitiative;

public interface JoinRequestService {

    /** Pass {@code null} for supervisorId to get every initiative (admin view). */
    List<Initiative> findManagedInitiatives(Long supervisorId);

    List<VolunteerInitiative> findRequestsForInitiative(Long initiativeId);

    VolunteerInitiative findById(Long id);

    VolunteerInitiative approve(Long id);

    VolunteerInitiative reject(Long id);
}
