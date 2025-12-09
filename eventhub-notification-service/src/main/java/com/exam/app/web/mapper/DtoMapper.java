package com.exam.app.web.mapper;

import com.exam.app.model.Notification;
import com.exam.app.web.dto.NotificationRequest;
import com.exam.app.web.dto.NotificationResponse;
import lombok.experimental.UtilityClass;

@UtilityClass
public class DtoMapper {

    public static Notification mapToEntity(NotificationRequest notificationRequest) {
        Notification notification = new Notification();

        notification.setRecipientId(notificationRequest.getRecipientId());
        notification.setRecipientEmail(notificationRequest.getRecipientEmail());
        notification.setSubject(notificationRequest.getSubject());
        notification.setMessage(notificationRequest.getMessage());

        return notification;
    }

    public static NotificationResponse mapToResponse(Notification notification) {
        NotificationResponse response = new NotificationResponse();

        response.setId(notification.getId());
        response.setRecipientId(notification.getRecipientId());
        response.setRecipientEmail(notification.getRecipientEmail());
        response.setSubject(notification.getSubject());
        response.setMessage(notification.getMessage());
        response.setStatus(notification.getStatus().name());
        response.setCreatedOn(notification.getCreatedOn());

        return response;
    }
}

