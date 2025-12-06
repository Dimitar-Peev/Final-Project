package com.exam.eventhub.util;

import com.exam.eventhub.category.model.Category;
import com.exam.eventhub.event.model.Event;
import com.exam.eventhub.event.model.EventStatus;
import com.exam.eventhub.user.model.Role;
import com.exam.eventhub.user.model.User;
import com.exam.eventhub.venue.model.Venue;
import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@UtilityClass
public class EventHelper {

    public static Event createEvent(UUID id, String title, User organizer, Venue venue, Category category, Integer maxCapacity) {
        Event event = new Event();
        event.setId(id);
        event.setTitle(title);
        event.setDescription("Description");
        event.setStartDate(LocalDateTime.now().plusDays(10));
        event.setEndDate(LocalDateTime.now().plusDays(10).plusHours(5));
        event.setTicketPrice(new BigDecimal("50.00"));
        event.setMaxCapacity(maxCapacity);
        event.setAvailableTickets(maxCapacity);
        event.setStatus(EventStatus.DRAFT);
        event.setOrganizer(organizer);
        event.setVenue(venue);
        event.setCategory(category);
        return event;
    }

    public static User createUser(UUID id, String username, Role role) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setRole(role);
        return user;
    }

    public static Venue createVenue(UUID id, String name) {
        Venue venue = new Venue();
        venue.setId(id);
        venue.setName(name);
        venue.setAddress("Address");
        venue.setCity("City");
        return venue;
    }

    public static Category createCategory(UUID id, String name) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setDescription("Description");
        category.setColor("#000000");
        return category;
    }
}
