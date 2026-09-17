package com.volunteerportal.app.service;

import com.volunteerportal.app.dto.ProfileForm;
import com.volunteerportal.app.model.User;
import com.volunteerportal.app.model.VolunteerProfile;

public interface ProfileService {

    VolunteerProfile findOrCreate(User user);

    VolunteerProfile updateProfile(User user, ProfileForm form);
}
