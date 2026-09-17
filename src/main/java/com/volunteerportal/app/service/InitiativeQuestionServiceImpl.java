package com.volunteerportal.app.service;

import java.util.List;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.volunteerportal.app.dto.InitiativeQuestionForm;
import com.volunteerportal.app.model.Initiative;
import com.volunteerportal.app.model.InitiativeQuestion;
import com.volunteerportal.app.repository.InitiativeQuestionRepository;
import com.volunteerportal.app.repository.InitiativeRepository;

@Service
public class InitiativeQuestionServiceImpl implements InitiativeQuestionService {

    private final InitiativeQuestionRepository initiativeQuestionRepository;
    private final InitiativeRepository initiativeRepository;

    public InitiativeQuestionServiceImpl(InitiativeQuestionRepository initiativeQuestionRepository,
            InitiativeRepository initiativeRepository) {
        this.initiativeQuestionRepository = initiativeQuestionRepository;
        this.initiativeRepository = initiativeRepository;
    }

    @Override
    public List<InitiativeQuestion> findAllForInitiative(Long initiativeId) {
        return initiativeQuestionRepository.findByInitiativeId(initiativeId);
    }

    @Override
    public InitiativeQuestion findById(Long id) {
        return initiativeQuestionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Initiative question not found: " + id));
    }

    @Override
    @Transactional
    public InitiativeQuestion create(Long initiativeId, InitiativeQuestionForm form) {
        Initiative initiative = initiativeRepository.findById(initiativeId)
                .orElseThrow(() -> new EntityNotFoundException("Initiative not found: " + initiativeId));

        InitiativeQuestion question = new InitiativeQuestion();
        question.setInitiative(initiative);
        applyForm(question, form);
        InitiativeQuestion saved = initiativeQuestionRepository.save(question);

        syncQuestionCount(initiative);
        return saved;
    }

    @Override
    @Transactional
    public InitiativeQuestion update(Long id, InitiativeQuestionForm form) {
        InitiativeQuestion question = findById(id);
        applyForm(question, form);
        return initiativeQuestionRepository.save(question);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        InitiativeQuestion question = findById(id);
        Initiative initiative = question.getInitiative();
        initiativeQuestionRepository.deleteById(id);
        syncQuestionCount(initiative);
    }

    private void applyForm(InitiativeQuestion question, InitiativeQuestionForm form) {
        question.setQuestionText(form.getQuestionText());
        question.setQuestionTypeId(form.getQuestionTypeId());
        question.setQuestionChoicesCount(form.getQuestionChoicesCount());
        question.setQuestionChoicesText(form.getQuestionChoicesText());
    }

    private void syncQuestionCount(Initiative initiative) {
        if (initiative == null) {
            return;
        }
        long count = initiativeQuestionRepository.countByInitiativeId(initiative.getId());
        initiative.setQuestionCount((int) count);
        initiativeRepository.save(initiative);
    }
}
