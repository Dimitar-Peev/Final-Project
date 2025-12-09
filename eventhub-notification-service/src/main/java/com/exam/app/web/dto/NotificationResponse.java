package com.exam.app.web.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class NotificationResponse {

    private UUID id;
    private UUID recipientId;
    private String recipientEmail;
    private String subject;
    private String message;
    private String status;
    private LocalDateTime createdOn;
}
