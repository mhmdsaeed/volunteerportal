package com.volunteerportal.app.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EventForm {

    @NotBlank
    @Size(max = 255)
    private String name;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime fromDttm;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime toDttm;

    private Double locLongitude;

    private Double locLatitude;

    @Size(max = 255)
    private String locUrl;

    private boolean enabled = true;
}
