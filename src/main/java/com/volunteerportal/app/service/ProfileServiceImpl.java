package com.volunteerportal.app.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.volunteerportal.app.dto.ProfileForm;
import com.volunteerportal.app.model.User;
import com.volunteerportal.app.model.VolunteerProfile;
import com.volunteerportal.app.repository.VolunteerProfileRepository;

@Service
public class ProfileServiceImpl implements ProfileService {

    private final VolunteerProfileRepository volunteerProfileRepository;

    public ProfileServiceImpl(VolunteerProfileRepository volunteerProfileRepository) {
        this.volunteerProfileRepository = volunteerProfileRepository;
    }

    @Override
    @Transactional
    public VolunteerProfile findOrCreate(User user) {
        return volunteerProfileRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    VolunteerProfile profile = new VolunteerProfile();
                    profile.setUser(user);
                    profile.setPoints(0L);
                    return volunteerProfileRepository.save(profile);
                });
    }

    @Override
    @Transactional
    public VolunteerProfile updateProfile(User user, ProfileForm form) {
        VolunteerProfile profile = findOrCreate(user);
        profile.setFirstName(form.getFirstName());
        profile.setLastName(form.getLastName());
        profile.setMobile(form.getMobile());
        profile.setCity(form.getCity());
        profile.setAddress(form.getAddress());
        return volunteerProfileRepository.save(profile);
    }
}
