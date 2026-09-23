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

/** A mobile app login: the SHA-256 hash of a bearer token (the token itself is never stored). */
@Entity
@Table(name = "api_token")
@Getter
@Setter
@NoArgsConstructor
public class ApiToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "device_name", length = 100)
    private String deviceName;

    @Column(name = "created_dttm", nullable = false)
    private LocalDateTime createdDttm;

    @Column(name = "expires_dttm", nullable = false)
    private LocalDateTime expiresDttm;

    @Column(name = "last_used_dttm")
    private LocalDateTime lastUsedDttm;
}
