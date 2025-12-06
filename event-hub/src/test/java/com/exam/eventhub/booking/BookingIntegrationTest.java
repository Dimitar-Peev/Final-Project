package com.exam.eventhub.booking;

import com.exam.eventhub.booking.model.Booking;
import com.exam.eventhub.booking.service.BookingService;
import com.exam.eventhub.security.AuthenticationMetadata;
import com.exam.eventhub.user.model.Role;
import com.exam.eventhub.web.dto.BookingCreateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;
import java.util.UUID;

import static com.exam.eventhub.util.ApiHelper.createMockBooking;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
public class BookingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookingService bookingService;

    private AuthenticationMetadata principal;

    @BeforeEach
    void setup() {
        principal = new AuthenticationMetadata
                (UUID.randomUUID(), "testUser", "password", Role.USER, false, null);
    }

    @Test
    void fullBookingFlow_createViewConfirmCancel() throws Exception {

        UUID bookingId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        Booking mockBooking = createMockBooking("Concert Ticket");
        mockBooking.setId(bookingId);

        String username = principal.getUsername();

        createBooking(username, mockBooking, eventId, bookingId);

        viewConfirmation(bookingId, mockBooking);

        viewMyBookings(username, mockBooking);

        cancelBooking(bookingId, username);

        verify(bookingService, times(1)).add(any(BookingCreateRequest.class), eq(username));
        verify(bookingService, times(1)).getById(bookingId);
        verify(bookingService, times(1)).getBookingsForUser(username);
        verify(bookingService, times(1)).cancelBooking(bookingId, username);
    }

    private void createBooking(String username, Booking mockBooking, UUID eventId, UUID bookingId) throws Exception {
        when(bookingService.add(any(BookingCreateRequest.class), eq(username))).thenReturn(mockBooking);

        MockHttpServletRequestBuilder request = post("/bookings")
                .param("eventId", eventId.toString())
                .param("numberOfTickets", "2")
                .param("customerEmail", "test@test.com")
                .param("customerPhone", "0899123456")
                .param("specialRequests", "No special requests")
                .with(user(principal))
                .with(csrf());

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/bookings/confirmation/" + bookingId));
    }

    private void viewConfirmation(UUID bookingId, Booking mockBooking) throws Exception {
        when(bookingService.getById(bookingId)).thenReturn(mockBooking);

        MockHttpServletRequestBuilder request = get("/bookings/confirmation/" + bookingId)
                .with(user(principal));

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isOk())
                .andExpect(view().name("booking-confirmation"));
    }

    private void viewMyBookings(String username, Booking mockBooking) throws Exception {
        when(bookingService.getBookingsForUser(username)).thenReturn(List.of(mockBooking));

        MockHttpServletRequestBuilder request = get("/bookings/my")
                .with(user(principal));

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isOk())
                .andExpect(model().attribute("bookings", List.of(mockBooking)));
    }

    private void cancelBooking(UUID bookingId, String username) throws Exception {
        doNothing().when(bookingService).cancelBooking(bookingId, username);

        MockHttpServletRequestBuilder request = post("/bookings/cancel/" + bookingId)
                .with(user(principal))
                .with(csrf());

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/bookings/my"));
    }
}
