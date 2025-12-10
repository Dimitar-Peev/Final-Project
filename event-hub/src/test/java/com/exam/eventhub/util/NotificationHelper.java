package com.exam.eventhub.util;

import com.exam.eventhub.notification.client.dto.NotificationResponse;
import com.exam.eventhub.user.model.User;
import lombok.experimental.UtilityClass;

import java.util.UUID;

@UtilityClass
public class NotificationHelper {

    public static User createUser(UUID id, String username, String email) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setNotificationsEnabled(false);
        return user;
    }

    public static NotificationResponse createNotificationResponse(UUID id, String subject, String message) {
        NotificationResponse response = new NotificationResponse();
        response.setId(id);
        response.setSubject(subject);
        response.setMessage(message);
        return response;
    }
}
