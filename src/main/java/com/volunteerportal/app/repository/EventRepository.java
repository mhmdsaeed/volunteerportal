package com.volunteerportal.app.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.volunteerportal.app.model.Event;

public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByInitiativeId(Long initiativeId);
}
