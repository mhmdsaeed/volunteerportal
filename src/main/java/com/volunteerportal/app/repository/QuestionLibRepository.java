package com.volunteerportal.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.volunteerportal.app.model.QuestionLib;

public interface QuestionLibRepository extends JpaRepository<QuestionLib, Long> {
}
