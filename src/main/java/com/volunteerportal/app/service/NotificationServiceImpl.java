package com.volunteerportal.app.service;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.volunteerportal.app.model.Notification;
import com.volunteerportal.app.model.User;
import com.volunteerportal.app.repository.NotificationRepository;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationServiceImpl(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    @Transactional
    public Notification notify(User user, String message, String link) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setMessage(message);
        notification.setLink(link);
        notification.setRead(false);
        notification.setCreatedDttm(LocalDateTime.now());
        return notificationRepository.save(notification);
    }

    @Override
    public List<Notification> findForUser(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedDttmDesc(userId);
    }

    @Override
    public long countUnread(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Override
    @Transactional
    public void markRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new EntityNotFoundException("Notification not found: " + notificationId));
        if (!notification.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Not the owner of notification " + notificationId);
        }
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void markAllRead(Long userId) {
        for (Notification notification : notificationRepository.findByUserIdOrderByCreatedDttmDesc(userId)) {
            if (!notification.isRead()) {
                notification.setRead(true);
                notificationRepository.save(notification);
            }
        }
    }
}
