package com.volunteerportal.app.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfileForm {

    @Size(max = 255)
    private String firstName;

    @Size(max = 255)
    private String lastName;

    @Size(max = 255)
    private String mobile;

    @Size(max = 255)
    private String city;

    @Size(max = 255)
    private String address;
}
