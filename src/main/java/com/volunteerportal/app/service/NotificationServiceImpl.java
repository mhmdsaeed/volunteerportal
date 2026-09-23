package com.volunteerportal.app.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.context.MessageSource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.volunteerportal.app.model.Notification;
import com.volunteerportal.app.model.User;
import com.volunteerportal.app.repository.NotificationRepository;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final int MESSAGE_MAX_LENGTH = 500;

    private final NotificationRepository notificationRepository;
    private final MessageSource messageSource;

    public NotificationServiceImpl(NotificationRepository notificationRepository, MessageSource messageSource) {
        this.notificationRepository = notificationRepository;
        this.messageSource = messageSource;
    }

    @Override
    @Transactional
    public Notification notify(User user, String messageKey, String link, String... args) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setMessageKey(messageKey);
        List<String> argList = Arrays.stream(args).map(arg -> arg == null ? "" : arg).toList();
        notification.setMessageArgs(new ArrayList<>(argList));
        // English copy for anything that reads the raw column; the page renders the key in the viewer's language
        String english = messageSource.getMessage(messageKey, argList.toArray(), Locale.ENGLISH);
        notification.setMessage(english.length() > MESSAGE_MAX_LENGTH ? english.substring(0, MESSAGE_MAX_LENGTH) : english);
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
