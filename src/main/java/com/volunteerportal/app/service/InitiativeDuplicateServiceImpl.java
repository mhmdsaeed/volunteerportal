package com.volunteerportal.app.service;

import java.util.List;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.volunteerportal.app.model.Initiative;
import com.volunteerportal.app.model.InitiativeQuestion;
import com.volunteerportal.app.repository.InitiativeQuestionRepository;
import com.volunteerportal.app.repository.InitiativeRepository;

@Service
public class InitiativeDuplicateServiceImpl implements InitiativeDuplicateService {

    private final InitiativeRepository initiativeRepository;
    private final InitiativeQuestionRepository initiativeQuestionRepository;

    public InitiativeDuplicateServiceImpl(InitiativeRepository initiativeRepository,
            InitiativeQuestionRepository initiativeQuestionRepository) {
        this.initiativeRepository = initiativeRepository;
        this.initiativeQuestionRepository = initiativeQuestionRepository;
    }

    @Override
    @Transactional
    public Initiative duplicate(Long initiativeId) {
        Initiative source = initiativeRepository.findById(initiativeId)
                .orElseThrow(() -> new EntityNotFoundException("Initiative not found: " + initiativeId));

        Initiative copy = new Initiative();
        copy.setName(source.getName() + " (Copy)");
        copy.setDescription(source.getDescription());
        copy.setOffice(source.getOffice());
        copy.setSupervisor(source.getSupervisor());
        copy.setEnabled(false);
        copy = initiativeRepository.save(copy);

        List<InitiativeQuestion> sourceQuestions = initiativeQuestionRepository.findByInitiativeId(initiativeId);
        for (InitiativeQuestion sourceQuestion : sourceQuestions) {
            InitiativeQuestion questionCopy = new InitiativeQuestion();
            questionCopy.setInitiative(copy);
            questionCopy.setQuestionText(sourceQuestion.getQuestionText());
            questionCopy.setQuestionTypeId(sourceQuestion.getQuestionTypeId());
            questionCopy.setQuestionChoicesCount(sourceQuestion.getQuestionChoicesCount());
            questionCopy.setQuestionChoicesText(sourceQuestion.getQuestionChoicesText());
            initiativeQuestionRepository.save(questionCopy);
        }

        copy.setQuestionCount(sourceQuestions.size());
        return initiativeRepository.save(copy);
    }
}
