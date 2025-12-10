package com.exam.eventhub.booking.service;

import com.exam.eventhub.booking.model.Booking;
import com.exam.eventhub.booking.model.BookingStatus;
import com.exam.eventhub.booking.repository.BookingRepository;
import com.exam.eventhub.event.model.Event;
import com.exam.eventhub.event.service.EventService;
import com.exam.eventhub.exception.*;
import com.exam.eventhub.notification.service.NotificationService;
import com.exam.eventhub.payment.client.dto.PaymentResponse;
import com.exam.eventhub.payment.service.PaymentService;
import com.exam.eventhub.user.model.User;
import com.exam.eventhub.user.service.UserService;
import com.exam.eventhub.web.dto.BookingCreateRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.exam.eventhub.common.Constants.ID_NOT_FOUND;

@Slf4j
@Service
@AllArgsConstructor
public class BookingService {

    private static final String ENTITY_NAME = "Booking";
    private static final int BOOKING_EXPIRATION_MINUTES = 1;
    private static final int CANCELLATION_HOURS_BEFORE_EVENT = 24;

    private final BookingRepository bookingRepository;
    private final UserService userService;
    private final EventService eventService;
    private final NotificationService notificationService;
    private final PaymentService paymentService;

    @Transactional
    public Booking add(BookingCreateRequest bookingCreateRequest, String username) {

        User user = userService.getByUsername(username);
        Event event = eventService.getById(bookingCreateRequest.getEventId());

        if (event.getAvailableTickets() < bookingCreateRequest.getNumberOfTickets()) {
            throw new IllegalStateException("Not enough tickets available for this event");
        }

        Booking booking = create(bookingCreateRequest);
        booking.setUser(user);
        booking.setEvent(event);

        BigDecimal totalAmount = event.getTicketPrice().multiply(BigDecimal.valueOf(bookingCreateRequest.getNumberOfTickets()));
        booking.setTotalAmount(totalAmount);

        event.setAvailableTickets(event.getAvailableTickets() - bookingCreateRequest.getNumberOfTickets());
        eventService.saveEvent(event);

        Booking saved = bookingRepository.save(booking);

        log.info("Booking (ID: [{}]) was successfully added.", saved.getId());

        return saved;
    }

    private static Booking create(BookingCreateRequest bookingCreateRequest) {
        Booking booking = new Booking();

        booking.setNumberOfTickets(bookingCreateRequest.getNumberOfTickets());
        booking.setCustomerEmail(bookingCreateRequest.getCustomerEmail());
        booking.setCustomerPhone(bookingCreateRequest.getCustomerPhone());
        booking.setSpecialRequests(bookingCreateRequest.getSpecialRequests());

        return booking;
    }

    @Transactional
    @CacheEvict(value = "bookings-by-user", allEntries = true)
    public void cancelBooking(UUID bookingId, String username) {
        Booking booking = getById(bookingId);

        if (!booking.getUser().getUsername().equals(username)) {
            throw new SecurityException("Cannot cancel someone else's booking!");
        }

        if (!canBeCancelled(booking)) {
            throw new IllegalStateException("Booking cannot be cancelled");
        }

        cancelBookingInternal(booking, "Cancelled by user");

        Event event = booking.getEvent();
        event.setAvailableTickets(event.getAvailableTickets() + booking.getNumberOfTickets());
        eventService.saveEvent(event);

        bookingRepository.save(booking);
    }

    protected void confirmBookingAfterPayment(UUID bookingId, UUID paymentId) {
        Booking booking = getById(bookingId);
        confirmBooking(booking, paymentId);
        bookingRepository.save(booking);
    }

