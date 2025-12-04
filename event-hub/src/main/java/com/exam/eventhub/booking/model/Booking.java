package com.exam.eventhub.booking.model;

import com.exam.eventhub.event.model.Event;
import com.exam.eventhub.user.model.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false, columnDefinition = "INT CHECK (number_of_tickets BETWEEN 1 AND 10)")
    private int numberOfTickets;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @CreationTimestamp
    private LocalDateTime bookingDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status = BookingStatus.PENDING;

    @Column(nullable = false)
    private String customerEmail;

    @Column(nullable = true, updatable = true, columnDefinition = "VARCHAR(13)")
    private String customerPhone;

    @Column(nullable = true, columnDefinition = "VARCHAR(200)")
    private String specialRequests;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", referencedColumnName = "id", nullable = false)
    @NotNull(message = "Event is required")
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    @NotNull(message = "User is required")
    private User user;

    private LocalDateTime cancelledAt;

    private String cancellationReason;

    private UUID paymentId;

    @UpdateTimestamp
    private LocalDateTime paymentCompletedAt;
}