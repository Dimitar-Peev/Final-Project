package com.exam.app.web.mapper;

import com.exam.app.model.Payment;
import com.exam.app.web.dto.PaymentRequest;
import com.exam.app.web.dto.PaymentResponse;
import lombok.experimental.UtilityClass;

@UtilityClass
public class DtoMapper {

    public static Payment mapToEntity(PaymentRequest paymentRequest) {
        Payment payment = new Payment();

        payment.setBookingId(paymentRequest.getBookingId());
        payment.setUserId(paymentRequest.getUserId());
        payment.setAmount(paymentRequest.getAmount());

        return payment;
    }

    public static PaymentResponse mapToResponse(Payment payment) {
        PaymentResponse paymentResponse = new PaymentResponse();

        paymentResponse.setPaymentId(payment.getId());
        paymentResponse.setBookingId(payment.getBookingId());
        paymentResponse.setUserId(payment.getUserId());
        paymentResponse.setAmount(payment.getAmount());
        paymentResponse.setStatus(payment.getStatus().name());
        paymentResponse.setCreatedOn(payment.getCreatedOn());

        return paymentResponse;
    }
}
