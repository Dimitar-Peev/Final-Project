package com.exam.eventhub.booking.repository;

import com.exam.eventhub.booking.model.Booking;
import com.exam.eventhub.booking.model.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    List<Booking> findAllByUserUsernameOrderByBookingDateDesc(String username);

    List<Booking> findAllByOrderByBookingDateDesc();

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.event.id = :eventId")
    int countByEventId(@Param("eventId") UUID eventId);

    boolean existsByUserUsernameAndEventId(String username, UUID eventId);

    @Query("SELECT b FROM Booking b WHERE b.status = :status AND b.bookingDate < :expirationDate")
    List<Booking> findExpiredPendingBookings(
            @Param("status") BookingStatus status,
            @Param("expirationDate") LocalDateTime expirationDate
    );
}