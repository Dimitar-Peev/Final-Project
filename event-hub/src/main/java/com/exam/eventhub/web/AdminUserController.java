package com.exam.eventhub.web;

import com.exam.eventhub.user.model.User;
import com.exam.eventhub.user.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.UUID;

import static com.exam.eventhub.common.Constants.SUCCESS_MESSAGE_ATTR;

@Controller
@RequestMapping("/admin/users")
@AllArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    public String listUsers(Model model) {
        List<User> allUsers = userService.getAll();
        model.addAttribute("users", allUsers);
        return "admin/manage-users";
    }

    @GetMapping("/{id}/edit")
    public String editUser(@PathVariable UUID id, Model model) {
        User user = userService.getById(id);
        model.addAttribute("user", user);
        return "admin/user-edit";
    }

    @PutMapping("/{id}")
    public String saveUser(@PathVariable UUID id, @ModelAttribute("user") User user) {
        userService.updateUser(id, user);
        return "redirect:/admin/users";
    }

    @PatchMapping("/{id}/status")
    public String toggleUserStatus(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        User user = userService.getById(id);

        if (user.isBlocked()) {
            userService.unblockUser(id);
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_ATTR, "User unblocked successfully!");
        } else {
            userService.blockUser(id);
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_ATTR, "User blocked successfully!");
        }

        return "redirect:/admin/users";
    }
}
