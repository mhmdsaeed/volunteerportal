package com.volunteerportal.app.service;

import java.util.List;

import com.volunteerportal.app.dto.GradeForm;
import com.volunteerportal.app.model.Grade;

public interface GradeService {

    List<Grade> findAll();

    Grade findById(Long id);

    Grade create(GradeForm form);

    Grade update(Long id, GradeForm form);

    void delete(Long id);
}
