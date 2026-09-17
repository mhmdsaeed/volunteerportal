package com.volunteerportal.app.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.volunteerportal.app.model.VolunteerProfile;

public interface VolunteerProfileRepository extends JpaRepository<VolunteerProfile, Long> {

    /**
     * grade is a lazy association that the profile/volunteer-admin views render the name of
     * after the request's Hibernate session has closed (open-in-view is disabled), which would
     * otherwise throw LazyInitializationException.
     */
    @EntityGraph(attributePaths = "grade")
    Optional<VolunteerProfile> findByUserId(Long userId);
}
