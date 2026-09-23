package com.volunteerportal.app.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.volunteerportal.app.model.Event;

public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByInitiativeId(Long initiativeId);

    List<Event> findByInitiativeIdAndEnabledTrueOrderByFromDttmAsc(Long initiativeId);

    Optional<Event> findFirstByInitiativeIdAndName(Long initiativeId, String name);

    /**
     * Enabled events of initiatives the user is an approved member of that haven't finished yet:
     * still running (ends after now), or with no end and starting today or later. Soonest first.
     */
    @EntityGraph(attributePaths = "initiative")
    @Query("""
            select e from Event e join e.initiative i, VolunteerInitiative vi
            where vi.initiative = i and vi.user.id = :userId and vi.enabled = true and e.enabled = true
              and (e.toDttm >= :now or (e.toDttm is null and (e.fromDttm is null or e.fromDttm >= :startOfToday)))
            order by e.fromDttm""")
    List<Event> findUpcomingForMember(@Param("userId") Long userId, @Param("now") LocalDateTime now,
            @Param("startOfToday") LocalDateTime startOfToday);

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
