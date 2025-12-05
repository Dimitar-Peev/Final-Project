package com.exam.eventhub.booking.model;

import lombok.Getter;

@Getter
public enum BookingStatus {
    PENDING("Pending Payment"),
    CONFIRMED("Confirmed"),
    CANCELLED("Cancelled"),
    REFUNDED("Refunded");

    private final String displayName;

    BookingStatus(String displayName) {
        this.displayName = displayName;
    }
}