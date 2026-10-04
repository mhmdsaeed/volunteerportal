package com.volunteerportal.app.service;

import java.util.List;
import java.util.Locale;

import com.volunteerportal.app.model.Notification;
import com.volunteerportal.app.model.User;

public interface NotificationService {

    /**
     * Sends a notification whose text is the {@code messageKey} message from messages*.properties,
     * rendered in each viewer's language with {@code args} ({@code {0}}, {@code {1}}, ...).
     */
    Notification notify(User user, String messageKey, String link, String... args);

    /**
     * The notification's text in {@code locale}: its message key rendered with its arguments (role codes in
     * them translated too), or the stored text for older notifications that have no key.
     */
    String text(Notification notification, Locale locale);

    List<Notification> findForUser(Long userId);

    long countUnread(Long userId);

    void markRead(Long notificationId, Long userId);

    void markAllRead(Long userId);
}
