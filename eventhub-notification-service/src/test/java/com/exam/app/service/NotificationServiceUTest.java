package com.exam.app.service;

import com.exam.app.exception.NotificationNotFoundException;
import com.exam.app.model.Notification;
import com.exam.app.model.NotificationStatus;
import com.exam.app.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NotificationServiceUTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    private Notification testNotification;
    private UUID testUserId;
    private UUID testNotificationId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testNotificationId = UUID.randomUUID();

        testNotification = new Notification();
        testNotification.setId(testNotificationId);
        testNotification.setRecipientEmail("test@example.com");
        testNotification.setSubject("Test Subject");
        testNotification.setRecipientId(testUserId);
        testNotification.setDeleted(false);
    }

    @Test
    void givenNotification_whenSendNotification_thenSaveWithSentStatus() {

        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        Notification result = notificationService.sendNotification(testNotification);

        assertNotNull(result);
        assertEquals(NotificationStatus.SENT, testNotification.getStatus());
        verify(notificationRepository, times(1)).save(testNotification);
    }

    @Test
    void sendNotification_shouldReturnSavedNotification() {

        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        Notification result = notificationService.sendNotification(testNotification);

        assertEquals(testNotification.getId(), result.getId());
        assertEquals(testNotification.getRecipientEmail(), result.getRecipientEmail());
        assertEquals(testNotification.getSubject(), result.getSubject());
    }

    @Test
    void getAll_shouldReturnAllNonDeletedNotifications() {

        Notification notification1 = new Notification();
        notification1.setId(UUID.randomUUID());
        notification1.setDeleted(false);

        Notification notification2 = new Notification();
        notification2.setId(UUID.randomUUID());
        notification2.setDeleted(false);

        List<Notification> notifications = Arrays.asList(notification1, notification2);
        when(notificationRepository.findAllByDeletedIsFalse()).thenReturn(notifications);

        List<Notification> result = notificationService.getAll();

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(notificationRepository, times(1)).findAllByDeletedIsFalse();
    }

    @Test
    void getAll_shouldReturnEmptyListWhenNoNotifications() {

        when(notificationRepository.findAllByDeletedIsFalse()).thenReturn(List.of());

        List<Notification> result = notificationService.getAll();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getNotificationsByUser_shouldReturnUserNotifications() {

        Notification notification1 = new Notification();
        notification1.setRecipientId(testUserId);
        notification1.setDeleted(false);

        Notification notification2 = new Notification();
        notification2.setRecipientId(testUserId);
        notification2.setDeleted(false);

        List<Notification> userNotifications = Arrays.asList(notification1, notification2);
        when(notificationRepository.findByRecipientIdAndDeletedIsFalse(testUserId)).thenReturn(userNotifications);

        List<Notification> result = notificationService.getNotificationsByUser(testUserId);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(notificationRepository, times(1)).findByRecipientIdAndDeletedIsFalse(testUserId);
    }

    @Test
    void getNotificationsByUser_shouldReturnEmptyListWhenNoNotifications() {

        when(notificationRepository.findByRecipientIdAndDeletedIsFalse(testUserId)).thenReturn(List.of());

        List<Notification> result = notificationService.getNotificationsByUser(testUserId);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void deleteNotification_shouldMarkAsDeleted() {

        testNotification.setDeleted(false);

        when(notificationRepository.findById(testNotificationId)).thenReturn(Optional.of(testNotification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        notificationService.deleteNotification(testNotificationId);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertTrue(captor.getValue().isDeleted());
    }

    @Test
    void deleteNotification_shouldThrowExceptionWhenNotFound() {

        UUID nonExistentId = UUID.randomUUID();
        when(notificationRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        NotificationNotFoundException exception =
                assertThrows(NotificationNotFoundException.class, () -> notificationService.deleteNotification(nonExistentId));
        assertTrue(exception.getMessage().contains("Notification with ID [%s] was not found.".formatted(nonExistentId)));

        assertTrue(exception.getMessage().contains(nonExistentId.toString()));
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void clearNotifications_shouldDeleteAllUserNotifications() {

        Notification notification1 = new Notification();
        notification1.setId(UUID.randomUUID());
        notification1.setRecipientId(testUserId);
        notification1.setDeleted(false);

        Notification notification2 = new Notification();
        notification2.setId(UUID.randomUUID());
        notification2.setRecipientId(testUserId);
        notification2.setDeleted(false);

        List<Notification> userNotifications = Arrays.asList(notification1, notification2);

        when(notificationRepository.findByRecipientIdAndDeletedIsFalse(testUserId)).thenReturn(userNotifications);
        when(notificationRepository.findById(notification1.getId())).thenReturn(Optional.of(notification1));
        when(notificationRepository.findById(notification2.getId())).thenReturn(Optional.of(notification2));

        notificationService.clearNotifications(testUserId);

        verify(notificationRepository, times(2)).save(any(Notification.class));
    }

    @Test
    void clearNotifications_shouldDoNothingWhenNoNotifications() {

        when(notificationRepository.findByRecipientIdAndDeletedIsFalse(testUserId)).thenReturn(List.of());

        notificationService.clearNotifications(testUserId);

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void deleteOldNotifications_shouldDeleteOldNotifications() {

        int days = 30;
        int deletedCount = 5;
        when(notificationRepository.deleteByCreatedOnBefore(any(LocalDateTime.class))).thenReturn(deletedCount);

        int result = notificationService.deleteOldNotifications(days);

        assertEquals(deletedCount, result);
        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(notificationRepository).deleteByCreatedOnBefore(captor.capture());

        LocalDateTime capturedDate = captor.getValue();
        LocalDateTime expectedDate = LocalDateTime.now().minusDays(days);

        assertTrue(capturedDate.isBefore(expectedDate.plusSeconds(1)));
        assertTrue(capturedDate.isAfter(expectedDate.minusSeconds(1)));
    }

    @Test
    void deleteOldNotifications_shouldReturnZeroWhenNoOldNotifications() {

        int days = 30;
        when(notificationRepository.deleteByCreatedOnBefore(any(LocalDateTime.class))).thenReturn(0);

        int result = notificationService.deleteOldNotifications(days);
        
        assertEquals(0, result);
    }
}
