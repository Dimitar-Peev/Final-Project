package com.exam.eventhub.web;

import com.exam.eventhub.booking.service.BookingService;
import com.exam.eventhub.category.service.CategoryService;
import com.exam.eventhub.config.TestMvcConfig;
import com.exam.eventhub.config.TestSecurityConfig;
import com.exam.eventhub.event.model.Event;
import com.exam.eventhub.event.service.EventService;
import com.exam.eventhub.security.AuthenticationMetadata;
import com.exam.eventhub.user.model.Role;
import com.exam.eventhub.user.model.User;
import com.exam.eventhub.user.service.UserService;
import com.exam.eventhub.venue.service.VenueService;
import com.exam.eventhub.web.dto.EventCreateRequest;
import com.exam.eventhub.web.dto.EventEditRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.exam.eventhub.common.Constants.*;
import static com.exam.eventhub.util.ApiHelper.createMockEvent;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EventController.class)
@Import({TestMvcConfig.class, TestSecurityConfig.class})
public class EventControllerApiTest {

    @MockitoBean
    private EventService eventService;
    @MockitoBean
    private UserService userService;
    @MockitoBean
    private CategoryService categoryService;
    @MockitoBean
    private VenueService venueService;
    @MockitoBean
    private BookingService bookingService;

    @Autowired
    private MockMvc mockMvc;

    private AuthenticationMetadata adminPrincipal;
    private AuthenticationMetadata organizerPrincipal;
    private AuthenticationMetadata principal;

    @BeforeEach
    void setUp() {
        adminPrincipal = new AuthenticationMetadata
                (UUID.randomUUID(), "adminUser", "password", Role.ADMIN, false, null);
        organizerPrincipal = new AuthenticationMetadata
                (UUID.randomUUID(), "organizerUser", "password123", Role.EVENT_ORGANIZER, false, null);
        principal = new AuthenticationMetadata
                (UUID.randomUUID(), "testUser", "password321", Role.USER, false, null);
    }

    @Test
    void getSearchEventsWithoutParams_returnsEventsView() throws Exception {

        Event event1 = createMockEvent("Concert", "Music event");
        Event event2 = createMockEvent("Conference", "Tech conference");
        List<Event> mockEvents = Arrays.asList(event1, event2);

        when(eventService.searchEvents(null, null, null, null)).thenReturn(mockEvents);
        when(categoryService.getAll()).thenReturn(Collections.emptyList());
        when(venueService.getAll()).thenReturn(Collections.emptyList());

        MockHttpServletRequestBuilder request = get("/events/search");

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isOk())
                .andExpect(view().name("events"))
                .andExpect(model().attributeExists("events", "categories", "venues"))
                .andExpect(model().attribute("events", mockEvents));

