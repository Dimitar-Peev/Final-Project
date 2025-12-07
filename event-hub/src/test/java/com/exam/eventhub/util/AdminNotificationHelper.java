package com.exam.eventhub.util;

import com.exam.eventhub.notification.client.dto.NotificationResponse;
import lombok.experimental.UtilityClass;

import java.time.LocalDateTime;
import java.util.UUID;

@UtilityClass
public class AdminNotificationHelper {

    public static NotificationResponse createMockNotification(String subject, String message, String status) {
        NotificationResponse notification = new NotificationResponse();
        notification.setId(UUID.randomUUID());
        notification.setRecipientId(UUID.randomUUID());
        notification.setRecipientEmail("user@example.com");
        notification.setSubject(subject);
        notification.setMessage(message);
        notification.setStatus(status);
        notification.setCreatedOn(LocalDateTime.now());
        return notification;
    }

    public static NotificationResponse createMockNotificationWithId(UUID id, String subject, String message, String status) {
        NotificationResponse notification = new NotificationResponse();
        notification.setId(id);
        notification.setRecipientId(UUID.randomUUID());
        notification.setRecipientEmail("user@example.com");
        notification.setSubject(subject);
        notification.setMessage(message);
        notification.setStatus(status);
        notification.setCreatedOn(LocalDateTime.now());
        return notification;
    }
}
