package com.volunteerportal.app.service;

import java.util.List;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.volunteerportal.app.dto.EventForm;
import com.volunteerportal.app.model.Event;
import com.volunteerportal.app.model.Initiative;
import com.volunteerportal.app.repository.EventRepository;
import com.volunteerportal.app.repository.InitiativeRepository;

@Service
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final InitiativeRepository initiativeRepository;

    public EventServiceImpl(EventRepository eventRepository, InitiativeRepository initiativeRepository) {
        this.eventRepository = eventRepository;
        this.initiativeRepository = initiativeRepository;
    }

    @Override
    public List<Event> findAllForInitiative(Long initiativeId) {
        return eventRepository.findByInitiativeId(initiativeId);
    }

    @Override
    public Event findById(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Event not found: " + id));
    }

    @Override
    @Transactional
    public Event create(Long initiativeId, EventForm form) {
        Initiative initiative = initiativeRepository.findById(initiativeId)
                .orElseThrow(() -> new EntityNotFoundException("Initiative not found: " + initiativeId));

        Event event = new Event();
        event.setInitiative(initiative);
        applyForm(event, form);
        return eventRepository.save(event);
    }

    @Override
    @Transactional
    public Event update(Long id, EventForm form) {
        Event event = findById(id);
        applyForm(event, form);
        return eventRepository.save(event);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        eventRepository.deleteById(id);
    }

    private void applyForm(Event event, EventForm form) {
        event.setName(form.getName());
        event.setFromDttm(form.getFromDttm());
        event.setToDttm(form.getToDttm());
        event.setLocLongitude(form.getLocLongitude());
        event.setLocLatitude(form.getLocLatitude());
        event.setLocUrl(form.getLocUrl());
        event.setEnabled(form.isEnabled());
    }
}
