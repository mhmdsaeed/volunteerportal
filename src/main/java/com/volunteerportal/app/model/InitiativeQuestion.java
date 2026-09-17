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
@Table(name = "initiative_question")
@Getter
@Setter
@NoArgsConstructor
public class InitiativeQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "question_text")
    private String questionText;

    @Column(name = "question_type_id")
    private Integer questionTypeId;

    @Column(name = "question_choices_count")
    private Integer questionChoicesCount;

    @Column(name = "question_choices_text")
    private String questionChoicesText;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initiative_id")
    private Initiative initiative;
}
