package com.exam.eventhub.web;

import com.exam.eventhub.booking.model.Booking;
import com.exam.eventhub.booking.service.BookingService;
import com.exam.eventhub.config.TestMvcConfig;
import com.exam.eventhub.config.TestSecurityConfig;
import com.exam.eventhub.event.model.Event;
import com.exam.eventhub.exception.BookingAlreadyConfirmedException;
import com.exam.eventhub.exception.BookingNotFoundException;
import com.exam.eventhub.exception.PaymentProcessingException;
import com.exam.eventhub.exception.UnauthorizedException;
import com.exam.eventhub.security.AuthenticationMetadata;
import com.exam.eventhub.user.model.Role;
import com.exam.eventhub.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.UUID;

import static com.exam.eventhub.common.Constants.*;
import static com.exam.eventhub.util.PaymentHelper.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@Import({TestMvcConfig.class, TestSecurityConfig.class})
public class PaymentControllerApiTest {

    @MockitoBean
    private BookingService bookingService;

    @Autowired
    private MockMvc mockMvc;

    private AuthenticationMetadata principal;
    private String username;
    private UUID bookingId;
    private User user;
    private Event event;
    private Booking booking;

    @BeforeEach
    void setup() {
        principal = new AuthenticationMetadata
                (UUID.randomUUID(), "testUser", "password", Role.USER, false, null);
        username = principal.getUsername();

        bookingId = UUID.randomUUID();

        user = createUser(UUID.randomUUID(), principal.getUsername());
        event = createEvent(UUID.randomUUID(), "Test Event");
        booking = createBooking(bookingId, user, event);
    }

    @Test
    void postAuthenticatedRequestToProcessPayment_whenSuccessful_redirectsToSuccessStatus() throws Exception {

        doNothing().when(bookingService).markAsPaid(eq(bookingId), eq(username));

        MockHttpServletRequestBuilder request = post("/payments/bookings/{bookingId}", bookingId)
                .with(user(principal))
                .with(csrf());

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/payments/bookings/" + bookingId + "/status?success=true"))
                .andExpect(flash().attributeExists(SUCCESS_MESSAGE_ATTR))
                .andExpect(flash().attribute(SUCCESS_MESSAGE_ATTR, "Payment successful! Your booking is confirmed."));

        verify(bookingService, times(1)).markAsPaid(bookingId, username);
    }

    @Test
    void postAuthenticatedRequestToProcessPayment_whenPaymentFails_redirectsToFailureStatus() throws Exception {

        doThrow(new PaymentProcessingException("Payment processing failed. Please try again."))
                .when(bookingService).markAsPaid(eq(bookingId), eq(username));

        when(bookingService.getById(bookingId)).thenReturn(booking);

        MockHttpServletRequestBuilder request = post("/payments/bookings/{bookingId}", bookingId)
                .with(user(principal))
                .with(csrf());

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/payments/bookings/" + bookingId + "/status?success=false"))
                .andExpect(flash().attributeExists(ERROR_MESSAGE_ATTR))
                .andExpect(flash().attribute(ERROR_MESSAGE_ATTR, "Payment failed: Payment processing failed. Please try again."));

        verify(bookingService, times(1)).markAsPaid(bookingId, username);
    }

    @Test
    void postAuthenticatedRequestToProcessPayment_whenBookingAlreadyConfirmed_redirectsToMyBookings() throws Exception {

        doThrow(new BookingAlreadyConfirmedException("This booking is already confirmed"))
                .when(bookingService).markAsPaid(eq(bookingId), eq(username));

        MockHttpServletRequestBuilder request = post("/payments/bookings/{bookingId}", bookingId)
                .with(user(principal))
                .with(csrf());

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/bookings/my"))
                .andExpect(flash().attributeExists(INFO_MESSAGE_ATTR))
                .andExpect(flash().attribute(INFO_MESSAGE_ATTR, "This booking is already confirmed!"));

        verify(bookingService, times(1)).markAsPaid(bookingId, username);
    }

    @Test
    void postAuthenticatedRequestToProcessPayment_whenBookingNotFound_redirectsToMyBookings() throws Exception {

        doThrow(new BookingNotFoundException("Booking not found with id: " + bookingId))
                .when(bookingService).markAsPaid(eq(bookingId), eq(username));

        MockHttpServletRequestBuilder request = post("/payments/bookings/{bookingId}", bookingId)
                .with(user(principal))
                .with(csrf());

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isNotFound());

        verify(bookingService, times(1)).markAsPaid(bookingId, username);
    }

    @Test
    void postAuthenticatedRequestToProcessPayment_whenUnauthorized_redirectsToMyBookings() throws Exception {

        doThrow(new UnauthorizedException("You are not authorized to pay for this booking"))
                .when(bookingService).markAsPaid(eq(bookingId), eq(username));

        MockHttpServletRequestBuilder request = post("/payments/bookings/{bookingId}", bookingId)
                .with(user(principal))
                .with(csrf());

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/notifications"))
                .andExpect(flash().attributeExists(ERROR_MESSAGE_ATTR))
                .andExpect(flash().attribute(ERROR_MESSAGE_ATTR, "You are not authorized to pay for this booking"));

        verify(bookingService, times(1)).markAsPaid(bookingId, username);
    }

    @Test
    void postUnauthenticatedRequestToProcessPayment_returnsForbidden() throws Exception {

        MockHttpServletRequestBuilder request = post("/payments/bookings/{bookingId}", bookingId)
                .with(csrf());

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isForbidden());

        verify(bookingService, never()).markAsPaid(any(), any());
    }

    @Test
    void postAuthenticatedRequestToProcessPayment_withoutCsrf_returnsForbidden() throws Exception {

        MockHttpServletRequestBuilder request = post("/payments/bookings/{bookingId}", bookingId)
                .with(user(principal));

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isForbidden());

        verify(bookingService, never()).markAsPaid(any(), any());
    }

    @Test
    void getAuthenticatedRequestToPaymentStatus_whenSuccess_returnsStatusPage() throws Exception {

        when(bookingService.getById(bookingId)).thenReturn(booking);

        MockHttpServletRequestBuilder request = get("/payments/bookings/{bookingId}/status", bookingId)
                .param("success", "true")
                .with(user(principal));

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isOk())
                .andExpect(view().name("payment-status"))
                .andExpect(model().attributeExists("booking"))
                .andExpect(model().attribute("booking", booking))
                .andExpect(model().attributeExists("success"))
                .andExpect(model().attribute("success", true));

        verify(bookingService, times(1)).getById(bookingId);
    }

    @Test
    void getAuthenticatedRequestToPaymentStatus_whenFailure_returnsStatusPage() throws Exception {

        when(bookingService.getById(bookingId)).thenReturn(booking);

        MockHttpServletRequestBuilder request = get("/payments/bookings/{bookingId}/status", bookingId)
                .param("success", "false")
                .with(user(principal));

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isOk())
                .andExpect(view().name("payment-status"))
                .andExpect(model().attributeExists("booking"))
                .andExpect(model().attribute("booking", booking))
                .andExpect(model().attributeExists("success"))
                .andExpect(model().attribute("success", false));

        verify(bookingService, times(1)).getById(bookingId);
    }

    @Test
    void getUnauthenticatedRequestToPaymentStatus_returnsForbidden() throws Exception {

        MockHttpServletRequestBuilder request = get("/payments/bookings/{bookingId}/status", bookingId)
                .param("success", "true");

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isForbidden());

        verify(bookingService, never()).getById(any());
    }
}