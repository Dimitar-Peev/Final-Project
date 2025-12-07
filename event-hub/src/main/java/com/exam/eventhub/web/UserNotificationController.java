package com.exam.eventhub.web;

import com.exam.eventhub.notification.client.dto.NotificationResponse;
import com.exam.eventhub.notification.service.NotificationService;
import com.exam.eventhub.user.model.User;
import com.exam.eventhub.user.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import static com.exam.eventhub.common.Constants.INFO_MESSAGE_ATTR;
import static com.exam.eventhub.common.Constants.SUCCESS_MESSAGE_ATTR;

@Controller
@RequestMapping("/notifications")
@AllArgsConstructor
@PreAuthorize("isAuthenticated()")
public class UserNotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    @GetMapping
    public String viewUserNotifications(Model model, Principal principal) {
        User user = userService.getByUsername(principal.getName());
        UUID userId = user.getId();

        List<NotificationResponse> notifications = notificationService.getNotificationsByUser(userId);

        model.addAttribute("notifications", notifications);
        model.addAttribute("notificationsEnabled", user.isNotificationsEnabled());

        return "notifications";
    }

    @PatchMapping("/settings")
    public String toggleNotifications(Principal principal, RedirectAttributes redirectAttributes) {
        User user = userService.getByUsername(principal.getName());

        boolean newState = userService.toggleNotifications(user);

        String status = newState ? "enabled" : "disabled";

        redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_ATTR, "Notifications have been " + status + "!");

        return "redirect:/notifications";
    }

    @DeleteMapping
    public String clearAllNotifications(Principal principal, RedirectAttributes redirectAttributes) {
        User user = userService.getByUsername(principal.getName());

        List<NotificationResponse> notifications = notificationService.getNotificationsByUser(user.getId());

        if (notifications.isEmpty()) {
            redirectAttributes.addFlashAttribute(INFO_MESSAGE_ATTR, "No notifications to clear.");
            return "redirect:/notifications";
        }

        notificationService.clearAllNotifications(notifications, user);

        redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_ATTR, "Notification history cleared successfully!");

        return "redirect:/notifications";
    }

    @DeleteMapping("/{id}")
    public String deleteNotification(@PathVariable UUID id, Principal principal, RedirectAttributes redirectAttributes) {
        User user = userService.getByUsername(principal.getName());

        notificationService.deleteUserNotification(id, user.getId());

        redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_ATTR, "Notification deleted successfully.");
        return "redirect:/notifications";
    }
}
