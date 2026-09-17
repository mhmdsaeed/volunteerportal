package com.volunteerportal.app.service;

import java.util.List;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.volunteerportal.app.dto.GradeForm;
import com.volunteerportal.app.model.Grade;
import com.volunteerportal.app.repository.GradeRepository;

@Service
public class GradeServiceImpl implements GradeService {

    private final GradeRepository gradeRepository;

    public GradeServiceImpl(GradeRepository gradeRepository) {
        this.gradeRepository = gradeRepository;
    }

    @Override
    public List<Grade> findAll() {
        return gradeRepository.findAll();
    }

    @Override
    public Grade findById(Long id) {
        return gradeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Grade not found: " + id));
    }

    @Override
    @Transactional
    public Grade create(GradeForm form) {
        Grade grade = new Grade();
        grade.setName(form.getName());
        return gradeRepository.save(grade);
    }

    @Override
    @Transactional
    public Grade update(Long id, GradeForm form) {
        Grade grade = findById(id);
        grade.setName(form.getName());
        return gradeRepository.save(grade);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        gradeRepository.deleteById(id);
    }
}
