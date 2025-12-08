package com.exam.app.service;

import com.exam.app.exception.NotificationNotFoundException;
import com.exam.app.model.Notification;
import com.exam.app.model.NotificationStatus;
import com.exam.app.repository.NotificationRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@AllArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public Notification sendNotification(Notification notification) {
        log.info("Received notification request: recipient [{}], subject [{}]", notification.getRecipientEmail(), notification.getSubject());

        notification.setStatus(NotificationStatus.SENT);

        Notification saved = notificationRepository.save(notification);

        log.info("Notification saved with id [{}] and status[{}]", saved.getId(), saved.getStatus());

        return saved;
    }

    public List<Notification> getAll() {
        log.info("Fetching all notifications");

        return notificationRepository.findAllByDeletedIsFalse();
    }

    public List<Notification> getNotificationsByUser(UUID userId) {
        log.info("Fetching notifications for userId [{}]", userId);

        return notificationRepository.findByRecipientIdAndDeletedIsFalse(userId);
    }

    public void deleteNotification(UUID id) {
        log.info("Deleting notification with id [{}]", id);
        Notification notification = getNotificationById(id);

        notification.setDeleted(true);
        notificationRepository.save(notification);
        log.info("Notification deleted.");
    }

    public void clearNotifications(UUID userId) {

        List<Notification> notifications = getNotificationsByUser(userId);

        for (Notification notification : notifications) {
            deleteNotification(notification.getId());
        }
    }

    public int deleteOldNotifications(int days) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);
        return notificationRepository.deleteByCreatedOnBefore(cutoffDate);
    }

    private Notification getNotificationById(UUID id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new NotificationNotFoundException("Notification with ID [%s] was not found.".formatted(id)));
    }
}
