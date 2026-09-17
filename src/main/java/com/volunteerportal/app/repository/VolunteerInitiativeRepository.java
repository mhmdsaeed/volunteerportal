package com.volunteerportal.app.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.volunteerportal.app.model.VolunteerInitiative;

public interface VolunteerInitiativeRepository extends JpaRepository<VolunteerInitiative, Long> {

    List<VolunteerInitiative> findByInitiativeId(Long initiativeId);

    List<VolunteerInitiative> findByUserId(Long userId);

    Optional<VolunteerInitiative> findByUserIdAndInitiativeId(Long userId, Long initiativeId);
}
