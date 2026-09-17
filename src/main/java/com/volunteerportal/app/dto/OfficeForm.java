package com.volunteerportal.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OfficeForm {

    @NotBlank
    @Size(max = 255)
    private String name;

    @Size(max = 500)
    private String description;

    private Long roleId;

    private Long userId;
}
