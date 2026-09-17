package com.volunteerportal.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConfigSetForm {

    @NotBlank
    @Size(max = 255)
    private String configsetKey;

    @NotBlank
    @Size(max = 255)
    private String configsetValue;
}
