package com.exam.app.service;

import com.exam.app.exception.PaymentNotFoundException;
import com.exam.app.model.*;
import com.exam.app.repository.PaymentRepository;
import com.exam.app.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceUTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private PaymentGateway paymentGateway;

    @InjectMocks
    private PaymentService paymentService;

    private Payment testPayment;
    private UUID bookingId;
    private UUID userId;
    private UUID paymentId;

    @BeforeEach
    void setUp() {
        bookingId = UUID.randomUUID();
        userId = UUID.randomUUID();
        paymentId = UUID.randomUUID();

        testPayment = new Payment();
        testPayment.setId(paymentId);
        testPayment.setBookingId(bookingId);
        testPayment.setUserId(userId);
        testPayment.setAmount(BigDecimal.valueOf(100.00));
    }

    @Test
    void processPayment_whenPaymentAlreadyExists_shouldReturnExistingPayment() {

        Payment existingPayment = new Payment();
        existingPayment.setId(paymentId);
        existingPayment.setBookingId(bookingId);
        existingPayment.setStatus(PaymentStatus.SUCCESS);

        when(paymentRepository.findByBookingId(bookingId)).thenReturn(Optional.of(existingPayment));

        Payment result = paymentService.processPayment(testPayment);

        assertThat(result).isEqualTo(existingPayment);
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        verify(paymentRepository).findByBookingId(bookingId);
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void processPayment_whenNewPayment_shouldProcessSuccessfully() {

        when(paymentRepository.findByBookingId(bookingId)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> {
                    Payment p = invocation.getArgument(0);
                    p.setId(paymentId);
                    return p;
                });
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentGateway.charge(any(UUID.class), any(BigDecimal.class))).thenReturn(true);

        Payment result = paymentService.processPayment(testPayment);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(result.getCreatedOn()).isNotNull();
        assertThat(result.getCompletedOn()).isNotNull();

        verify(paymentRepository).findByBookingId(bookingId);
        verify(paymentRepository, times(2)).save(any(Payment.class));
        verify(paymentGateway).charge(userId, testPayment.getAmount());
        verify(transactionRepository).save(argThat(tx ->
                tx.getPaymentId().equals(paymentId) &&
                        tx.getAmount().equals(testPayment.getAmount()) &&
                        tx.getType() == TransactionType.PAYMENT &&
                        tx.getStatus() == TransactionStatus.SUCCESS
        ));
    }

    @Test
    void processPayment_whenExceptionOccurs_shouldHandleGracefully() {

        when(paymentRepository.findByBookingId(bookingId)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> {
                    Payment p = invocation.getArgument(0);
                    p.setId(paymentId);
                    return p;
                })
                .thenThrow(new RuntimeException("Database error"))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Payment result = paymentService.processPayment(testPayment);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.FAILED);

        verify(transactionRepository).save(argThat(tx ->
                tx.getStatus() == TransactionStatus.FAILED &&
                        tx.getMessage().contains("Payment error")
        ));
    }

    @Test
    void processPayment_whenGatewayReturnsFalse_shouldSetPaymentStatusToFailed() {

        when(paymentRepository.findByBookingId(bookingId)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> {
                    Payment p = invocation.getArgument(0);
                    p.setId(paymentId);
                    return p;
                });
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(paymentGateway.charge(any(UUID.class), any(BigDecimal.class))).thenReturn(false);

        Payment result = paymentService.processPayment(testPayment);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(result.getCompletedOn()).isNotNull();

        verify(paymentGateway).charge(userId, testPayment.getAmount());
        verify(paymentRepository, times(2)).save(any(Payment.class));
    }

    @Test
    void processPayment_whenExistingPaymentIsPending_shouldProcessAnyway() {

        Payment existingPayment = new Payment();
        existingPayment.setId(UUID.randomUUID());
        existingPayment.setBookingId(bookingId);
        existingPayment.setStatus(PaymentStatus.PENDING);

        when(paymentRepository.findByBookingId(bookingId)).thenReturn(Optional.of(existingPayment));
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> {
                    Payment p = invocation.getArgument(0);
                    if (p.getId() == null) p.setId(paymentId);
                    return p;
                });

        Payment result = paymentService.processPayment(testPayment);

        assertThat(result).isNotNull();
        verify(paymentRepository, times(2)).save(any(Payment.class));
    }

    @Test
    void refundPayment_whenPaymentExists_shouldRefundSuccessfully() {

        testPayment.setStatus(PaymentStatus.SUCCESS);
        BigDecimal refundAmount = BigDecimal.valueOf(50.00);

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Payment result = paymentService.refundPayment(paymentId, refundAmount);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.REFUNDED);

        verify(paymentRepository).findById(paymentId);
        verify(paymentRepository).save(testPayment);
        verify(transactionRepository).save(argThat(tx ->
                tx.getPaymentId().equals(paymentId) &&
                        tx.getAmount().equals(refundAmount) &&
                        tx.getType() == TransactionType.REFUND &&
                        tx.getStatus() == TransactionStatus.SUCCESS
        ));
    }

    @Test
    void refundPayment_WhenPaymentNotFound_ShouldThrowException() {

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

        PaymentNotFoundException exception =
                assertThrows(PaymentNotFoundException.class, () -> paymentService.refundPayment(paymentId, BigDecimal.valueOf(50.00)));
        assertTrue(exception.getMessage().contains("Payment with ID [%s] was not found.".formatted(paymentId)));

        verify(paymentRepository).findById(paymentId);
        verify(paymentRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void getPaymentByBookingId_whenPaymentExists_shouldReturnPayment() {

        when(paymentRepository.findByBookingId(bookingId)).thenReturn(Optional.of(testPayment));

        Payment result = paymentService.getPaymentByBookingId(bookingId);

        assertThat(result).isEqualTo(testPayment);
        verify(paymentRepository).findByBookingId(bookingId);
    }

    @Test
    void getPaymentByBookingId_whenPaymentNotFound_shouldThrowException() {

        when(paymentRepository.findByBookingId(bookingId)).thenReturn(Optional.empty());

        PaymentNotFoundException exception =
                assertThrows(PaymentNotFoundException.class, () -> paymentService.getPaymentByBookingId(bookingId));
        assertTrue(exception.getMessage().contains("Payment not found for booking " + bookingId));

        verify(paymentRepository).findByBookingId(bookingId);
    }

    @Test
    void getAllPayments_shouldReturnAllPayments() {

        Payment payment1 = new Payment();
        payment1.setId(UUID.randomUUID());

        Payment payment2 = new Payment();
        payment2.setId(UUID.randomUUID());

        List<Payment> payments = Arrays.asList(payment1, payment2);

        when(paymentRepository.findAll()).thenReturn(payments);

        List<Payment> result = paymentService.getAllPayments();

        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyInAnyOrder(payment1, payment2);
        verify(paymentRepository).findAll();
    }

    @Test
    void getPaymentsByUser_shouldReturnUserPayments() {

        Payment payment1 = new Payment();
        payment1.setId(UUID.randomUUID());
        payment1.setUserId(userId);

        Payment payment2 = new Payment();
        payment2.setId(UUID.randomUUID());
        payment2.setUserId(userId);

        List<Payment> userPayments = Arrays.asList(payment1, payment2);

        when(paymentRepository.findAllByUserId(userId)).thenReturn(userPayments);

        List<Payment> result = paymentService.getPaymentsByUser(userId);

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(p -> p.getUserId().equals(userId));
        verify(paymentRepository).findAllByUserId(userId);
    }

    @Test
    void getPaymentById_whenPaymentExists_shouldReturnPayment() {

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(testPayment));

        Payment result = paymentService.getPaymentById(paymentId);

        assertThat(result).isEqualTo(testPayment);
        verify(paymentRepository).findById(paymentId);
    }

    @Test
    void getPaymentById_whenPaymentNotFound_shouldThrowException() {

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

        PaymentNotFoundException exception =
                assertThrows(PaymentNotFoundException.class, () -> paymentService.getPaymentById(paymentId));
        assertTrue(exception.getMessage().contains("Payment with ID [%s] was not found.".formatted(paymentId)));

        verify(paymentRepository).findById(paymentId);
    }

    @Test
    void charge_shouldReturnTrue() {

        UUID userId = UUID.randomUUID();
        BigDecimal amount = BigDecimal.valueOf(100.00);
        PaymentGateway paymentGateway = new PaymentGateway();

        boolean result = paymentGateway.charge(userId, amount);

        assertThat(result).isTrue();
    }

    @Test
    void charge_withNullValues_shouldNotThrowException() {

        PaymentGateway paymentGateway = new PaymentGateway();

        assertThat(paymentGateway.charge(null, null)).isTrue();
    }
}
