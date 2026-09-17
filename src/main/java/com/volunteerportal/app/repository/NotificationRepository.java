package com.volunteerportal.app.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.volunteerportal.app.model.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreatedDttmDesc(Long userId);

    long countByUserIdAndReadFalse(Long userId);
}