        verify(eventService, times(1)).searchEvents(null, null, null, null);
        verify(categoryService, times(1)).getAll();
        verify(venueService, times(1)).getAll();
    }

    @Test
    void getSearchEventsWithAllParams_returnsFilteredEvents() throws Exception {

        UUID categoryId = UUID.randomUUID();
        UUID venueId = UUID.randomUUID();
        List<Event> mockEvents = List.of(createMockEvent("Concert", "Music event"));

        when(eventService.searchEvents("music", "Sofia", categoryId, venueId)).thenReturn(mockEvents);
        when(categoryService.getAll()).thenReturn(Collections.emptyList());
        when(venueService.getAll()).thenReturn(Collections.emptyList());

        MockHttpServletRequestBuilder request = get("/events/search")
                .param("keyword", "music")
                .param("city", "Sofia")
                .param("categoryId", categoryId.toString())
                .param("venueId", venueId.toString());

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isOk())
                .andExpect(view().name("events"))
                .andExpect(model().attribute("events", mockEvents));

        verify(eventService, times(1)).searchEvents("music", "Sofia", categoryId, venueId);
    }

    @Test
    void getAuthenticatedOrganizerRequestToManageEvents_returnsOrganizerEvents() throws Exception {

        String username = organizerPrincipal.getUsername();

        when(userService.hasRole(username, Role.ADMIN)).thenReturn(false);
        when(eventService.getEventsByOrganizer(username)).thenReturn(Collections.emptyList());

        MockHttpServletRequestBuilder request = get("/events/my")
                .with(user(organizerPrincipal));

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isOk())
                .andExpect(view().name("event/manage-events"))
                .andExpect(model().attributeExists("myEvents"));

        verify(userService, times(1)).hasRole(username, Role.ADMIN);
        verify(eventService, times(1)).getEventsByOrganizer(username);
        verify(eventService, never()).getAllWithDetails();
    }

    @Test
    void getAuthenticatedAdminRequestToManageEvents_returnsAllEvents() throws Exception {

        String username = adminPrincipal.getUsername();

        when(userService.hasRole(username, Role.ADMIN)).thenReturn(true);
        when(eventService.getAllWithDetails()).thenReturn(Collections.emptyList());

        MockHttpServletRequestBuilder request = get("/events/my")
                .with(user(adminPrincipal));

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isOk())
                .andExpect(view().name("event/manage-events"))
                .andExpect(model().attributeExists("myEvents"));

        verify(userService, times(1)).hasRole(username, Role.ADMIN);
        verify(eventService, times(1)).getAllWithDetails();
        verify(eventService, never()).getEventsByOrganizer(any());
    }

    @Test
    void getAuthenticatedUserRequestToManageEvents_returnsForbidden() throws Exception {

        MockHttpServletRequestBuilder request = get("/events/my")
                .with(user(principal));

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isForbidden());

        verify(eventService, never()).getEventsByOrganizer(any());
        verify(eventService, never()).getAllWithDetails();
    }

    @Test
    void getCreateFormWithExistingModelAttribute_doesNotOverrideAttribute() throws Exception {

        EventCreateRequest existingRequest = new EventCreateRequest();
        existingRequest.setTitle("Existing Event");

        when(categoryService.getAll()).thenReturn(Collections.emptyList());
        when(venueService.getAll()).thenReturn(Collections.emptyList());

        MockHttpServletRequestBuilder request = get("/events/new")
                .flashAttr("eventCreateRequest", existingRequest)
                .with(user(organizerPrincipal));

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isOk())
                .andExpect(view().name("event/event-add"))
                .andExpect(model().attribute("eventCreateRequest", existingRequest));
    }

    @Test
    void getCreateForm_returnsCreateView() throws Exception {
        when(categoryService.getAll()).thenReturn(Collections.emptyList());
        when(venueService.getAll()).thenReturn(Collections.emptyList());

        MockHttpServletRequestBuilder request = get("/events/new")
                .with(user(organizerPrincipal));

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isOk())
                .andExpect(view().name("event/event-add"))
                .andExpect(model().attributeExists("eventCreateRequest", "categories", "venues"));

        verify(categoryService, times(1)).getAll();
        verify(venueService, times(1)).getAll();
    }

    @Test
    void getAuthenticatedUserRequestToCreateForm_returnsForbidden() throws Exception {

        MockHttpServletRequestBuilder request = get("/events/new")
                .with(user(principal));

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isForbidden());

        verify(categoryService, never()).getAll();
    }

    @Test
    void postValidEventCreateRequest_createsEventAndRedirects() throws Exception {

        UUID categoryId = UUID.randomUUID();
        UUID venueId = UUID.randomUUID();

        MockHttpServletRequestBuilder request = post("/events")
                .param("title", "Concert")
                .param("description", "Desc")
                .param("startDate", "2030-01-01T10:00")
                .param("endDate", "2030-01-01T12:00")
                .param("ticketPrice", "50.00")
                .param("maxCapacity", "200")
                .param("availableTickets", "200")
                .param("venueId", venueId.toString())
                .param("categoryId", categoryId.toString())
                .with(user(organizerPrincipal))
                .with(csrf());

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/events/my"))
                .andExpect(flash().attributeExists(SUCCESS_MESSAGE_ATTR));

        verify(eventService, times(1)).add(any(EventCreateRequest.class), eq("organizerUser"));
    }

    @Test
    void postInvalidEventCreateRequest_redirectsToFormWithErrors() throws Exception {
        // 1. Build Request
        MockHttpServletRequestBuilder request = post("/events")
                .param("title", "")
                .param("description", "")
                .with(user(organizerPrincipal))
                .with(csrf());

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/events/new"))
                .andExpect(flash().attributeExists("eventCreateRequest"))
                .andExpect(flash().attributeExists(BINDING_MODEL + "eventCreateRequest"))
                .andExpect(flash().attributeExists(ERROR_MESSAGE_ATTR));

        verify(eventService, never()).add(any(), any());
    }

    @Test
    void postAuthenticatedUserEventCreateRequest_returnsForbidden() throws Exception {

        MockHttpServletRequestBuilder request = post("/events")
                .param("title", "New Event")
                .with(user(principal))
                .with(csrf());

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isForbidden());

        verify(eventService, never()).add(any(), any());
    }

    @Test
    void getAuthenticatedOrganizerRequestToEditOwnEvent_returnsEditView() throws Exception {

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername(organizerPrincipal.getUsername());
        user.setRole(organizerPrincipal.getRole());
        user.setEmail("organizer@example.com");
        user.setPassword("encodedPassword");
        user.setBlocked(false);

        UUID eventId = UUID.randomUUID();
        Event mockEvent = createMockEvent("My Event", "Description");
        mockEvent.setOrganizer(user);

        when(eventService.getByIdWithDetails(eventId)).thenReturn(mockEvent);
        when(userService.hasRole(organizerPrincipal.getUsername(), Role.ADMIN)).thenReturn(false);
        when(categoryService.getAll()).thenReturn(Collections.emptyList());
        when(venueService.getAll()).thenReturn(Collections.emptyList());

        MockHttpServletRequestBuilder request = get("/events/" + eventId + "/edit")
                .with(user(organizerPrincipal));

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isOk())
                .andExpect(view().name("event/event-edit"))
                .andExpect(model().attributeExists("eventEditRequest", "categories", "venues"));

        verify(eventService, times(1)).getByIdWithDetails(eventId);
    }

    @Test
    void getAuthenticatedOrganizerRequestToEditOthersEvent_redirectsWithError() throws Exception {

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("otherOrganizer");
        user.setRole(Role.EVENT_ORGANIZER);
        user.setEmail("organizer2@example.com");
        user.setPassword("encodedPassword");
        user.setBlocked(false);

        UUID eventId = UUID.randomUUID();
        Event mockEvent = createMockEvent("Other Event", "Description");
        mockEvent.setOrganizer(user);

        when(eventService.getByIdWithDetails(eventId)).thenReturn(mockEvent);
        when(userService.hasRole(organizerPrincipal.getUsername(), Role.ADMIN)).thenReturn(false);

        MockHttpServletRequestBuilder request = get("/events/" + eventId + "/edit")
                .with(user(organizerPrincipal));

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/events/my"))
                .andExpect(flash().attributeExists(ERROR_MESSAGE_ATTR));

        verify(eventService, times(1)).getByIdWithDetails(eventId);
        verify(categoryService, never()).getAll();
    }

    @Test
    void getAuthenticatedAdminRequestToEditAnyEvent_returnsEditView() throws Exception {

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("someOrganizer");
        user.setRole(Role.EVENT_ORGANIZER);
        user.setEmail("organizer2@example.com");
        user.setPassword("encodedPassword");
        user.setBlocked(false);

        UUID eventId = UUID.randomUUID();
        Event mockEvent = createMockEvent("Any Event", "Description");
        mockEvent.setOrganizer(user);

        when(eventService.getByIdWithDetails(eventId)).thenReturn(mockEvent);
        when(userService.hasRole(adminPrincipal.getUsername(), Role.ADMIN)).thenReturn(true);
        when(categoryService.getAll()).thenReturn(Collections.emptyList());
        when(venueService.getAll()).thenReturn(Collections.emptyList());

        MockHttpServletRequestBuilder request = get("/events/" + eventId + "/edit")
                .with(user(adminPrincipal));

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isOk())
                .andExpect(view().name("event/event-edit"))
                .andExpect(model().attributeExists("eventEditRequest", "categories", "venues"));;

        verify(eventService, times(1)).getByIdWithDetails(eventId);
    }

    @Test
    void getAuthenticatedOrganizerRequestToEditEvent_withExistingModelAttribute() throws Exception {

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername(organizerPrincipal.getUsername());
        user.setRole(Role.EVENT_ORGANIZER);
        user.setEmail("organizer@example.com");
        user.setPassword("encodedPassword");
        user.setBlocked(false);

        UUID eventId = UUID.randomUUID();
        Event mockEvent = createMockEvent("My Event", "Description");
        mockEvent.setId(eventId);
        mockEvent.setOrganizer(user);

        EventEditRequest existingRequest = new EventEditRequest();
        existingRequest.setTitle("Existing Title");

        when(eventService.getByIdWithDetails(eventId)).thenReturn(mockEvent);
        when(userService.hasRole(organizerPrincipal.getUsername(), Role.ADMIN)).thenReturn(false);
        when(categoryService.getAll()).thenReturn(Collections.emptyList());
        when(venueService.getAll()).thenReturn(Collections.emptyList());

        MockHttpServletRequestBuilder request = get("/events/" + eventId + "/edit")
                .flashAttr("eventEditRequest", existingRequest)
                .with(user(organizerPrincipal));

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isOk())
                .andExpect(view().name("event/event-edit"))
                .andExpect(model().attribute("eventEditRequest", existingRequest))
                .andExpect(model().attributeExists("categories", "venues"));

        verify(eventService, times(1)).getByIdWithDetails(eventId);
        verify(categoryService, times(1)).getAll();
        verify(venueService, times(1)).getAll();
    }

    @Test
    void putValidEventEditRequest_updatesEventAndRedirects() throws Exception {

        UUID eventId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        UUID venueId = UUID.randomUUID();

        MockHttpServletRequestBuilder request = put("/events/" + eventId)
                .param("title", "Updated Event")
                .param("description", "Updated description")
                .param("startDate", "2026-01-01T10:00")
                .param("endDate", "2026-01-01T12:00")
                .param("ticketPrice", "60.00")
                .param("maxCapacity", "200")
                .param("availableTickets", "200")
                .param("venueId", venueId.toString())
                .param("categoryId", categoryId.toString())
                .with(user(organizerPrincipal))
                .with(csrf());

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/events/my"))
                .andExpect(flash().attributeExists(SUCCESS_MESSAGE_ATTR));

        verify(eventService, times(1)).updateEvent(eq(eventId), any(EventEditRequest.class), eq(organizerPrincipal.getUsername()));
    }

    @Test
    void putInvalidEventEditRequest_redirectsToFormWithErrors() throws Exception {

        UUID eventId = UUID.randomUUID();

        MockHttpServletRequestBuilder request = put("/events/" + eventId)
                .param("title", "")
                .param("description", "")
                .with(user(organizerPrincipal))
                .with(csrf());

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/events/" + eventId + "/edit"))
                .andExpect(flash().attributeExists("eventEditRequest"))
                .andExpect(flash().attributeExists(BINDING_MODEL + "eventEditRequest"))
                .andExpect(flash().attributeExists(ERROR_MESSAGE_ATTR));

        // 3. Verify
        verify(eventService, never()).updateEvent(any(), any(), any());
    }

    @Test
    void getUnauthenticatedRequestToEventDetails_returnsEventDetailsView() throws Exception {

        UUID eventId = UUID.randomUUID();
        Event mockEvent = createMockEvent("Event", "Description");
        mockEvent.setMaxCapacity(100);

        when(eventService.getById(eventId)).thenReturn(mockEvent);
        when(bookingService.getCountTicketsByEventId(eventId)).thenReturn(20);

        MockHttpServletRequestBuilder request = get("/events/" + eventId);

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isOk())
                .andExpect(view().name("event/event-details"))
                .andExpect(model().attributeExists("event", "alreadyBooked", "availableTickets"))
                .andExpect(model().attribute("alreadyBooked", false));

        verify(eventService, times(1)).getById(eventId);
        verify(bookingService, times(1)).getCountTicketsByEventId(eventId);
        verify(bookingService, never()).hasUserBookedEvent(any(), any());
    }

    @Test
    void getAuthenticatedUserRequestToEventDetails_checksIfAlreadyBooked() throws Exception {

        UUID eventId = UUID.randomUUID();
        Event mockEvent = createMockEvent("Event", "Description");
        mockEvent.setMaxCapacity(100);

        when(eventService.getById(eventId)).thenReturn(mockEvent);
        when(bookingService.getCountTicketsByEventId(eventId)).thenReturn(20);
        when(bookingService.hasUserBookedEvent(principal.getUsername(), eventId)).thenReturn(true);

        MockHttpServletRequestBuilder request = get("/events/" + eventId)
                .with(user(principal));

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isOk())
                .andExpect(view().name("event/event-details"))
                .andExpect(model().attribute("alreadyBooked", true));

        verify(bookingService, times(1)).hasUserBookedEvent(principal.getUsername(), eventId);
    }

    @Test
    void deleteAuthenticatedOrganizerRequestToDeleteOwnEvent_deletesEventAndRedirects() throws Exception {

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername(organizerPrincipal.getUsername());
        user.setRole(Role.EVENT_ORGANIZER);
        user.setEmail("organizer2@example.com");
        user.setPassword("encodedPassword");
        user.setBlocked(false);

        UUID eventId = UUID.randomUUID();
        Event mockEvent = createMockEvent("My Event", "Description");
        mockEvent.setOrganizer(user);

        when(eventService.getByIdWithDetails(eventId)).thenReturn(mockEvent);
        when(userService.hasRole(organizerPrincipal.getUsername(), Role.ADMIN)).thenReturn(false);

        MockHttpServletRequestBuilder request = delete("/events/" + eventId)
                .with(user(organizerPrincipal))
                .with(csrf());

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/events/my"))
                .andExpect(flash().attributeExists(SUCCESS_MESSAGE_ATTR));

        verify(eventService, times(1)).deleteEvent(eventId);
    }

    @Test
    void deleteAuthenticatedOrganizerRequestToDeleteOthersEvent_redirectsWithError() throws Exception {

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("someOrganizer");
        user.setRole(Role.EVENT_ORGANIZER);
        user.setEmail("organizer2@example.com");
        user.setPassword("encodedPassword");
        user.setBlocked(false);

        UUID eventId = UUID.randomUUID();
        Event mockEvent = createMockEvent("Other Event", "Description");
        mockEvent.setOrganizer(user);

        when(eventService.getByIdWithDetails(eventId)).thenReturn(mockEvent);
        when(userService.hasRole(organizerPrincipal.getUsername(), Role.ADMIN)).thenReturn(false);

        MockHttpServletRequestBuilder request = delete("/events/" + eventId)
                .with(user(organizerPrincipal))
                .with(csrf());

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/events/my"))
                .andExpect(flash().attributeExists(ERROR_MESSAGE_ATTR));

        verify(eventService, never()).deleteEvent(any());
    }

    @Test
    void deleteAuthenticatedAdminRequestToDeleteAnyEvent_deletesEventAndRedirects() throws Exception {

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("someOrganizer");
        user.setRole(Role.EVENT_ORGANIZER);
        user.setEmail("organizer2@example.com");
        user.setPassword("encodedPassword");
        user.setBlocked(false);

        UUID eventId = UUID.randomUUID();
        Event mockEvent = createMockEvent("Any Event", "Description");
        mockEvent.setOrganizer(user);

        when(eventService.getByIdWithDetails(eventId)).thenReturn(mockEvent);
        when(userService.hasRole(adminPrincipal.getUsername(), Role.ADMIN)).thenReturn(true);

        MockHttpServletRequestBuilder request = delete("/events/" + eventId)
                .with(user(adminPrincipal))
                .with(csrf());

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/events/my"))
                .andExpect(flash().attributeExists(SUCCESS_MESSAGE_ATTR));

        verify(eventService, times(1)).deleteEvent(eventId);
    }

    @Test
    void deleteAuthenticatedUserRequestToDeleteEvent_returnsForbidden() throws Exception {

        UUID eventId = UUID.randomUUID();

        MockHttpServletRequestBuilder request = delete("/events/" + eventId)
                .with(user(principal))
                .with(csrf());

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isForbidden());

        verify(eventService, never()).deleteEvent(any());
    }

    // ============================
    // GET /events/new
    // ============================
