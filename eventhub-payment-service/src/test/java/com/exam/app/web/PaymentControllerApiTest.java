package com.exam.app.web;

import com.exam.app.model.Payment;
import com.exam.app.model.PaymentStatus;
import com.exam.app.service.PaymentService;
import com.exam.app.web.dto.RefundRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static com.exam.app.util.TestBuilder.createMockPayment;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
@AutoConfigureMockMvc
public class PaymentControllerApiTest {

    @MockitoBean
    private PaymentService paymentService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void processPayment_happyPath() throws Exception {

        Payment mockPayment = createMockPayment();
        when(paymentService.processPayment(any(Payment.class))).thenReturn(mockPayment);

        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setBookingId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));
        payment.setUserId(UUID.fromString("223e4567-e89b-12d3-a456-426614174001"));
        payment.setAmount(new BigDecimal("150.00"));
        payment.setStatus(PaymentStatus.PENDING);

        MockHttpServletRequestBuilder request = post("/api/v1/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(payment));
        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentId").isNotEmpty())
                .andExpect(jsonPath("$.bookingId").isNotEmpty())
                .andExpect(jsonPath("$.userId").isNotEmpty())
                .andExpect(jsonPath("$.amount").isNotEmpty())
                .andExpect(jsonPath("$.status").isNotEmpty());

        verify(paymentService, times(1)).processPayment(any(Payment.class));
    }

    @Test
    void refundPayment_happyPath() throws Exception {

        UUID paymentId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("150.00");
        Payment mockPayment = createMockPayment();
        mockPayment.setStatus(PaymentStatus.REFUNDED);

        when(paymentService.refundPayment(paymentId, amount)).thenReturn(mockPayment);

        RefundRequest refundRequest = new RefundRequest();
        refundRequest.setAmount(amount);

        MockHttpServletRequestBuilder request = post("/api/v1/payments/{paymentId}/refunds", paymentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(refundRequest));

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentId").isNotEmpty())
                .andExpect(jsonPath("$.status").value("REFUNDED"));

        verify(paymentService, times(1)).refundPayment(paymentId, amount);
    }

    @Test
    void getAllPayments_happyPath() throws Exception {

        Payment mockPayment1 = createMockPayment();
        Payment mockPayment2 = createMockPayment();
        List<Payment> paymentList = List.of(mockPayment1, mockPayment2);
        when(paymentService.getAllPayments()).thenReturn(paymentList);

        MockHttpServletRequestBuilder request = get("/api/v1/payments");

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isOk())
                .andDo(print())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].paymentId").isNotEmpty())
                .andExpect(jsonPath("$[0].bookingId").isNotEmpty())
                .andExpect(jsonPath("$[1].paymentId").isNotEmpty())
                .andExpect(jsonPath("$[1].bookingId").isNotEmpty());

        verify(paymentService, times(1)).getAllPayments();
    }

    @Test
    void getAllPayments_emptyList() throws Exception {

        when(paymentService.getAllPayments()).thenReturn(List.of());

        MockHttpServletRequestBuilder request = get("/api/v1/payments");

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getPaymentByBookingId_happyPath() throws Exception {

        UUID bookingId = UUID.randomUUID();
        Payment mockPayment = createMockPayment();
        mockPayment.setBookingId(bookingId);

        when(paymentService.getPaymentByBookingId(bookingId)).thenReturn(mockPayment);

        MockHttpServletRequestBuilder request = get("/api/v1/payments/bookings/{bookingId}", bookingId);

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").isNotEmpty())
                .andExpect(jsonPath("$.bookingId").value(bookingId.toString()))
                .andExpect(jsonPath("$.userId").isNotEmpty())
                .andExpect(jsonPath("$.amount").isNotEmpty())
                .andExpect(jsonPath("$.status").isNotEmpty());

        verify(paymentService, times(1)).getPaymentByBookingId(bookingId);
    }

    @Test
    void getPaymentsByUser_happyPath() throws Exception {

        UUID userId = UUID.randomUUID();
        Payment payment1 = createMockPayment();
        Payment payment2 = createMockPayment();
        List<Payment> paymentList = List.of(payment1, payment2);

        when(paymentService.getPaymentsByUser(userId)).thenReturn(paymentList);

        MockHttpServletRequestBuilder request = get("/api/v1/payments/users/{userId}", userId);

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].paymentId").isNotEmpty())
                .andExpect(jsonPath("$[0].bookingId").isNotEmpty())
                .andExpect(jsonPath("$[1].paymentId").isNotEmpty())
                .andExpect(jsonPath("$[1].bookingId").isNotEmpty());

        verify(paymentService, times(1)).getPaymentsByUser(userId);
    }

    @Test
    void getPaymentsByUser_emptyList() throws Exception {

        UUID userId = UUID.randomUUID();
        when(paymentService.getPaymentsByUser(userId)).thenReturn(List.of());

        MockHttpServletRequestBuilder request = get("/api/v1/payments/users/{userId}", userId);

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getPaymentById_happyPath() throws Exception {

        UUID paymentId = UUID.randomUUID();
        Payment mockPayment = createMockPayment();
        mockPayment.setId(paymentId);

        when(paymentService.getPaymentById(paymentId)).thenReturn(mockPayment);

        MockHttpServletRequestBuilder request = get("/api/v1/payments/{paymentId}", paymentId);

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(paymentId.toString()))
                .andExpect(jsonPath("$.bookingId").isNotEmpty())
                .andExpect(jsonPath("$.userId").isNotEmpty())
                .andExpect(jsonPath("$.amount").isNotEmpty())
                .andExpect(jsonPath("$.status").isNotEmpty());

        verify(paymentService, times(1)).getPaymentById(paymentId);
    }
}
