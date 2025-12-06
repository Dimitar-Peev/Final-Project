package com.exam.eventhub.web;

import com.exam.eventhub.booking.model.Booking;
import com.exam.eventhub.booking.service.BookingService;
import com.exam.eventhub.config.TestMvcConfig;
import com.exam.eventhub.config.TestSecurityConfig;
import com.exam.eventhub.security.AuthenticationMetadata;
import com.exam.eventhub.user.model.Role;
import com.exam.eventhub.web.dto.BookingCreateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.exam.eventhub.common.Constants.SUCCESS_MESSAGE_ATTR;
import static com.exam.eventhub.util.ApiHelper.createMockBooking;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookingController.class)
@Import({TestMvcConfig.class, TestSecurityConfig.class})
class BookingControllerApiTest {

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
    void getAuthenticatedUserRequestToMyBookings_returnsMyBookingsView() throws Exception {

        Booking booking1 = createMockBooking("Concert Ticket");
        Booking booking2 = createMockBooking("Conference Pass");

        List<Booking> mockBookings = Arrays.asList(booking1, booking2);
        when(bookingService.getBookingsForUser(principal.getUsername())).thenReturn(mockBookings);

        MockHttpServletRequestBuilder request = get("/bookings/my")
                .with(user(principal));

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isOk())
                .andExpect(view().name("my-bookings"))
                .andExpect(model().attributeExists("bookings"))
                .andExpect(model().attribute("bookings", mockBookings));

        verify(bookingService, times(1)).getBookingsForUser(principal.getUsername());
    }

    @Test
    void getMyBookingsWithEmptyList_returnsViewWithEmptyList() throws Exception {

        when(bookingService.getBookingsForUser(principal.getUsername())).thenReturn(Collections.emptyList());

        MockHttpServletRequestBuilder request = get("/bookings/my")
                .with(user(principal));

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isOk())
                .andExpect(view().name("my-bookings"))
                .andExpect(model().attribute("bookings", Collections.emptyList()));

        verify(bookingService, times(1)).getBookingsForUser(principal.getUsername());
    }

    @Test
    void getMyBookingsWithExistingModelAttribute_doesNotOverrideAttribute() throws Exception {

        List<Booking> existingBookings = List.of(createMockBooking("Existing"));
        when(bookingService.getBookingsForUser(principal.getUsername())).thenReturn(List.of(createMockBooking("New")));

        MockHttpServletRequestBuilder request = get("/bookings/my")
                .flashAttr("bookings", existingBookings)
                .with(user(principal));

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isOk())
                .andExpect(view().name("my-bookings"))
                .andExpect(model().attribute("bookings", existingBookings));

        verify(bookingService, never()).getBookingsForUser(any());
    }

    @Test
    void getUnauthenticatedRequestToMyBookings_returnsForbidden() throws Exception {

        MockHttpServletRequestBuilder request = get("/bookings/my");

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isForbidden());

        verify(bookingService, never()).getBookingsForUser(any());
    }

    @Test
    void postValidBookingCreateRequest_createsBookingAndRedirects() throws Exception {

        UUID bookingId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        Booking mockBooking = createMockBooking("Concert Ticket");
        mockBooking.setId(bookingId);

        when(bookingService.add(any(BookingCreateRequest.class), eq(principal.getUsername()))).thenReturn(mockBooking);

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
                .andExpect(redirectedUrl("/bookings/confirmation/" + bookingId))
                .andExpect(flash().attributeExists("booking"))
                .andExpect(flash().attribute("booking", mockBooking));

        verify(bookingService, times(1)).add(any(BookingCreateRequest.class), eq(principal.getUsername()));
    }

    @Test
    void postBookingCreateRequestWithInvalidData_handlesValidationErrors() throws Exception {

        MockHttpServletRequestBuilder request = post("/bookings")
                .param("numberOfTickets", "0")
                .with(user(principal))
                .with(csrf());

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().is3xxRedirection());
    }

    @Test
    void postBookingWithoutCsrf_returnsForbidden() throws Exception {

        UUID eventId = UUID.randomUUID();

        MockHttpServletRequestBuilder request = post("/bookings")
                .param("eventId", eventId.toString())
                .param("numberOfTickets", "2")
                .with(user(principal));

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isForbidden());

        verify(bookingService, never()).add(any(), any());
    }

    @Test
    void postUnauthenticatedBookingCreateRequest_returnsForbidden() throws Exception {

        UUID eventId = UUID.randomUUID();

        MockHttpServletRequestBuilder request = post("/bookings")
                .param("eventId", eventId.toString())
                .param("numberOfTickets", "2")
                .with(csrf());

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isForbidden());

        verify(bookingService, never()).add(any(), any());
    }

    @Test
    void getAuthenticatedRequestToBookingConfirmation_returnsConfirmationView() throws Exception {

        UUID bookingId = UUID.randomUUID();
        Booking mockBooking = createMockBooking("Concert Ticket");
        mockBooking.setId(bookingId);

        when(bookingService.getById(bookingId)).thenReturn(mockBooking);

        MockHttpServletRequestBuilder request = get("/bookings/confirmation/" + bookingId)
                .with(user(principal));

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isOk())
                .andExpect(view().name("booking-confirmation"))
                .andExpect(model().attributeExists("booking"))
                .andExpect(model().attribute("booking", mockBooking));

        verify(bookingService, times(1)).getById(bookingId);
    }

    @Test
    void getUnauthenticatedRequestToBookingConfirmation_returnsForbidden() throws Exception {

        UUID bookingId = UUID.randomUUID();

        MockHttpServletRequestBuilder request = get("/bookings/confirmation/" + bookingId);

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isForbidden());

        verify(bookingService, never()).getById(any());
    }

    @Test
    void postAuthenticatedRequestToCancelBooking_cancelsBookingAndRedirects() throws Exception {

        UUID bookingId = UUID.randomUUID();

        MockHttpServletRequestBuilder request = post("/bookings/cancel/" + bookingId)
                .with(user(principal))
                .with(csrf());

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/bookings/my"))
                .andExpect(flash().attributeExists(SUCCESS_MESSAGE_ATTR))
                .andExpect(flash().attribute(SUCCESS_MESSAGE_ATTR, "Booking cancelled successfully!"));

        verify(bookingService, times(1)).cancelBooking(bookingId, principal.getUsername());
    }

    @Test
    void postCancelBookingWithoutCsrf_returnsForbidden() throws Exception {

        UUID bookingId = UUID.randomUUID();

        MockHttpServletRequestBuilder request = post("/bookings/cancel/" + bookingId)
                .with(user(principal));

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isForbidden());

        verify(bookingService, never()).cancelBooking(any(), any());
    }
}
