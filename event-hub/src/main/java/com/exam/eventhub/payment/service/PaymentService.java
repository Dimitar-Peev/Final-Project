package com.exam.eventhub.payment.service;

import com.exam.eventhub.exception.PaymentProcessingException;
import com.exam.eventhub.exception.PaymentServiceUnavailableException;
import com.exam.eventhub.payment.client.PaymentClient;
import com.exam.eventhub.payment.client.dto.PaymentRequest;
import com.exam.eventhub.payment.client.dto.PaymentResponse;
import com.exam.eventhub.payment.client.dto.RefundRequest;
import feign.FeignException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@AllArgsConstructor
public class PaymentService {

    private final PaymentClient paymentClient;

    /**
     * Обработва плащане за резервация.
     *
     * @param bookingId ID на резервацията
     * @param userId ID на потребителя
     * @param amount Сума за плащане
     * @return PaymentResponse от външния сервис
     * @throws PaymentProcessingException ако плащането се провали
     */
    public PaymentResponse processPayment(UUID bookingId, UUID userId, BigDecimal amount) {
        log.info("Processing payment for booking {} with amount {}", bookingId, amount);

        try {
            PaymentRequest request = new PaymentRequest(bookingId, userId, amount);
            ResponseEntity<PaymentResponse> responseEntity = paymentClient.processPayment(request);

            if (!responseEntity.getStatusCode().is2xxSuccessful() || responseEntity.getBody() == null) {
                log.warn("Payment failed for booking {}: HTTP status {}", bookingId, responseEntity.getStatusCode());
                throw new PaymentProcessingException("Payment request failed with status: " + responseEntity.getStatusCode());
            }

            PaymentResponse response = responseEntity.getBody();

            if (!"SUCCESS".equalsIgnoreCase(response.getStatus())) {
                log.warn("Payment failed for booking {}: {}", bookingId, response.getMessage());
                throw new PaymentProcessingException("Payment was declined: " + response.getMessage());
            }

            log.info("Payment successful for booking {}, paymentId: {}", bookingId, response.getPaymentId());
            return response;

        } catch (FeignException.ServiceUnavailable e) {
            log.error("Payment service is unavailable for booking {}", bookingId, e);
            throw new PaymentServiceUnavailableException("Payment service is temporarily unavailable. Please try again later.");

        } catch (FeignException e) {
            log.error("Error communicating with payment service for booking {}", bookingId, e);
            throw new PaymentProcessingException("Unable to process payment. Please try again.");
        }
    }

    /**
     * Обработва refund (възстановяване) на плащане.
     *
     * @param paymentId ID на плащането, което да се възстанови
     * @param amount Сума за възстановяване
     * @throws PaymentProcessingException ако refund-ът се провали
     */
    public void refundPayment(UUID paymentId, BigDecimal amount) {
        log.info("Processing refund for payment {} with amount {}", paymentId, amount);

        try {
            RefundRequest request = new RefundRequest(paymentId, amount);
            ResponseEntity<PaymentResponse> responseEntity = paymentClient.refundPayment(paymentId, request);

            if (!responseEntity.getStatusCode().is2xxSuccessful()) {
                log.warn("Refund failed for payment {}: HTTP status {}", paymentId, responseEntity.getStatusCode());
                throw new PaymentProcessingException("Refund request failed with status: " + responseEntity.getStatusCode());
            }

            log.info("Refund successful for payment {}", paymentId);

        } catch (FeignException.ServiceUnavailable e) {
            log.error("Payment service is unavailable for refund of payment {}", paymentId, e);
            throw new PaymentServiceUnavailableException("Payment service is temporarily unavailable. Please try again later.");

        } catch (FeignException e) {
            log.error("Error processing refund for payment {}", paymentId, e);
            throw new PaymentProcessingException("Unable to process refund. Please contact support.");
        }
    }

    /**
     * Връща всички плащания за даден потребител.
     */
    public List<PaymentResponse> getPaymentsByUser(UUID userId) {
        log.debug("Fetching payments for user {}", userId);

        try {
            ResponseEntity<List<PaymentResponse>> responseEntity = paymentClient.getPaymentsByUser(userId);

            if (!responseEntity.getStatusCode().is2xxSuccessful() || responseEntity.getBody() == null) {
                log.warn("Failed to fetch payments for user {}: HTTP status {}", userId, responseEntity.getStatusCode());
                throw new PaymentProcessingException("Unable to fetch payment history.");
            }

            return responseEntity.getBody();

        } catch (FeignException e) {
            log.error("Error fetching payments for user {}", userId, e);
            throw new PaymentProcessingException("Unable to fetch payment history.");
        }
    }

}