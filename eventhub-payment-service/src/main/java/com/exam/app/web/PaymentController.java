package com.exam.app.web;

import com.exam.app.model.Payment;
import com.exam.app.service.PaymentService;
import com.exam.app.web.dto.PaymentRequest;
import com.exam.app.web.dto.PaymentResponse;
import com.exam.app.web.dto.RefundRequest;
import com.exam.app.web.mapper.DtoMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(Paths.API_V1_BASE_PATH + "/payments")
@AllArgsConstructor
@Tag(name = "Payment Controller", description = "Payment Controller")
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "Process payment", description = "Process payment")
    @PostMapping
    public ResponseEntity<PaymentResponse> processPayment(@RequestBody PaymentRequest paymentRequest) {

        Payment saved = paymentService.processPayment(DtoMapper.mapToEntity(paymentRequest));

        PaymentResponse response = DtoMapper.mapToResponse(saved);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Refund payment", description = "Create a refund transaction for a payment")
    @PostMapping("/{paymentId}/refunds")
    public ResponseEntity<PaymentResponse> refundPayment(@PathVariable UUID paymentId, @RequestBody @Valid RefundRequest refundRequest) {

        Payment refunded = paymentService.refundPayment(paymentId, refundRequest.getAmount());

        PaymentResponse response = DtoMapper.mapToResponse(refunded);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get payment status by booking ID", description = "Get payment status by booking ID")
    @GetMapping("/bookings/{bookingId}")
    public ResponseEntity<PaymentResponse> getPaymentByBookingId(@PathVariable UUID bookingId) {

        Payment payment = paymentService.getPaymentByBookingId(bookingId);

        PaymentResponse response = DtoMapper.mapToResponse(payment);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get payments by user", description = "Get payments by user")
    @GetMapping("/users/{userId}")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByUser(@PathVariable UUID userId) {

        List<PaymentResponse> responseList = paymentService.getPaymentsByUser(userId)
                .stream()
                .map(DtoMapper::mapToResponse)
                .toList();

        return ResponseEntity.ok(responseList);
    }

    @Operation(summary = "Get payment by ID", description = "Get payment by ID")
    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> getPaymentById(@PathVariable UUID paymentId) {

        Payment payment = paymentService.getPaymentById(paymentId);

        PaymentResponse response = DtoMapper.mapToResponse(payment);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get all payments", description = "Get all payments")
    @GetMapping
    public ResponseEntity<List<PaymentResponse>> getAllPayments() {

        List<PaymentResponse> responseList = paymentService.getAllPayments()
                .stream()
                .map(DtoMapper::mapToResponse)
                .toList();

        return ResponseEntity.ok(responseList);
    }
}
