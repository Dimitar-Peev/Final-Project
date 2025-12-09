package com.exam.eventhub.util;

import com.exam.eventhub.notification.client.dto.NotificationResponse;
import com.exam.eventhub.user.model.Role;
import com.exam.eventhub.user.model.User;
import lombok.experimental.UtilityClass;

import java.time.LocalDateTime;
import java.util.UUID;

@UtilityClass
public class UserNotificationHelper {

    public static User createMockUser(String username, UUID userId, boolean notificationsEnabled) {
        User user = new User();
        user.setId(userId);
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setPassword("encodedPassword");
        user.setRole(Role.USER);
        user.setNotificationsEnabled(notificationsEnabled);
        user.setBlocked(false);
        return user;
    }

    public static NotificationResponse createMockNotification(String subject, String message) {
        NotificationResponse notification = new NotificationResponse();
        notification.setId(UUID.randomUUID());
        notification.setRecipientId(UUID.randomUUID());
        notification.setRecipientEmail("test@example.com");
        notification.setSubject(subject);
        notification.setMessage(message);
        notification.setStatus("SENT");
        notification.setCreatedOn(LocalDateTime.now());
        return notification;
    }
}
