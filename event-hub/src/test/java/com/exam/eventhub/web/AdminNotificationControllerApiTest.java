package com.exam.eventhub.web;

import com.exam.eventhub.config.TestMvcConfig;
import com.exam.eventhub.config.TestSecurityConfig;
import com.exam.eventhub.notification.client.dto.NotificationResponse;
import com.exam.eventhub.notification.service.NotificationService;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.exam.eventhub.common.Constants.SUCCESS_MESSAGE_ATTR;
import static com.exam.eventhub.util.AdminNotificationHelper.createMockNotification;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminNotificationController.class)
@Import({TestMvcConfig.class, TestSecurityConfig.class})
public class AdminNotificationControllerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    private AuthenticationMetadata adminPrincipal;
    private AuthenticationMetadata principal;

    @BeforeEach
    void setup() {
        adminPrincipal = new AuthenticationMetadata
                (UUID.randomUUID(), "adminUser", "password", Role.ADMIN, false, null);
        principal = new AuthenticationMetadata
                (UUID.randomUUID(), "testUser", "password", Role.USER, false, null);
    }

    @Test
    void getAuthenticatedAdminRequestToViewNotifications_returnsManageNotificationsView() throws Exception {

        NotificationResponse notification1 = createMockNotification("Event Reminder", "User event reminder", "SENT");
        NotificationResponse notification2 = createMockNotification("Booking Confirmation", "Booking confirmed", "DELIVERED");
        NotificationResponse notification3 = createMockNotification("System Alert", "System maintenance", "FAILED");

        List<NotificationResponse> mockNotifications = Arrays.asList(notification1, notification2, notification3);

        when(notificationService.getAll()).thenReturn(mockNotifications);

        MockHttpServletRequestBuilder request = get("/admin/notifications")
                .with(user(adminPrincipal));

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isOk())
                .andExpect(view().name("admin/manage-notifications"))
                .andExpect(model().attributeExists("notifications"))
                .andExpect(model().attribute("notifications", mockNotifications));

        verify(notificationService, times(1)).getAll();
    }

    @Test
    void getViewNotificationsWithEmptyList_returnsViewWithEmptyList() throws Exception {

        when(notificationService.getAll()).thenReturn(Collections.emptyList());

        MockHttpServletRequestBuilder request = get("/admin/notifications")
                .with(user(adminPrincipal));

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isOk())
                .andExpect(view().name("admin/manage-notifications"))
                .andExpect(model().attribute("notifications", Collections.emptyList()));

        verify(notificationService, times(1)).getAll();
    }

    @Test
    void getAuthenticatedNonAdminRequestToViewNotifications_returnsForbidden() throws Exception {

        MockHttpServletRequestBuilder request = get("/admin/notifications")
                .with(user(principal));

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isForbidden());

        verify(notificationService, never()).getAll();
    }

    @Test
    void postAuthenticatedAdminRequestToDeleteNotification_deletesAndRedirects() throws Exception {

        UUID notificationId = UUID.randomUUID();

        MockHttpServletRequestBuilder request = post("/admin/notifications/" + notificationId)
                .with(user(adminPrincipal))
                .with(csrf());

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/notifications"))
                .andExpect(flash().attributeExists(SUCCESS_MESSAGE_ATTR))
                .andExpect(flash().attribute(SUCCESS_MESSAGE_ATTR, "Notification deleted successfully!"));

        verify(notificationService, times(1)).delete(notificationId);
    }

    @Test
    void postDeleteNotificationWithoutCsrf_returnsForbidden() throws Exception {

        UUID notificationId = UUID.randomUUID();

        MockHttpServletRequestBuilder request = post("/admin/notifications/" + notificationId)
                .with(user(adminPrincipal));

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isForbidden());

        verify(notificationService, never()).delete(any());
    }

    @Test
    void postAuthenticatedNonAdminRequestToDeleteNotification_returnsForbidden() throws Exception {

        UUID notificationId = UUID.randomUUID();

        MockHttpServletRequestBuilder request = post("/admin/notifications/" + notificationId)
                .with(user(principal))
                .with(csrf());

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isForbidden());

        verify(notificationService, never()).delete(any());
    }

    @Test
    void postUnauthenticatedRequestToDeleteNotification_returnsForbidden() throws Exception {

        UUID notificationId = UUID.randomUUID();

        MockHttpServletRequestBuilder request = post("/admin/notifications/" + notificationId)
                .with(csrf());

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isForbidden());

        verify(notificationService, never()).delete(any());
    }
}