package com.exam.eventhub.payment.service;

import com.exam.eventhub.exception.PaymentProcessingException;
import com.exam.eventhub.exception.PaymentServiceUnavailableException;
import com.exam.eventhub.payment.client.PaymentClient;
import com.exam.eventhub.payment.client.dto.PaymentRequest;
import com.exam.eventhub.payment.client.dto.PaymentResponse;
import com.exam.eventhub.payment.client.dto.RefundRequest;
import feign.FeignException;
import feign.Request;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.exam.eventhub.util.PaymentHelper.createPaymentResponse;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceUTest {

    @Mock
    private PaymentClient paymentClient;

    @InjectMocks
    private PaymentService paymentService;

    private UUID bookingId;
    private UUID userId;
    private UUID paymentId;
    private BigDecimal amount;

    @BeforeEach
    void setUp() {
        bookingId = UUID.randomUUID();
        userId = UUID.randomUUID();
        paymentId = UUID.randomUUID();
        amount = new BigDecimal("100.00");
    }

    @Test
    void processPayment_whenSuccessful_shouldReturnPaymentResponse() {

        PaymentResponse expectedResponse = createPaymentResponse(paymentId, "SUCCESS", "Payment completed");
        ResponseEntity<PaymentResponse> responseEntity = ResponseEntity.status(HttpStatus.CREATED).body(expectedResponse);

        when(paymentClient.processPayment(any(PaymentRequest.class))).thenReturn(responseEntity);

        PaymentResponse result = paymentService.processPayment(bookingId, userId, amount);

        assertNotNull(result);
        assertEquals(paymentId, result.getPaymentId());
        assertEquals("SUCCESS", result.getStatus());
        assertEquals("Payment completed", result.getMessage());

        verify(paymentClient, times(1)).processPayment(any(PaymentRequest.class));
    }

    @Test
    void processPayment_shouldCreateCorrectPaymentRequest() {

        PaymentResponse response = createPaymentResponse(UUID.randomUUID(), "SUCCESS", "Payment completed");
        ResponseEntity<PaymentResponse> responseEntity = ResponseEntity.status(HttpStatus.CREATED).body(response);
        when(paymentClient.processPayment(any(PaymentRequest.class))).thenReturn(responseEntity);

        ArgumentCaptor<PaymentRequest> requestCaptor = ArgumentCaptor.forClass(PaymentRequest.class);

        paymentService.processPayment(bookingId, userId, amount);

        verify(paymentClient).processPayment(requestCaptor.capture());
        PaymentRequest capturedRequest = requestCaptor.getValue();

        assertEquals(bookingId, capturedRequest.getBookingId());
        assertEquals(userId, capturedRequest.getUserId());
        assertEquals(amount, capturedRequest.getAmount());
    }

    @Test
    void processPayment_whenStatusIsNotSuccess_shouldThrowPaymentProcessingException() {

        PaymentResponse failedResponse = createPaymentResponse(null, "DECLINED", "Insufficient funds");
        ResponseEntity<PaymentResponse> responseEntity = ResponseEntity.ok(failedResponse);
        when(paymentClient.processPayment(any(PaymentRequest.class))).thenReturn(responseEntity);

        PaymentProcessingException exception =
                assertThrows(PaymentProcessingException.class, () -> paymentService.processPayment(bookingId, userId, amount));
        assertTrue(exception.getMessage().contains("Payment was declined: Insufficient funds"));

        verify(paymentClient, times(1)).processPayment(any(PaymentRequest.class));
    }

    @Test
    void processPayment_whenHttpStatusIsNotSuccessful_shouldThrowPaymentProcessingException() {

        ResponseEntity<PaymentResponse> responseEntity = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);

        when(paymentClient.processPayment(any(PaymentRequest.class))).thenReturn(responseEntity);

        PaymentProcessingException exception =
                assertThrows(PaymentProcessingException.class, () -> paymentService.processPayment(bookingId, userId, amount));
        assertTrue(exception.getMessage().contains("Payment request failed with status: 400 BAD_REQUEST"));

        verify(paymentClient, times(1)).processPayment(any(PaymentRequest.class));
    }

    @Test
    void processPayment_whenResponseBodyIsNull_shouldThrowPaymentProcessingException() {

        ResponseEntity<PaymentResponse> responseEntity = ResponseEntity.ok().build();

        when(paymentClient.processPayment(any(PaymentRequest.class))).thenReturn(responseEntity);

        PaymentProcessingException exception =
                assertThrows(PaymentProcessingException.class, () -> paymentService.processPayment(bookingId, userId, amount));
        assertTrue(exception.getMessage().contains("Payment request failed with status"));

        verify(paymentClient, times(1)).processPayment(any(PaymentRequest.class));
    }

    @Test
    void processPayment_whenStatusIsSuccessLowerCase_shouldReturnResponse() {

        PaymentResponse response = createPaymentResponse(UUID.randomUUID(), "success", "Completed");
        ResponseEntity<PaymentResponse> responseEntity = ResponseEntity.status(HttpStatus.CREATED).body(response);
        when(paymentClient.processPayment(any(PaymentRequest.class))).thenReturn(responseEntity);

        PaymentResponse result = paymentService.processPayment(bookingId, userId, amount);

        assertNotNull(result);
        assertEquals("success", result.getStatus());
    }

    @Test
    void processPayment_whenServiceUnavailable_shouldThrowPaymentServiceUnavailableException() {

        Request request = Request.create(Request.HttpMethod.POST, "/payments", Map.of(), null, null, null);
        FeignException.ServiceUnavailable exception =
                new FeignException.ServiceUnavailable("Service unavailable", request, null, null);

        when(paymentClient.processPayment(any(PaymentRequest.class))).thenThrow(exception);

        PaymentServiceUnavailableException thrownException =
                assertThrows(PaymentServiceUnavailableException.class, () -> paymentService.processPayment(bookingId, userId, amount));
        assertTrue(thrownException.getMessage().contains("Payment service is temporarily unavailable"));

        verify(paymentClient, times(1)).processPayment(any(PaymentRequest.class));
    }

    @Test
    void processPayment_whenFeignExceptionOccurs_shouldThrowPaymentProcessingException() {

        Request request = Request.create(Request.HttpMethod.POST, "/payments", Map.of(), null, null, null);
        FeignException.BadRequest exception =
                new FeignException.BadRequest("Bad request", request, null, null);

        when(paymentClient.processPayment(any(PaymentRequest.class))).thenThrow(exception);

        PaymentProcessingException thrownException =
                assertThrows(PaymentProcessingException.class, () -> paymentService.processPayment(bookingId, userId, amount));
        assertTrue(thrownException.getMessage().contains("Unable to process payment"));

        verify(paymentClient, times(1)).processPayment(any(PaymentRequest.class));
    }

    @Test
    void refundPayment_whenSuccessful_shouldCompleteWithoutException() {

        PaymentResponse refundResponse = createPaymentResponse(paymentId, "REFUNDED", "Refund processed");
        ResponseEntity<PaymentResponse> responseEntity = ResponseEntity.status(HttpStatus.CREATED).body(refundResponse);

        when(paymentClient.refundPayment(eq(paymentId), any(RefundRequest.class))).thenReturn(responseEntity);

        paymentService.refundPayment(paymentId, amount);

        verify(paymentClient, times(1)).refundPayment(eq(paymentId), any(RefundRequest.class));
    }

    @Test
    void refundPayment_shouldCreateCorrectRefundRequest() {

        PaymentResponse refundResponse = createPaymentResponse(paymentId, "REFUNDED", "Refund processed");
        ResponseEntity<PaymentResponse> responseEntity = ResponseEntity.status(HttpStatus.CREATED).body(refundResponse);

        when(paymentClient.refundPayment(eq(paymentId), any(RefundRequest.class))).thenReturn(responseEntity);

        ArgumentCaptor<RefundRequest> requestCaptor = ArgumentCaptor.forClass(RefundRequest.class);

        paymentService.refundPayment(paymentId, amount);

        verify(paymentClient).refundPayment(eq(paymentId), requestCaptor.capture());
        RefundRequest capturedRequest = requestCaptor.getValue();

        assertEquals(amount, capturedRequest.getAmount());
    }

    @Test
    void refundPayment_whenServiceUnavailable_shouldThrowPaymentServiceUnavailableException() {

        Request request = Request.create(Request.HttpMethod.POST, "/payments/" + paymentId + "/refunds", Map.of(), null, null, null);
        FeignException.ServiceUnavailable exception =
                new FeignException.ServiceUnavailable("Service unavailable", request, null, null);

        doThrow(exception).when(paymentClient).refundPayment(eq(paymentId), any(RefundRequest.class));

        PaymentServiceUnavailableException thrownException =
                assertThrows(PaymentServiceUnavailableException.class, () -> paymentService.refundPayment(paymentId, amount));
        assertTrue(thrownException.getMessage().contains("Payment service is temporarily unavailable"));

        verify(paymentClient, times(1)).refundPayment(eq(paymentId), any(RefundRequest.class));
    }

    @Test
    void refundPayment_whenFeignExceptionOccurs_shouldThrowPaymentProcessingException() {

        Request request = Request.create(Request.HttpMethod.POST, "/payments/" + paymentId + "/refunds", Map.of(), null, null, null);
        FeignException.InternalServerError exception =
                new FeignException.InternalServerError("Server error", request, null, null);

        doThrow(exception).when(paymentClient).refundPayment(eq(paymentId), any(RefundRequest.class));

        PaymentProcessingException thrownException =
                assertThrows(PaymentProcessingException.class, () -> paymentService.refundPayment(paymentId, amount));
        assertTrue(thrownException.getMessage().contains("Unable to process refund"));

        verify(paymentClient, times(1)).refundPayment(eq(paymentId), any(RefundRequest.class));
    }

    @Test
    void refundPayment_whenHttpStatusIsNotSuccessful_shouldThrowPaymentProcessingException() {

        ResponseEntity<PaymentResponse> responseEntity = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        when(paymentClient.refundPayment(eq(paymentId), any(RefundRequest.class))).thenReturn(responseEntity);

        PaymentProcessingException exception =
                assertThrows(PaymentProcessingException.class, () -> paymentService.refundPayment(paymentId, amount));
        assertTrue(exception.getMessage().contains("Refund request failed with status: 400 BAD_REQUEST"));

        verify(paymentClient, times(1)).refundPayment(eq(paymentId), any(RefundRequest.class));
    }

    @Test
    void refundPayment_whenFeignServiceUnavailable_shouldThrowPaymentServiceUnavailableException() {

        when(paymentClient.refundPayment(eq(paymentId), any(RefundRequest.class)))
                .thenThrow(FeignException.ServiceUnavailable.class);

        PaymentServiceUnavailableException exception =
                assertThrows(PaymentServiceUnavailableException.class, () -> paymentService.refundPayment(paymentId, amount));
        assertTrue(exception.getMessage().contains("Payment service is temporarily unavailable"));

        verify(paymentClient, times(1)).refundPayment(eq(paymentId), any(RefundRequest.class));
    }

    @Test
    void refundPayment_whenFeignException_shouldThrowPaymentProcessingException() {

        when(paymentClient.refundPayment(eq(paymentId), any(RefundRequest.class)))
                .thenThrow(FeignException.InternalServerError.class);

        PaymentProcessingException exception =
                assertThrows(PaymentProcessingException.class, () -> paymentService.refundPayment(paymentId, amount));
        assertTrue(exception.getMessage().contains("Unable to process refund"));

        verify(paymentClient, times(1)).refundPayment(eq(paymentId), any(RefundRequest.class));
    }

    @Test
    void getPaymentsByUser_whenSuccessful_shouldReturnPaymentList() {

        PaymentResponse response1 = createPaymentResponse(UUID.randomUUID(), "SUCCESS", "Payment 1");
        PaymentResponse response2 = createPaymentResponse(UUID.randomUUID(), "SUCCESS", "Payment 2");
        PaymentResponse response3 = createPaymentResponse(UUID.randomUUID(), "REFUNDED", "Payment 3");
        List<PaymentResponse> expectedPayments = List.of(response1, response2, response3);

        ResponseEntity<List<PaymentResponse>> responseEntity = ResponseEntity.ok(expectedPayments);
        when(paymentClient.getPaymentsByUser(userId)).thenReturn(responseEntity);

        List<PaymentResponse> result = paymentService.getPaymentsByUser(userId);

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals(expectedPayments, result);

        verify(paymentClient, times(1)).getPaymentsByUser(userId);
    }

    @Test
    void getPaymentsByUser_whenNoPayments_shouldReturnEmptyList() {

        ResponseEntity<List<PaymentResponse>> responseEntity = ResponseEntity.ok(List.of());
        when(paymentClient.getPaymentsByUser(userId)).thenReturn(responseEntity);

        List<PaymentResponse> result = paymentService.getPaymentsByUser(userId);

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(paymentClient, times(1)).getPaymentsByUser(userId);
    }

    @Test
    void getPaymentsByUser_whenFeignExceptionOccurs_shouldThrowPaymentProcessingException() {

        Request request = Request.create(Request.HttpMethod.GET, "/payments/user", Map.of(), null, null, null);
        FeignException.InternalServerError exception =
                new FeignException.InternalServerError("Server error", request, null, null);

        when(paymentClient.getPaymentsByUser(userId)).thenThrow(exception);

        PaymentProcessingException thrownException =
                assertThrows(PaymentProcessingException.class, () -> paymentService.getPaymentsByUser(userId));
        assertTrue(thrownException.getMessage().contains("Unable to fetch payment history"));

        verify(paymentClient, times(1)).getPaymentsByUser(userId);
    }

    @Test
    void getPaymentsByUser_whenHttpStatusIsNotSuccessful_shouldThrowPaymentProcessingException() {

        ResponseEntity<List<PaymentResponse>> responseEntity = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);

        when(paymentClient.getPaymentsByUser(userId)).thenReturn(responseEntity);

        PaymentProcessingException exception =
                assertThrows(PaymentProcessingException.class, () -> paymentService.getPaymentsByUser(userId));
        assertTrue(exception.getMessage().contains("Unable to fetch payment history"));

        verify(paymentClient, times(1)).getPaymentsByUser(userId);
    }

    @Test
    void getPaymentsByUser_whenResponseBodyIsNull_shouldThrowPaymentProcessingException() {

        ResponseEntity<List<PaymentResponse>> responseEntity = ResponseEntity.ok().build();

        when(paymentClient.getPaymentsByUser(userId)).thenReturn(responseEntity);

        PaymentProcessingException exception =
                assertThrows(PaymentProcessingException.class, () -> paymentService.getPaymentsByUser(userId));
        assertTrue(exception.getMessage().contains("Unable to fetch payment history"));

        verify(paymentClient, times(1)).getPaymentsByUser(userId);
    }
}