package com.volunteerportal.app.service;

import java.util.List;

import com.volunteerportal.app.dto.AttendForm;
import com.volunteerportal.app.model.Attend;

public interface AttendService {

    List<Attend> findAllForEvent(Long eventId);

    Attend findById(Long id);

    Attend create(Long eventId, AttendForm form);

    Attend update(Long id, AttendForm form);

    void delete(Long id);
}
