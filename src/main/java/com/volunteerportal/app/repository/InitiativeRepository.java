package com.volunteerportal.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.volunteerportal.app.model.Initiative;

public interface InitiativeRepository extends JpaRepository<Initiative, Long> {
}
