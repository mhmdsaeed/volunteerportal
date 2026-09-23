package com.volunteerportal.app.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.util.MultiValueMap;

import com.volunteerportal.app.model.Event;
import com.volunteerportal.app.model.Initiative;
import com.volunteerportal.app.model.InitiativeQuestion;
import com.volunteerportal.app.model.User;
import com.volunteerportal.app.model.VolunteerInitiative;

public interface VolunteerInitiativeService {

    List<Initiative> findAvailableInitiatives();

    Initiative findInitiativeDetail(Long initiativeId);

    List<InitiativeQuestion> findQuestions(Long initiativeId);

    Optional<VolunteerInitiative> findMembership(Long userId, Long initiativeId);

    Map<Long, VolunteerInitiative> findMembershipsForUser(Long userId);

    /** Enabled events of the initiative, soonest first - shown to approved members. */
    List<Event> findOpenEvents(Long initiativeId);

    VolunteerInitiative join(Long initiativeId, User user, MultiValueMap<String, String> answers);

    /** Only allowed while the request is still pending (no coordinator response yet). */
    void withdraw(Long initiativeId, Long userId);
}
