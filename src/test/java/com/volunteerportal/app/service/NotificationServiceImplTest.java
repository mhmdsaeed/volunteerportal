package com.volunteerportal.app.service;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityNotFoundException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @Test
    void notify_savesUnreadNotificationWithMessageAndLink() {
        User user = new User();
        user.setId(1L);
        given(notificationRepository.save(any(Notification.class))).willAnswer(inv -> inv.getArgument(0));

        Notification result = notificationService.notify(user, "Approved!", "/initiatives/5");

        assertThat(result.getUser()).isEqualTo(user);
        assertThat(result.getMessage()).isEqualTo("Approved!");
        assertThat(result.getLink()).isEqualTo("/initiatives/5");
        assertThat(result.isRead()).isFalse();
        assertThat(result.getCreatedDttm()).isNotNull();
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
