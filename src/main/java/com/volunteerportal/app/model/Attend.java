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
@Table(name = "attend")
@Getter
@Setter
@NoArgsConstructor
public class Attend {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "attend_in_out")
    private Integer attendInOut;

    @Column(name = "attend_dttm")
    private LocalDateTime attendDttm;

    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "volunteer_initiative_id")
    private VolunteerInitiative volunteerInitiative;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Event event;
}
