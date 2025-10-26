package com.exam.eventhub.user.model;

import lombok.Getter;

@Getter
public enum Role {
    USER("User"),
    EVENT_ORGANIZER("Event Organizer"),
    ADMIN("Administrator");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }
}
