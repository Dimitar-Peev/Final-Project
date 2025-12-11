package com.exam.eventhub.notification.client;

import com.exam.eventhub.notification.client.dto.NotificationRequest;
import com.exam.eventhub.notification.client.dto.NotificationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "notification-service", url = "${eventhub.notification-service.base-url}" + "/api/v1/notifications")
public interface NotificationClient {

    @PostMapping
    ResponseEntity<NotificationResponse> sendNotification(@RequestBody NotificationRequest notification);

    @GetMapping
    ResponseEntity<List<NotificationResponse>> getAllNotifications();

    @GetMapping("/{userId}")
    ResponseEntity<List<NotificationResponse>> getNotificationsByUser(@PathVariable("userId") UUID userId);

    @DeleteMapping("/{id}")
    ResponseEntity<String> deleteNotification(@PathVariable("id") UUID id);

    @DeleteMapping
    ResponseEntity<String> clearHistory(@RequestParam(name = "userId") UUID userId);

}
