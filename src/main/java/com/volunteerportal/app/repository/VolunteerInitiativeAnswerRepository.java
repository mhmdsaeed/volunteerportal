package com.volunteerportal.app.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.volunteerportal.app.model.VolunteerInitiativeAnswer;

public interface VolunteerInitiativeAnswerRepository extends JpaRepository<VolunteerInitiativeAnswer, Long> {

    void deleteByVolunteerInitiativeId(Long volunteerInitiativeId);

    List<VolunteerInitiativeAnswer> findByVolunteerInitiativeId(Long volunteerInitiativeId);
}
