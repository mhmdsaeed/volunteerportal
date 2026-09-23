package com.volunteerportal.app.service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import jakarta.persistence.EntityNotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.security.access.AccessDeniedException;

import com.volunteerportal.app.model.Notification;
import com.volunteerportal.app.model.User;
import com.volunteerportal.app.repository.NotificationRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    private NotificationServiceImpl notificationService;

    @BeforeEach
    void setUp() {
        // The real bundles, so the stored English text is what the app would actually produce
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setFallbackToSystemLocale(false);
        notificationService = new NotificationServiceImpl(notificationRepository, messageSource);
    }

    @Test
    void notify_savesUnreadNotificationWithKeyArgsEnglishTextAndLink() {
        User user = new User();
        user.setId(1L);
        given(notificationRepository.save(any(Notification.class))).willAnswer(inv -> inv.getArgument(0));

        Notification result = notificationService.notify(user, "notification.joinApproved", "/initiatives/5", "Beach Cleanup");

        assertThat(result.getUser()).isEqualTo(user);
        assertThat(result.getMessageKey()).isEqualTo("notification.joinApproved");
        assertThat(result.getMessageArgs()).containsExactly("Beach Cleanup");
        assertThat(result.getMessage()).isEqualTo("Your request to join 'Beach Cleanup' was approved.");
        assertThat(result.getLink()).isEqualTo("/initiatives/5");
        assertThat(result.isRead()).isFalse();
        assertThat(result.getCreatedDttm()).isNotNull();
    }

    @Test
    void notify_nullArgument_isStoredAsEmptyString() {
        given(notificationRepository.save(any(Notification.class))).willAnswer(inv -> inv.getArgument(0));

        Notification result = notificationService.notify(new User(), "notification.joinRejected", "/initiatives/5", (String) null);

        assertThat(result.getMessageArgs()).containsExactly("");
        assertThat(result.getMessage()).isEqualTo("Your request to join '' was not approved.");
    }

    @Test
    void notify_everyNotificationKey_rendersInArabicWithTheArgumentsFilledIn() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        messageSource.setDefaultEncoding("UTF-8");
        Locale arabic = Locale.forLanguageTag("ar");

        assertThat(messageSource.getMessage("notification.joinApproved", new Object[] { "X" }, arabic)).isEqualTo("تم قبول طلب انضمامك إلى «X».");
        assertThat(messageSource.getMessage("notification.joinRejected", new Object[] { "X" }, arabic)).contains("«X»");
        assertThat(messageSource.getMessage("notification.joinRequested", new Object[] { "vol", "X" }, arabic)).isEqualTo("طلب vol الانضمام إلى «X».");
        assertThat(messageSource.getMessage("notification.profileUpdated", new Object[] { "Gold", "12" }, arabic)).contains("Gold").contains("12");
        assertThat(messageSource.getMessage("notification.profileUpdatedUnranked", new Object[] { "12" }, arabic)).contains("12");
    }

    @Test
    void findForUser_delegatesToRepository() {
        Notification notification = new Notification();
        given(notificationRepository.findByUserIdOrderByCreatedDttmDesc(1L)).willReturn(List.of(notification));

        assertThat(notificationService.findForUser(1L)).containsExactly(notification);
    }

    @Test
    void countUnread_delegatesToRepository() {
        given(notificationRepository.countByUserIdAndReadFalse(1L)).willReturn(3L);

        assertThat(notificationService.countUnread(1L)).isEqualTo(3L);
    }

    @Test
    void markRead_ownedByUser_marksAsRead() {
        User owner = new User();
        owner.setId(1L);
        Notification notification = new Notification();
        notification.setId(10L);
        notification.setUser(owner);
        given(notificationRepository.findById(10L)).willReturn(Optional.of(notification));

        notificationService.markRead(10L, 1L);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().isRead()).isTrue();
    }

    @Test
    void markRead_notOwnedByUser_throwsAccessDenied() {
        User owner = new User();
        owner.setId(1L);
        Notification notification = new Notification();
        notification.setId(10L);
        notification.setUser(owner);
        given(notificationRepository.findById(10L)).willReturn(Optional.of(notification));

        assertThatThrownBy(() -> notificationService.markRead(10L, 999L))
                .isInstanceOf(AccessDeniedException.class);

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void markRead_notFound_throwsEntityNotFound() {
        given(notificationRepository.findById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markRead(10L, 1L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void markAllRead_onlySavesPreviouslyUnreadNotifications() {
        Notification alreadyRead = new Notification();
        alreadyRead.setId(1L);
        alreadyRead.setRead(true);

        Notification unread1 = new Notification();
        unread1.setId(2L);
        unread1.setRead(false);

        Notification unread2 = new Notification();
        unread2.setId(3L);
        unread2.setRead(false);

        given(notificationRepository.findByUserIdOrderByCreatedDttmDesc(1L))
                .willReturn(List.of(alreadyRead, unread1, unread2));

        notificationService.markAllRead(1L);

        verify(notificationRepository, times(2)).save(any(Notification.class));
        assertThat(unread1.isRead()).isTrue();
        assertThat(unread2.isRead()).isTrue();
    }
}
