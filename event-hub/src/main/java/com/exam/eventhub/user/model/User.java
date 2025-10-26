package com.exam.eventhub.user.model;

import com.exam.eventhub.booking.model.Booking;
import com.exam.eventhub.event.model.Event;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 20)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, length = 60)
    private String password;

    @Column(length = 20)
    private String firstName;

    @Column(length = 20)
    private String lastName;

    @Column(length = 13)
    private String phoneNumber;

    private String profileImageUrl;

    @Column(nullable = false)
    private boolean notificationsEnabled = true;

    @Column(updatable = false, nullable = false)
    @CreationTimestamp
    private LocalDateTime registrationDate;

    @Column(nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private boolean isBlocked;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("bookingDate DESC")
    private Set<Booking> bookings = new HashSet<>();

    @OneToMany(mappedBy = "organizer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("startDate ASC")
    private Set<Event> organizedEvents = new HashSet<>();

    public User(String username, String email, String password, String firstName, String lastName, String phoneNumber, Role role) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
        this.role = role;
    }
}