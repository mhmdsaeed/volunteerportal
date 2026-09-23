package com.volunteerportal.app.service;

import java.util.List;

import com.volunteerportal.app.model.Initiative;
import com.volunteerportal.app.model.VolunteerInitiative;

public interface JoinRequestService {

    /**
     * Initiatives the user supervises or coordinates through the initiative's office.
     * Pass {@code null} for managerId to get every initiative (admin view).
     */
    List<Initiative> findManagedInitiatives(Long managerId);

    /** Whether the user is the initiative's supervisor or its office's coordinator (admins are checked separately). */
    boolean canManage(Initiative initiative, Long userId);

    List<VolunteerInitiative> findRequestsForInitiative(Long initiativeId);

    /** Requests awaiting a decision, oldest first. Pass {@code null} for managerId to get all of them (admin view). */
    List<VolunteerInitiative> findPendingRequests(Long managerId);

    /** Same scoping as {@link #findPendingRequests(Long)}. */
    long countPendingRequests(Long managerId);

    VolunteerInitiative findById(Long id);

    /** @throws IllegalStateException if the request has already been approved or rejected */
    VolunteerInitiative approve(Long id);

    /** @throws IllegalStateException if the request has already been approved or rejected */
    VolunteerInitiative reject(Long id);
}
