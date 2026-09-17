package com.volunteerportal.app.service;

import java.util.List;

import com.volunteerportal.app.model.Notification;
import com.volunteerportal.app.model.User;

public interface NotificationService {

    Notification notify(User user, String message, String link);

    List<Notification> findForUser(Long userId);

    long countUnread(Long userId);

    void markRead(Long notificationId, Long userId);

    void markAllRead(Long userId);
}
