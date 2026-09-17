package com.volunteerportal.app.model;

import java.time.LocalDateTime;

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
@Table(name = "volunteer_initiative_answer")
@Getter
@Setter
@NoArgsConstructor
public class VolunteerInitiativeAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "answer_text")
    private String answerText;

    @Column(name = "answer_dttm")
    private LocalDateTime answerDttm;

    @Column(name = "answer_choice_number")
    private Integer answerChoiceNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "volunteer_initiative_id")
    private VolunteerInitiative volunteerInitiative;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initiative_question_id")
    private InitiativeQuestion initiativeQuestion;
}
