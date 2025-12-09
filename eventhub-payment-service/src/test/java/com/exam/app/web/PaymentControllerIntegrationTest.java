package com.exam.app.web;

import com.exam.app.model.Payment;
import com.exam.app.repository.PaymentRepository;
import com.exam.app.web.dto.PaymentRequest;
import com.exam.app.web.dto.PaymentResponse;
import com.exam.app.web.dto.RefundRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.UUID;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class PaymentControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private PaymentRepository paymentRepository;

    private String url(String path) {
        return "http://localhost:" + port + "/api/v1/payments" + path;
    }

    @Test
    void testFullPaymentFlow_process_get_refund() {

        UUID bookingId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        BigDecimal amount = BigDecimal.valueOf(49.99);
        PaymentRequest paymentRequest = new PaymentRequest(bookingId, userId, amount);

        ResponseEntity<PaymentResponse> createResponse = rest.postForEntity(url(""), paymentRequest, PaymentResponse.class);

        Assertions.assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        Assertions.assertNotNull(createResponse.getBody());
        Assertions.assertNotNull(createResponse.getBody().getPaymentId());

        UUID paymentId = createResponse.getBody().getPaymentId();


        ResponseEntity<PaymentResponse> getResponse = rest.getForEntity(url("/" + paymentId), PaymentResponse.class);

        Assertions.assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        Assertions.assertNotNull(getResponse.getBody());
        Assertions.assertEquals(BigDecimal.valueOf(49.99), getResponse.getBody().getAmount());


        RefundRequest refundRequest = new RefundRequest(BigDecimal.valueOf(49.99));

        ResponseEntity<PaymentResponse> refundResponse = rest.postForEntity(url("/" + paymentId + "/refunds"), refundRequest, PaymentResponse.class);

        Assertions.assertEquals(HttpStatus.CREATED, refundResponse.getStatusCode());
        Assertions.assertNotNull(refundResponse.getBody());
        Assertions.assertEquals("REFUNDED", refundResponse.getBody().getStatus());


        Payment dbPayment = paymentRepository.findById(paymentId).orElseThrow();
        Assertions.assertEquals("REFUNDED", dbPayment.getStatus().name());
    }
}
