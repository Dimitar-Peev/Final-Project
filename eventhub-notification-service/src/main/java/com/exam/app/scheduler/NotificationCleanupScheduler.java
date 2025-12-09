package com.exam.app.scheduler;

import com.exam.app.service.NotificationService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@AllArgsConstructor
public class NotificationCleanupScheduler {

    private final NotificationService notificationService;
    private final static int DAYS_TO_KEEP = 30;

    @Scheduled(fixedDelay = 3600000)
    public void cleanupOldNotifications() {
        log.info("Cleanup scheduler triggered at {}", LocalDateTime.now());

        int deletedCount = notificationService.deleteOldNotifications(DAYS_TO_KEEP);

        if (deletedCount > 0) {
            log.info("Deleted {} old notifications (older than {} days).", deletedCount, DAYS_TO_KEEP);
        } else {
            log.info("No old notifications to delete.");
        }
    }
}