//    @Test
//    void testShowCreateForm() throws Exception {
//
//        when(categoryService.getAll()).thenReturn(Collections.emptyList());
//        when(venueService.getAll()).thenReturn(Collections.emptyList());
//
//        MockHttpServletRequestBuilder request = get("/events/new")
//                .with(user(organizerPrincipal));
//
//        ResultActions response = mockMvc.perform(request);
//
//        response.andExpect(status().isOk())
//                .andExpect(view().name("event/event-add"))
//                .andExpect(model().attributeExists("eventCreateRequest"))
//                .andExpect(model().attributeExists("categories"))
//                .andExpect(model().attributeExists("venues"));
//    }

    // ============================
    // POST /events  (create event)
    // ============================
//    @Test
//    void testCreateEvent_success() throws Exception {
//
//        MockHttpServletRequestBuilder request = post("/events")
//                .param("title", "Concert")
//                .param("description", "Desc")
//                .param("startDate", "2030-01-01T10:00")
//                .param("endDate", "2030-01-01T12:00")
//                .param("ticketPrice", "50.00")
//                .param("maxCapacity", "200")
//                .param("availableTickets", "200")
//                .param("venueId", UUID.randomUUID().toString())
//                .param("categoryId", UUID.randomUUID().toString())
//                .with(user(organizerPrincipal))
//                .with(csrf());
//
//        ResultActions response = mockMvc.perform(request);
//
//        response.andExpect(status().is3xxRedirection())
//                .andExpect(redirectedUrl("/events/my"));
//    }

    // ============================
    // GET /events/{id}
    // ============================
