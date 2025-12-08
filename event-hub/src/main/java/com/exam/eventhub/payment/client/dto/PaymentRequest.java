package com.exam.eventhub.payment.client.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {

    private UUID bookingId;
    private UUID userId;
    private BigDecimal amount;
}
