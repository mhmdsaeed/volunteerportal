package com.volunteerportal.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InitiativeForm {

    @NotBlank
    @Size(max = 255)
    private String name;

    @Size(max = 500)
    private String description;

    private Long officeId;

    private Long supervisorId;

    private boolean enabled = true;
}