//    @Test
//    void testGetEventDetails() throws Exception {
//
//        UUID id = UUID.randomUUID();
//
//        // ---- Valid Event Instance ----
//        Venue venue = new Venue();
//        venue.setId(UUID.randomUUID());
//        venue.setName("Test Venue");
//
//        Category category = new Category();
//        category.setId(UUID.randomUUID());
//        category.setName("Music");
//
//        User organizer = new User();
//        organizer.setUsername("testUser");
//
//        Event e = new Event();
//        e.setId(id);
//        e.setTitle("Test Event");
//        e.setDescription("Desc");
//        e.setStartDate(LocalDateTime.now());
//        e.setEndDate(LocalDateTime.now().plusHours(2));
//        e.setTicketPrice(BigDecimal.TEN);
//        e.setMaxCapacity(100);         // REQUIRED
//        e.setAvailableTickets(100);    // not required but fine
//        e.setVenue(venue);
//        e.setOrganizer(organizer);
//        e.setCategory(category);
//
//        when(eventService.getById(id)).thenReturn(e);
//
//        // Controller calls this:
//        when(bookingService.hasUserBookedEvent("testUser", id)).thenReturn(false);
//        when(bookingService.getCountTicketsByEventId(id)).thenReturn(5);
//
//        mockMvc.perform(get("/events/" + id).with(user(organizerPrincipal)))
//                .andExpect(status().isOk())
//                .andExpect(view().name("event/event-details"))
//                .andExpect(model().attributeExists("event"))
//                .andExpect(model().attribute("alreadyBooked", false))
//                .andExpect(model().attribute("availableTickets", 95)); // 100 - 5
//    }

    // ============================
    // GET /events/{id}/edit
    // ============================

