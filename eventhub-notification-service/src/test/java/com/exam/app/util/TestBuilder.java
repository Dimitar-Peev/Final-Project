package com.exam.app.util;

import com.exam.app.model.Notification;
import com.exam.app.model.NotificationStatus;
import lombok.experimental.UtilityClass;

import java.time.LocalDateTime;
import java.util.UUID;

@UtilityClass
public class TestBuilder {

    public static Notification createMockNotification() {
        Notification notification = new Notification();

        notification.setId(UUID.randomUUID());
        notification.setRecipientId(UUID.randomUUID());
        notification.setRecipientEmail("test@example.com");
        notification.setSubject("Test Subject");
        notification.setMessage("Test Message");
        notification.setStatus(NotificationStatus.SENT);
        notification.setCreatedOn(LocalDateTime.now());
        notification.setDeleted(false);

        return notification;
    }

    public static Notification createNotification(UUID recipientId, String email, boolean deleted) {
        Notification notification = new Notification();

        notification.setRecipientId(recipientId);
        notification.setRecipientEmail(email);
        notification.setSubject("Test Subject");
        notification.setMessage("Test Message");
        notification.setStatus(NotificationStatus.PENDING);
        notification.setDeleted(deleted);

        return notification;
    }
}
