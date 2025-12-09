package com.exam.app.web.mapper;

import com.exam.app.model.Notification;
import com.exam.app.model.NotificationStatus;
import com.exam.app.web.dto.NotificationRequest;
import com.exam.app.web.dto.NotificationResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DtoMapperUTest {

    @Test
    void givenHappyPath_whenMappingNotificationRequestToNotification() {

        NotificationRequest notificationRequest = new NotificationRequest();
        notificationRequest.setRecipientId(UUID.randomUUID());
        notificationRequest.setRecipientEmail("test@test.bg");
        notificationRequest.setSubject("Test Subject");
        notificationRequest.setMessage("Test Message");

        Notification resultDto = DtoMapper.mapToEntity(notificationRequest);

        assertNotNull(resultDto);
        assertEquals(notificationRequest.getRecipientId(), resultDto.getRecipientId());
        assertEquals(notificationRequest.getRecipientEmail(), resultDto.getRecipientEmail());
        assertEquals(notificationRequest.getSubject(), resultDto.getSubject());
        assertEquals(notificationRequest.getMessage(), resultDto.getMessage());
    }

    @Test
    void givenHappyPath_whenMappingNotificationToNotificationResponse() {

        Notification notification = new Notification();
        notification.setId(UUID.randomUUID());
        notification.setRecipientId(UUID.randomUUID());
        notification.setRecipientEmail("test@test.bg");
        notification.setSubject("Test Subject");
        notification.setMessage("Test Message");
        notification.setStatus(NotificationStatus.SENT);
        notification.setCreatedOn(LocalDateTime.now());

        NotificationResponse resultDto = DtoMapper.mapToResponse(notification);

        assertNotNull(resultDto);
        assertEquals(notification.getId(), resultDto.getId());
        assertEquals(notification.getRecipientId(), resultDto.getRecipientId());
        assertEquals(notification.getRecipientEmail(), resultDto.getRecipientEmail());
        assertEquals(notification.getSubject(), resultDto.getSubject());
        assertEquals(notification.getMessage(), resultDto.getMessage());
        assertEquals(notification.getStatus().name(), resultDto.getStatus());
        assertEquals(notification.getCreatedOn(), resultDto.getCreatedOn());
    }
}
