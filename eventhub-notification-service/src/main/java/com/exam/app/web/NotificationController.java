package com.exam.app.web;

import com.exam.app.model.Notification;
import com.exam.app.service.NotificationService;
import com.exam.app.web.dto.NotificationRequest;
import com.exam.app.web.dto.NotificationResponse;
import com.exam.app.web.mapper.DtoMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(Paths.API_V1_BASE_PATH + "/notifications")
@AllArgsConstructor
@Tag(name = "Notification Controller", description = "Notification Controller")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "Send notification", description = "Send notification to a user.")
    @PostMapping
    public ResponseEntity<NotificationResponse> sendNotification(@RequestBody NotificationRequest notificationRequest) {

        Notification saved = notificationService.sendNotification(DtoMapper.mapToEntity(notificationRequest));

        NotificationResponse response = DtoMapper.mapToResponse(saved);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get all notifications", description = "Get all notifications.")
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getAllNotifications() {

        List<NotificationResponse> responseList = notificationService.getAll().stream()
                .map(DtoMapper::mapToResponse)
                .toList();

        return ResponseEntity.ok(responseList);
    }

    @Operation(summary = "Get notifications by user", description = "Get notifications by user.")
    @GetMapping("/{userId}")
    public ResponseEntity<List<NotificationResponse>> getNotificationsByUser(@PathVariable UUID userId) {

        List<NotificationResponse> responseList = notificationService.getNotificationsByUser(userId)
                .stream()
                .map(DtoMapper::mapToResponse)
                .toList();

        return ResponseEntity.ok(responseList);
    }

    @Operation(summary = "Delete notification", description = "Delete notification by id.")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteNotification(@PathVariable UUID id) {

        notificationService.deleteNotification(id);

        return ResponseEntity.ok("Notification deleted.");
    }

    @Operation(summary = "Clear notification history", description = "Clear notification history by user id.")
    @DeleteMapping
    public ResponseEntity<String> clearHistory(@RequestParam(name = "userId") UUID userId) {

        notificationService.clearNotifications(userId);

        return ResponseEntity.ok("Notification history cleared.");
    }
}
