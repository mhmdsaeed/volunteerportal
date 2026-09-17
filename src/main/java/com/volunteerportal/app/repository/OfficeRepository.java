package com.volunteerportal.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.volunteerportal.app.model.Office;

public interface OfficeRepository extends JpaRepository<Office, Long> {
}
