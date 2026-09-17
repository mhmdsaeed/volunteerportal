package com.volunteerportal.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.volunteerportal.app.model.Event;

public interface EventRepository extends JpaRepository<Event, Long> {
}
