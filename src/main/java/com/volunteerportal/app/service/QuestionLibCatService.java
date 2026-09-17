package com.volunteerportal.app.service;

import java.util.List;

import com.volunteerportal.app.dto.QuestionLibCatForm;
import com.volunteerportal.app.model.QuestionLibCat;

public interface QuestionLibCatService {

    List<QuestionLibCat> findAll();

    QuestionLibCat findById(Long id);

    QuestionLibCat create(QuestionLibCatForm form);

    QuestionLibCat update(Long id, QuestionLibCatForm form);

    void delete(Long id);
}
