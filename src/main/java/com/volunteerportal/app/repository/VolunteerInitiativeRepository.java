package com.volunteerportal.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.volunteerportal.app.model.VolunteerInitiative;

public interface VolunteerInitiativeRepository extends JpaRepository<VolunteerInitiative, Long> {
}
