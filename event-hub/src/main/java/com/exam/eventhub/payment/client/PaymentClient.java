package com.exam.eventhub.payment.client;

import com.exam.eventhub.payment.client.dto.PaymentRequest;
import com.exam.eventhub.payment.client.dto.PaymentResponse;
import com.exam.eventhub.payment.client.dto.RefundRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "payment-service", url = "${eventhub.payment-service.base-url}" + "/api/v1/payments")
public interface PaymentClient {

    @PostMapping
    ResponseEntity<PaymentResponse> processPayment(@RequestBody PaymentRequest paymentRequest);

    @PostMapping("/{paymentId}/refunds")
    ResponseEntity<PaymentResponse> refundPayment(@PathVariable UUID paymentId, @RequestBody RefundRequest refundRequest);

    @GetMapping("/bookings/{bookingId}")
    ResponseEntity<PaymentResponse> getPaymentByBookingId(@PathVariable UUID bookingId);

    @GetMapping("/users/{userId}")
    ResponseEntity<List<PaymentResponse>> getPaymentsByUser(@PathVariable UUID userId);

    @GetMapping("/{paymentId}")
    ResponseEntity<PaymentResponse> getPaymentById(@PathVariable UUID paymentId);

    @GetMapping
    ResponseEntity<List<PaymentResponse>> getAllPayments();
}
