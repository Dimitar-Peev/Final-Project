package com.exam.eventhub.web.dto;

import com.exam.eventhub.validation.PhoneNumber;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class BookingCreateRequest {

    @NotNull(message = "Event is required")
    private UUID eventId;

    @NotNull(message = "Number of tickets is required")
    @Min(value = 1, message = "Must book at least 1 ticket")
    @Max(value = 10, message = "Cannot book more than 10 tickets at once")
    private int numberOfTickets;

    @NotBlank(message = "Customer email is required")
    @Email(message = "Please provide a valid email")
    private String customerEmail;

    @NotBlank(message = "Customer phone is required")
    @PhoneNumber
    private String customerPhone;

    @Size(max = 200, message = "Special requests cannot exceed 200 characters")
    private String specialRequests;
}
