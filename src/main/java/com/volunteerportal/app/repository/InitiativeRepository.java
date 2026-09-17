package com.volunteerportal.app.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.volunteerportal.app.model.Initiative;

public interface InitiativeRepository extends JpaRepository<Initiative, Long> {

    List<Initiative> findByEnabledTrue();

    /**
     * office/supervisor are lazy associations that list and detail views render the
     * name/username of after the request's Hibernate session has closed (open-in-view is
     * disabled), which would otherwise throw LazyInitializationException.
     */
    @Override
    @EntityGraph(attributePaths = {"office", "supervisor"})
    List<Initiative> findAll();

    @Override
    @EntityGraph(attributePaths = {"office", "supervisor"})
    Optional<Initiative> findById(Long id);

    @EntityGraph(attributePaths = {"office", "supervisor"})
    List<Initiative> findBySupervisorId(Long supervisorId);
}
