package com.volunteerportal.app.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /** Initiatives the user manages: as the initiative's supervisor or as its office's coordinator. */
    @EntityGraph(attributePaths = {"office", "supervisor"})
    @Query("""
            select i from Initiative i left join i.supervisor s left join i.office o
            where s.id = :userId or o.user.id = :userId
            order by i.name""")
    List<Initiative> findManagedBy(@Param("userId") Long userId);
}
