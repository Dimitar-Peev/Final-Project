package com.exam.eventhub.exception;

import java.util.UUID;

public class VenueDuplicateException extends RuntimeException {

    private final UUID venueId;

    public VenueDuplicateException(String message, UUID venueId) {
        super(message);
        this.venueId = venueId;
    }

    public UUID getVenueId() {
        return venueId;
    }
}