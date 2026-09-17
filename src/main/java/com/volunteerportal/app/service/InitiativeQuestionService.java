package com.volunteerportal.app.service;

import java.util.List;

import com.volunteerportal.app.dto.InitiativeQuestionForm;
import com.volunteerportal.app.model.InitiativeQuestion;

public interface InitiativeQuestionService {

    List<InitiativeQuestion> findAllForInitiative(Long initiativeId);

    InitiativeQuestion findById(Long id);

    InitiativeQuestion create(Long initiativeId, InitiativeQuestionForm form);

    InitiativeQuestion update(Long id, InitiativeQuestionForm form);

    void delete(Long id);
}
