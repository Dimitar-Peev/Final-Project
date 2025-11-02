package com.exam.eventhub.venue.service;

import com.exam.eventhub.exception.VenueAlreadyExistException;
import com.exam.eventhub.exception.VenueDuplicateException;
import com.exam.eventhub.exception.VenueNotFoundException;
import com.exam.eventhub.venue.model.Venue;
import com.exam.eventhub.venue.repository.VenueRepository;
import com.exam.eventhub.web.dto.VenueCreateRequest;
import com.exam.eventhub.web.dto.VenueEditRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@AllArgsConstructor
public class VenueService {

    private final VenueRepository venueRepository;

    public void initData() {
        if (venueRepository.count() == 0) {
            initializeVenues();
        }
    }

    private void initializeVenues() {
        log.info("Initializing venues...");
        List<Venue> defaultVenues = new ArrayList<>();

        Venue centralPark = new Venue("Central Park Amphitheater", "Central Park Avenue 1", "Sofia",
                5000, new BigDecimal("500.00"), "info@centralpark.bg",
                "+359888123456", "Beautiful outdoor amphitheater in the heart of Sofia");

        Venue techHub = new Venue("Tech Hub Conference Center", "Tech Park Street 15", "Plovdiv",
                300, new BigDecimal("200.00"), "contact@techhub.bg",
                "+359888234567", "Modern conference center with state-of-the-art technology");

        Venue gallery = new Venue("Gallery Modern", "Art Street 5", "Varna",
                150, new BigDecimal("100.00"), "info@gallerymodern.bg",
                "+359888345678", "Contemporary art gallery with exhibition spaces");

        Venue stadium = new Venue("City Stadium", "Sports Complex 10", "Burgas",
                15000, new BigDecimal("1000.00"), "bookings@citystadium.bg",
                "+359888456789", "Large stadium for major sporting events");

        Venue business = new Venue("Business Center Elite", "Business District 20", "Sofia",
                500, new BigDecimal("300.00"), "events@businesselite.bg",
                "+359888567890", "Premium business center for corporate events");

        Venue airport = new Venue("Airport", "Business District 20", "Sliven",
                50_000, new BigDecimal("30.00"), "events@businesselite.bg",
                "+359888567890", "Airport venue with a large business space");

        defaultVenues.add(centralPark);
        defaultVenues.add(techHub);
        defaultVenues.add(gallery);
        defaultVenues.add(stadium);
        defaultVenues.add(business);
        defaultVenues.add(airport);

        this.venueRepository.saveAll(defaultVenues);

        log.info("Venues initialized!");
    }

    @Cacheable("venues")
    public List<Venue> getAll() {
        return this.venueRepository.findAll();
    }

    @CacheEvict(value = "venues", allEntries = true)
    public Venue add(VenueCreateRequest venueCreateRequest) {
        String name = venueCreateRequest.getName();
        log.info("Creating venue: {}", name);

        Optional<Venue> byName = venueRepository.findByName(name);
        if (byName.isPresent()) {
            throw new VenueAlreadyExistException("The venue '" + name + "' already exists.");
        }

        Venue venue = create(venueCreateRequest);

        Venue saved = venueRepository.save(venue);

        log.info("Venue [{}] (ID: [{}]) was successfully added.", saved.getName(), saved.getId());

        return saved;
    }

    private Venue create(VenueCreateRequest venueCreateRequest) {
        Venue venue = new Venue();

        venue.setName(venueCreateRequest.getName());
        venue.setAddress(venueCreateRequest.getAddress());
        venue.setCity(venueCreateRequest.getCity());
        venue.setCapacity(venueCreateRequest.getCapacity());
        venue.setHourlyRate(venueCreateRequest.getHourlyRate());
        venue.setContactEmail(venueCreateRequest.getContactEmail());
        venue.setContactPhone(venueCreateRequest.getContactPhone());
        venue.setDescription(venueCreateRequest.getDescription());

        return venue;
    }

    @CacheEvict(value = "venues", allEntries = true)
    public Venue update(UUID id, VenueEditRequest venueEditRequest) {

        Venue venue = getById(id);

        Optional<Venue> byName = venueRepository.findByName(venueEditRequest.getName());
        if (byName.isPresent()) {
            throw new VenueDuplicateException("The venue '" + venueEditRequest.getName() + "' already exists.", id);
        }

        venue.setName(venueEditRequest.getName());
        venue.setAddress(venueEditRequest.getAddress());
        venue.setCity(venueEditRequest.getCity());
        venue.setCapacity(venueEditRequest.getCapacity());
        venue.setHourlyRate(venueEditRequest.getHourlyRate());
        venue.setContactEmail(venueEditRequest.getContactEmail());
        venue.setContactPhone(venueEditRequest.getContactPhone());
        venue.setDescription(venueEditRequest.getDescription());

        Venue saved = venueRepository.save(venue);

        log.info("Venue [{}] (ID: [{}]) was successfully updated.", saved.getName(), saved.getId());

        return saved;
    }

    @CacheEvict(value = "venues", allEntries = true)
    public void delete(UUID id) {
        Venue venue = getById(id);

        this.venueRepository.delete(venue);

        log.info("Venue with ID [{}] was successfully deleted.", id);
    }

    public Venue getById(UUID id) {
        return this.venueRepository.findById(id)
                .orElseThrow(() -> new VenueNotFoundException("Venue with ID [%s] was not found.".formatted(id)));
    }

    public Venue getByName(String name) {
        return this.venueRepository.findByName(name)
                .orElseThrow(() -> new VenueNotFoundException("Venue with name [%s] was not found.".formatted(name)));
    }

}
