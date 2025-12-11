package com.exam.eventhub.notification;

import com.exam.eventhub.notification.client.dto.NotificationResponse;
import com.exam.eventhub.notification.service.NotificationService;
import com.exam.eventhub.security.AuthenticationMetadata;
import com.exam.eventhub.user.model.Role;
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

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.exam.eventhub.common.Constants.SUCCESS_MESSAGE_ATTR;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class AdminNotificationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    private AuthenticationMetadata adminPrincipal;

    @BeforeEach
    void setup() {
        adminPrincipal =
                new AuthenticationMetadata(UUID.randomUUID(), "adminUser", "password", Role.ADMIN, false, null);
    }

    @Test
    void adminWorkflow_viewAllThenDeleteOneThenViewUpdatedList() throws Exception {

        UUID notificationToDelete = UUID.randomUUID();

        NotificationResponse mockNotificationWithId = createMockNotificationWithId(notificationToDelete, "Old Alert", "System message", "DELIVERED");

        NotificationResponse notification = createMockNotification("Recent Alert", "New message", "SENT");
        NotificationResponse notification2 = createMockNotification("Warning", "Security warning", "SENT");
        List<NotificationResponse> initialNotifications = Arrays.asList(mockNotificationWithId, notification, notification2);

        when(notificationService.getAll()).thenReturn(initialNotifications);

        MockHttpServletRequestBuilder request1 = get("/admin/notifications")
                .with(user(adminPrincipal));

        ResultActions response1 = mockMvc.perform(request1);

        response1.andExpect(status().isOk())
                .andExpect(view().name("admin/manage-notifications"))
                .andExpect(model().attribute("notifications", initialNotifications));

        doNothing().when(notificationService).delete(notificationToDelete);

        MockHttpServletRequestBuilder request2 = post("/admin/notifications/" + notificationToDelete)
                .with(user(adminPrincipal))
                .with(csrf());

        ResultActions response2 = mockMvc.perform(request2);

        response2.andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/notifications"))
                .andExpect(flash().attributeExists(SUCCESS_MESSAGE_ATTR))
                .andExpect(flash().attribute(SUCCESS_MESSAGE_ATTR, "Notification deleted successfully!"));


        NotificationResponse notification3 = createMockNotification("Recent Alert", "New message", "SENT");
        NotificationResponse notification4 = createMockNotification("Warning", "Security warning", "SENT");
        List<NotificationResponse> updatedNotifications = Arrays.asList(notification3, notification4);

        when(notificationService.getAll()).thenReturn(updatedNotifications);

        MockHttpServletRequestBuilder request3 = get("/admin/notifications")
                .with(user(adminPrincipal));

        ResultActions response3 = mockMvc.perform(request3);

        response3.andExpect(status().isOk())
                .andExpect(view().name("admin/manage-notifications"))
                .andExpect(model().attribute("notifications", updatedNotifications));

        verify(notificationService, times(2)).getAll();
        verify(notificationService, times(1)).delete(notificationToDelete);
    }

    private NotificationResponse createMockNotification(String subject, String message, String status) {
        NotificationResponse notification = new NotificationResponse();
        notification.setId(UUID.randomUUID());
        notification.setRecipientId(UUID.randomUUID());
        notification.setRecipientEmail("user@example.com");
        notification.setSubject(subject);
        notification.setMessage(message);
        notification.setStatus(status);
        notification.setCreatedOn(LocalDateTime.now());
        return notification;
    }

    private NotificationResponse createMockNotificationWithId(UUID id, String subject, String message, String status) {
        NotificationResponse notification = new NotificationResponse();
        notification.setId(id);
        notification.setRecipientId(UUID.randomUUID());
        notification.setRecipientEmail("user@example.com");
        notification.setSubject(subject);
        notification.setMessage(message);
        notification.setStatus(status);
        notification.setCreatedOn(LocalDateTime.now());
        return notification;
    }
}