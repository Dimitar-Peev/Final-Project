package com.exam.eventhub.util;

import com.exam.eventhub.booking.model.Booking;
import com.exam.eventhub.booking.model.BookingStatus;
import com.exam.eventhub.event.model.Event;
import com.exam.eventhub.user.model.User;
import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@UtilityClass
public class BookingHelper {

    public static User createUser(UUID id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setNotificationsEnabled(false);
        return user;
    }

    public static Event createEvent(UUID id, String title, BigDecimal ticketPrice, int maxCapacity, int availableTickets) {
        Event event = new Event();
        event.setId(id);
        event.setTitle(title);
        event.setTicketPrice(ticketPrice);
        event.setMaxCapacity(maxCapacity);
        event.setAvailableTickets(availableTickets);
        event.setStartDate(LocalDateTime.now().plusDays(30));
        return event;
    }

    public static Booking createBooking(UUID id, User user, Event event, int numberOfTickets, BigDecimal totalAmount) {
        Booking booking = new Booking();
        booking.setId(id);
        booking.setUser(user);
        booking.setEvent(event);
        booking.setNumberOfTickets(numberOfTickets);
        booking.setTotalAmount(totalAmount);
        booking.setStatus(BookingStatus.PENDING);
        booking.setBookingDate(LocalDateTime.now());
        booking.setCustomerEmail("customer@example.com");
        booking.setCustomerPhone("+359888123456");
        return booking;
    }
}