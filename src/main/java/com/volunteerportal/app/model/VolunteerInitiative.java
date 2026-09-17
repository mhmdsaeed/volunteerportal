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
@Table(name = "volunteer_initiative")
@Getter
@Setter
@NoArgsConstructor
public class VolunteerInitiative {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_join_dttm")
    private LocalDateTime requestJoinDttm;

    @Column(name = "response_join_dttm")
    private LocalDateTime responseJoinDttm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initiative_id")
    private Initiative initiative;

    @Column(name = "answer_count")
    private Integer answerCount;

    private Boolean enabled;
}
