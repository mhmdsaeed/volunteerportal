package com.volunteerportal.app.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.volunteerportal.app.model.QuestionLib;

public interface QuestionLibRepository extends JpaRepository<QuestionLib, Long> {

    List<QuestionLib> findByQuestionLibCatId(Long questionLibCatId);
}
