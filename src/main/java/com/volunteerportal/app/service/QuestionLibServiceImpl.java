package com.volunteerportal.app.service;

import java.util.List;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.volunteerportal.app.dto.QuestionLibForm;
import com.volunteerportal.app.model.QuestionLib;
import com.volunteerportal.app.model.QuestionLibCat;
import com.volunteerportal.app.repository.QuestionLibCatRepository;
import com.volunteerportal.app.repository.QuestionLibRepository;

@Service
public class QuestionLibServiceImpl implements QuestionLibService {

    private final QuestionLibRepository questionLibRepository;
    private final QuestionLibCatRepository questionLibCatRepository;

    public QuestionLibServiceImpl(QuestionLibRepository questionLibRepository,
            QuestionLibCatRepository questionLibCatRepository) {
        this.questionLibRepository = questionLibRepository;
        this.questionLibCatRepository = questionLibCatRepository;
    }

    @Override
    public List<QuestionLib> findAllForCategory(Long categoryId) {
        return questionLibRepository.findByQuestionLibCatId(categoryId);
    }

    @Override
    public QuestionLib findById(Long id) {
        return questionLibRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Question library entry not found: " + id));
    }

    @Override
    @Transactional
    public QuestionLib create(Long categoryId, QuestionLibForm form) {
        QuestionLibCat category = questionLibCatRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Question library category not found: " + categoryId));

        QuestionLib question = new QuestionLib();
        question.setQuestionLibCat(category);
        applyForm(question, form);
        return questionLibRepository.save(question);
    }

    @Override
    @Transactional
    public QuestionLib update(Long id, QuestionLibForm form) {
        QuestionLib question = findById(id);
        applyForm(question, form);
        return questionLibRepository.save(question);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        questionLibRepository.deleteById(id);
    }

    private void applyForm(QuestionLib question, QuestionLibForm form) {
        question.setQuestionText(form.getQuestionText());
        question.setQuestionType(form.getQuestionType());
        question.setQuestionChoicesCount(form.getQuestionChoicesCount());
        question.setQuestionChoices(form.getQuestionChoices());
    }
}
