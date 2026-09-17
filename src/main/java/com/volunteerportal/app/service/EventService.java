package com.volunteerportal.app.service;

import java.util.List;

import com.volunteerportal.app.dto.EventForm;
import com.volunteerportal.app.model.Event;

public interface EventService {

    List<Event> findAllForInitiative(Long initiativeId);

    Event findById(Long id);

    Event create(Long initiativeId, EventForm form);

    Event update(Long id, EventForm form);

    void delete(Long id);
}
