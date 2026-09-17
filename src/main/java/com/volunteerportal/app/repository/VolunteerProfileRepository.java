package com.volunteerportal.app.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.volunteerportal.app.model.VolunteerProfile;

public interface VolunteerProfileRepository extends JpaRepository<VolunteerProfile, Long> {

    Optional<VolunteerProfile> findByUserId(Long userId);
}
