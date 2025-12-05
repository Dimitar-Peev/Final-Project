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
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.exam.eventhub.common.Constants.*;

@Slf4j
@Service
@AllArgsConstructor
public class EventService {

    private static final String ENTITY_NAME = "Event";

    private final EventRepository eventRepository;
    private final UserService userService;
    private final VenueService venueService;
    private final CategoryService categoryService;

    public void initData() {
        if (eventRepository.count() == 0) {
            initializeEvents();
        }
    }

    private void initializeEvents() {
        log.info("Initializing events...");
        List<Event> defaultEvents = new ArrayList<>();

        User admin = userService.getByUsername("admin");
        User organizer = userService.getByUsername("organizer");

        Category music = categoryService.getByName("Music");
        Category tech = categoryService.getByName("Technology");
        Category business = categoryService.getByName("Business");
        Category art = categoryService.getByName("Art");
        Category sports = categoryService.getByName("Sports");
        Category education = categoryService.getByName("Education");
        Category cars = categoryService.getByName("Cars");

        Venue centralPark = venueService.getByName("Central Park Amphitheater");
        Venue techHub = venueService.getByName("Tech Hub Conference Center");
        Venue gallery = venueService.getByName("Gallery Modern");
        Venue stadium = venueService.getByName("City Stadium");
        Venue businessCenter = venueService.getByName("Business Center Elite");
        Venue airport = venueService.getByName("Airport");

        Event musicFestival = new Event(
                "Summer Music Festival 2026",
                "Join us for an amazing summer music festival featuring top artists from around the world. " +
                        "Experience live performances, food trucks, and unforgettable memories under the stars.",
                LocalDateTime.of(2026, 6, 15, 18, 0),
                LocalDateTime.of(2026, 6, 15, 23, 0),
                new BigDecimal("45.00"),
                3000,
                centralPark, admin, music
        );
        musicFestival.setStatus(EventStatus.PUBLISHED);

        Event jazzNight = new Event(
                "Jazz Night at the Gallery",
                "Intimate jazz performance in a cozy gallery setting. Enjoy smooth jazz melodies while " +
                        "surrounded by beautiful artwork.",
                LocalDateTime.of(2026, 3, 20, 20, 0),
                LocalDateTime.of(2026, 3, 20, 23, 0),
                new BigDecimal("25.00"),
                100,
                gallery, organizer, music
        );
        jazzNight.setStatus(EventStatus.PUBLISHED);

        Event techConf = new Event(
                "Tech Innovation Conference 2026",
                "Leading technology conference featuring keynotes from industry experts, workshops on " +
                        "emerging technologies, and networking opportunities.",
                LocalDateTime.of(2026, 4, 12, 9, 0),
                LocalDateTime.of(2026, 4, 13, 17, 0),
                new BigDecimal("89.00"),
                250,
                techHub, admin, tech
        );
        techConf.setStatus(EventStatus.PUBLISHED);

        Event aiWorkshop = new Event(
                "AI & Machine Learning Workshop",
                "Hands-on workshop covering the fundamentals of AI and machine learning. Perfect for " +
                        "developers looking to expand their skillset.",
                LocalDateTime.of(2026, 5, 8, 10, 0),
                LocalDateTime.of(2026, 5, 8, 16, 0),
                new BigDecimal("120.00"),
                50,
                techHub, organizer, education
        );
        aiWorkshop.setStatus(EventStatus.PUBLISHED);

        Event networkingEvent = new Event(
                "Business Networking Gala",
                "Premium networking event for business professionals. Connect with industry leaders, " +
                        "explore partnership opportunities, and expand your professional network.",
                LocalDateTime.of(2026, 3, 25, 18, 30),
                LocalDateTime.of(2026, 3, 25, 22, 0),
                new BigDecimal("75.00"),
                300,
                businessCenter, admin, business
        );
        networkingEvent.setStatus(EventStatus.PUBLISHED);

        Event marathon = new Event(
                "City Marathon 2026",
                "Annual city marathon open to runners of all levels. Choose from 5K, 10K, half marathon, " +
                        "or full marathon distances. Registration includes race packet and finisher medal.",
                LocalDateTime.of(2026, 4, 20, 7, 0),
                LocalDateTime.of(2026, 4, 20, 15, 0),
                new BigDecimal("15.00"),
                2000,
                stadium, organizer, sports
        );
        marathon.setStatus(EventStatus.PUBLISHED);

        Event artExhibition = new Event(
                "Contemporary Art Exhibition",
                "Showcase of contemporary art from local and international artists. Explore diverse " +
                        "artistic expressions and meet the artists during the opening reception.",
                LocalDateTime.of(2026, 5, 15, 17, 0),
                LocalDateTime.of(2026, 5, 15, 21, 0),
                new BigDecimal("20.00"),
                120,
                gallery, admin, art
        );
        artExhibition.setStatus(EventStatus.PUBLISHED);

        Event digitalMarketing = new Event(
                "Digital Marketing Masterclass",
                "Comprehensive workshop covering digital marketing strategies, social media marketing, " +
                        "SEO, and analytics. Includes practical exercises and case studies.",
                LocalDateTime.of(2026, 6, 5, 9, 0),
                LocalDateTime.of(2026, 6, 5, 17, 0),
                new BigDecimal("150.00"),
                80,
                businessCenter, organizer, education
        );
        digitalMarketing.setStatus(EventStatus.PUBLISHED);

        Event carsEvent = new Event(
                "Drag Weekend Sliven",
                "Закриваме пистов сезон 2025 със събитие само за собственици на американски возила, без значение от вид, марка и модел.",
                LocalDateTime.of(2026, 10, 4, 9, 0),
                LocalDateTime.of(2026, 10, 5, 17, 0),
                new BigDecimal("30.00"),
                50_000, airport, organizer, cars
        );
        carsEvent.setStatus(EventStatus.PUBLISHED);

        Event draftEvent = new Event(
                "Wine Tasting Experience",
                "Premium wine tasting event featuring selections from top Bulgarian wineries. " +
                        "Learn about wine making process and food pairing techniques.",
                LocalDateTime.of(2026, 7, 10, 19, 0),
                LocalDateTime.of(2026, 7, 10, 22, 0),
                new BigDecimal("65.00"),
                60,
                gallery, admin, art
        );

        defaultEvents.add(musicFestival);
        defaultEvents.add(jazzNight);
        defaultEvents.add(techConf);
        defaultEvents.add(aiWorkshop);
        defaultEvents.add(networkingEvent);
        defaultEvents.add(marathon);
        defaultEvents.add(artExhibition);
        defaultEvents.add(digitalMarketing);
        defaultEvents.add(carsEvent);
        defaultEvents.add(draftEvent);

        this.eventRepository.saveAll(defaultEvents);

        log.info("Events initialized!");
    }

