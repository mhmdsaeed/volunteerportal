package com.volunteerportal.app.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AttendForm {

    @NotNull
    private Long volunteerInitiativeId;

    /** 1: check-in, 2: check-out */
    @NotNull
    private Integer attendInOut;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime attendDttm;

    @Size(max = 255)
    private String note;
}
