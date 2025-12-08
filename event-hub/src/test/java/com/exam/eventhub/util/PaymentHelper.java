package com.exam.eventhub.util;

import com.exam.eventhub.booking.model.Booking;
import com.exam.eventhub.booking.model.BookingStatus;
import com.exam.eventhub.category.model.Category;
import com.exam.eventhub.event.model.Event;
import com.exam.eventhub.event.model.EventStatus;
import com.exam.eventhub.payment.client.dto.PaymentResponse;
import com.exam.eventhub.user.model.Role;
import com.exam.eventhub.user.model.User;
import com.exam.eventhub.venue.model.Venue;
import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@UtilityClass
public class PaymentHelper {

    public static PaymentResponse createPaymentResponse(UUID paymentId, String status, String message) {
        PaymentResponse response = new PaymentResponse();
        response.setPaymentId(paymentId);
        response.setStatus(status);
        response.setMessage(message);
        return response;
    }

    public static User createUser(UUID id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(username + "@test.com");
        user.setPassword("password");
        user.setRole(Role.USER);
        user.setBlocked(false);
        user.setNotificationsEnabled(true);
        return user;
    }

    public static Event createEvent(UUID id, String title) {
        Event event = new Event();
        event.setId(id);
        event.setTitle(title);
        event.setDescription("Test event description");
        event.setStartDate(LocalDateTime.now().plusDays(30));
        event.setEndDate(LocalDateTime.now().plusDays(30).plusHours(3));
        event.setTicketPrice(new BigDecimal("50.00"));
        event.setMaxCapacity(100);
        event.setAvailableTickets(100);
        event.setStatus(EventStatus.PUBLISHED);

        Category category = new Category();
        category.setId(UUID.randomUUID());
        category.setName("Music");
        event.setCategory(category);

        Venue venue = new Venue();
        venue.setId(UUID.randomUUID());
        venue.setName("Test Venue");
        event.setVenue(venue);

        User organizer = createUser(UUID.randomUUID(), "organizer");
        event.setOrganizer(organizer);

        return event;
    }

    public static Booking createBooking(UUID id, User user, Event event) {
        Booking booking = new Booking();
        booking.setId(id);
        booking.setUser(user);
        booking.setEvent(event);
        booking.setNumberOfTickets(2);
        booking.setTotalAmount(new BigDecimal("100.00"));
        booking.setStatus(BookingStatus.PENDING);
        booking.setBookingDate(LocalDateTime.now());
        return booking;
    }
}