    @Transactional
    @CacheEvict(value = {"events-simple", "events-detailed"}, allEntries = true)
    public Event add(EventCreateRequest eventCreateRequest, String username) {
        String title = eventCreateRequest.getTitle();
        log.info("Creating event: {}", title);

        Optional<Event> byTitle = eventRepository.findByTitle(title);
        if (byTitle.isPresent()) {
            throw new EventAlreadyExistException("The event '" + title + "' already exists.");
        }

        User organizer = userService.getByUsername(username);
        Venue venue = venueService.getById(eventCreateRequest.getVenueId());
        Category category = categoryService.getById(eventCreateRequest.getCategoryId());

        Event event = create(eventCreateRequest);
        event.setVenue(venue);
        event.setOrganizer(organizer);
        event.setCategory(category);

        Event saved = eventRepository.save(event);

        log.info("Event [{}] (ID: [{}]) was successfully added.", saved.getTitle(), saved.getId());

        return saved;
    }

    private Event create(EventCreateRequest eventCreateRequest) {
        Event event = new Event();

        event.setTitle(eventCreateRequest.getTitle());
        event.setDescription(eventCreateRequest.getDescription());
        event.setStartDate(eventCreateRequest.getStartDate());
        event.setEndDate(eventCreateRequest.getEndDate());
        event.setTicketPrice(eventCreateRequest.getTicketPrice());
        event.setMaxCapacity(eventCreateRequest.getMaxCapacity());
        event.setStatus(EventStatus.DRAFT);
        event.setAvailableTickets(eventCreateRequest.getMaxCapacity());

        return event;
    }

    public void saveEvent(Event event) {
        this.eventRepository.save(event);
    }

    @Cacheable("events-simple")
    public List<Event> getAll() {
        return eventRepository.findAll();
    }

    public List<Event> getEventsByOrganizer(String username) {
        return eventRepository.findWithDetailsByOrganizerUsername(username);
    }

    @Cacheable("events-detailed")
    public List<Event> getAllWithDetails() {
        return eventRepository.findAllWithDetails();
    }

    @CacheEvict(value = {"events-simple", "events-detailed"}, allEntries = true)
    public void updateEvent(UUID id, EventEditRequest model, String username) {
        Event event = getByIdWithDetails(id);

        User user = userService.getByUsername(username);
        if (!user.getRole().equals(Role.ADMIN) && !event.getOrganizer().getUsername().equals(username)) {
            throw new SecurityException(NOT_ALLOWED.formatted("edit"));
        }

        Venue venue = venueService.getById(model.getVenueId());
        Category category = categoryService.getById(model.getCategoryId());

        event.setTitle(model.getTitle());
        event.setDescription(model.getDescription());
        event.setStartDate(model.getStartDate());
        event.setEndDate(model.getEndDate());
        event.setTicketPrice(model.getTicketPrice());
        event.setVenue(venue);
        event.setCategory(category);

        int soldTickets = event.getMaxCapacity() - event.getAvailableTickets();
        int newAvailableTickets = model.getMaxCapacity() - soldTickets;

        if (newAvailableTickets < 0) {
            throw new IllegalStateException("Cannot reduce capacity below sold tickets");
        }

        event.setMaxCapacity(model.getMaxCapacity());
        event.setAvailableTickets(newAvailableTickets);

        eventRepository.save(event);
    }

    @CacheEvict(value = {"events-simple", "events-detailed"}, allEntries = true)
    public void deleteEvent(UUID id) {
        Event event = getByIdWithDetails(id);

        this.eventRepository.delete(event);

        String message = ID_DELETED_SUCCESSFUL.formatted(ENTITY_NAME, id);
        log.info(message);
    }

    @Transactional(readOnly = true)
    public List<Event> searchEvents(String keyword, String city, UUID categoryId, UUID venueId) {
        String searchKeyword = keyword != null && !keyword.isBlank() ? keyword : null;
        String searchCity = city != null && !city.isBlank() ? city : null;

        return eventRepository.searchEvents(searchKeyword, searchCity, categoryId, venueId);
    }

    public Event getById(UUID id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new EventNotFoundException(ID_NOT_FOUND.formatted(ENTITY_NAME, id)));
    }

    public Event getByIdWithDetails(UUID id) {
        return eventRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new EventNotFoundException(ID_NOT_FOUND.formatted(ENTITY_NAME, id)));
    }
}
