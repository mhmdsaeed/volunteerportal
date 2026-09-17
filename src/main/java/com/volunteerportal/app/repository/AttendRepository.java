package com.volunteerportal.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.volunteerportal.app.model.Attend;

public interface AttendRepository extends JpaRepository<Attend, Long> {
}
