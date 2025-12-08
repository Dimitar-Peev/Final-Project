package com.exam.app.service;

import com.exam.app.exception.PaymentNotFoundException;
import com.exam.app.model.*;
import com.exam.app.repository.PaymentRepository;
import com.exam.app.repository.TransactionRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@AllArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final TransactionRepository transactionRepository;
    private final PaymentGateway paymentGateway;

    @Transactional
    public Payment processPayment(Payment payment) {
        log.info("Processing payment for booking [{}]", payment.getBookingId());

        Optional<Payment> existing = paymentRepository.findByBookingId(payment.getBookingId());
        if (existing.isPresent() && existing.get().getStatus() == PaymentStatus.SUCCESS) {
            log.info("Payment already exists for booking [{}]", payment.getBookingId());
            return existing.get();
        }

        payment.setStatus(PaymentStatus.PENDING);
        payment.setCreatedOn(LocalDateTime.now());
        Payment saved = paymentRepository.save(payment);

        try {
            boolean success = paymentGateway.charge(payment.getUserId(), payment.getAmount());

            saved.setStatus(success ? PaymentStatus.SUCCESS : PaymentStatus.FAILED);
            saved.setCompletedOn(LocalDateTime.now());
            paymentRepository.save(saved);

            Transaction transaction = new Transaction();
            transaction.setPaymentId(saved.getId());
            transaction.setAmount(payment.getAmount());
            transaction.setType(TransactionType.PAYMENT);
            transaction.setStatus(success ? TransactionStatus.SUCCESS : TransactionStatus.FAILED);
            transaction.setMessage(success ? "Payment completed successfully" : "Payment failed");
            transaction.setTimestamp(LocalDateTime.now());
            transactionRepository.save(transaction);

            log.info("Payment id [{}] - status [{}]", saved.getId(), saved.getStatus());

            return saved;

        } catch (Exception ex) {
            log.error("Payment processing error: {}", ex.getMessage(), ex);
            saved.setStatus(PaymentStatus.FAILED);
            saved.setCompletedOn(LocalDateTime.now());
            paymentRepository.save(saved);

            Transaction tx = new Transaction();
            tx.setPaymentId(saved.getId());
            tx.setAmount(payment.getAmount());
            tx.setType(TransactionType.PAYMENT);
            tx.setStatus(TransactionStatus.FAILED);
            tx.setMessage("Payment error: " + ex.getMessage());
            tx.setTimestamp(LocalDateTime.now());
            transactionRepository.save(tx);

            return saved;
        }
    }

    @Transactional
    public Payment refundPayment(UUID paymentId, BigDecimal amount) {
        Payment payment = getPaymentById(paymentId);

        payment.setStatus(PaymentStatus.REFUNDED);
        paymentRepository.save(payment);

        Transaction refundTx = new Transaction();
        refundTx.setPaymentId(payment.getId());
        refundTx.setAmount(amount);
        refundTx.setType(TransactionType.REFUND);
        refundTx.setStatus(TransactionStatus.SUCCESS);
        refundTx.setMessage("Refund processed successfully");
        refundTx.setTimestamp(LocalDateTime.now());
        transactionRepository.save(refundTx);

        return payment;
    }

    public Payment getPaymentByBookingId(UUID bookingId) {
        return paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found for booking " + bookingId));
    }

    public List<Payment> getAllPayments() {
        log.info("Fetching all payments");
        return paymentRepository.findAll();
    }

    public List<Payment> getPaymentsByUser(UUID userId) {
        log.info("Fetching payments for userId [{}]", userId);
        return paymentRepository.findAllByUserId(userId);
    }

    public Payment getPaymentById(UUID paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment with ID [%s] was not found.".formatted(paymentId)));
    }
}

