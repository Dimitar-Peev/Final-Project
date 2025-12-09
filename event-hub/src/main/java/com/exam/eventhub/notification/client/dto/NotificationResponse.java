package com.exam.eventhub.notification.client.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private UUID id;
    private UUID recipientId;
    private String recipientEmail;
    private String subject;
    private String message;
    private String status;
    private LocalDateTime createdOn;
}
