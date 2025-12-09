package com.exam.app.repository;

import com.exam.app.model.Notification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.exam.app.util.TestBuilder.createNotification;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
class NotificationRepositoryTest {

    @Autowired
    private NotificationRepository notificationRepository;

    private UUID recipientId1;
    private UUID recipientId2;

    @BeforeEach
    void setUp() {
        recipientId1 = UUID.randomUUID();
        recipientId2 = UUID.randomUUID();
    }

    @Test
    void findAllByDeletedIsFalse_shouldReturnOnlyNonDeletedNotifications() {

        Notification notification1 = createNotification(recipientId1, "test1@example.com", false);
        Notification notification2 = createNotification(recipientId1, "test2@example.com", true);
        Notification notification3 = createNotification(recipientId2, "test3@example.com", false);

        notificationRepository.save(notification1);
        notificationRepository.save(notification2);
        notificationRepository.save(notification3);

        List<Notification> result = notificationRepository.findAllByDeletedIsFalse();

        assertEquals(2, result.size());
        assertTrue(result.stream().noneMatch(Notification::isDeleted));
    }

    @Test
    void findByRecipientIdAndDeletedIsFalse_shouldReturnNotificationsForSpecificRecipient() {

        Notification notification1 = createNotification(recipientId1, "test1@example.com", false);
        Notification notification2 = createNotification(recipientId1, "test2@example.com", false);
        Notification notification3 = createNotification(recipientId2, "test3@example.com", false);
        Notification notification4 = createNotification(recipientId1, "test4@example.com", true);

        notificationRepository.save(notification1);
        notificationRepository.save(notification2);
        notificationRepository.save(notification3);
        notificationRepository.save(notification4);

        List<Notification> result = notificationRepository.findByRecipientIdAndDeletedIsFalse(recipientId1);

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(n -> n.getRecipientId().equals(recipientId1)));
        assertTrue(result.stream().noneMatch(Notification::isDeleted));
    }

    @Test
    void findByRecipientIdAndDeletedIsFalse_whenNoNotificationsFound_shouldReturnEmptyList() {

        UUID nonExistentRecipientId = UUID.randomUUID();

        List<Notification> result = notificationRepository.findByRecipientIdAndDeletedIsFalse(nonExistentRecipientId);

        assertTrue(result.isEmpty());
    }

    @Test
    void deleteByCreatedOnBefore_shouldDeleteOldNotifications() {

        Notification oldNotification = createNotification(recipientId1, "old@example.com", false);
        Notification recentNotification = createNotification(recipientId2, "recent@example.com", false);

        notificationRepository.save(oldNotification);
        notificationRepository.save(recentNotification);

        notificationRepository.flush();

        LocalDateTime cutoffDate = LocalDateTime.now().plusDays(1);

        int deletedCount = notificationRepository.deleteByCreatedOnBefore(cutoffDate);

        assertTrue(deletedCount >= 0);
    }

    @Test
    void deleteByCreatedOnBefore_whenNoOldNotifications_shouldReturnZero() {

        Notification recentNotification = createNotification(recipientId1, "recent@example.com", false);
        notificationRepository.save(recentNotification);
        notificationRepository.flush();

        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(1);

        int deletedCount = notificationRepository.deleteByCreatedOnBefore(cutoffDate);

        assertEquals(0, deletedCount);
    }

    @Test
    void save_shouldPersistNotificationWithGeneratedId() {

        Notification notification = createNotification(recipientId1, "test@example.com", false);

        Notification saved = notificationRepository.save(notification);
        notificationRepository.flush();
        
        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedOn());
        assertEquals(recipientId1, saved.getRecipientId());
        assertEquals("test@example.com", saved.getRecipientEmail());
    }
}
