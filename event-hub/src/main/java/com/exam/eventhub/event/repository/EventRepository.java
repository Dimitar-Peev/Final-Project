package com.exam.eventhub.event.repository;

import com.exam.eventhub.event.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {

    Optional<Event> findByTitle(String title);

    @Query("""
            SELECT DISTINCT e FROM Event e
            LEFT JOIN FETCH e.bookings b
            JOIN FETCH e.category
            JOIN FETCH e.venue
            JOIN FETCH e.organizer
            WHERE e.organizer.username = :username
            """)
    List<Event> findWithDetailsByOrganizerUsername(@Param("username") String username);

    @Query("""
            SELECT DISTINCT e FROM Event e
            LEFT JOIN FETCH e.bookings b
            JOIN FETCH e.category
            JOIN FETCH e.venue
            JOIN FETCH e.organizer
            """)
    List<Event> findAllWithDetails();

    @Query("""
            SELECT e FROM Event e
            LEFT JOIN FETCH e.bookings b
            JOIN FETCH e.category
            JOIN FETCH e.venue
            JOIN FETCH e.organizer
            WHERE e.id = :id
            """)
    Optional<Event> findByIdWithDetails(UUID id);

    @Query("""
        SELECT e FROM Event e
        WHERE (:keyword IS NULL OR LOWER(e.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:city IS NULL OR LOWER(e.venue.city) = LOWER(:city))
          AND (:categoryId IS NULL OR e.category.id = :categoryId)
          AND (:venueId IS NULL OR e.venue.id = :venueId)
    """)
    List<Event> searchEvents(@Param("keyword") String keyword,
                             @Param("city") String city,
                             @Param("categoryId") UUID categoryId,
                             @Param("venueId") UUID venueId);
}