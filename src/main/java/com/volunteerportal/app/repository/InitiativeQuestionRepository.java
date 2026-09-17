package com.volunteerportal.app.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.volunteerportal.app.model.InitiativeQuestion;

public interface InitiativeQuestionRepository extends JpaRepository<InitiativeQuestion, Long> {

    List<InitiativeQuestion> findByInitiativeId(Long initiativeId);

    long countByInitiativeId(Long initiativeId);
}
