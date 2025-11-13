package com.exam.eventhub.web.mapper;

import com.exam.eventhub.category.model.Category;
import com.exam.eventhub.event.model.Event;
import com.exam.eventhub.user.model.User;
import com.exam.eventhub.venue.model.Venue;
import com.exam.eventhub.web.dto.CategoryEditRequest;
import com.exam.eventhub.web.dto.EventEditRequest;
import com.exam.eventhub.web.dto.UserEditRequest;
import com.exam.eventhub.web.dto.VenueEditRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class DtoMapperUTest {

    @Test
    void givenHappyPath_whenMappingCategoryToCategoryEditRequest() {
        // Given
        Category category = new Category();
        category.setId(UUID.randomUUID());
        category.setName("Test Category");
        category.setDescription("Test Description");
        category.setColor("#000000");

        // When
        CategoryEditRequest resultDto = DtoMapper.mapCategoryToCategoryEditRequest(category);

        // Then
        assertEquals(category.getId(), resultDto.getId());
        assertEquals(category.getName(), resultDto.getName());
        assertEquals(category.getDescription(), resultDto.getDescription());
        assertEquals(category.getColor(), resultDto.getColor());
    }

    @Test
    void givenHappyPath_whenMappingUserToUserEditRequest(){
        // Given
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("testUser");
        user.setEmail("");
        user.setFirstName("Dimitar");
        user.setLastName("Peev");
        user.setPhoneNumber("0777777777");
        user.setProfileImageUrl("http://www.image.com");

        // When
        UserEditRequest resultDto = DtoMapper.mapUserToUserEditRequest(user);

        // Then
        assertEquals(user.getId(), resultDto.getId());
        assertEquals(user.getUsername(), resultDto.getUsername());
        assertEquals(user.getEmail(), resultDto.getEmail());
        assertEquals(user.getFirstName(), resultDto.getFirstName());
        assertEquals(user.getLastName(), resultDto.getLastName());
        assertEquals(user.getPhoneNumber(), resultDto.getPhoneNumber());
        assertEquals(user.getProfileImageUrl(), resultDto.getProfileImageUrl());
    }

    @Test
    void givenNullFields_whenMappingUserToUserEditRequest() {
        // Given
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("testUser");
        user.setEmail("");
        user.setFirstName(null);
        user.setLastName(null);
        user.setPhoneNumber(null);
        user.setProfileImageUrl(null);

        // When
        UserEditRequest resultDto = DtoMapper.mapUserToUserEditRequest(user);

        // Then
        assertEquals(user.getId(), resultDto.getId());
        assertEquals(user.getUsername(), resultDto.getUsername());
        assertEquals(user.getEmail(), resultDto.getEmail());
        assertNull(resultDto.getFirstName());
        assertNull(resultDto.getLastName());
        assertNull(resultDto.getPhoneNumber());
        assertNull(resultDto.getProfileImageUrl());
    }

    @Test
    void givenHappyPath_whenMappingVenueToVenueEditRequest() {
        // Given
        Venue venue = new Venue();
        venue.setId(UUID.randomUUID());
        venue.setName("Test Venue");
        venue.setAddress("Test Address");
        venue.setCity("Test City");
        venue.setCapacity(100);
        venue.setHourlyRate(BigDecimal.valueOf(50.00));
        venue.setContactEmail("test@test.com");
        venue.setContactPhone("1234567890");
        venue.setDescription("Test Description");

        // When
        VenueEditRequest resultDto = DtoMapper.mapVenueToVenueEditRequest(venue);

        // Then
        assertEquals(venue.getId(), resultDto.getId());
        assertEquals(venue.getName(), resultDto.getName());
        assertEquals(venue.getAddress(), resultDto.getAddress());
        assertEquals(venue.getCity(), resultDto.getCity());
        assertEquals(venue.getCapacity(), resultDto.getCapacity());
        assertEquals(venue.getHourlyRate(), resultDto.getHourlyRate());
        assertEquals(venue.getContactEmail(), resultDto.getContactEmail());
        assertEquals(venue.getContactPhone(), resultDto.getContactPhone());
        assertEquals(venue.getDescription(), resultDto.getDescription());
    }

    @Test
    void givenHappyPath_whenMappingEventToEventEditRequest() {
        // Given
        Event event = new Event();
        Category category = new Category();
        category.setId(UUID.randomUUID());
        Venue venue = new Venue();
        venue.setId(UUID.randomUUID());

        event.setId(UUID.randomUUID());
        event.setTitle("Test Event");
        event.setDescription("Test Description");
        event.setStartDate(LocalDateTime.now());
        event.setEndDate(LocalDateTime.now().plusHours(2));
        event.setTicketPrice(BigDecimal.valueOf(20.00));
        event.setMaxCapacity(50);
        event.setVenue(venue);
        event.setCategory(category);

        // When
        EventEditRequest resultDto = DtoMapper.mapEventToEventEditRequest(event);

        // Then
        assertEquals(event.getId(), resultDto.getId());
        assertEquals(event.getTitle(), resultDto.getTitle());
        assertEquals(event.getDescription(), resultDto.getDescription());
        assertEquals(event.getStartDate(), resultDto.getStartDate());
        assertEquals(event.getEndDate(), resultDto.getEndDate());
        assertEquals(event.getTicketPrice(), resultDto.getTicketPrice());
        assertEquals(event.getMaxCapacity(), resultDto.getMaxCapacity());
        assertEquals(event.getVenue().getId(), resultDto.getVenueId());
        assertEquals(event.getCategory().getId(), resultDto.getCategoryId());
    }
}
