package com.volunteerportal.app.service;

import java.util.List;

import com.volunteerportal.app.dto.QuestionLibForm;
import com.volunteerportal.app.model.QuestionLib;

public interface QuestionLibService {

    List<QuestionLib> findAllForCategory(Long categoryId);

    QuestionLib findById(Long id);

    QuestionLib create(Long categoryId, QuestionLibForm form);

    QuestionLib update(Long id, QuestionLibForm form);

    void delete(Long id);
}
