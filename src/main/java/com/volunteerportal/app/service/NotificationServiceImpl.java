package com.volunteerportal.app.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

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

    /** Its first argument is the user's role codes, comma-separated (see UserAdminServiceImpl). */
    static final String ROLES_CHANGED = "notification.rolesChanged";

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
        // English copy for anything that reads the raw column; pages render the key in the viewer's language
        String english = messageSource.getMessage(messageKey, displayArgs(messageKey, argList, Locale.ENGLISH), Locale.ENGLISH);
        notification.setMessage(english.length() > MESSAGE_MAX_LENGTH ? english.substring(0, MESSAGE_MAX_LENGTH) : english);
        notification.setLink(link);
        notification.setRead(false);
        notification.setCreatedDttm(LocalDateTime.now());
        return notificationRepository.save(notification);
    }

    @Override
    public String text(Notification notification, Locale locale) {
        if (notification.getMessageKey() == null) {
            return notification.getMessage(); // older notifications only have their stored text
        }
        return messageSource.getMessage(notification.getMessageKey(),
                displayArgs(notification.getMessageKey(), notification.getMessageArgs(), locale),
                notification.getMessage(), locale);
    }

    /**
     * The arguments as shown to the reader. A role-change notification stores role codes ("ADMIN, VOLUNTEER"),
     * which are shown with their names in {@code locale} (role.ADMIN, ...), joined with the language's list
     * separator. Stored rows keep the codes, so older notifications are translated too.
     */
    private Object[] displayArgs(String messageKey, List<String> args, Locale locale) {
        if (!ROLES_CHANGED.equals(messageKey) || args.isEmpty()) {
            return args.toArray();
        }
        // "," or the Arabic "،", then a space (kept out of the .properties file, where trailing spaces get trimmed)
        String separator = messageSource.getMessage("common.listSeparator", null, ",", locale) + " ";
        String roles = Arrays.stream(args.get(0).split(",")).map(String::strip).filter(code -> !code.isEmpty())
                .map(code -> messageSource.getMessage("role." + code, null, code, locale))
                .collect(Collectors.joining(separator));
        List<String> shown = new ArrayList<>(args);
        shown.set(0, roles);
        return shown.toArray();
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
