package com.volunteerportal.app.service;

import java.util.List;
import java.util.Optional;

import com.volunteerportal.app.dto.VolunteerGradeForm;
import com.volunteerportal.app.model.User;
import com.volunteerportal.app.model.VolunteerProfile;

public interface VolunteerAdminService {

    List<User> findVolunteers();

    User findUserById(Long userId);

    Optional<VolunteerProfile> findProfileForUser(Long userId);

    VolunteerProfile updateGradeAndPoints(Long userId, VolunteerGradeForm form);
}
