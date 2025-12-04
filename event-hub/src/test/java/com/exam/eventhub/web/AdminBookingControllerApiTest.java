package com.exam.eventhub.web;

import com.exam.eventhub.booking.service.BookingService;
import com.exam.eventhub.config.TestMvcConfig;
import com.exam.eventhub.config.TestSecurityConfig;
import com.exam.eventhub.security.AuthenticationMetadata;
import com.exam.eventhub.user.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;
import java.util.UUID;

import static com.exam.eventhub.common.Constants.SUCCESS_MESSAGE_ATTR;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminBookingController.class)
@Import({TestMvcConfig.class, TestSecurityConfig.class})
public class AdminBookingControllerApiTest {

    @MockitoBean
    private BookingService bookingService;

    @Autowired
    private MockMvc mockMvc;

    private AuthenticationMetadata adminPrincipal;

    @BeforeEach
    void setup() {
        adminPrincipal = new AuthenticationMetadata
                (UUID.randomUUID(), "adminUser", "admin@mail.com", Role.ADMIN, false, null);
    }

    @Test
    void getAllBookings_asAdmin_returnsManageBookingsView() throws Exception {

        when(bookingService.getAllBookings()).thenReturn(List.of());

        MockHttpServletRequestBuilder request = get("/admin/bookings")
                .with(user(adminPrincipal));

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isOk())
                .andExpect(view().name("admin/manage-bookings"))
                .andExpect(model().attributeExists("bookings"));

        verify(bookingService, times(1)).getAllBookings();
    }

    @Test
    void cancelBooking_asAdmin_redirectsWithFlashMessage() throws Exception {

        UUID id = UUID.randomUUID();

        MockHttpServletRequestBuilder request = post("/admin/bookings/{id}/cancel", id)
                .with(user(adminPrincipal))
                .with(csrf());

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/bookings"))
                .andExpect(flash().attribute(SUCCESS_MESSAGE_ATTR, "Booking cancelled!"));

        verify(bookingService, times(1)).adminCancelBooking(id);
    }

    @Test
    void refundBooking_asAdmin_redirectsWithFlashMessage() throws Exception {

        UUID id = UUID.randomUUID();

        MockHttpServletRequestBuilder request = post("/admin/bookings/{id}/refund", id)
                .with(user(adminPrincipal))
                .with(csrf());

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/bookings"))
                .andExpect(flash().attribute(SUCCESS_MESSAGE_ATTR, "Booking refunded!"));

        verify(bookingService, times(1)).refundBooking(id);
    }

    @Test
    void getAuthenticatedNonAdminRequestToBookings_returnsForbidden() throws Exception {

        AuthenticationMetadata normalUser = new AuthenticationMetadata
                (UUID.randomUUID(), "User123", "user@mail.com", Role.USER, false, null);

        MockHttpServletRequestBuilder request = get("/admin/bookings")
                .with(user(normalUser));

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isForbidden());

        verify(bookingService, never()).getAllBookings();
    }
}
