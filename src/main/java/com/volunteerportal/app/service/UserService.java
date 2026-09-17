package com.volunteerportal.app.service;

import com.volunteerportal.app.dto.RegistrationForm;
import com.volunteerportal.app.model.User;

public interface UserService {

    User registerNewUser(RegistrationForm form);

    boolean usernameExists(String username);

    boolean emailExists(String email);
}
