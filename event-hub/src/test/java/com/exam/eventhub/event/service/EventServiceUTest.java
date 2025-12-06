package com.exam.eventhub.event.service;

import com.exam.eventhub.category.model.Category;
import com.exam.eventhub.category.service.CategoryService;
import com.exam.eventhub.event.model.Event;
import com.exam.eventhub.event.model.EventStatus;
import com.exam.eventhub.event.repository.EventRepository;
import com.exam.eventhub.exception.EventAlreadyExistException;
import com.exam.eventhub.exception.EventNotFoundException;
import com.exam.eventhub.user.model.Role;
import com.exam.eventhub.user.model.User;
import com.exam.eventhub.user.service.UserService;
import com.exam.eventhub.venue.model.Venue;
import com.exam.eventhub.venue.service.VenueService;
import com.exam.eventhub.web.dto.EventCreateRequest;
import com.exam.eventhub.web.dto.EventEditRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.exam.eventhub.common.Constants.ID_NOT_FOUND;
import static com.exam.eventhub.common.Constants.NOT_ALLOWED;
import static com.exam.eventhub.util.EventHelper.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EventServiceUTest {

    private static final String ENTITY_NAME = "Event";

    @Captor
    private ArgumentCaptor<List<Event>> listEventCaptor;

    @Mock
    private EventRepository eventRepository;
    @Mock
    private UserService userService;
    @Mock
    private VenueService venueService;
    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private EventService eventService;

    private Event event1;
    private Event event2;

    @BeforeEach
    void setUp() {
        event1 = createEvent(UUID.randomUUID(), "Event 1", null, null, null, 100);
        event2 = createEvent(UUID.randomUUID(), "Event 2", null, null, null, 200);
    }

    @Test
    void initData_whenRepositoryIsEmpty_shouldInitializeEvents() {

        when(eventRepository.count()).thenReturn(0L);
        when(eventRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        User admin = createUser(UUID.randomUUID(), "admin", Role.ADMIN);
        User organizer = createUser(UUID.randomUUID(), "organizer", Role.EVENT_ORGANIZER);
        when(userService.getByUsername("admin")).thenReturn(admin);
        when(userService.getByUsername("organizer")).thenReturn(organizer);

        Category music = createCategory(UUID.randomUUID(), "Music");
        Category tech = createCategory(UUID.randomUUID(), "Technology");
        Category business = createCategory(UUID.randomUUID(), "Business");
        Category art = createCategory(UUID.randomUUID(), "Art");
        Category sports = createCategory(UUID.randomUUID(), "Sports");
        Category education = createCategory(UUID.randomUUID(), "Education");
        Category cars = createCategory(UUID.randomUUID(), "Cars");

        when(categoryService.getByName("Music")).thenReturn(music);
        when(categoryService.getByName("Technology")).thenReturn(tech);
        when(categoryService.getByName("Business")).thenReturn(business);
        when(categoryService.getByName("Art")).thenReturn(art);
        when(categoryService.getByName("Sports")).thenReturn(sports);
        when(categoryService.getByName("Education")).thenReturn(education);
        when(categoryService.getByName("Cars")).thenReturn(cars);

        Venue centralPark = createVenue(UUID.randomUUID(), "Central Park Amphitheater");
        Venue techHub = createVenue(UUID.randomUUID(), "Tech Hub Conference Center");
        Venue gallery = createVenue(UUID.randomUUID(), "Gallery Modern");
        Venue stadium = createVenue(UUID.randomUUID(), "City Stadium");
        Venue businessCenter = createVenue(UUID.randomUUID(), "Business Center Elite");
        Venue airport = createVenue(UUID.randomUUID(), "Airport");

        when(venueService.getByName("Central Park Amphitheater")).thenReturn(centralPark);
        when(venueService.getByName("Tech Hub Conference Center")).thenReturn(techHub);
        when(venueService.getByName("Gallery Modern")).thenReturn(gallery);
        when(venueService.getByName("City Stadium")).thenReturn(stadium);
        when(venueService.getByName("Business Center Elite")).thenReturn(businessCenter);
        when(venueService.getByName("Airport")).thenReturn(airport);

        eventService.initData();

        verify(eventRepository).saveAll(listEventCaptor.capture());

        List<Event> savedEvents = listEventCaptor.getValue();
        assertEquals(10, savedEvents.size());

        List<String> expectedTitles = List.of(
                "Summer Music Festival 2026", "Jazz Night at the Gallery", "Tech Innovation Conference 2026",
                "AI & Machine Learning Workshop", "Business Networking Gala", "City Marathon 2026",
                "Contemporary Art Exhibition", "Digital Marketing Masterclass", "Drag Weekend Sliven",
                "Wine Tasting Experience"
        );

        List<String> actualTitles = savedEvents.stream()
                .map(Event::getTitle)
                .sorted()
                .toList();

        List<String> expectedSorted = expectedTitles.stream()
                .sorted()
                .toList();

        assertEquals(expectedSorted, actualTitles);

        long publishedCount = savedEvents.stream().filter(e -> e.getStatus() == EventStatus.PUBLISHED).count();
        long draftCount = savedEvents.stream().filter(e -> e.getStatus() == EventStatus.DRAFT).count();

        assertEquals(9, publishedCount);
        assertEquals(1, draftCount);
    }

    @Test
    void initData_whenRepositoryIsNotEmpty_shouldNotInitializeEvents() {

        when(eventRepository.count()).thenReturn(5L);

        eventService.initData();

        verify(eventRepository, never()).saveAll(anyList());
        verify(userService, never()).getByUsername(anyString());
        verify(categoryService, never()).getByName(anyString());
        verify(venueService, never()).getByName(anyString());
    }

    @Test
    void add_whenEventDoesNotExist_shouldCreateAndReturnEvent() {

        String username = "organizer";
        UUID venueId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        String title = "New Event";

        EventCreateRequest request = new EventCreateRequest();
        request.setTitle(title);
        request.setDescription("Event description");
        request.setStartDate(LocalDateTime.of(2026, 6, 1, 10, 0));
        request.setEndDate(LocalDateTime.of(2026, 6, 1, 18, 0));
        request.setTicketPrice(new BigDecimal("50.00"));
        request.setMaxCapacity(100);
        request.setVenueId(venueId);
        request.setCategoryId(categoryId);

        when(eventRepository.findByTitle(title)).thenReturn(Optional.empty());

        User organizer = createUser(UUID.randomUUID(), username, Role.EVENT_ORGANIZER);
        Venue venue = createVenue(venueId, "Test Venue");
        Category category = createCategory(categoryId, "Test Category");

        when(userService.getByUsername(username)).thenReturn(organizer);
        when(venueService.getById(venueId)).thenReturn(venue);
        when(categoryService.getById(categoryId)).thenReturn(category);

        Event savedEvent = createEvent(UUID.randomUUID(), title, organizer, venue, category, 100);
        when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);

        Event result = eventService.add(request, username);

        assertNotNull(result);
        assertEquals(title, result.getTitle());
        assertEquals(organizer, result.getOrganizer());
        assertEquals(venue, result.getVenue());
        assertEquals(category, result.getCategory());

        verify(eventRepository).findByTitle(title);
        verify(userService).getByUsername(username);
        verify(venueService).getById(venueId);
        verify(categoryService).getById(categoryId);
        verify(eventRepository).save(any(Event.class));
    }

    @Test
    void add_whenEventAlreadyExists_shouldThrowException() {

        String title = "Existing Event";
        String username = "organizer";

        EventCreateRequest request = new EventCreateRequest();
        request.setTitle(title);

        Event existingEvent = createEvent(UUID.randomUUID(), title, null, null, null, 100);
        when(eventRepository.findByTitle(title)).thenReturn(Optional.of(existingEvent));

        EventAlreadyExistException exception =
                assertThrows(EventAlreadyExistException.class, () -> eventService.add(request, username));
        assertTrue(exception.getMessage().contains("The event '" + title + "' already exists."));

        verify(eventRepository).findByTitle(title);
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void add_shouldSetInitialStatusToDraft() {

        String username = "organizer";
        UUID venueId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        String title = "Draft Event";

        EventCreateRequest request = new EventCreateRequest();
        request.setTitle(title);
        request.setDescription("Description");
        request.setStartDate(LocalDateTime.now().plusDays(10));
        request.setEndDate(LocalDateTime.now().plusDays(10).plusHours(5));
        request.setTicketPrice(new BigDecimal("30.00"));
        request.setMaxCapacity(50);
        request.setVenueId(venueId);
        request.setCategoryId(categoryId);

        when(eventRepository.findByTitle(title)).thenReturn(Optional.empty());

        User organizer = createUser(UUID.randomUUID(), username, Role.EVENT_ORGANIZER);
        Venue venue = createVenue(venueId, "Venue");
        Category category = createCategory(categoryId, "Category");

        when(userService.getByUsername(username)).thenReturn(organizer);
        when(venueService.getById(venueId)).thenReturn(venue);
        when(categoryService.getById(categoryId)).thenReturn(category);

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        Event savedEvent = createEvent(UUID.randomUUID(), title, organizer, venue, category, 50);
        savedEvent.setStatus(EventStatus.DRAFT);
        when(eventRepository.save(eventCaptor.capture())).thenReturn(savedEvent);

        eventService.add(request, username);

        Event capturedEvent = eventCaptor.getValue();
        assertEquals(EventStatus.DRAFT, capturedEvent.getStatus());
        assertEquals(50, capturedEvent.getAvailableTickets());
    }

    @Test
    void saveEvent_shouldSaveEvent() {

        when(eventRepository.save(event1)).thenReturn(event1);

        eventService.saveEvent(event1);

        verify(eventRepository, times(1)).save(event1);
    }

    @Test
    void getAll_shouldReturnAllEvents() {

        List<Event> expectedEvents = List.of(event1, event2);
        when(eventRepository.findAll()).thenReturn(expectedEvents);

        List<Event> actualEvents = eventService.getAll();

        assertEquals(expectedEvents, actualEvents);
        verify(eventRepository).findAll();
    }

    @Test
    void getEventsByOrganizer_shouldReturnOrganizerEvents() {

        String username = "organizer";
        List<Event> expectedEvents = List.of(event1, event2);
        when(eventRepository.findWithDetailsByOrganizerUsername(username)).thenReturn(expectedEvents);

        List<Event> actualEvents = eventService.getEventsByOrganizer(username);

        assertEquals(expectedEvents, actualEvents);
        verify(eventRepository).findWithDetailsByOrganizerUsername(username);
    }

    @Test
    void getAllWithDetails_shouldReturnAllEventsWithDetails() {

        List<Event> expectedEvents = List.of(event1, event2);
        when(eventRepository.findAllWithDetails()).thenReturn(expectedEvents);

        List<Event> actualEvents = eventService.getAllWithDetails();

        assertEquals(expectedEvents, actualEvents);
        verify(eventRepository).findAllWithDetails();
    }

    @Test
    void updateEvent_whenUserIsOrganizer_shouldUpdateEvent() {

        UUID eventId = UUID.randomUUID();
        UUID venueId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        String username = "organizer";
        int maxCapacity = 100;
        int soldTickets = 20;

        User organizer = createUser(UUID.randomUUID(), username, Role.EVENT_ORGANIZER);
        Venue venue = createVenue(venueId, "Venue");
        Category category = createCategory(categoryId, "Category");

        Event existingEvent = createEvent(eventId, "Old Title", organizer, venue, category, maxCapacity);
        existingEvent.setAvailableTickets(maxCapacity - soldTickets);

        EventEditRequest request = new EventEditRequest();
        request.setTitle("Updated Title");
        request.setDescription("Updated description");
        request.setStartDate(LocalDateTime.now().plusDays(5));
        request.setEndDate(LocalDateTime.now().plusDays(5).plusHours(3));
        request.setTicketPrice(new BigDecimal("75.00"));
        request.setMaxCapacity(maxCapacity);
        request.setVenueId(venueId);
        request.setCategoryId(categoryId);

        when(eventRepository.findByIdWithDetails(eventId)).thenReturn(Optional.of(existingEvent));
        when(userService.getByUsername(username)).thenReturn(organizer);
        when(venueService.getById(venueId)).thenReturn(venue);
        when(categoryService.getById(categoryId)).thenReturn(category);
        when(eventRepository.save(any(Event.class))).thenReturn(existingEvent);

        eventService.updateEvent(eventId, request, username);

        assertEquals("Updated Title", existingEvent.getTitle());
        assertEquals("Updated description", existingEvent.getDescription());
        assertEquals(new BigDecimal("75.00"), existingEvent.getTicketPrice());
        assertEquals(maxCapacity, existingEvent.getMaxCapacity());
        assertEquals(maxCapacity - soldTickets, existingEvent.getAvailableTickets());

        verify(eventRepository).findByIdWithDetails(eventId);
        verify(eventRepository).save(existingEvent);
    }

    @Test
    void updateEvent_whenUserIsAdmin_shouldUpdateEvent() {

        UUID eventId = UUID.randomUUID();
        UUID venueId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        String adminUsername = "admin";
        String organizerUsername = "organizer";
        int maxCapacity = 100;

        User admin = createUser(UUID.randomUUID(), adminUsername, Role.ADMIN);
        User organizer = createUser(UUID.randomUUID(), organizerUsername, Role.EVENT_ORGANIZER);
        Venue venue = createVenue(venueId, "Venue");
        Category category = createCategory(categoryId, "Category");

        Event existingEvent = createEvent(eventId, "Event", organizer, venue, category, maxCapacity);

        EventEditRequest request = new EventEditRequest();
        request.setTitle("Admin Updated");
        request.setDescription("Description");
        request.setStartDate(LocalDateTime.now().plusDays(3));
        request.setEndDate(LocalDateTime.now().plusDays(3).plusHours(4));
        request.setTicketPrice(new BigDecimal("60.00"));
        request.setMaxCapacity(maxCapacity);
        request.setVenueId(venueId);
        request.setCategoryId(categoryId);

        when(eventRepository.findByIdWithDetails(eventId)).thenReturn(Optional.of(existingEvent));
        when(userService.getByUsername(adminUsername)).thenReturn(admin);
        when(venueService.getById(venueId)).thenReturn(venue);
        when(categoryService.getById(categoryId)).thenReturn(category);

        eventService.updateEvent(eventId, request, adminUsername);

        verify(eventRepository).save(existingEvent);
    }

    @Test
    void updateEvent_whenUserIsNotOrganizerOrAdmin_shouldThrowException() {

        UUID eventId = UUID.randomUUID();
        String username = "otherUser";
        String organizerUsername = "organizer";

        User otherUser = createUser(UUID.randomUUID(), username, Role.USER);
        User organizer = createUser(UUID.randomUUID(), organizerUsername, Role.EVENT_ORGANIZER);

        Event existingEvent = createEvent(eventId, "Event", organizer, null, null, 100);

        EventEditRequest request = new EventEditRequest();
        request.setTitle("Updated");
        request.setVenueId(UUID.randomUUID());
        request.setCategoryId(UUID.randomUUID());

        when(eventRepository.findByIdWithDetails(eventId)).thenReturn(Optional.of(existingEvent));
        when(userService.getByUsername(username)).thenReturn(otherUser);

        SecurityException exception =
                assertThrows(SecurityException.class, () -> eventService.updateEvent(eventId, request, username));
        assertTrue(exception.getMessage().contains(NOT_ALLOWED.formatted("edit")));

        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void updateEvent_whenReducingCapacityBelowSoldTickets_shouldThrowException() {

        UUID eventId = UUID.randomUUID();
        UUID venueId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        String username = "organizer";
        int maxCapacity = 100;
        int soldTickets = 80;

        User organizer = createUser(UUID.randomUUID(), username, Role.EVENT_ORGANIZER);
        Venue venue = createVenue(venueId, "Venue");
        Category category = createCategory(categoryId, "Category");

        Event existingEvent = createEvent(eventId, "Event", organizer, venue, category, maxCapacity);
        existingEvent.setAvailableTickets(maxCapacity - soldTickets);

        EventEditRequest request = new EventEditRequest();
        request.setTitle("Event");
        request.setDescription("Description");
        request.setStartDate(LocalDateTime.now().plusDays(5));
        request.setEndDate(LocalDateTime.now().plusDays(5).plusHours(3));
        request.setTicketPrice(new BigDecimal("50.00"));
        request.setMaxCapacity(50);
        request.setVenueId(venueId);
        request.setCategoryId(categoryId);

        when(eventRepository.findByIdWithDetails(eventId)).thenReturn(Optional.of(existingEvent));
        when(userService.getByUsername(username)).thenReturn(organizer);
        when(venueService.getById(venueId)).thenReturn(venue);
        when(categoryService.getById(categoryId)).thenReturn(category);

        IllegalStateException exception =
                assertThrows(IllegalStateException.class, () -> eventService.updateEvent(eventId, request, username));
        assertTrue(exception.getMessage().contains("Cannot reduce capacity below sold tickets"));

        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void deleteEvent_shouldDeleteEvent() {

        UUID eventId = UUID.randomUUID();
        Event event = createEvent(eventId, "Event", null, null, null, 100);

        when(eventRepository.findByIdWithDetails(eventId)).thenReturn(Optional.of(event));

        eventService.deleteEvent(eventId);

        verify(eventRepository, times(1)).findByIdWithDetails(eventId);
        verify(eventRepository, times(1)).delete(event);
    }

    @Test
    void deleteEvent_whenEventDoesNotExist_shouldThrowException() {

        UUID eventId = UUID.randomUUID();
        when(eventRepository.findByIdWithDetails(eventId)).thenReturn(Optional.empty());

        EventNotFoundException exception =
                assertThrows(EventNotFoundException.class, () -> eventService.deleteEvent(eventId));
        assertTrue(exception.getMessage().contains(ID_NOT_FOUND.formatted(ENTITY_NAME, eventId)));

        verify(eventRepository, times(1)).findByIdWithDetails(eventId);
        verify(eventRepository, never()).delete(any(Event.class));
    }

    @Test
    void searchEvents_withAllParameters_shouldReturnFilteredEvents() {

        String keyword = "Music";
        String city = "Sofia";
        UUID categoryId = UUID.randomUUID();
        UUID venueId = UUID.randomUUID();

        Event event = createEvent(UUID.randomUUID(), "Music Festival", null, null, null, 100);
        List<Event> expectedEvents = List.of(event);

        when(eventRepository.searchEvents(keyword, city, categoryId, venueId)).thenReturn(expectedEvents);

        List<Event> actualEvents = eventService.searchEvents(keyword, city, categoryId, venueId);

        assertEquals(expectedEvents, actualEvents);
        verify(eventRepository, times(1)).searchEvents(keyword, city, categoryId, venueId);
    }

    @Test
    void searchEvents_withNullParameters_shouldHandleNulls() {

        List<Event> expectedEvents = List.of(event1, event2);

        when(eventRepository.searchEvents(null, null, null, null)).thenReturn(expectedEvents);

        List<Event> actualEvents = eventService.searchEvents(null, null, null, null);

        assertEquals(expectedEvents, actualEvents);
        verify(eventRepository).searchEvents(null, null, null, null);
    }

    @Test
    void searchEvents_withBlankKeyword_shouldConvertToNull() {

        String blankKeyword = "   ";
        List<Event> expectedEvents = List.of();

        when(eventRepository.searchEvents(null, null, null, null)).thenReturn(expectedEvents);

        eventService.searchEvents(blankKeyword, null, null, null);

        verify(eventRepository, times(1)).searchEvents(null, null, null, null);
    }

    @Test
    void searchEvents_withBlankCity_shouldConvertToNull() {

        String blankCity = "   ";
        List<Event> expectedEvents = List.of();

        when(eventRepository.searchEvents(null, null, null, null)).thenReturn(expectedEvents);

        eventService.searchEvents(null, blankCity, null, null);

        verify(eventRepository, times(1)).searchEvents(null, null, null, null);
    }

    @Test
    void getById_whenEventExists_shouldReturnEvent() {

        UUID eventId = UUID.randomUUID();
        Event event = createEvent(eventId, "Event", null, null, null, 100);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        Event result = eventService.getById(eventId);

        assertEquals(event, result);
        assertEquals(eventId, result.getId());
        verify(eventRepository, times(1)).findById(eventId);
    }

    @Test
    void getById_whenEventDoesNotExist_shouldThrowException() {

        UUID eventId = UUID.randomUUID();
        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        EventNotFoundException exception =
                assertThrows(EventNotFoundException.class, () -> eventService.getById(eventId));
        assertTrue(exception.getMessage().contains(ID_NOT_FOUND.formatted(ENTITY_NAME, eventId)));

        verify(eventRepository, times(1)).findById(eventId);
    }

    @Test
    void getByIdWithDetails_whenEventExists_shouldReturnEvent() {

        UUID eventId = UUID.randomUUID();
        Event event = createEvent(eventId, "Event", null, null, null, 100);

        when(eventRepository.findByIdWithDetails(eventId)).thenReturn(Optional.of(event));

        Event result = eventService.getByIdWithDetails(eventId);

        assertEquals(event, result);
        assertEquals(eventId, result.getId());
        verify(eventRepository, times(1)).findByIdWithDetails(eventId);
    }

    @Test
    void getByIdWithDetails_whenEventDoesNotExist_shouldThrowException() {

        UUID eventId = UUID.randomUUID();
        when(eventRepository.findByIdWithDetails(eventId)).thenReturn(Optional.empty());

        EventNotFoundException exception =
                assertThrows(EventNotFoundException.class, () -> eventService.getByIdWithDetails(eventId));
        assertTrue(exception.getMessage().contains(ID_NOT_FOUND.formatted(ENTITY_NAME, eventId)));

        verify(eventRepository, times(1)).findByIdWithDetails(eventId);
    }
}
