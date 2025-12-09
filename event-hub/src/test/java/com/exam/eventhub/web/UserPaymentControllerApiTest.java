package com.exam.eventhub.web;

import com.exam.eventhub.config.TestMvcConfig;
import com.exam.eventhub.config.TestSecurityConfig;
import com.exam.eventhub.exception.PaymentProcessingException;
import com.exam.eventhub.payment.service.PaymentService;
import com.exam.eventhub.security.AuthenticationMetadata;
import com.exam.eventhub.user.model.Role;
import com.exam.eventhub.user.model.User;
import com.exam.eventhub.user.service.UserService;
import com.exam.eventhub.payment.client.dto.PaymentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.exam.eventhub.common.Constants.ERROR_MESSAGE_ATTR;
import static com.exam.eventhub.util.UserPaymentHelper.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@WebMvcTest(UserPaymentController.class)
@Import({TestMvcConfig.class, TestSecurityConfig.class})
public class UserPaymentControllerApiTest {

    @MockitoBean
    private PaymentService paymentService;
    @MockitoBean
    private UserService userService;

    @Autowired
    private MockMvc mockMvc;

    private AuthenticationMetadata principal;
    private AuthenticationMetadata adminPrincipal;
    private AuthenticationMetadata organizerPrincipal;

    private String username;
    private UUID userId;
    private User user;
    private List<PaymentResponse> payments;

    @BeforeEach
    void setUp() {
        principal = new AuthenticationMetadata
                (UUID.randomUUID(), "testUser", "password", Role.USER, false, null);
        adminPrincipal = new AuthenticationMetadata
                (UUID.randomUUID(), "adminUser", "password", Role.ADMIN, false, null);
        organizerPrincipal = new AuthenticationMetadata
                (UUID.randomUUID(), "organizerUser", "password123", Role.EVENT_ORGANIZER, false, null);

        username = principal.getUsername();
        userId = principal.getUserId();
        user = createUser(userId, username);

        PaymentResponse payment1 = createPaymentResponse(
                UUID.randomUUID(),
                principal.getUserId(),
                new BigDecimal("50.00"),
                "COMPLETED",
                LocalDateTime.now().minusDays(5)
        );

        PaymentResponse payment2 = createPaymentResponse(
                UUID.randomUUID(),
                principal.getUserId(),
                new BigDecimal("100.00"),
                "COMPLETED",
                LocalDateTime.now().minusDays(2)
        );

        payments = Arrays.asList(payment1, payment2);
    }

    @Test
    void getAuthenticatedUserRequestToViewPaymentHistory_returnsPaymentHistoryPage() throws Exception {

        when(userService.getByUsername(username)).thenReturn(user);
        when(paymentService.getPaymentsByUser(userId)).thenReturn(payments);

        MockHttpServletRequestBuilder request = get("/profile/payments")
                .with(user(principal));

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isOk())
                .andExpect(view().name("user/payment-history"))
                .andExpect(model().attributeExists("payments"))
                .andExpect(model().attribute("payments", payments));

        verify(userService, times(1)).getByUsername(username);
        verify(paymentService, times(1)).getPaymentsByUser(userId);
    }

    @Test
    void getAuthenticatedUserRequestToViewPaymentHistory_whenNoPayments_returnsEmptyList() throws Exception {

        when(userService.getByUsername(username)).thenReturn(user);
        when(paymentService.getPaymentsByUser(userId)).thenReturn(Collections.emptyList());

        MockHttpServletRequestBuilder request = get("/profile/payments")
                .with(user(principal));

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isOk())
                .andExpect(view().name("user/payment-history"))
                .andExpect(model().attributeExists("payments"))
                .andExpect(model().attribute("payments", Collections.emptyList()));

        verify(userService, times(1)).getByUsername(username);
        verify(paymentService, times(1)).getPaymentsByUser(userId);
    }

    @Test
    void getAuthenticatedEventOrganizerRequestToViewPaymentHistory_returnsPaymentHistoryPage() throws Exception {

        String username = organizerPrincipal.getUsername();
        User organizer = createUser(organizerPrincipal.getUserId(), organizerPrincipal.getUsername());
        organizer.setRole(Role.EVENT_ORGANIZER);

        when(userService.getByUsername(username)).thenReturn(organizer);
        when(paymentService.getPaymentsByUser(organizer.getId())).thenReturn(payments);

        MockHttpServletRequestBuilder request = get("/profile/payments")
                .with(user(organizerPrincipal));

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isOk())
                .andExpect(view().name("user/payment-history"))
                .andExpect(model().attributeExists("payments"))
                .andExpect(model().attribute("payments", payments));

        verify(userService, times(1)).getByUsername(username);
        verify(paymentService, times(1)).getPaymentsByUser(organizer.getId());
    }

    @Test
    void getAuthenticatedAdminRequestToViewPaymentHistory_returnsPaymentHistoryPage() throws Exception {

        String username = adminPrincipal.getUsername();
        User admin = createUser(adminPrincipal.getUserId(), username);
        admin.setRole(Role.ADMIN);

        when(userService.getByUsername(username)).thenReturn(admin);
        when(paymentService.getPaymentsByUser(admin.getId())).thenReturn(payments);

        MockHttpServletRequestBuilder request = get("/profile/payments")
                .with(user(adminPrincipal));

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isOk())
                .andExpect(view().name("user/payment-history"))
                .andExpect(model().attributeExists("payments"))
                .andExpect(model().attribute("payments", payments));

        verify(userService, times(1)).getByUsername(username);
        verify(paymentService, times(1)).getPaymentsByUser(admin.getId());
    }

    @Test
    void getAuthenticatedUserRequestToViewPaymentHistory_whenServiceThrowsException_handledByGlobalExceptionHandler() throws Exception {

        when(userService.getByUsername(username)).thenReturn(user);
        when(paymentService.getPaymentsByUser(userId)).thenThrow(new PaymentProcessingException("Service unavailable"));

        MockHttpServletRequestBuilder request = get("/profile/payments")
                .with(user(principal));

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/bookings/my"))
                .andExpect(flash().attribute(ERROR_MESSAGE_ATTR, "Payment failed: Service unavailable"));

        verify(userService, times(1)).getByUsername(username);
        verify(paymentService, times(1)).getPaymentsByUser(userId);
    }

    @Test
    void getUnauthenticatedRequestToViewPaymentHistory_returnsForbidden() throws Exception {

        MockHttpServletRequestBuilder request = get("/profile/payments");

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isForbidden());

        verify(userService, never()).getByUsername(any());
        verify(paymentService, never()).getPaymentsByUser(any());
    }
}
