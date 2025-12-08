package com.exam.app.util;

import com.exam.app.model.Payment;
import com.exam.app.model.PaymentStatus;
import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@UtilityClass
public class TestBuilder {

    public static Payment createMockPayment() {
        Payment payment = new Payment();

        payment.setId(UUID.randomUUID());
        payment.setBookingId(UUID.randomUUID());
        payment.setUserId(UUID.randomUUID());
        payment.setAmount(new BigDecimal("150.00"));
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setCreatedOn(LocalDateTime.now());
        payment.setCompletedOn(LocalDateTime.now());

        return payment;
    }
}
