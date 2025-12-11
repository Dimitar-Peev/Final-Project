package com.exam.eventhub.notification.service;

import com.exam.eventhub.exception.NotificationServiceFeignCallException;
import com.exam.eventhub.exception.UnauthorizedException;
import com.exam.eventhub.notification.client.NotificationClient;
import com.exam.eventhub.notification.client.dto.NotificationRequest;
import com.exam.eventhub.notification.client.dto.NotificationResponse;
import com.exam.eventhub.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.exam.eventhub.common.Constants.DELETE_UNSUCCESSFUL;
import static com.exam.eventhub.common.Constants.NOT_AUTHORIZED;
import static com.exam.eventhub.util.NotificationHelper.createNotificationResponse;
import static com.exam.eventhub.util.NotificationHelper.createUser;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NotificationServiceUTest {

    @Mock
    private NotificationClient notificationClient;

    @InjectMocks
    private NotificationService notificationService;

    private NotificationResponse response1;
    private NotificationResponse response2;
    private NotificationResponse response3;

    @BeforeEach
    void setUp() {
        response1 = createNotificationResponse(UUID.randomUUID(), "Subject 1", "Message 1");
        response2 = createNotificationResponse(UUID.randomUUID(), "Subject 2", "Message 2");
        response3 = createNotificationResponse(UUID.randomUUID(), "Subject 3", "Message 3");
    }

    @Test
    void sendIfEnabled_whenUserIsNull_shouldLogWarningAndReturn() {

        notificationService.sendIfEnabled(null, "Subject", "Message");

        verify(notificationClient, never()).sendNotification(any());
    }

    @Test
    void sendIfEnabled_whenNotificationsDisabled_shouldThrowException() {

        User user = createUser(UUID.randomUUID(), "user", "user@example.com");
        user.setNotificationsEnabled(false);

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> notificationService.sendIfEnabled(user, "Subject", "Message"));
        assertTrue(exception.getMessage().contains("User [user] does not allow to receive notifications."));

        verify(notificationClient, never()).sendNotification(any());
    }

    @Test
    void sendIfEnabled_shouldCreateCorrectNotificationRequest() {

        UUID userId = UUID.randomUUID();
        User user = createUser(userId, "john", "john@example.com");
        user.setNotificationsEnabled(true);

        String subject = "Payment Successful";
        String message = "Your payment was processed successfully.";

        NotificationResponse response = new NotificationResponse();
        ResponseEntity<NotificationResponse> responseEntity = new ResponseEntity<>(response, HttpStatus.OK);

        when(notificationClient.sendNotification(any(NotificationRequest.class))).thenReturn(responseEntity);

        notificationService.sendIfEnabled(user, subject, message);

        verify(notificationClient).sendNotification(argThat(request ->
                request.getRecipientId().equals(userId) &&
                        request.getRecipientEmail().equals("john@example.com") &&
                        request.getSubject().equals(subject) &&
                        request.getMessage().equals(message)
        ));
    }

    @Test
    void sendIfEnabled_whenClientReturnsNon2xxStatus_shouldLogError() {

        UUID userId = UUID.randomUUID();
        User user = createUser(userId, "user", "user@example.com");
        user.setNotificationsEnabled(true);

        NotificationResponse response = new NotificationResponse();
        ResponseEntity<NotificationResponse> responseEntity = new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);

        when(notificationClient.sendNotification(any(NotificationRequest.class))).thenReturn(responseEntity);

        notificationService.sendIfEnabled(user, "Subject", "Message");

        verify(notificationClient, times(1)).sendNotification(any(NotificationRequest.class));
    }

    @Test
    void getNotificationsByUser_shouldReturnNotifications() {

        UUID userId = UUID.randomUUID();

        List<NotificationResponse> expectedNotifications = List.of(response1, response2);

        ResponseEntity<List<NotificationResponse>> responseEntity = ResponseEntity.ok(expectedNotifications);

        when(notificationClient.getNotificationsByUser(userId)).thenReturn(responseEntity);

        List<NotificationResponse> actualNotifications = notificationService.getNotificationsByUser(userId);

        assertEquals(expectedNotifications, actualNotifications);
        assertEquals(2, actualNotifications.size());
        verify(notificationClient, times(1)).getNotificationsByUser(userId);
    }

    @Test
    void getNotificationsByUser_whenResponseBodyIsNull_shouldReturnEmptyList() {

        UUID userId = UUID.randomUUID();
        ResponseEntity<List<NotificationResponse>> responseEntity = new ResponseEntity<>(null, HttpStatus.OK);

        when(notificationClient.getNotificationsByUser(userId)).thenReturn(responseEntity);

        List<NotificationResponse> actualNotifications = notificationService.getNotificationsByUser(userId);

        assertNotNull(actualNotifications);
        assertTrue(actualNotifications.isEmpty());
        verify(notificationClient, times(1)).getNotificationsByUser(userId);
    }

    @Test
    void getNotificationsByUser_whenResponseBodyIsEmpty_shouldReturnEmptyList() {

        UUID userId = UUID.randomUUID();
        ResponseEntity<List<NotificationResponse>> responseEntity = ResponseEntity.ok(Collections.emptyList());

        when(notificationClient.getNotificationsByUser(userId)).thenReturn(responseEntity);

        List<NotificationResponse> actualNotifications = notificationService.getNotificationsByUser(userId);

        assertNotNull(actualNotifications);
        assertTrue(actualNotifications.isEmpty());
        verify(notificationClient, times(1)).getNotificationsByUser(userId);
    }

    @Test
    void getAll_shouldReturnAllNotifications() {

        List<NotificationResponse> expectedNotifications = List.of(response1, response2, response3);

        ResponseEntity<List<NotificationResponse>> responseEntity = ResponseEntity.ok(expectedNotifications);

        when(notificationClient.getAllNotifications()).thenReturn(responseEntity);

        List<NotificationResponse> actualNotifications = notificationService.getAll();

        assertEquals(expectedNotifications, actualNotifications);
        assertEquals(3, actualNotifications.size());
        verify(notificationClient, times(1)).getAllNotifications();
    }

    @Test
    void getAll_whenResponseBodyIsNull_shouldReturnEmptyList() {

        ResponseEntity<List<NotificationResponse>> responseEntity = new ResponseEntity<>(null, HttpStatus.OK);

        when(notificationClient.getAllNotifications()).thenReturn(responseEntity);

        List<NotificationResponse> actualNotifications = notificationService.getAll();

        assertNotNull(actualNotifications);
        assertTrue(actualNotifications.isEmpty());
        verify(notificationClient, times(1)).getAllNotifications();
    }

    @Test
    void getAll_whenResponseBodyIsEmpty_shouldReturnEmptyList() {

        ResponseEntity<List<NotificationResponse>> responseEntity = ResponseEntity.ok(Collections.emptyList());

        when(notificationClient.getAllNotifications()).thenReturn(responseEntity);

        List<NotificationResponse> actualNotifications = notificationService.getAll();

        assertNotNull(actualNotifications);
        assertTrue(actualNotifications.isEmpty());
        verify(notificationClient, times(1)).getAllNotifications();
    }

    @Test
    void deleteUserNotification_whenUserOwnsNotification_shouldDeleteSuccessfully() {

        UUID userId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();

        NotificationResponse ownedNotification = createNotificationResponse(notificationId, "Subject", "Message");
        List<NotificationResponse> userNotifications = List.of(ownedNotification, response2);

        ResponseEntity<List<NotificationResponse>> responseEntity = ResponseEntity.ok(userNotifications);
        when(notificationClient.getNotificationsByUser(userId)).thenReturn(responseEntity);

        notificationService.deleteUserNotification(notificationId, userId);

        verify(notificationClient, times(1)).getNotificationsByUser(userId);
        verify(notificationClient, times(1)).deleteNotification(notificationId);
    }

    @Test
    void deleteUserNotification_whenUserDoesNotOwnNotification_shouldThrowUnauthorizedException() {

        UUID userId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        UUID differentNotificationId = UUID.randomUUID();

        NotificationResponse notification = createNotificationResponse(differentNotificationId, "Subject", "Message");
        List<NotificationResponse> userNotifications = List.of(notification, response2);

        ResponseEntity<List<NotificationResponse>> responseEntity = ResponseEntity.ok(userNotifications);
        when(notificationClient.getNotificationsByUser(userId)).thenReturn(responseEntity);

        UnauthorizedException exception =
                assertThrows(UnauthorizedException.class, () -> notificationService.deleteUserNotification(notificationId, userId));

        assertTrue(exception.getMessage().contains(NOT_AUTHORIZED));
        verify(notificationClient, times(1)).getNotificationsByUser(userId);
        verify(notificationClient, never()).deleteNotification(any(UUID.class));
    }

    @Test
    void delete_whenClientThrowsException_shouldThrowNotificationServiceFeignCallException() {

        UUID notificationId = UUID.randomUUID();
        doThrow(new RuntimeException("Connection timeout")).when(notificationClient).deleteNotification(notificationId);

        NotificationServiceFeignCallException exception =
                assertThrows(NotificationServiceFeignCallException.class, () -> notificationService.delete(notificationId));
        assertTrue(exception.getMessage().contains(DELETE_UNSUCCESSFUL));

        verify(notificationClient, times(1)).deleteNotification(notificationId);
    }

    @Test
    void clearAllNotifications_shouldDeleteAllNotifications() {

        User user = createUser(UUID.randomUUID(), "user", "user@example.com");

        List<NotificationResponse> notifications = List.of(response1,response2,response3);

        notificationService.clearAllNotifications(notifications, user);

        verify(notificationClient, times(1)).deleteNotification(response1.getId());
        verify(notificationClient, times(1)).deleteNotification(response2.getId());
        verify(notificationClient, times(1)).deleteNotification(response3.getId());
        verify(notificationClient, times(3)).deleteNotification(any(UUID.class));
    }

}
