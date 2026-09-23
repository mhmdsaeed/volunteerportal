package com.volunteerportal.app.service;

import java.util.List;

import com.volunteerportal.app.model.Notification;
import com.volunteerportal.app.model.User;

public interface NotificationService {

    /**
     * Sends a notification whose text is the {@code messageKey} message from messages*.properties,
     * rendered in each viewer's language with {@code args} ({@code {0}}, {@code {1}}, ...).
     */
    Notification notify(User user, String messageKey, String link, String... args);

    List<Notification> findForUser(Long userId);

    long countUnread(Long userId);

    void markRead(Long notificationId, Long userId);

    void markAllRead(Long userId);
}
