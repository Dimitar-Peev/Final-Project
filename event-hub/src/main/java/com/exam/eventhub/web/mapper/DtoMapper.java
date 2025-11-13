package com.exam.eventhub.web.mapper;

import com.exam.eventhub.category.model.Category;
import com.exam.eventhub.event.model.Event;
import com.exam.eventhub.user.model.User;
import com.exam.eventhub.venue.model.Venue;
import com.exam.eventhub.web.dto.CategoryEditRequest;
import com.exam.eventhub.web.dto.EventEditRequest;
import com.exam.eventhub.web.dto.UserEditRequest;
import com.exam.eventhub.web.dto.VenueEditRequest;
import lombok.experimental.UtilityClass;

import java.time.LocalDateTime;

@UtilityClass
public class DtoMapper {

    public static CategoryEditRequest mapCategoryToCategoryEditRequest(Category category) {
        CategoryEditRequest categoryEditRequest = new CategoryEditRequest();

        categoryEditRequest.setId(category.getId());
        categoryEditRequest.setName(category.getName());
        categoryEditRequest.setDescription(category.getDescription());
        categoryEditRequest.setColor(category.getColor());

        return categoryEditRequest;
    }

    public static UserEditRequest mapUserToUserEditRequest(User user) {
        UserEditRequest userEditRequest = new UserEditRequest();

        userEditRequest.setId(user.getId());
        userEditRequest.setUsername(user.getUsername());
        userEditRequest.setEmail(user.getEmail());
        userEditRequest.setFirstName(user.getFirstName() != null ? user.getFirstName() : null);
        userEditRequest.setLastName(user.getLastName() != null ? user.getLastName() : null);
        userEditRequest.setPhoneNumber(user.getPhoneNumber() != null ? user.getPhoneNumber() : null);
        userEditRequest.setProfileImageUrl(user.getProfileImageUrl() != null ? user.getProfileImageUrl() : null);
        userEditRequest.setUpdatedAt(LocalDateTime.now());

        return userEditRequest;
    }

    public static VenueEditRequest mapVenueToVenueEditRequest(Venue venue) {
        VenueEditRequest venueEditRequest = new VenueEditRequest();

        venueEditRequest.setId(venue.getId());
        venueEditRequest.setName(venue.getName());
        venueEditRequest.setAddress(venue.getAddress());
        venueEditRequest.setCity(venue.getCity());
        venueEditRequest.setCapacity(venue.getCapacity());
        venueEditRequest.setHourlyRate(venue.getHourlyRate());
        venueEditRequest.setContactEmail(venue.getContactEmail());
        venueEditRequest.setContactPhone(venue.getContactPhone());
        venueEditRequest.setDescription(venue.getDescription());

        return venueEditRequest;
    }

    public static EventEditRequest mapEventToEventEditRequest(Event event) {
        EventEditRequest eventEditRequest = new EventEditRequest();

        eventEditRequest.setId(event.getId());
        eventEditRequest.setTitle(event.getTitle());
        eventEditRequest.setDescription(event.getDescription());
        eventEditRequest.setStartDate(event.getStartDate());
        eventEditRequest.setEndDate(event.getEndDate());
        eventEditRequest.setTicketPrice(event.getTicketPrice());
        eventEditRequest.setMaxCapacity(event.getMaxCapacity());
        eventEditRequest.setVenueId(event.getVenue().getId());
        eventEditRequest.setCategoryId(event.getCategory().getId());

        return eventEditRequest;
    }
}
