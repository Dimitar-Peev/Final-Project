package com.exam.eventhub.web;

import com.exam.eventhub.config.TestMvcConfig;
import com.exam.eventhub.config.TestSecurityConfig;
import com.exam.eventhub.exception.UnauthorizedException;
import com.exam.eventhub.notification.client.dto.NotificationResponse;
import com.exam.eventhub.notification.service.NotificationService;
import com.exam.eventhub.security.AuthenticationMetadata;
import com.exam.eventhub.user.model.Role;
import com.exam.eventhub.user.model.User;
import com.exam.eventhub.user.service.UserService;
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

import static com.exam.eventhub.common.Constants.*;
import static com.exam.eventhub.util.UserNotificationHelper.createMockNotification;
import static com.exam.eventhub.util.UserNotificationHelper.createMockUser;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserNotificationController.class)
@Import({TestMvcConfig.class, TestSecurityConfig.class})
public class UserNotificationControllerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private UserService userService;

    private NotificationResponse notification1;
    private NotificationResponse notification2;
    private AuthenticationMetadata principal;
    private String username;

    @BeforeEach
    void setup() {
        notification1 = createMockNotification("Event Reminder", "Your event starts tomorrow");
        notification2 = createMockNotification("Booking Confirmed", "Your booking was confirmed");
        principal = new AuthenticationMetadata
                (UUID.randomUUID(), "testUser", "password", Role.USER, false, null);
        username = principal.getUsername();
    }

    @Test
    void getAuthenticatedUserRequestToViewNotifications_returnsNotificationsView() throws Exception {

        UUID userId = UUID.randomUUID();
        User mockUser = createMockUser(username, userId, true);

        List<NotificationResponse> mockNotifications = Arrays.asList(notification1, notification2);

        when(userService.getByUsername(username)).thenReturn(mockUser);
        when(notificationService.getNotificationsByUser(userId)).thenReturn(mockNotifications);

        MockHttpServletRequestBuilder request = get("/notifications")
                .with(user(principal));

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isOk())
                .andExpect(view().name("notifications"))
                .andExpect(model().attributeExists("notifications", "notificationsEnabled"))
                .andExpect(model().attribute("notifications", mockNotifications))
                .andExpect(model().attribute("notificationsEnabled", true));

        verify(userService, times(1)).getByUsername(username);
        verify(notificationService, times(1)).getNotificationsByUser(userId);
    }

    @Test
    void getViewNotificationsWithEmptyList_returnsViewWithEmptyList() throws Exception {

        UUID userId = UUID.randomUUID();
        User mockUser = createMockUser(username, userId, true);

        when(userService.getByUsername(username)).thenReturn(mockUser);
        when(notificationService.getNotificationsByUser(userId)).thenReturn(Collections.emptyList());

        MockHttpServletRequestBuilder request = get("/notifications")
                .with(user(principal));

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isOk())
                .andExpect(view().name("notifications"))
                .andExpect(model().attribute("notifications", Collections.emptyList()))
                .andExpect(model().attribute("notificationsEnabled", true));

        verify(userService, times(1)).getByUsername(username);
        verify(notificationService, times(1)).getNotificationsByUser(userId);
    }

    @Test
    void getViewNotificationsWhenDisabled_returnsViewWithDisabledFlag() throws Exception {

        UUID userId = UUID.randomUUID();
        User mockUser = createMockUser(username, userId, false);

        when(userService.getByUsername(username)).thenReturn(mockUser);
        when(notificationService.getNotificationsByUser(userId)).thenReturn(Collections.emptyList());

        MockHttpServletRequestBuilder request = get("/notifications")
                .with(user(principal));

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isOk())
                .andExpect(view().name("notifications"))
                .andExpect(model().attribute("notificationsEnabled", false));

        verify(userService, times(1)).getByUsername(username);
    }

    @Test
    void getUnauthenticatedRequestToViewNotifications_returnsForbidden() throws Exception {

        MockHttpServletRequestBuilder request = get("/notifications");

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isForbidden());

        verify(userService, never()).getByUsername(any());
        verify(notificationService, never()).getNotificationsByUser(any());
    }

    @Test
    void patchAuthenticatedRequestToToggleNotificationsFromEnabledToDisabled_disablesAndRedirects() throws Exception {

        User mockUser = createMockUser(username, UUID.randomUUID(), true);

        when(userService.getByUsername(username)).thenReturn(mockUser);
        when(userService.toggleNotifications(mockUser)).thenReturn(false);

        MockHttpServletRequestBuilder request = patch("/notifications/settings")
                .with(user(principal))
                .with(csrf());

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/notifications"))
                .andExpect(flash().attributeExists(SUCCESS_MESSAGE_ATTR))
                .andExpect(flash().attribute(SUCCESS_MESSAGE_ATTR, "Notifications have been disabled!"));

        verify(userService, times(1)).getByUsername(username);
        verify(userService, times(1)).toggleNotifications(mockUser);
    }

    @Test
    void patchAuthenticatedRequestToToggleNotificationsFromDisabledToEnabled_enablesAndRedirects() throws Exception {

        User mockUser = createMockUser(username, UUID.randomUUID(), false);

        when(userService.getByUsername(username)).thenReturn(mockUser);
        when(userService.toggleNotifications(mockUser)).thenReturn(true);

        MockHttpServletRequestBuilder request = patch("/notifications/settings")
                .with(user(principal))
                .with(csrf());

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/notifications"))
                .andExpect(flash().attributeExists(SUCCESS_MESSAGE_ATTR))
                .andExpect(flash().attribute(SUCCESS_MESSAGE_ATTR, "Notifications have been enabled!"));

        verify(userService, times(1)).toggleNotifications(mockUser);
    }

    @Test
    void patchToggleNotificationsWithoutCsrf_returnsForbidden() throws Exception {

        MockHttpServletRequestBuilder request = patch("/notifications/settings")
                .with(user(principal));

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isForbidden());

        verify(userService, never()).toggleNotifications(any());
    }

    @Test
    void deleteAuthenticatedRequestToClearNotificationsWithExistingNotifications_clearsAndRedirects() throws Exception {

        UUID userId = UUID.randomUUID();
        User mockUser = createMockUser(username, userId, true);

        List<NotificationResponse> mockNotifications = Arrays.asList(notification1, notification2);

        when(userService.getByUsername(username)).thenReturn(mockUser);
        when(notificationService.getNotificationsByUser(userId)).thenReturn(mockNotifications);

        MockHttpServletRequestBuilder request = delete("/notifications")
                .with(user(principal))
                .with(csrf());

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/notifications"))
                .andExpect(flash().attributeExists(SUCCESS_MESSAGE_ATTR))
                .andExpect(flash().attribute(SUCCESS_MESSAGE_ATTR, "Notification history cleared successfully!"));

        verify(userService, times(1)).getByUsername(username);
        verify(notificationService, times(1)).getNotificationsByUser(userId);
        verify(notificationService, times(1)).clearAllNotifications(mockNotifications, mockUser);
    }

    @Test
    void deleteAuthenticatedRequestToClearNotificationsWithNoNotifications_redirectsWithInfo() throws Exception {

        UUID userId = UUID.randomUUID();
        User mockUser = createMockUser(username, userId, true);

        when(userService.getByUsername(username)).thenReturn(mockUser);
        when(notificationService.getNotificationsByUser(userId)).thenReturn(Collections.emptyList());

        MockHttpServletRequestBuilder request = delete("/notifications")
                .with(user(principal))
                .with(csrf());

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/notifications"))
                .andExpect(flash().attributeExists(INFO_MESSAGE_ATTR))
                .andExpect(flash().attribute(INFO_MESSAGE_ATTR, "No notifications to clear."));

        verify(userService, times(1)).getByUsername(username);
        verify(notificationService, times(1)).getNotificationsByUser(userId);
        verify(notificationService, never()).clearAllNotifications(any(), any());
    }

    @Test
    void deleteClearNotificationsWithoutCsrf_returnsForbidden() throws Exception {

        MockHttpServletRequestBuilder request = delete("/notifications/clear")
                .with(user(principal));

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isForbidden());

        verify(notificationService, never()).clearAllNotifications(any(), any());
    }

    @Test
    void deleteAuthenticatedRequestToDeleteNotification_deletesAndRedirects() throws Exception {

        UUID notificationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        User mockUser = new User();
        mockUser.setId(userId);
        mockUser.setUsername(username);
        when(userService.getByUsername(username)).thenReturn(mockUser);

        doNothing().when(notificationService).deleteUserNotification(notificationId, userId);

        MockHttpServletRequestBuilder request = delete("/notifications/" + notificationId)
                .with(user(principal))
                .with(csrf());

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/notifications"))
                .andExpect(flash().attributeExists(SUCCESS_MESSAGE_ATTR))
                .andExpect(flash().attribute(SUCCESS_MESSAGE_ATTR, "Notification deleted successfully."));

        verify(userService, times(1)).getByUsername(username);
        verify(notificationService, times(1)).deleteUserNotification(notificationId, userId);
    }

    @Test
    void deleteAuthenticatedRequestToDeleteNotification_whenUnauthorized_redirectsWithError() throws Exception {

        UUID notificationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        User mockUser = new User();
        mockUser.setId(userId);
        mockUser.setUsername(username);
        when(userService.getByUsername(username)).thenReturn(mockUser);

        doThrow(new UnauthorizedException(NOT_AUTHORIZED))
                .when(notificationService).deleteUserNotification(notificationId, userId);

        MockHttpServletRequestBuilder request = delete("/notifications/" + notificationId)
                .with(user(principal))
                .with(csrf());

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/notifications"))
                .andExpect(flash().attributeExists(ERROR_MESSAGE_ATTR))
                .andExpect(flash().attribute(ERROR_MESSAGE_ATTR, NOT_AUTHORIZED));

        verify(userService, times(1)).getByUsername(username);
        verify(notificationService, times(1)).deleteUserNotification(notificationId, userId);
    }

    @Test
    void deleteNotificationWithoutCsrf_returnsForbidden() throws Exception {

        UUID notificationId = UUID.randomUUID();

        MockHttpServletRequestBuilder request = delete("/notifications/" + notificationId)
                .with(user(principal));

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isForbidden());

        verify(notificationService, never()).delete(any());
    }
}