package com.volunteerportal.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.volunteerportal.app.model.QuestionLibCat;

public interface QuestionLibCatRepository extends JpaRepository<QuestionLibCat, Long> {
}
