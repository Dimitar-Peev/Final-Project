package com.exam.eventhub.scheduler;

import com.exam.eventhub.booking.model.Booking;
import com.exam.eventhub.booking.service.BookingService;
import com.exam.eventhub.notification.service.NotificationService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@AllArgsConstructor
public class BookingCleanupScheduler {

    private final BookingService bookingService;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 */59 * * * *")
    @Transactional
    public void autoCancelExpiredBookings() {
        log.info("Scheduler triggered to cancel unpaid bookings at {}", LocalDateTime.now());

        List<Booking> expiredBookings = bookingService.getExpiredPendingBookings();

        if (expiredBookings.isEmpty()) {
            log.info("No expired bookings found.");
            return;
        }

        log.info("Found {} expired bookings. Cancelling...", expiredBookings.size());

        for (Booking booking : expiredBookings) {

            bookingService.autoCancelBooking(booking.getId(), "Auto-cancelled after timeout.");

            log.info("Booking {} for event '{}' was auto-cancelled.", booking.getId(), booking.getEvent().getTitle());

            notificationService.sendIfEnabled(
                    booking.getUser(),
                    "Booking Cancelled",
                    "Your booking for \"" + booking.getEvent().getTitle() +
                            "\" has been automatically cancelled because it was not paid in time."
            );
        }

        log.info("Scheduler finished at {}", LocalDateTime.now());
    }
}