//    @Test
//    void testShowEditForm() throws Exception {
//
//        UUID id = UUID.randomUUID();
//
//        // VALID VENUE
//        Venue venue = new Venue();
//        venue.setId(UUID.randomUUID());
//        venue.setName("Venue1");
//
//        // VALID CATEGORY
//        Category category = new Category();
//        category.setId(UUID.randomUUID());
//        category.setName("Cat1");
//
//        // ORGANIZER
//        User organizer = new User();
//        organizer.setUsername("testUser");
//
//        // FULL VALID EVENT (DtoMapper-safe)
//        Event e = new Event();
//        e.setId(id);
//        e.setTitle("Test Event");
//        e.setDescription("Desc");
//        e.setStartDate(LocalDateTime.now());
//        e.setEndDate(LocalDateTime.now().plusHours(2));
//        e.setTicketPrice(BigDecimal.TEN);
//        e.setMaxCapacity(100);
//        e.setAvailableTickets(100);
//        e.setVenue(venue);
//        e.setOrganizer(organizer);
//        e.setCategory(category);
//
//        when(eventService.getByIdWithDetails(id)).thenReturn(e);
//        when(userService.hasRole("testUser", Role.ADMIN)).thenReturn(false);
//        when(categoryService.getAll()).thenReturn(Collections.emptyList());
//        when(venueService.getAll()).thenReturn(Collections.emptyList());
//
//        mockMvc.perform(get("/events/" + id + "/edit")
//                        .with(user(organizerPrincipal)))
//                .andExpect(status().isOk())
//                .andExpect(view().name("event/event-edit"))
//                .andExpect(model().attributeExists("eventEditRequest"))
//                .andExpect(model().attributeExists("categories"))
//                .andExpect(model().attributeExists("venues"));
//    }

    // ============================
    // PUT /events/{id}
    // ============================
