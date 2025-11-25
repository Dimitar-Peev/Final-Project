package com.exam.eventhub.venue.service;

import com.exam.eventhub.exception.VenueAlreadyExistException;
import com.exam.eventhub.exception.VenueDuplicateException;
import com.exam.eventhub.exception.VenueNotFoundException;
import com.exam.eventhub.venue.model.Venue;
import com.exam.eventhub.venue.repository.VenueRepository;
import com.exam.eventhub.web.dto.VenueCreateRequest;
import com.exam.eventhub.web.dto.VenueEditRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.exam.eventhub.common.Constants.ID_NOT_FOUND;
import static com.exam.eventhub.common.Constants.NAME_NOT_FOUND;
import static com.exam.eventhub.util.TestBuilder.createVenue;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class VenueServiceUTest {

    private static final String ENTITY_NAME = "Venue";

    @Captor
    private ArgumentCaptor<List<Venue>> listVenueCaptor;

    @Mock
    private VenueRepository venueRepository;

    @InjectMocks
    private VenueService venueService;

    @Test
    void initData_whenRepositoryIsEmpty_shouldInitializeVenues() {

        when(venueRepository.count()).thenReturn(0L);
        when(venueRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        venueService.initData();

        verify(venueRepository).saveAll(listVenueCaptor.capture());

        List<Venue> savedVenues = listVenueCaptor.getValue();
        assertEquals(6, savedVenues.size());

        List<String> expectedNames = List.of(
                "Central Park Amphitheater", "Tech Hub Conference Center", "Gallery Modern",
                "City Stadium", "Business Center Elite", "Airport"
        );

        List<String> actualNames = savedVenues.stream()
                .map(Venue::getName)
                .sorted()
                .toList();

        List<String> expectedSorted = expectedNames.stream()
                .sorted()
                .toList();

        assertEquals(expectedSorted, actualNames);
    }

    @Test
    void initData_whenRepositoryIsNotEmpty_shouldNotInitializeVenues() {

        when(venueRepository.count()).thenReturn(5L);

        venueService.initData();

        verify(venueRepository, never()).saveAll(anyList());
    }

    @Test
    void getAll_shouldReturnAllVenues() {
        Venue venue1 = createVenue(UUID.randomUUID(), "Central Park Amphitheater", "Central Park Avenue 1", "Sofia", 5000, new BigDecimal("500.00"));
        Venue venue2 = createVenue(UUID.randomUUID(), "Tech Hub Conference Center", "Tech Park Street 15", "Plovdiv", 300, new BigDecimal("200.00"));

        List<Venue> expectedVenues = List.of(venue1, venue2);
        when(venueRepository.findAll()).thenReturn(expectedVenues);

        List<Venue> actualVenues = venueService.getAll();

        assertEquals(expectedVenues, actualVenues);
        assertEquals(expectedVenues.size(), actualVenues.size());
        verify(venueRepository).findAll();
    }

    @Test
    void add_whenVenueDoesNotExist_shouldCreateAndReturnVenue() {

        String name = "New Venue";
        VenueCreateRequest request = new VenueCreateRequest();
        request.setName(name);
        request.setAddress("New Address 10");
        request.setCity("Sofia");
        request.setCapacity(1000);
        request.setHourlyRate(new BigDecimal("150.00"));
        request.setContactEmail("info@newvenue.bg");
        request.setContactPhone("+359888999000");
        request.setDescription("New venue description");

        when(venueRepository.findByName(name)).thenReturn(Optional.empty());

        Venue savedVenue = createVenue(UUID.randomUUID(), name, request.getAddress(), request.getCity(), request.getCapacity(), request.getHourlyRate());
        when(venueRepository.save(any(Venue.class))).thenReturn(savedVenue);

        Venue result = venueService.add(request);

        assertNotNull(result);
        assertEquals(savedVenue.getName(), result.getName());
        assertEquals(savedVenue.getAddress(), result.getAddress());
        assertEquals(savedVenue.getCity(), result.getCity());
        assertEquals(savedVenue.getCapacity(), result.getCapacity());
        assertEquals(savedVenue.getHourlyRate(), result.getHourlyRate());
        verify(venueRepository,times(1)).findByName(name);
        verify(venueRepository,times(1)).save(any(Venue.class));
    }

    @Test
    void add_whenVenueAlreadyExists_shouldThrowException() {

        String name = "Existing Venue";
        VenueCreateRequest request = new VenueCreateRequest();
        request.setName(name);

        Venue existingVenue = createVenue(UUID.randomUUID(), name, "Address", "City", 500, new BigDecimal("100.00"));
        when(venueRepository.findByName(name)).thenReturn(Optional.of(existingVenue));

        VenueAlreadyExistException exception =
                assertThrows(VenueAlreadyExistException.class, () -> venueService.add(request));
        assertTrue(exception.getMessage().contains("The venue '" + name + "' already exists."));

        verify(venueRepository, times(1)).findByName(name);
        verify(venueRepository, never()).save(any(Venue.class));
    }

    @Test
    void update_whenVenueExists_shouldUpdateAndReturnVenue() {

        UUID venueId = UUID.randomUUID();
        Venue existingVenue = createVenue(venueId, "Old Name", "Old Address", "Old City", 500, new BigDecimal("100.00"));

        VenueEditRequest request = new VenueEditRequest();
        request.setName("Updated Name");
        request.setAddress("Updated Address");
        request.setCity("Updated City");
        request.setCapacity(1000);
        request.setHourlyRate(new BigDecimal("200.00"));
        request.setContactEmail("updated@venue.bg");
        request.setContactPhone("+359888111222");
        request.setDescription("Updated description");

        when(venueRepository.findById(venueId)).thenReturn(Optional.of(existingVenue));
        when(venueRepository.findByName(request.getName())).thenReturn(Optional.empty());

        Venue updatedVenue = createVenue(venueId, request.getName(), request.getAddress(), request.getCity(), request.getCapacity(), request.getHourlyRate());
        when(venueRepository.save(any(Venue.class))).thenReturn(updatedVenue);

        Venue result = venueService.update(venueId, request);

        assertNotNull(result);
        assertEquals(venueId, result.getId());
        assertEquals(request.getName(), result.getName());
        assertEquals(request.getAddress(), result.getAddress());
        assertEquals(request.getCity(), result.getCity());
        assertEquals(request.getCapacity(), result.getCapacity());
        assertEquals(request.getHourlyRate(), result.getHourlyRate());
        verify(venueRepository, times(1)).findById(venueId);
        verify(venueRepository, times(1)).findByName(request.getName());
        verify(venueRepository, times(1)).save(existingVenue);
    }

    @Test
    void update_whenVenueDoesNotExist_shouldThrowException() {

        UUID venueId = UUID.randomUUID();
        VenueEditRequest request = new VenueEditRequest();
        request.setName("Updated Name");

        when(venueRepository.findById(venueId)).thenReturn(Optional.empty());

        VenueNotFoundException exception =
                assertThrows(VenueNotFoundException.class, () -> venueService.update(venueId, request));
        assertTrue(exception.getMessage().contains(ID_NOT_FOUND.formatted(ENTITY_NAME, venueId)));

        verify(venueRepository, times(1)).findById(venueId);
        verify(venueRepository, never()).save(any(Venue.class));
    }

    @Test
    void update_whenVenueNameAlreadyExistsForDifferentVenue_shouldThrowException() {

        UUID venueId = UUID.randomUUID();
        UUID otherVenueId = UUID.randomUUID();

        String name = "Duplicate Name";
        Venue existingVenue = createVenue(venueId, "Old Name", "Address", "City", 500, new BigDecimal("100.00"));
        Venue otherVenue = createVenue(otherVenueId, name, "Other Address", "City", 300, new BigDecimal("150.00"));

        VenueEditRequest request = new VenueEditRequest();
        request.setName(name);

        when(venueRepository.findById(venueId)).thenReturn(Optional.of(existingVenue));
        when(venueRepository.findByName(name)).thenReturn(Optional.of(otherVenue));

        VenueDuplicateException exception =
                assertThrows(VenueDuplicateException.class, () -> venueService.update(venueId, request));
        assertTrue(exception.getMessage().contains("The venue '" + name + "' already exists."));

        verify(venueRepository, times(1)).findById(venueId);
        verify(venueRepository, times(1)).findByName(name);
        verify(venueRepository, never()).save(any(Venue.class));
    }

    @Test
    void update_whenVenueNameIsSameAsExistingVenue_shouldUpdateSuccessfully() {
        // Arrange
        UUID venueId = UUID.randomUUID();
        String name = "Same Name";
        Venue existingVenue = createVenue(venueId, name, "Old Address", "Old City", 500, new BigDecimal("100.00"));

        VenueEditRequest request = new VenueEditRequest();
        request.setName(name);
        request.setAddress("Updated Address");
        request.setCity("Updated City");
        request.setCapacity(1000);
        request.setHourlyRate(new BigDecimal("200.00"));
        request.setContactEmail("updated@venue.bg");
        request.setContactPhone("+359888111222");
        request.setDescription("Updated description");

        when(venueRepository.findById(venueId)).thenReturn(Optional.of(existingVenue));
        when(venueRepository.findByName(name)).thenReturn(Optional.of(existingVenue));

        Venue updatedVenue = createVenue(venueId, request.getName(), request.getAddress(), request.getCity(), request.getCapacity(), request.getHourlyRate());
        when(venueRepository.save(any(Venue.class))).thenReturn(updatedVenue);

        // Act
        Venue result = venueService.update(venueId, request);

        // Assert
        assertNotNull(result);
        assertEquals(venueId, result.getId());
        assertEquals(updatedVenue.getName(), result.getName());
        assertEquals(updatedVenue.getAddress(), result.getAddress());
        assertEquals(updatedVenue.getCity(), result.getCity());

        verify(venueRepository, times(1)).findById(venueId);
        verify(venueRepository, times(1)).findByName(name);
        verify(venueRepository, times(1)).save(existingVenue);
    }

    @Test
    void delete_whenVenueExists_shouldDeleteVenue() {

        UUID venueId = UUID.randomUUID();
        Venue venue = createVenue(venueId, "Venue", "Address", "City", 500, new BigDecimal("100.00"));

        when(venueRepository.findById(venueId)).thenReturn(Optional.of(venue));

        venueService.delete(venueId);

        verify(venueRepository, times(1)).findById(venueId);
        verify(venueRepository, times(1)).delete(venue);
    }

    @Test
    void delete_whenVenueDoesNotExist_shouldThrowException() {

        UUID venueId = UUID.randomUUID();
        when(venueRepository.findById(venueId)).thenReturn(Optional.empty());

        VenueNotFoundException exception =
                assertThrows(VenueNotFoundException.class, () -> venueService.delete(venueId));
        assertTrue(exception.getMessage().contains(ID_NOT_FOUND.formatted(ENTITY_NAME, venueId)));

        verify(venueRepository, times(1)).findById(venueId);
        verify(venueRepository, never()).delete(any(Venue.class));
    }

    @Test
    void getById_whenVenueExists_shouldReturnVenue() {

        UUID venueId = UUID.randomUUID();
        Venue venue = createVenue(venueId, "Venue", "Address", "City", 500, new BigDecimal("100.00"));

        when(venueRepository.findById(venueId)).thenReturn(Optional.of(venue));

        Venue result = venueService.getById(venueId);

        assertEquals(venue, result);
        assertEquals(venueId, result.getId());
        assertEquals(venue.getName(), result.getName());
        assertEquals(venue.getAddress(), result.getAddress());
        assertEquals(venue.getCity(), result.getCity());
        assertEquals(venue.getCapacity(), result.getCapacity());
        assertEquals(venue.getHourlyRate(), result.getHourlyRate());
        verify(venueRepository, times(1)).findById(venueId);
    }

    @Test
    void getById_whenVenueDoesNotExist_shouldThrowException() {

        UUID venueId = UUID.randomUUID();
        when(venueRepository.findById(venueId)).thenReturn(Optional.empty());

        VenueNotFoundException exception =
                assertThrows(VenueNotFoundException.class, () -> venueService.getById(venueId));
        assertTrue(exception.getMessage().contains(ID_NOT_FOUND.formatted(ENTITY_NAME, venueId)));

        verify(venueRepository, times(1)).findById(venueId);
    }

    @Test
    void getByName_whenVenueExists_shouldReturnVenue() {

        String venueName = "Central Park Amphitheater";
        Venue venue = createVenue(UUID.randomUUID(), venueName, "Central Park Avenue 1", "Sofia", 5000, new BigDecimal("500.00"));

        when(venueRepository.findByName(venueName)).thenReturn(Optional.of(venue));

        Venue result = venueService.getByName(venueName);

        assertEquals(venue, result);
        assertEquals(venueName, result.getName());
        assertEquals(venue.getAddress(), result.getAddress());
        assertEquals(venue.getCity(), result.getCity());
        assertEquals(venue.getCapacity(), result.getCapacity());
        assertEquals(venue.getHourlyRate(), result.getHourlyRate());
        verify(venueRepository, times(1)).findByName(venueName);
    }

    @Test
    void getByName_whenVenueDoesNotExist_shouldThrowException() {

        String venueName = "NonExistent";
        when(venueRepository.findByName(venueName)).thenReturn(Optional.empty());

        VenueNotFoundException exception =
                assertThrows(VenueNotFoundException.class, () -> venueService.getByName(venueName));
        assertTrue(exception.getMessage().contains(NAME_NOT_FOUND.formatted(ENTITY_NAME, venueName)));

        verify(venueRepository, times(1)).findByName(venueName);
    }
}
