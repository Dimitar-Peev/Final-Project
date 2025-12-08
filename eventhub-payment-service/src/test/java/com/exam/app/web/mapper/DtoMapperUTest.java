package com.exam.app.web.mapper;

import com.exam.app.model.Payment;
import com.exam.app.web.dto.PaymentRequest;
import com.exam.app.web.dto.PaymentResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DtoMapperUTest {

    @Test
    void givenHappyPath_whenMappingPaymentRequestToPayment() {

        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setBookingId(java.util.UUID.randomUUID());
        paymentRequest.setUserId(java.util.UUID.randomUUID());
        paymentRequest.setAmount(new java.math.BigDecimal("150.00"));

        Payment resultDto = DtoMapper.mapToEntity(paymentRequest);

        assertNotNull(resultDto);
        assertEquals(paymentRequest.getBookingId(), resultDto.getBookingId());
        assertEquals(paymentRequest.getUserId(), resultDto.getUserId());
        assertEquals(paymentRequest.getAmount(), resultDto.getAmount());
    }

    @Test
    void givenHappyPath_whenMappingPaymentToPaymentResponse() {

        Payment payment = new Payment();
        payment.setBookingId(java.util.UUID.randomUUID());
        payment.setUserId(java.util.UUID.randomUUID());
        payment.setAmount(new java.math.BigDecimal("150.00"));

        PaymentResponse resultDto = DtoMapper.mapToResponse(payment);

        assertNotNull(resultDto);
        assertEquals(payment.getBookingId(), resultDto.getBookingId());
        assertEquals(payment.getUserId(), resultDto.getUserId());
        assertEquals(payment.getAmount(), resultDto.getAmount());
    }
}
