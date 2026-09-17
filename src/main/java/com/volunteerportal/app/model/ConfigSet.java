package com.volunteerportal.app.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "configset")
@Getter
@Setter
@NoArgsConstructor
public class ConfigSet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "configset_key", nullable = false, unique = true)
    private String configsetKey;

    @Column(name = "configset_value", nullable = false)
    private String configsetValue;
}
