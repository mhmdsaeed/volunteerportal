package com.volunteerportal.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.volunteerportal.app.model.VolunteerInitiativeAnswer;

public interface VolunteerInitiativeAnswerRepository extends JpaRepository<VolunteerInitiativeAnswer, Long> {
}
