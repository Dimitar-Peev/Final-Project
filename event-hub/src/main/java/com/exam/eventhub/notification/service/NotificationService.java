package com.exam.eventhub.notification.service;

import com.exam.eventhub.exception.NotificationServiceFeignCallException;
import com.exam.eventhub.exception.UnauthorizedException;
import com.exam.eventhub.notification.client.NotificationClient;
import com.exam.eventhub.notification.client.dto.NotificationRequest;
import com.exam.eventhub.notification.client.dto.NotificationResponse;
import com.exam.eventhub.user.model.User;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.exam.eventhub.common.Constants.DELETE_UNSUCCESSFUL;
import static com.exam.eventhub.common.Constants.NOT_AUTHORIZED;

@Slf4j
@Service
@AllArgsConstructor
public class NotificationService {

    private final NotificationClient notificationClient;

    public void sendIfEnabled(User user, String subject, String message) {
        if (user == null) {
            log.warn("Cannot send notification: user is null.");
            return;
        }

        if (!user.isNotificationsEnabled()) {
            log.info("Notifications disabled for user {} — skipping.", user.getUsername());
            throw new IllegalArgumentException("User [%s] does not allow to receive notifications.".formatted(user.getUsername()));
        }

        NotificationRequest notificationRequest = new NotificationRequest();
        notificationRequest.setRecipientId(user.getId());
        notificationRequest.setRecipientEmail(user.getEmail());
        notificationRequest.setSubject(subject);
        notificationRequest.setMessage(message);

        ResponseEntity<NotificationResponse> notificationResponse = notificationClient.sendNotification(notificationRequest);
        if (!notificationResponse.getStatusCode().is2xxSuccessful()) {
            log.error("[Feign call to notification-service failed] Failed to send notification to {}.", user.getUsername());
        }

        log.info("📨 Notification sent successfully to {}", user.getUsername());
    }

    public List<NotificationResponse> getNotificationsByUser(UUID userId) {

        ResponseEntity<List<NotificationResponse>> response = notificationClient.getNotificationsByUser(userId);

        return response.getBody() != null
                ? response.getBody()
                : Collections.emptyList();
    }

    public List<NotificationResponse> getAll() {

        ResponseEntity<List<NotificationResponse>> response = notificationClient.getAllNotifications();

        return response.getBody() != null
                ? response.getBody()
                : Collections.emptyList();
    }

    public void delete(UUID id) {

        try {
            notificationClient.deleteNotification(id);
            log.info("Notification with ID [{}] was successfully deleted.", id);
        } catch (Exception e) {
            log.error("Error deleting notification: {}", e.getMessage());
            throw new NotificationServiceFeignCallException(DELETE_UNSUCCESSFUL);
        }
    }

    public void deleteUserNotification(UUID notificationId, UUID userId) {
        List<NotificationResponse> userNotifications = getNotificationsByUser(userId);

        boolean isOwner = userNotifications.stream()
                .anyMatch(n -> n.getId().equals(notificationId));

        if (!isOwner) {
            log.warn("User [{}] attempted to delete notification [{}] that doesn't belong to them", userId, notificationId);
            throw new UnauthorizedException(NOT_AUTHORIZED);
        }

        delete(notificationId);
        log.info("Notification [{}] successfully deleted by user [{}]", notificationId, userId);
    }

    public void clearAllNotifications(List<NotificationResponse> notifications, User user) {
        for (NotificationResponse n : notifications) {
            delete(n.getId());
        }

        log.info("User {} cleared his notification history.", user.getUsername());
    }

}
