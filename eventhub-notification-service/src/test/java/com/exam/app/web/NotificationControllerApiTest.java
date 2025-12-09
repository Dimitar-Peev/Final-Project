package com.exam.app.web;

import com.exam.app.model.Notification;
import com.exam.app.model.NotificationStatus;
import com.exam.app.service.NotificationService;
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

import java.util.List;
import java.util.UUID;

import static com.exam.app.util.TestBuilder.createMockNotification;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
@AutoConfigureMockMvc
public class NotificationControllerApiTest {

    @MockitoBean
    private NotificationService notificationService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void sendNotification_happyPath() throws Exception {

        Notification mockNotification = createMockNotification();
        when(notificationService.sendNotification(any(Notification.class))).thenReturn(mockNotification);

        Notification notification = new Notification();
        notification.setId(UUID.randomUUID());
        notification.setRecipientId(UUID.randomUUID());
        notification.setRecipientEmail("test@test.com");
        notification.setSubject("Test Subject");
        notification.setMessage("Test message content");
        notification.setStatus(NotificationStatus.SENT);

        MockHttpServletRequestBuilder request = post("/api/v1/notifications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(notification));

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.recipientId").isNotEmpty())
                .andExpect(jsonPath("$.recipientEmail").isNotEmpty())
                .andExpect(jsonPath("$.subject").isNotEmpty())
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.status").isNotEmpty());

        verify(notificationService, times(1)).sendNotification(any(Notification.class));
    }

    @Test
    void getAllNotificationsNotifications_happyPath() throws Exception {

        Notification mockNotification1 = createMockNotification();
        Notification mockNotification2 = createMockNotification();

        List<Notification> notificationList = List.of(mockNotification1, mockNotification2);
        when(notificationService.getAll()).thenReturn(notificationList);

        MockHttpServletRequestBuilder request = get("/api/v1/notifications");

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isOk())
                .andDo(print())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").isNotEmpty())
                .andExpect(jsonPath("$[0].recipientEmail").isNotEmpty())
                .andExpect(jsonPath("$[1].id").isNotEmpty())
                .andExpect(jsonPath("$[1].recipientEmail").isNotEmpty());

        verify(notificationService, times(1)).getAll();
    }

    @Test
    void getAllNotificationsNotifications_emptyList() throws Exception {

        when(notificationService.getAll()).thenReturn(List.of());

        MockHttpServletRequestBuilder request = get("/api/v1/notifications");

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getNotificationsByUser_happyPath() throws Exception {

        UUID userId = UUID.randomUUID();
        Notification notification1 = createMockNotification();
        Notification notification2 = createMockNotification();
        List<Notification> notificationList = List.of(notification1, notification2);

        when(notificationService.getNotificationsByUser(userId)).thenReturn(notificationList);

        MockHttpServletRequestBuilder request = get("/api/v1/notifications/{userId}", userId);

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").isNotEmpty())
                .andExpect(jsonPath("$[0].recipientEmail").isNotEmpty())
                .andExpect(jsonPath("$[1].id").isNotEmpty())
                .andExpect(jsonPath("$[1].recipientEmail").isNotEmpty());

        verify(notificationService, times(1)).getNotificationsByUser(userId);
    }

    @Test
    void getNotificationsByUser_emptyList() throws Exception {

        UUID userId = UUID.randomUUID();
        when(notificationService.getNotificationsByUser(userId)).thenReturn(List.of());

        MockHttpServletRequestBuilder request = get("/api/v1/notifications/{userId}", userId);

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void deleteNotification_happyPath() throws Exception {

        UUID notificationId = UUID.randomUUID();
        doNothing().when(notificationService).deleteNotification(notificationId);

        MockHttpServletRequestBuilder request = delete("/api/v1/notifications/{id}", notificationId);

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isOk())
                .andExpect(content().string("Notification deleted."));

        verify(notificationService, times(1)).deleteNotification(notificationId);
    }

    @Test
    void clearHistory_happyPath() throws Exception {

        UUID userId = UUID.randomUUID();

        MockHttpServletRequestBuilder request = delete("/api/v1/notifications")
                .param("userId", userId.toString());

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isOk())
                .andExpect(content().string("Notification history cleared."));

        verify(notificationService, times(1)).clearNotifications(userId);
    }
}