    public Booking getById(UUID id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new BookingNotFoundException(ID_NOT_FOUND.formatted(ENTITY_NAME, id)));
    }

    @Transactional(readOnly = true)
    public List<Booking> getAllBookings() {
        return bookingRepository.findAllByOrderByBookingDateDesc();
    }

    @Transactional(readOnly = true)
    @Cacheable("bookings-by-user")
    public List<Booking> getBookingsForUser(String username) {
        return bookingRepository.findAllByUserUsernameOrderByBookingDateDesc(username);
    }

    @Transactional
    public void adminCancelBooking(UUID id) {
        Booking booking = getById(id);
        cancelBookingInternal(booking, "Cancelled by admin");
        bookingRepository.save(booking);

        Event event = booking.getEvent();
        event.setAvailableTickets(event.getAvailableTickets() + booking.getNumberOfTickets());
        eventService.saveEvent(event);
    }

    public void refundBooking(UUID bookingId) {
        Booking booking = getById(bookingId);
        User user = booking.getUser();
        Event event = booking.getEvent();

        if (booking.getStatus() == BookingStatus.REFUNDED) {
            log.warn("Booking {} is already refunded", bookingId);
            return;
        }

        if (booking.getPaymentId() == null) {
            throw new IllegalStateException("Cannot refund booking without payment ID");
        }

        try {
            paymentService.refundPayment(booking.getPaymentId(), booking.getTotalAmount());
        } catch (PaymentProcessingException | PaymentServiceUnavailableException e) {
            log.error("Refund failed for booking {}: {}", bookingId, e.getMessage());

            sendRefundFailedNotification(user, event);

            throw new IllegalStateException("Refund processing failed. Please contact support.");
        }

        updateBookingStatusToRefunded(bookingId);

        sendRefundSuccessNotification(user, event);

        log.info("Refund processed successfully for booking {}", bookingId);
    }

    protected void updateBookingStatusToRefunded(UUID bookingId) {
        Booking booking = getById(bookingId);
        booking.setStatus(BookingStatus.REFUNDED);
        bookingRepository.save(booking);
    }

    public int getCountTicketsByEventId(UUID id) {
        return this.bookingRepository.countByEventId(id);
    }

    public void markAsPaid(UUID bookingId, String username) {
        Booking booking = getById(bookingId);

        if (!booking.getUser().getUsername().equals(username)) {
            throw new UnauthorizedException("You are not authorized to pay for this booking");
        }

        if (booking.getPaymentId() != null) {
            log.warn("Booking {} already has payment ID {}", bookingId, booking.getPaymentId());
            throw new BookingAlreadyConfirmedException("This booking already has a payment");
        }

        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            log.info("Booking {} is already confirmed", bookingId);
            throw new BookingAlreadyConfirmedException("This booking is already confirmed");
        }

        User user = booking.getUser();
        Event event = booking.getEvent();
        BigDecimal amount = booking.getTotalAmount();

        PaymentResponse paymentResponse;
        try {
            paymentResponse = paymentService.processPayment(bookingId, user.getId(), amount);
        } catch (PaymentProcessingException e) {
            log.error("Payment processing failed for booking {}: {}", bookingId, e.getMessage());
            sendPaymentFailedNotification(user, event);
            throw new PaymentProcessingException("Payment processing failed. Please try again.");

        } catch (PaymentServiceUnavailableException e) {
            log.error("Payment service unavailable for booking {}: {}", bookingId, e.getMessage());
            throw new PaymentProcessingException("Payment service is temporarily unavailable. Please try again later.");
        }

        confirmBookingAfterPayment(bookingId, paymentResponse.getPaymentId());
        sendPaymentSuccessNotification(user, event);

        log.info("Payment confirmed for booking {} by user {}", bookingId, username);
    }

    public boolean hasUserBookedEvent(String username, UUID eventId) {
        return bookingRepository.existsByUserUsernameAndEventId(username, eventId);
    }

    public List<Booking> getExpiredPendingBookings() {
        LocalDateTime expirationThreshold = LocalDateTime.now().minusMinutes(BOOKING_EXPIRATION_MINUTES);

        return bookingRepository.findExpiredPendingBookings(BookingStatus.PENDING, expirationThreshold);
    }

    public void save(Booking booking) {
        this.bookingRepository.save(booking);
    }

    @Transactional
    public void autoCancelBooking(UUID bookingId, String reason) {
        Booking booking = getById(bookingId);
        cancelBookingInternal(booking, reason);
        bookingRepository.save(booking);

        Event event = booking.getEvent();
        event.setAvailableTickets(event.getAvailableTickets() + booking.getNumberOfTickets());
        eventService.saveEvent(event);
    }

    private boolean canBeCancelled(Booking booking) {
        return booking.getStatus() == BookingStatus.CONFIRMED
                && booking.getEvent().getStartDate().isAfter(LocalDateTime.now().plusHours(CANCELLATION_HOURS_BEFORE_EVENT));
    }

    private void cancelBookingInternal(Booking booking, String reason) {
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(LocalDateTime.now());
        booking.setCancellationReason(reason);
    }

    private void confirmBooking(Booking booking, UUID paymentId) {
        booking.setPaymentId(paymentId);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPaymentCompletedAt(LocalDateTime.now());
    }

    private void sendRefundSuccessNotification(User user, Event event) {
        if (user.isNotificationsEnabled()) {
            try {
                notificationService.sendIfEnabled(user,
                        "Refund Processed",
                        "Your payment for event '" + event.getTitle() + "' has been refunded successfully."
                );
            } catch (Exception e) {
                log.warn("Failed to send refund success notification for user {}: {}", user.getUsername(), e.getMessage());
            }
        }
    }

    private void sendRefundFailedNotification(User user, Event event) {
        if (user.isNotificationsEnabled()) {
            try {
                notificationService.sendIfEnabled(user,
                        "Refund Failed",
                        "We were unable to process your refund for event '" + event.getTitle() + "'. Please contact support."
                );
            } catch (Exception e) {
                log.warn("Failed to send refund failed notification for user {}: {}", user.getUsername(), e.getMessage());
            }
        }
    }

    private void sendPaymentSuccessNotification(User user, Event event) {
        if (user.isNotificationsEnabled()) {
            try {
                notificationService.sendIfEnabled(user,
                        "Payment Successful",
                        "Your payment for \"" + event.getTitle() + "\" was successful! Enjoy the event 🎉"
                );
            } catch (Exception e) {
                log.warn("Failed to send payment success notification for user {}: {}", user.getUsername(), e.getMessage());
            }
        }
    }

    private void sendPaymentFailedNotification(User user, Event event) {
        if (user.isNotificationsEnabled()) {
            try {
                notificationService.sendIfEnabled(user,
                        "Payment Failed",
                        "Your payment for \"" + event.getTitle() + "\" was not successful. Please try again."
                );
            } catch (Exception e) {
                log.warn("Failed to send payment failed notification for user {}: {}", user.getUsername(), e.getMessage());
            }
        }
    }

}
