package com.exam.eventhub.category.model;

import com.exam.eventhub.event.model.Event;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false, length = 50)
    private String name;

    @Column(length = 5000)
    private String description;

    @Column(length = 7, nullable = false)
    private String color = "#667eea";

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Event> events = new HashSet<>();

    public Category(String name, String description, String color) {
        this.name = name;
        this.description = description;
        this.color = color;
    }
}