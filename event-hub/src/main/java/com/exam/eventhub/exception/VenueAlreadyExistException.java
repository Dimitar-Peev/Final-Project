package com.exam.eventhub.exception;

public class VenueAlreadyExistException extends RuntimeException {
    public VenueAlreadyExistException(String message) {
        super(message);
    }
}
