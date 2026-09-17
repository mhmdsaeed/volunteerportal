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
@Table(name = "event")
@Getter
@Setter
@NoArgsConstructor
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(name = "from_dttm")
    private LocalDateTime fromDttm;

    @Column(name = "to_dttm")
    private LocalDateTime toDttm;

    @Column(name = "loc_longitude")
    private Double locLongitude;

    @Column(name = "loc_latitude")
    private Double locLatitude;

    @Column(name = "loc_url")
    private String locUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initiative_id")
    private Initiative initiative;

    private Boolean enabled;
}
