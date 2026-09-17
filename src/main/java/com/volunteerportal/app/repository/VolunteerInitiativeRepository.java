package com.volunteerportal.app.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.volunteerportal.app.model.VolunteerInitiative;

public interface VolunteerInitiativeRepository extends JpaRepository<VolunteerInitiative, Long> {

    /**
     * user is a lazy association that the coordinator's join-requests view renders the
     * username of after the request's Hibernate session has closed (open-in-view is
     * disabled), which would otherwise throw LazyInitializationException.
     */
    @EntityGraph(attributePaths = "user")
    List<VolunteerInitiative> findByInitiativeId(Long initiativeId);

    List<VolunteerInitiative> findByUserId(Long userId);

    Optional<VolunteerInitiative> findByUserIdAndInitiativeId(Long userId, Long initiativeId);
}
