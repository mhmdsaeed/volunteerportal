package com.volunteerportal.app.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.volunteerportal.app.model.Initiative;

public interface InitiativeRepository extends JpaRepository<Initiative, Long> {

    List<Initiative> findByEnabledTrue();

    List<Initiative> findBySupervisorId(Long supervisorId);
}
