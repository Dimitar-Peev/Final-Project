package com.exam.eventhub.web.dto;

import com.exam.eventhub.validation.PhoneNumber;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class VenueEditRequest {

    @NotBlank(message = "Venue name is required")
    @Size(min = 2, max = 100, message = "Venue name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Address is required")
    @Size(min = 5, max = 200, message = "Address must be between 5 and 200 characters")
    private String address;

    @NotBlank(message = "City is required")
    @Size(min = 2, max = 50, message = "City must be between 2 and 50 characters")
    private String city;

    @NotNull(message = "Capacity is required")
    @Min(value = 1, message = "Capacity must be at least 1")
    @Max(value = 100000, message = "Capacity cannot exceed 100,000")
    private Integer capacity;

    @NotNull(message = "Hourly rate is required")
    @DecimalMin(value = "0.0", message = "Hourly rate cannot be negative")
    @DecimalMax(value = "10000.0", message = "Hourly rate cannot exceed 10,000")
    private BigDecimal hourlyRate;

    @Email(message = "Please provide a valid email")
    @Size(max = 50, message = "Email cannot exceed 50 characters")
    private String contactEmail;

    @PhoneNumber
    private String contactPhone;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;
}
