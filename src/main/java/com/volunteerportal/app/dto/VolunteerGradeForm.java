package com.volunteerportal.app.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VolunteerGradeForm {

    private Long gradeId;

    @NotNull
    @Min(0)
    private Long points;
}
