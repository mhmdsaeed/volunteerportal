package com.volunteerportal.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.volunteerportal.app.model.InitiativeQuestion;

public interface InitiativeQuestionRepository extends JpaRepository<InitiativeQuestion, Long> {
}
