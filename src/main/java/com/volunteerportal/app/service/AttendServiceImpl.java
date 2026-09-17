package com.volunteerportal.app.service;

import java.util.List;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.volunteerportal.app.dto.AttendForm;
import com.volunteerportal.app.model.Attend;
import com.volunteerportal.app.model.Event;
import com.volunteerportal.app.model.VolunteerInitiative;
import com.volunteerportal.app.repository.AttendRepository;
import com.volunteerportal.app.repository.EventRepository;
import com.volunteerportal.app.repository.VolunteerInitiativeRepository;

@Service
public class AttendServiceImpl implements AttendService {

    private final AttendRepository attendRepository;
    private final EventRepository eventRepository;
    private final VolunteerInitiativeRepository volunteerInitiativeRepository;

    public AttendServiceImpl(AttendRepository attendRepository, EventRepository eventRepository,
            VolunteerInitiativeRepository volunteerInitiativeRepository) {
        this.attendRepository = attendRepository;
        this.eventRepository = eventRepository;
        this.volunteerInitiativeRepository = volunteerInitiativeRepository;
    }

    @Override
    public List<Attend> findAllForEvent(Long eventId) {
        return attendRepository.findByEventId(eventId);
    }

    @Override
    public Attend findById(Long id) {
        return attendRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Attendance record not found: " + id));
    }

    @Override
    @Transactional
    public Attend create(Long eventId, AttendForm form) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found: " + eventId));

        Attend attend = new Attend();
        attend.setEvent(event);
        applyForm(attend, form);
        return attendRepository.save(attend);
    }

    @Override
    @Transactional
    public Attend update(Long id, AttendForm form) {
        Attend attend = findById(id);
        applyForm(attend, form);
        return attendRepository.save(attend);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        attendRepository.deleteById(id);
    }

    private void applyForm(Attend attend, AttendForm form) {
        VolunteerInitiative volunteerInitiative = volunteerInitiativeRepository.findById(form.getVolunteerInitiativeId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Volunteer initiative not found: " + form.getVolunteerInitiativeId()));

        attend.setVolunteerInitiative(volunteerInitiative);
        attend.setAttendInOut(form.getAttendInOut());
        attend.setAttendDttm(form.getAttendDttm());
        attend.setNote(form.getNote());
    }
}
