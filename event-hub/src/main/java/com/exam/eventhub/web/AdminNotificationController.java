package com.exam.eventhub.web;

import com.exam.eventhub.notification.client.dto.NotificationResponse;
import com.exam.eventhub.notification.service.NotificationService;
import lombok.AllArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.UUID;

import static com.exam.eventhub.common.Constants.SUCCESS_MESSAGE_ATTR;

@Controller
@RequestMapping("/admin/notifications")
@AllArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminNotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public String viewNotifications(Model model) {
        List<NotificationResponse> notifications = notificationService.getAll();
        model.addAttribute("notifications", notifications);
        return "admin/manage-notifications";
    }

    @PostMapping("/{id}")
    public String deleteNotification(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        notificationService.delete(id);
        redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_ATTR, "Notification deleted successfully!");
        return "redirect:/admin/notifications";
    }
}
