package com.exam.eventhub.util;

import com.exam.eventhub.booking.model.Booking;
import com.exam.eventhub.booking.model.BookingStatus;
import com.exam.eventhub.category.model.Category;
import com.exam.eventhub.contact.model.Contact;
import com.exam.eventhub.event.model.Event;
import com.exam.eventhub.user.model.Role;
import com.exam.eventhub.user.model.User;
import com.exam.eventhub.venue.model.Venue;
import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@UtilityClass
public class ApiHelper {

    public static User createMockUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("testUser");
        user.setEmail("test@example.com");
        user.setPassword("encodedPassword");
        user.setRole(Role.USER);
        user.setBlocked(false);
        user.setFirstName("Test");
        user.setLastName("User");
        user.setProfileImageUrl("https://example.com/profile.jpg");
        return user;
    }

    public static Category createMockCategory(String name, String description) {
        Category category = new Category();
        category.setId(UUID.randomUUID());
        category.setName(name);
        category.setDescription(description);
        return category;
    }

    public static Contact createMockContact(String name, String email, String message) {
        Contact contact = new Contact();
        contact.setId(UUID.randomUUID());
        contact.setName(name);
        contact.setEmail(email);
        contact.setMessage(message);
        contact.setCreatedAt(LocalDateTime.now());
        return contact;
    }

    public static Venue createMockVenue(String name, String description) {
        Venue venue = new Venue();
        venue.setId(UUID.randomUUID());
        venue.setName(name);
        venue.setDescription(description);
        return venue;
    }

    public static Booking createMockBooking(String eventTitle) {
        Booking booking = new Booking();
        booking.setId(UUID.randomUUID());
        booking.setNumberOfTickets(2);
        booking.setTotalAmount(new BigDecimal("100.0"));
        booking.setBookingDate(LocalDateTime.now());
        booking.setStatus(BookingStatus.CONFIRMED);

        Event event = new Event();
        event.setId(UUID.randomUUID());
        event.setTitle(eventTitle);
        event.setDescription("Event description");
        event.setStartDate(LocalDateTime.now().plusDays(7));
        event.setEndDate(LocalDateTime.now().plusDays(7).plusHours(3));

        Venue venue = new Venue();
        venue.setId(UUID.randomUUID());
        venue.setName("Test Venue");
        venue.setCity("Sofia");
        venue.setAddress("Test Address 123");
        venue.setCapacity(1000);
        event.setVenue(venue);

        Category category = new Category();
        category.setId(UUID.randomUUID());
        category.setName("Music");
        category.setDescription("Music events");
        event.setCategory(category);

        User organizer = new User();
        organizer.setId(UUID.randomUUID());
        organizer.setUsername("organizer");
        organizer.setRole(Role.EVENT_ORGANIZER);
        event.setOrganizer(organizer);

        booking.setEvent(event);

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("testUser");
        user.setRole(Role.USER);
        booking.setUser(user);

        return booking;
    }

    public static Event createMockEvent(String title, String description) {
        Event event = new Event();
        event.setId(UUID.randomUUID());
        event.setTitle(title);
        event.setDescription(description);
        event.setStartDate(LocalDateTime.now().plusDays(7));
        event.setEndDate(LocalDateTime.now().plusDays(7).plusHours(3));
        event.setMaxCapacity(100);
        event.setTicketPrice(BigDecimal.valueOf(50.0));

        Category category = new Category();
        category.setId(UUID.randomUUID());
        category.setName("Music");
        category.setDescription("Music events");
        event.setCategory(category);

        Venue venue = new Venue();
        venue.setId(UUID.randomUUID());
        venue.setName("Test Venue");
        venue.setCity("Sofia");
        venue.setAddress("Test Address 123");
        venue.setCapacity(1000);
        event.setVenue(venue);

        User organizer = new User();
        organizer.setId(UUID.randomUUID());
        organizer.setUsername("organizer");
        organizer.setRole(Role.EVENT_ORGANIZER);
        event.setOrganizer(organizer);

        return event;
    }
}