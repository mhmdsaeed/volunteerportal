package com.volunteerportal.app.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.volunteerportal.app.model.Attend;

public interface AttendRepository extends JpaRepository<Attend, Long> {

    /**
     * volunteerInitiative.user is a lazy association chain that the attendance list view
     * renders the username of after the request's Hibernate session has closed (open-in-view
     * is disabled), which would otherwise throw LazyInitializationException.
     */
    @EntityGraph(attributePaths = {"volunteerInitiative", "volunteerInitiative.user"})
    List<Attend> findByEventId(Long eventId);

    long countByEventIdAndAttendInOut(Long eventId, Integer attendInOut);
}
