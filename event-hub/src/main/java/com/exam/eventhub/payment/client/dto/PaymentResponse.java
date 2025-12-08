package com.exam.eventhub.payment.client.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private UUID paymentId;
    private UUID bookingId;
    private UUID userId;
    private BigDecimal amount;
    private String status;
    private String message;
    private LocalDateTime createdOn;
}
