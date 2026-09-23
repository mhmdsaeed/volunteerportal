package com.volunteerportal.app.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.volunteerportal.app.model.Event;

public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByInitiativeId(Long initiativeId);

    List<Event> findByInitiativeIdAndEnabledTrueOrderByFromDttmAsc(Long initiativeId);

    Optional<Event> findFirstByInitiativeIdAndName(Long initiativeId, String name);

    /**
     * Overridden to eagerly fetch initiative: the attendance report renders its name after the
     * request's Hibernate session has closed (open-in-view is disabled), which would otherwise
     * throw LazyInitializationException.
     */
    @Override
    @EntityGraph(attributePaths = "initiative")
    List<Event> findAll();

    /** Overridden for the same reason: the check-in pages render the event's initiative name. */
    @Override
    @EntityGraph(attributePaths = "initiative")
    Optional<Event> findById(Long id);
}
