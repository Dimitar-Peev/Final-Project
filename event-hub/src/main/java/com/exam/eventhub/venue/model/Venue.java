package com.exam.eventhub.venue.model;

import com.exam.eventhub.event.model.Event;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "venues")
public class Venue {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false, length = 200)
    private String address;

    @Column(nullable = false, length = 50)
    private String city;

    @Column(nullable = false, columnDefinition = "INT CHECK (capacity BETWEEN 1 AND 100000)")
    private Integer capacity;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal hourlyRate;

    @Column(length = 50)
    private String contactEmail;

    @Column(length = 13)
    private String contactPhone;

    @Column(length = 500)
    private String description;

    @OneToMany(mappedBy = "venue", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Event> events = new HashSet<>();

    public Venue(String name, String address, String city, Integer capacity, BigDecimal hourlyRate,
                 String contactEmail, String contactPhone, String description) {
        this.name = name;
        this.address = address;
        this.city = city;
        this.capacity = capacity;
        this.hourlyRate = hourlyRate;
        this.contactEmail = contactEmail;
        this.contactPhone = contactPhone;
        this.description = description;
    }
}