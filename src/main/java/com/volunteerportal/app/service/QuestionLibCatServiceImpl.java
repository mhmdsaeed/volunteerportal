package com.volunteerportal.app.service;

import java.util.List;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.volunteerportal.app.dto.QuestionLibCatForm;
import com.volunteerportal.app.model.QuestionLibCat;
import com.volunteerportal.app.repository.QuestionLibCatRepository;

@Service
public class QuestionLibCatServiceImpl implements QuestionLibCatService {

    private final QuestionLibCatRepository questionLibCatRepository;

    public QuestionLibCatServiceImpl(QuestionLibCatRepository questionLibCatRepository) {
        this.questionLibCatRepository = questionLibCatRepository;
    }

    @Override
    public List<QuestionLibCat> findAll() {
        return questionLibCatRepository.findAll();
    }

    @Override
    public QuestionLibCat findById(Long id) {
        return questionLibCatRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Question library category not found: " + id));
    }

    @Override
    @Transactional
    public QuestionLibCat create(QuestionLibCatForm form) {
        QuestionLibCat cat = new QuestionLibCat();
        applyForm(cat, form);
        return questionLibCatRepository.save(cat);
    }

    @Override
    @Transactional
    public QuestionLibCat update(Long id, QuestionLibCatForm form) {
        QuestionLibCat cat = findById(id);
        applyForm(cat, form);
        return questionLibCatRepository.save(cat);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        questionLibCatRepository.deleteById(id);
    }

    private void applyForm(QuestionLibCat cat, QuestionLibCatForm form) {
        cat.setName(form.getName());
        cat.setDescription(form.getDescription());
    }
}