//    @Test
//    void testUpdateEvent() throws Exception {
//
//        UUID id = UUID.randomUUID();
//
//        doNothing().when(eventService).updateEvent(eq(id), any(EventEditRequest.class), eq("testUser"));
//
//        mockMvc.perform(put("/events/" + id)
//                        .with(user(principal))
//                        .param("title", "Updated")
//                        .param("description", "Updated")
//                        .param("venueId", UUID.randomUUID().toString())
//                        .param("categoryId", UUID.randomUUID().toString())
//                        .param("startDate", "2030-01-01T10:00")
//                        .param("endDate", "2030-01-01T12:00")
//                )
//                .andExpect(status().is3xxRedirection())
//                .andExpect(redirectedUrl("/events/my"));
//    }

    // ============================
    // DELETE /events/{id}
    // ============================
//    @Test
//    void testDeleteEvent() throws Exception {
//
//        UUID id = UUID.randomUUID();
//
//        doNothing().when(eventService).deleteEvent(id);
//
//        mockMvc.perform(delete("/events/" + id).with(user(principal)))
//                .andExpect(status().is3xxRedirection())
//                .andExpect(redirectedUrl("/events/my"));
//    }

//    @Test
//    void testDeleteEvent() throws Exception {
//
//        UUID id = UUID.randomUUID();
//
//        // Organizer matches principal
//        User organizer = new User();
//        organizer.setUsername("testUser");
//
//        Event event = new Event();
//        event.setId(id);
//        event.setOrganizer(organizer);
//
//        // Mock services:
//        when(eventService.getByIdWithDetails(id)).thenReturn(event);
//        when(userService.hasRole("testUser", Role.ADMIN)).thenReturn(false);
//
//        doNothing().when(eventService).deleteEvent(id);
//
//        AuthenticationMetadata organizerPrincipal =
//                new AuthenticationMetadata(UUID.randomUUID(), "testUser", "password",
//                        Role.EVENT_ORGANIZER, false, null);
//
//        mockMvc.perform(delete("/events/" + id)
//                        .with(user(organizerPrincipal)))
//                .andExpect(status().is3xxRedirection())
//                .andExpect(redirectedUrl("/events/my"));
//    }

}
