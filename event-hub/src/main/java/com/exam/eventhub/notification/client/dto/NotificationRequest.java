package com.exam.eventhub.notification.client.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class NotificationRequest {

    private UUID recipientId;
    private String recipientEmail;
    private String subject;
    private String message;
}
