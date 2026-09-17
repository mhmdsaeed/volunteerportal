package com.volunteerportal.app.service;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.volunteerportal.app.dto.VolunteerGradeForm;
import com.volunteerportal.app.model.Grade;
import com.volunteerportal.app.model.User;
import com.volunteerportal.app.model.VolunteerProfile;
import com.volunteerportal.app.repository.GradeRepository;
import com.volunteerportal.app.repository.UserRepository;
import com.volunteerportal.app.repository.VolunteerProfileRepository;

@Service
public class VolunteerAdminServiceImpl implements VolunteerAdminService {

    private static final String VOLUNTEER_ROLE = "VOLUNTEER";

    private final UserRepository userRepository;
    private final VolunteerProfileRepository volunteerProfileRepository;
    private final GradeRepository gradeRepository;
    private final ProfileService profileService;

    public VolunteerAdminServiceImpl(UserRepository userRepository,
            VolunteerProfileRepository volunteerProfileRepository, GradeRepository gradeRepository,
            ProfileService profileService) {
        this.userRepository = userRepository;
        this.volunteerProfileRepository = volunteerProfileRepository;
        this.gradeRepository = gradeRepository;
        this.profileService = profileService;
    }

    @Override
    public List<User> findVolunteers() {
        return userRepository.findByRoles_Name(VOLUNTEER_ROLE);
    }

    @Override
    public User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
    }

    @Override
    public Optional<VolunteerProfile> findProfileForUser(Long userId) {
        return volunteerProfileRepository.findByUserId(userId);
    }

    @Override
    @Transactional
    public VolunteerProfile updateGradeAndPoints(Long userId, VolunteerGradeForm form) {
        User user = findUserById(userId);
        VolunteerProfile profile = profileService.findOrCreate(user);

        if (form.getGradeId() != null) {
            Grade grade = gradeRepository.findById(form.getGradeId())
                    .orElseThrow(() -> new EntityNotFoundException("Grade not found: " + form.getGradeId()));
            profile.setGrade(grade);
        } else {
            profile.setGrade(null);
        }
        profile.setPoints(form.getPoints());

        return volunteerProfileRepository.save(profile);
    }
}
