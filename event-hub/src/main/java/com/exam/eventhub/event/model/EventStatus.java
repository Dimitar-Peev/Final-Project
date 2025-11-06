package com.exam.eventhub.event.model;

import lombok.Getter;

@Getter
public enum EventStatus {
    DRAFT("Draft"),
    PUBLISHED("Published"),
    CANCELLED("Cancelled"),
    COMPLETED("Completed");

    private final String displayName;

    EventStatus(String displayName) {
        this.displayName = displayName;
    }
}