package com.exam.eventhub.booking.service;

import com.exam.eventhub.booking.model.Booking;
import com.exam.eventhub.booking.model.BookingStatus;
import com.exam.eventhub.booking.repository.BookingRepository;
import com.exam.eventhub.event.model.Event;
import com.exam.eventhub.event.service.EventService;
import com.exam.eventhub.exception.BookingNotFoundException;
import com.exam.eventhub.exception.PaymentProcessingException;
import com.exam.eventhub.exception.PaymentServiceUnavailableException;
import com.exam.eventhub.notification.service.NotificationService;
import com.exam.eventhub.payment.client.dto.PaymentResponse;
import com.exam.eventhub.payment.service.PaymentService;
import com.exam.eventhub.user.model.User;
import com.exam.eventhub.user.service.UserService;
import com.exam.eventhub.web.dto.BookingCreateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.exam.eventhub.common.Constants.ID_NOT_FOUND;
import static com.exam.eventhub.util.BookingHelper.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BookingServiceUTest {

    private static final String ENTITY_NAME = "Booking";

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private UserService userService;
    @Mock
    private EventService eventService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private BookingService bookingService;

    private Booking booking1;
    private Booking booking2;

    @BeforeEach
    void setUp() {
        booking1 = createBooking(UUID.randomUUID(), null, null, 2, new BigDecimal("100.00"));
        booking2 = createBooking(UUID.randomUUID(), null, null, 3, new BigDecimal("150.00"));
    }

    @Test
    void add_whenEnoughTicketsAvailable_shouldCreateBooking() {

        String username = "user";
        UUID eventId = UUID.randomUUID();
        int availableTickets = 100;
        int numberOfTickets = 2;

        BookingCreateRequest request = new BookingCreateRequest();
        request.setEventId(eventId);
        request.setNumberOfTickets(numberOfTickets);
        request.setCustomerEmail("user@example.com");
        request.setCustomerPhone("+359888123456");
        request.setSpecialRequests("Window seat");

        User user = createUser(UUID.randomUUID(), username);
        Event event = createEvent(UUID.randomUUID(), "Event", new BigDecimal("50.00"), 100, availableTickets);

        when(userService.getByUsername(username)).thenReturn(user);
        when(eventService.getById(eventId)).thenReturn(event);

        Booking savedBooking = createBooking(UUID.randomUUID(), user, event, numberOfTickets, new BigDecimal("100.00"));
        when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);

        Booking result = bookingService.add(request, username);

        assertNotNull(result);
        assertEquals(user, result.getUser());
        assertEquals(event, result.getEvent());
        assertEquals(savedBooking.getNumberOfTickets(), result.getNumberOfTickets());
        assertEquals(savedBooking.getTotalAmount(), result.getTotalAmount());
        assertEquals(availableTickets - numberOfTickets, event.getAvailableTickets());

        verify(userService).getByUsername(username);
        verify(eventService).getById(eventId);
        verify(eventService).saveEvent(event);
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    void add_whenNotEnoughTickets_shouldThrowException() {

        String username = "user";
        UUID eventId = UUID.randomUUID();
        int numberOfTickets = 10;

        BookingCreateRequest request = new BookingCreateRequest();
        request.setEventId(eventId);
        request.setNumberOfTickets(numberOfTickets);

        User user = createUser(UUID.randomUUID(), username);
        Event event = createEvent(UUID.randomUUID(), "Event", new BigDecimal("50.00"), 100, 5);

        when(userService.getByUsername(username)).thenReturn(user);
        when(eventService.getById(eventId)).thenReturn(event);

        IllegalStateException exception =
                assertThrows(IllegalStateException.class, () -> bookingService.add(request, username));
        assertTrue(exception.getMessage().contains("Not enough tickets available for this event"));

        verify(bookingRepository, never()).save(any(Booking.class));
        verify(eventService, never()).saveEvent(any(Event.class));
    }

    @Test
    void cancelBooking_whenUserOwnsBookingAndCanBeCancelled_shouldCancelSuccessfully() {

        UUID bookingId = UUID.randomUUID();
        String username = "user";

        User user = createUser(UUID.randomUUID(), username);
        Event event = createEvent(UUID.randomUUID(), "Event", new BigDecimal("50.00"), 100, 98);
        event.setStartDate(LocalDateTime.now().plusDays(5));

        Booking booking = createBooking(bookingId, user, event, 2, new BigDecimal("100.00"));
        booking.setStatus(BookingStatus.CONFIRMED);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        bookingService.cancelBooking(bookingId, username);

        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        assertNotNull(booking.getCancelledAt());
        assertEquals("Cancelled by user", booking.getCancellationReason());
        assertEquals(100, event.getAvailableTickets());

        verify(eventService).saveEvent(event);
        verify(bookingRepository).save(booking);
    }

    @Test
    void cancelBooking_whenUserDoesNotOwnBooking_shouldThrowSecurityException() {

        UUID bookingId = UUID.randomUUID();
        String username = "user";
        String ownerUsername = "owner";

        User owner = createUser(UUID.randomUUID(), ownerUsername);
        Event event = createEvent(UUID.randomUUID(), "Event", new BigDecimal("50.00"), 100, 98);

        Booking booking = createBooking(bookingId, owner, event, 2, new BigDecimal("100.00"));

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        SecurityException exception =
                assertThrows(SecurityException.class, () -> bookingService.cancelBooking(bookingId, username));
        assertTrue(exception.getMessage().contains("Cannot cancel someone else's booking!"));

        verify(bookingRepository, never()).save(any(Booking.class));
        verify(eventService, never()).saveEvent(any(Event.class));
    }

    @Test
    void cancelBooking_whenBookingCannotBeCancelled_shouldThrowException() {

        UUID bookingId = UUID.randomUUID();
        String username = "user";

        User user = createUser(UUID.randomUUID(), username);
        Event event = createEvent(UUID.randomUUID(), "Event", new BigDecimal("50.00"), 100, 98);
        event.setStartDate(LocalDateTime.now().plusHours(12));

        Booking booking = createBooking(bookingId, user, event, 2, new BigDecimal("100.00"));
        booking.setStatus(BookingStatus.CONFIRMED);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        IllegalStateException exception =
                assertThrows(IllegalStateException.class, () -> bookingService.cancelBooking(bookingId, username));
        assertTrue(exception.getMessage().contains("Booking cannot be cancelled"));

        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void cancelBooking_whenBookingNotConfirmed_shouldThrowException() {

        UUID bookingId = UUID.randomUUID();
        String username = "user";

        User user = createUser(UUID.randomUUID(), username);
        Event event = createEvent(UUID.randomUUID(), "Event", new BigDecimal("50.00"), 100, 98);
        event.setStartDate(LocalDateTime.now().plusDays(5));

        Booking booking = createBooking(bookingId, user, event, 2, new BigDecimal("100.00"));
        booking.setStatus(BookingStatus.PENDING); // not CONFIRMED

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        IllegalStateException exception =
                assertThrows(IllegalStateException.class, () -> bookingService.cancelBooking(bookingId, username));
        assertTrue(exception.getMessage().contains("Booking cannot be cancelled"));

        verify(bookingRepository, never()).save(any(Booking.class));
        verify(eventService, never()).saveEvent(any(Event.class));
    }

    @Test
    void confirmBookingAfterPayment_shouldUpdateBookingStatus() {

        UUID bookingId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();

        User user = createUser(UUID.randomUUID(), "user");
        Event event = createEvent(UUID.randomUUID(), "Event", new BigDecimal("50.00"), 100, 100);

        Booking booking = createBooking(bookingId, user, event, 2, new BigDecimal("100.00"));
        booking.setStatus(BookingStatus.PENDING);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        bookingService.confirmBookingAfterPayment(bookingId, paymentId);

        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
        assertEquals(paymentId, booking.getPaymentId());
        assertNotNull(booking.getPaymentCompletedAt());

        verify(bookingRepository).save(booking);
    }

    @Test
    void getById_whenBookingDoesNotExist_shouldThrowException() {

        UUID bookingId = UUID.randomUUID();
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.empty());

        BookingNotFoundException exception =
                assertThrows(BookingNotFoundException.class, () -> bookingService.getById(bookingId));
        assertTrue(exception.getMessage().contains(ID_NOT_FOUND.formatted(ENTITY_NAME, bookingId)));

        verify(bookingRepository).findById(bookingId);
    }

    @Test
    void getAllBookings_shouldReturnAllBookings() {

        List<Booking> expectedBookings = List.of(booking1, booking2);
        when(bookingRepository.findAllByOrderByBookingDateDesc()).thenReturn(expectedBookings);

        List<Booking> actualBookings = bookingService.getAllBookings();

        assertEquals(expectedBookings, actualBookings);
        verify(bookingRepository).findAllByOrderByBookingDateDesc();
    }

    @Test
    void getBookingsForUser_shouldReturnUserBookings() {

        String username = "user";

        List<Booking> expectedBookings = List.of(booking1, booking2);
        when(bookingRepository.findAllByUserUsernameOrderByBookingDateDesc(username)).thenReturn(expectedBookings);

        List<Booking> actualBookings = bookingService.getBookingsForUser(username);

        assertEquals(expectedBookings, actualBookings);
        verify(bookingRepository).findAllByUserUsernameOrderByBookingDateDesc(username);
    }

    @Test
    void adminCancelBooking_shouldCancelAndReturnTickets() {

        UUID bookingId = UUID.randomUUID();

        User user = createUser(UUID.randomUUID(), "user");
        Event event = createEvent(UUID.randomUUID(), "Event", new BigDecimal("50.00"), 100, 95);

        Booking booking = createBooking(bookingId, user, event, 5, new BigDecimal("250.00"));
        booking.setStatus(BookingStatus.CONFIRMED);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        bookingService.adminCancelBooking(bookingId);

        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        assertEquals("Cancelled by admin", booking.getCancellationReason());
        assertNotNull(booking.getCancelledAt());
        assertEquals(100, event.getAvailableTickets());

        verify(bookingRepository).save(booking);
        verify(eventService).saveEvent(event);
    }

    @Test
    void refundBooking_whenRefundSuccessful_shouldUpdateStatusAndNotify() {

        UUID bookingId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();

        User user = createUser(UUID.randomUUID(), "user");
        user.setNotificationsEnabled(true);
        Event event = createEvent(UUID.randomUUID(), "Event", new BigDecimal("50.00"), 100, 100);

        Booking booking = createBooking(bookingId, user, event, 2, new BigDecimal("100.00"));
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setPaymentId(paymentId);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        bookingService.refundBooking(bookingId);

        verify(paymentService).refundPayment(paymentId, booking.getTotalAmount());
        verify(bookingRepository, times(2)).findById(bookingId);
        verify(notificationService).sendIfEnabled(eq(user), eq("Refund Processed"), anyString());
    }

    @Test
    void refundBooking_whenAlreadyRefunded_shouldReturnWithoutProcessing() {

        UUID bookingId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();

        User user = createUser(UUID.randomUUID(), "user");
        Event event = createEvent(UUID.randomUUID(), "Event", new BigDecimal("50.00"), 100, 100);

        Booking booking = createBooking(bookingId, user, event, 2, new BigDecimal("100.00"));
        booking.setStatus(BookingStatus.REFUNDED);
        booking.setPaymentId(paymentId);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        bookingService.refundBooking(bookingId);

        verify(paymentService, never()).refundPayment(any(), any());
        verify(bookingRepository, times(1)).findById(bookingId);
    }

    @Test
    void refundBooking_whenNoPaymentId_shouldThrowException() {

        UUID bookingId = UUID.randomUUID();

        User user = createUser(UUID.randomUUID(), "user");
        Event event = createEvent(UUID.randomUUID(), "Event", new BigDecimal("50.00"), 100, 100);

        Booking booking = createBooking(bookingId, user, event, 2, new BigDecimal("100.00"));
        booking.setStatus(BookingStatus.PENDING);
        booking.setPaymentId(null);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        IllegalStateException exception =
                assertThrows(IllegalStateException.class, () -> bookingService.refundBooking(bookingId));
        assertTrue(exception.getMessage().contains("Cannot refund booking without payment ID"));

        verify(paymentService, never()).refundPayment(any(), any());
    }

    @Test
    void refundBooking_whenPaymentServiceFails_shouldThrowExceptionAndNotify() {

        UUID bookingId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();

        User user = createUser(UUID.randomUUID(), "user");
        user.setNotificationsEnabled(true);
        Event event = createEvent(UUID.randomUUID(), "Event", new BigDecimal("50.00"), 100, 100);

        Booking booking = createBooking(bookingId, user, event, 2, new BigDecimal("100.00"));
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setPaymentId(paymentId);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        doThrow(new PaymentProcessingException("Refund failed"))
                .when(paymentService).refundPayment(paymentId, booking.getTotalAmount());

        IllegalStateException exception =
                assertThrows(IllegalStateException.class, () -> bookingService.refundBooking(bookingId));
        assertTrue(exception.getMessage().contains("Refund processing failed"));

        verify(notificationService).sendIfEnabled(eq(user), eq("Refund Failed"), anyString());
        verify(bookingRepository, times(1)).findById(bookingId);
    }

    @Test
    void refundBooking_whenNotificationServiceThrowsException_shouldStillProcessRefund() {

        UUID bookingId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();

        User user = createUser(UUID.randomUUID(), "user");
        user.setNotificationsEnabled(true);
        Event event = createEvent(UUID.randomUUID(), "Event", new BigDecimal("50.00"), 100, 100);

        Booking booking = createBooking(bookingId, user, event, 2, new BigDecimal("100.00"));
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setPaymentId(paymentId);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        doThrow(new RuntimeException("Notification failed"))
                .when(notificationService).sendIfEnabled(any(), eq("Refund Processed"), anyString());

        bookingService.refundBooking(bookingId);

        verify(paymentService).refundPayment(paymentId, booking.getTotalAmount());
        verify(notificationService).sendIfEnabled(eq(user), eq("Refund Processed"), anyString());
        verify(bookingRepository, times(2)).findById(bookingId);
    }

    @Test
    void refundBooking_whenNotificationDisabled_shouldNotSendNotification() {

        UUID bookingId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();

        User user = createUser(UUID.randomUUID(), "user");
        user.setNotificationsEnabled(false);
        Event event = createEvent(UUID.randomUUID(), "Event", new BigDecimal("50.00"), 100, 100);

        Booking booking = createBooking(bookingId, user, event, 2, new BigDecimal("100.00"));
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setPaymentId(paymentId);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        bookingService.refundBooking(bookingId);

        verify(paymentService).refundPayment(paymentId, booking.getTotalAmount());
        verify(notificationService, never()).sendIfEnabled(any(), any(), any());
    }

    @Test
    void refundBooking_whenPaymentFailsAndNotificationThrowsException_shouldStillThrowOriginalException() {

        UUID bookingId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();

        User user = createUser(UUID.randomUUID(), "user");
        user.setNotificationsEnabled(true);
        Event event = createEvent(UUID.randomUUID(), "Art Gallery", new BigDecimal("30.00"), 80, 80);

        Booking booking = createBooking(bookingId, user, event, 2, new BigDecimal("60.00"));
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setPaymentId(paymentId);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        doThrow(new PaymentProcessingException("Refund failed"))
                .when(paymentService).refundPayment(paymentId, booking.getTotalAmount());

        doThrow(new RuntimeException("Email service down"))
                .when(notificationService).sendIfEnabled(eq(user), eq("Refund Failed"), anyString());

        IllegalStateException exception =
                assertThrows(IllegalStateException.class, () -> bookingService.refundBooking(bookingId));
        assertTrue(exception.getMessage().contains("Refund processing failed"));

        verify(notificationService).sendIfEnabled(eq(user), eq("Refund Failed"), anyString());
        verify(bookingRepository, times(1)).findById(bookingId);
    }

    @Test
    void refundBooking_whenPaymentFailsAndNotificationDisabled_shouldThrowExceptionAndNotNotify() {

        UUID bookingId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();

        User user = createUser(UUID.randomUUID(), "user");
        user.setNotificationsEnabled(false);
        Event event = createEvent(UUID.randomUUID(), "Event", new BigDecimal("50.00"), 100, 100);

        Booking booking = createBooking(bookingId, user, event, 2, new BigDecimal("100.00"));
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setPaymentId(paymentId);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        doThrow(new PaymentProcessingException("Refund failed"))
                .when(paymentService).refundPayment(paymentId, booking.getTotalAmount());

        IllegalStateException exception =
                assertThrows(IllegalStateException.class, () -> bookingService.refundBooking(bookingId));
        assertTrue(exception.getMessage().contains("Refund processing failed"));

        verify(notificationService, never()).sendIfEnabled(any(), any(), any());
        verify(bookingRepository, times(1)).findById(bookingId);
    }

    @Test
    void getCountTicketsByEventId_shouldReturnTicketCount() {

        UUID eventId = UUID.randomUUID();
        when(bookingRepository.countByEventId(eventId)).thenReturn(150);

        int count = bookingService.getCountTicketsByEventId(eventId);

        assertEquals(150, count);
        verify(bookingRepository).countByEventId(eventId);
    }

    @Test
    void markAsPaid_whenAlreadyPaid_shouldReturnWithoutProcessing() {

        UUID bookingId = UUID.randomUUID();
        UUID existingPaymentId = UUID.randomUUID();

        User user = createUser(UUID.randomUUID(), "user");
        Event event = createEvent(UUID.randomUUID(), "Event", new BigDecimal("50.00"), 100, 100);

        Booking booking = createBooking(bookingId, user, event, 2, new BigDecimal("100.00"));
        booking.setPaymentId(existingPaymentId);
        booking.setStatus(BookingStatus.CONFIRMED);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        bookingService.markAsPaid(bookingId);

        verify(paymentService, never()).processPayment(any(), any(), any());
        verify(bookingRepository, times(1)).findById(bookingId);
    }

    @Test
    void markAsPaid_whenBookingAlreadyConfirmed_shouldReturnWithoutProcessing() {

        UUID bookingId = UUID.randomUUID();

        User user = createUser(UUID.randomUUID(), "user");
        Event event = createEvent(UUID.randomUUID(), "Event", new BigDecimal("50.00"), 100, 100);

        Booking booking = createBooking(bookingId, user, event, 2, new BigDecimal("100.00"));
        booking.setPaymentId(null);
        booking.setStatus(BookingStatus.CONFIRMED);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        bookingService.markAsPaid(bookingId);

        verify(paymentService, never()).processPayment(any(), any(), any());
        verify(bookingRepository, times(1)).findById(bookingId);
    }

    @Test
    void markAsPaid_whenPaymentSuccessful_shouldConfirmBookingAndNotify() {

        UUID bookingId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();

        User user = createUser(userId, "user");
        user.setNotificationsEnabled(true);
        Event event = createEvent(UUID.randomUUID(), "Event", new BigDecimal("50.00"), 100, 100);

        Booking booking = createBooking(bookingId, user, event, 2, new BigDecimal("100.00"));
        booking.setStatus(BookingStatus.PENDING);

        PaymentResponse paymentResponse = new PaymentResponse();
        paymentResponse.setPaymentId(paymentId);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(paymentService.processPayment(bookingId, userId, booking.getTotalAmount())).thenReturn(paymentResponse);

        bookingService.markAsPaid(bookingId);

        verify(paymentService).processPayment(bookingId, userId, booking.getTotalAmount());
        verify(bookingRepository, times(2)).findById(bookingId);
        verify(notificationService).sendIfEnabled(eq(user), eq("Payment Successful"), anyString());
    }

    @Test
    void markAsPaid_whenPaymentFails_shouldThrowExceptionAndNotify() {

        UUID bookingId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        User user = createUser(userId, "user");
        user.setNotificationsEnabled(true);
        Event event = createEvent(UUID.randomUUID(), "Event", new BigDecimal("50.00"), 100, 100);

        Booking booking = createBooking(bookingId, user, event, 2, new BigDecimal("100.00"));
        booking.setStatus(BookingStatus.PENDING);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(paymentService.processPayment(bookingId, userId, booking.getTotalAmount()))
                .thenThrow(new PaymentProcessingException("Payment declined"));

        IllegalStateException exception =
                assertThrows(IllegalStateException.class, () -> bookingService.markAsPaid(bookingId));
        assertTrue(exception.getMessage().contains("Payment processing failed"));

        verify(notificationService).sendIfEnabled(eq(user), eq("Payment Failed"), anyString());
        verify(bookingRepository, times(1)).findById(bookingId);
    }

    @Test
    void markAsPaid_whenPaymentServiceUnavailable_shouldThrowException() {

        UUID bookingId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        User user = createUser(userId, "user");
        Event event = createEvent(UUID.randomUUID(), "Event", new BigDecimal("50.00"), 100, 100);

        Booking booking = createBooking(bookingId, user, event, 2, new BigDecimal("100.00"));
        booking.setStatus(BookingStatus.PENDING);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(paymentService.processPayment(bookingId, userId, booking.getTotalAmount()))
                .thenThrow(new PaymentServiceUnavailableException("Service down"));

        IllegalStateException exception =
                assertThrows(IllegalStateException.class, () -> bookingService.markAsPaid(bookingId));
        assertTrue(exception.getMessage().contains("Payment service is temporarily unavailable"));
    }

    @Test
    void markAsPaid_whenNotificationDisabled_shouldNotSendNotification() {

        UUID bookingId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();

        User user = createUser(userId, "user");
        user.setNotificationsEnabled(false);
        Event event = createEvent(UUID.randomUUID(), "Event", new BigDecimal("50.00"), 100, 100);

        Booking booking = createBooking(bookingId, user, event, 2, new BigDecimal("100.00"));
        booking.setStatus(BookingStatus.PENDING);

        PaymentResponse paymentResponse = new PaymentResponse();
        paymentResponse.setPaymentId(paymentId);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(paymentService.processPayment(bookingId, userId, booking.getTotalAmount())).thenReturn(paymentResponse);

        bookingService.markAsPaid(bookingId);

        verify(paymentService).processPayment(bookingId, userId, booking.getTotalAmount());
        verify(notificationService, never()).sendIfEnabled(any(), any(), any());
    }

    @Test
    void markAsPaid_whenNotificationServiceThrowsException_shouldStillProcessPayment() {

        UUID bookingId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();

        User user = createUser(userId, "user");
        user.setNotificationsEnabled(true);
        Event event = createEvent(UUID.randomUUID(), "Event", new BigDecimal("50.00"), 100, 100);

        Booking booking = createBooking(bookingId, user, event, 2, new BigDecimal("100.00"));
        booking.setStatus(BookingStatus.PENDING);

        PaymentResponse paymentResponse = new PaymentResponse();
        paymentResponse.setPaymentId(paymentId);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(paymentService.processPayment(bookingId, userId, booking.getTotalAmount())).thenReturn(paymentResponse);

        doThrow(new RuntimeException("Notification failed"))
                .when(notificationService).sendIfEnabled(any(), eq("Payment Successful"), anyString());

        bookingService.markAsPaid(bookingId);

        verify(paymentService).processPayment(bookingId, userId, booking.getTotalAmount());
        verify(notificationService).sendIfEnabled(eq(user), eq("Payment Successful"), anyString());
        verify(bookingRepository, times(2)).findById(bookingId);
    }

    @Test
    void markAsPaid_whenPaymentFailsAndNotificationThrowsException_shouldStillThrowOriginalException() {

        UUID bookingId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        User user = createUser(userId, "user");
        user.setNotificationsEnabled(true);
        Event event = createEvent(UUID.randomUUID(), "Marathon", new BigDecimal("15.00"), 500, 500);

        Booking booking = createBooking(bookingId, user, event, 1, new BigDecimal("15.00"));
        booking.setStatus(BookingStatus.PENDING);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(paymentService.processPayment(bookingId, userId, booking.getTotalAmount()))
                .thenThrow(new PaymentProcessingException("Insufficient funds"));

        doThrow(new RuntimeException("SMS gateway error"))
                .when(notificationService).sendIfEnabled(eq(user), eq("Payment Failed"), anyString());

        IllegalStateException exception =
                assertThrows(IllegalStateException.class, () -> bookingService.markAsPaid(bookingId));
        assertTrue(exception.getMessage().contains("Payment processing failed"));

        verify(notificationService).sendIfEnabled(eq(user), eq("Payment Failed"), anyString());
        verify(bookingRepository, times(1)).findById(bookingId);
    }

    @Test
    void markAsPaid_whenPaymentFailsAndNotificationDisabled_shouldThrowExceptionAndNotNotify() {

        UUID bookingId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        User user = createUser(userId, "user");
        user.setNotificationsEnabled(false);
        Event event = createEvent(UUID.randomUUID(), "Event", new BigDecimal("50.00"), 100, 100);

        Booking booking = createBooking(bookingId, user, event, 2, new BigDecimal("100.00"));
        booking.setStatus(BookingStatus.PENDING);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(paymentService.processPayment(bookingId, userId, booking.getTotalAmount()))
                .thenThrow(new PaymentProcessingException("Payment declined"));

        IllegalStateException exception =
                assertThrows(IllegalStateException.class, () -> bookingService.markAsPaid(bookingId));
        assertTrue(exception.getMessage().contains("Payment processing failed"));

        verify(notificationService, never()).sendIfEnabled(any(), any(), any());
        verify(bookingRepository, times(1)).findById(bookingId);
    }

    @Test
    void hasUserBookedEvent_whenUserHasBooked_shouldReturnTrue() {

        String username = "user";
        UUID eventId = UUID.randomUUID();

        when(bookingRepository.existsByUserUsernameAndEventId(username, eventId)).thenReturn(true);

        boolean result = bookingService.hasUserBookedEvent(username, eventId);

        assertTrue(result);
        verify(bookingRepository).existsByUserUsernameAndEventId(username, eventId);
    }

    @Test
    void getExpiredPendingBookings_shouldReturnExpiredBookings() {

        List<Booking> expiredBookings = List.of(booking1, booking2);

        when(bookingRepository.findExpiredPendingBookings(eq(BookingStatus.PENDING), any(LocalDateTime.class)))
                .thenReturn(expiredBookings);

        List<Booking> result = bookingService.getExpiredPendingBookings();

        assertEquals(expiredBookings, result);
        verify(bookingRepository).findExpiredPendingBookings(eq(BookingStatus.PENDING), any(LocalDateTime.class));
    }

    @Test
    void save_shouldSaveBooking() {

        Booking booking = createBooking(UUID.randomUUID(), null, null, 2, new BigDecimal("100.00"));

        bookingService.save(booking);

        verify(bookingRepository).save(booking);
    }

    @Test
    void autoCancelBooking_shouldCancelBookingAndReturnTickets() {

        UUID bookingId = UUID.randomUUID();
        String reason = "Booking expired";

        User user = createUser(UUID.randomUUID(), "user");
        Event event = createEvent(UUID.randomUUID(), "Event", new BigDecimal("50.00"), 100, 97);

        Booking booking = createBooking(bookingId, user, event, 3, new BigDecimal("150.00"));
        booking.setStatus(BookingStatus.PENDING);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        bookingService.autoCancelBooking(bookingId, reason);

        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        assertEquals(reason, booking.getCancellationReason());
        assertNotNull(booking.getCancelledAt());
        assertEquals(100, event.getAvailableTickets());

        verify(bookingRepository).save(booking);
        verify(eventService).saveEvent(event);
    }
}
