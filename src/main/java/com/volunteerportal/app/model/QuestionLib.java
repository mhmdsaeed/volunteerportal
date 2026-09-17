package com.volunteerportal.app.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "question_lib")
@Getter
@Setter
@NoArgsConstructor
public class QuestionLib {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "question_text")
    private String questionText;

    /** 1: true_false, 2: one_of_n, 3: multi_of_n, 4: free_text */
    @Column(name = "question_type")
    private Integer questionType;

    @Column(name = "question_choices_count")
    private Integer questionChoicesCount;

    @Column(name = "question_choices")
    private String questionChoices;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_lib_cat_id")
    private QuestionLibCat questionLibCat;
}
