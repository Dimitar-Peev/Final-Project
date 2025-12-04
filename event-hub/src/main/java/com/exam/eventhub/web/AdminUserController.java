package com.exam.eventhub.web;

import com.exam.eventhub.user.model.User;
import com.exam.eventhub.user.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

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

    @GetMapping("/edit/{id}")
    public String editUser(@PathVariable UUID id, Model model) {
        User user = userService.getById(id);
        model.addAttribute("user", user);
        return "admin/user-edit";
    }

    @PutMapping("/edit/{id}")
    public String saveUser(@PathVariable UUID id, @ModelAttribute("user") User user) {
        userService.updateUser(id, user);
        return "redirect:/admin/users";
    }

    @PatchMapping("/block/{id}")
    public String blockUser(@PathVariable UUID id) {
        userService.blockUser(id);
        return "redirect:/admin/users";
    }

    @PatchMapping("/unblock/{id}")
    public String unblockUser(@PathVariable UUID id) {
        userService.unblockUser(id);
        return "redirect:/admin/users";
    }
}
