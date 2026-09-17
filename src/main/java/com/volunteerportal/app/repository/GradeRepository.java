package com.volunteerportal.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.volunteerportal.app.model.Grade;

public interface GradeRepository extends JpaRepository<Grade, Long> {
}
