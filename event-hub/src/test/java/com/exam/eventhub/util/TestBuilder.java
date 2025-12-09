package com.exam.eventhub.util;

import com.exam.eventhub.category.model.Category;
import com.exam.eventhub.contact.model.Contact;
import com.exam.eventhub.event.model.Event;
import com.exam.eventhub.event.model.EventStatus;
import com.exam.eventhub.user.model.User;
import com.exam.eventhub.venue.model.Venue;
import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@UtilityClass
public class TestBuilder {

    public static Category createCategory(UUID id, String name, String description, String color) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setDescription(description);
        category.setColor(color);
        return category;
    }

    public static Venue createVenue(UUID id, String name, String address, String city, Integer capacity, BigDecimal hourlyRate) {
        Venue venue = new Venue();
        venue.setId(id);
        venue.setName(name);
        venue.setAddress(address);
        venue.setCity(city);
        venue.setCapacity(capacity);
        venue.setHourlyRate(hourlyRate);
        venue.setContactEmail("info@venue.bg");
        venue.setContactPhone("+359888000000");
        venue.setDescription("Venue description");
        return venue;
    }

    public static Contact createContact(UUID id, String name, String email, String message) {
        Contact contact = new Contact();
        contact.setId(id);
        contact.setName(name);
        contact.setEmail(email);
        contact.setMessage(message);
        return contact;
    }

    public static Event createEvent(String title, Venue venue, User organizer, Category category) {
        Event event = new Event(
                title,
                "Description for " + title,
                LocalDateTime.now().plusDays(10),
                LocalDateTime.now().plusDays(10).plusHours(3),
                new BigDecimal("20.00"),
                100,
                venue, organizer, category
        );

        event.setStatus(EventStatus.PUBLISHED);
        return event;
    }
}