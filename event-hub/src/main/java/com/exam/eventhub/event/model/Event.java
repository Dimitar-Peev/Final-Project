package com.exam.eventhub.event.model;

import com.exam.eventhub.booking.model.Booking;
import com.exam.eventhub.category.model.Category;
import com.exam.eventhub.user.model.User;
import com.exam.eventhub.venue.model.Venue;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(unique = true, nullable = false)
    private String title;

    @Column(length = 5000, nullable = true)
    private String description;

    @Column(nullable = false)
    private LocalDateTime startDate;

    @Column(nullable = false)
    private LocalDateTime endDate;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal ticketPrice;

    @Column(nullable = false)
    private Integer maxCapacity;

    @Column(nullable = false)
    private int availableTickets;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventStatus status = EventStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id", referencedColumnName = "id", nullable = false)
    @NotNull(message = "Venue is required")
    private Venue venue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizer_id", referencedColumnName = "id", nullable = false)
    @NotNull(message = "Organizer is required")
    private User organizer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", referencedColumnName = "id", nullable = false)
    @NotNull(message = "Category is required")
    private Category category;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Booking> bookings = new HashSet<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public Event(String title, String description, LocalDateTime startDate, LocalDateTime endDate,
                 BigDecimal ticketPrice, Integer maxCapacity, Venue venue, User organizer, Category category) {
        this.title = title;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
        this.ticketPrice = ticketPrice;
        this.maxCapacity = maxCapacity;
        this.availableTickets = maxCapacity;
        this.venue = venue;
        this.organizer = organizer;
        this.category = category;
    }

    public int getAvailableTicketsSafe(int bookingCount) {
        return this.maxCapacity - bookingCount;
    }
}