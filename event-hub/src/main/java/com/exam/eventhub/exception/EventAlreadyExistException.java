package com.exam.eventhub.exception;

public class EventAlreadyExistException extends RuntimeException {
    public EventAlreadyExistException(String message) {
        super(message);
    }
}
