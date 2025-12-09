package com.exam.eventhub.exception;

public class BookingAlreadyConfirmedException extends RuntimeException {
    public BookingAlreadyConfirmedException(String message) {
        super(message);
    }
}