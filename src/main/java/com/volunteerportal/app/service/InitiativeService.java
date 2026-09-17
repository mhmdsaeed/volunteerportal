package com.volunteerportal.app.service;

import java.util.List;

import com.volunteerportal.app.dto.InitiativeForm;
import com.volunteerportal.app.model.Initiative;

public interface InitiativeService {

    List<Initiative> findAll();

    Initiative findById(Long id);

    Initiative create(InitiativeForm form);

    Initiative update(Long id, InitiativeForm form);

    void delete(Long id);
}
