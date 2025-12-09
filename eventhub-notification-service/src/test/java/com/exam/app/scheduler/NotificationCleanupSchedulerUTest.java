package com.exam.app.scheduler;

import com.exam.app.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationCleanupSchedulerUTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationCleanupScheduler scheduler;

    @Test
    void cleanupOldNotifications_shouldCallServiceWithCorrectDays() {

        int expectedDeletedCount = 5;
        when(notificationService.deleteOldNotifications(30)).thenReturn(expectedDeletedCount);

        scheduler.cleanupOldNotifications();

        verify(notificationService, times(1)).deleteOldNotifications(eq(30));
    }

    @Test
    void cleanupOldNotifications_whenNoNotificationsDeleted_shouldStillCallService() {

        when(notificationService.deleteOldNotifications(30)).thenReturn(0);

        scheduler.cleanupOldNotifications();

        verify(notificationService, times(1)).deleteOldNotifications(eq(30));
    }

    @Test
    void cleanupOldNotifications_whenMultipleNotificationsDeleted_shouldLogCorrectly() {

        int expectedDeletedCount = 15;
        when(notificationService.deleteOldNotifications(30)).thenReturn(expectedDeletedCount);

        scheduler.cleanupOldNotifications();
        
        verify(notificationService, times(1)).deleteOldNotifications(eq(30));
    }
}
