package com.volunteerportal.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuestionLibForm {

    @NotBlank
    @Size(max = 255)
    private String questionText;

    /** 1: true_false, 2: one_of_n, 3: multi_of_n, 4: free_text */
    @NotNull
    private Integer questionType;

    private Integer questionChoicesCount;

    @Size(max = 255)
    private String questionChoices;
}
