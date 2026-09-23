package com.volunteerportal.app.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
@Table(name = "notification")
@Getter
@Setter
@NoArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** English rendering; shown as-is for older notifications that have no {@link #messageKey}. */
    @Column(nullable = false, length = 500)
    private String message;

    /** messages.properties key, rendered in the viewer's language with {@link #messageArgs}. */
    @Column(name = "message_key", length = 100)
    private String messageKey;

    @Convert(converter = MessageArgsConverter.class)
    @Column(name = "message_args", length = 1000)
    private List<String> messageArgs = new ArrayList<>();

    private String link;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "created_dttm", nullable = false)
    private LocalDateTime createdDttm;
}
