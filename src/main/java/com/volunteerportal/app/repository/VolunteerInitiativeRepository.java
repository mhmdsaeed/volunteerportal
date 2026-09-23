package com.volunteerportal.app.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.volunteerportal.app.model.VolunteerInitiative;

public interface VolunteerInitiativeRepository extends JpaRepository<VolunteerInitiative, Long> {

    /**
     * user is a lazy association that the coordinator's join-requests view renders the
     * username of after the request's Hibernate session has closed (open-in-view is
     * disabled), which would otherwise throw LazyInitializationException.
     */
    @EntityGraph(attributePaths = "user")
    List<VolunteerInitiative> findByInitiativeId(Long initiativeId);

    /** Approved members only: the volunteers whose attendance can be recorded for the initiative's events. */
    @EntityGraph(attributePaths = "user")
    List<VolunteerInitiative> findByInitiativeIdAndEnabledTrue(Long initiativeId);

    /**
     * Overridden so the coordinator controller can check who manages the request's initiative
     * (supervisor / office coordinator) after the Hibernate session has closed.
     */
    @Override
    @EntityGraph(attributePaths = {"user", "initiative", "initiative.office", "initiative.supervisor"})
    Optional<VolunteerInitiative> findById(Long id);

    List<VolunteerInitiative> findByUserId(Long userId);

    Optional<VolunteerInitiative> findByUserIdAndInitiativeId(Long userId, Long initiativeId);

    long countByInitiativeId(Long initiativeId);

    long countByInitiativeIdAndEnabledTrue(Long initiativeId);

    long countByInitiativeIdAndEnabledFalse(Long initiativeId);

    long countByInitiativeIdAndEnabledIsNull(Long initiativeId);

    /** Every request still awaiting a decision, oldest first (admin view). */
    @EntityGraph(attributePaths = {"user", "initiative"})
    List<VolunteerInitiative> findByResponseJoinDttmIsNullOrderByRequestJoinDttmAsc();

    long countByResponseJoinDttmIsNull();

    /** Requests awaiting a decision on initiatives the user supervises or coordinates through the office. */
    @EntityGraph(attributePaths = {"user", "initiative"})
    @Query("""
            select vi from VolunteerInitiative vi join vi.initiative i left join i.supervisor s left join i.office o
            where vi.responseJoinDttm is null and (s.id = :userId or o.user.id = :userId)
            order by vi.requestJoinDttm""")
    List<VolunteerInitiative> findPendingManagedBy(@Param("userId") Long userId);

    @Query("""
            select count(vi) from VolunteerInitiative vi join vi.initiative i left join i.supervisor s left join i.office o
            where vi.responseJoinDttm is null and (s.id = :userId or o.user.id = :userId)""")
    long countPendingManagedBy(@Param("userId") Long userId);
}
