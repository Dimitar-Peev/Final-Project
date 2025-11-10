package com.exam.eventhub.web;

import com.exam.eventhub.security.AuthenticationMetadata;
import com.exam.eventhub.user.model.User;
import com.exam.eventhub.user.service.UserService;
import com.exam.eventhub.web.dto.UserEditRequest;
import com.exam.eventhub.web.mapper.DtoMapper;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

import static com.exam.eventhub.common.Constants.*;

@Controller
@RequestMapping("/profile")
@AllArgsConstructor
public class ProfileController {

    private final UserService userService;

    @GetMapping
    public String profilePage(Model model, Principal principal) {
        User user = userService.getByUsername(principal.getName());
        model.addAttribute("user", user);
        return "user/profile-menu";
    }

    @GetMapping("/edit")
    public String editProfileForm(Model model, Principal principal) {
        User user = userService.getByUsername(principal.getName());

        if (!model.containsAttribute("user")) {
            model.addAttribute("user", DtoMapper.mapUserToUserEditRequest(user));
        }

        return "user/edit-profile";
    }

    @PutMapping
    public String updateProfile(@Valid @ModelAttribute("user") UserEditRequest userEditRequest,
                                BindingResult bindingResult,
                                Principal principal,
                                RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("user", userEditRequest);
            redirectAttributes.addFlashAttribute(BINDING_MODEL + "user", bindingResult);
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR, ERROR_MESSAGE);
            return "redirect:profile/edit";
        }

        userService.updateUserProfile(principal.getName(), userEditRequest);

        User updatedUser = userService.getByUsername(principal.getName());

        AuthenticationMetadata newAuthMetadata = new AuthenticationMetadata();
        newAuthMetadata.setUserId(updatedUser.getId());
        newAuthMetadata.setUsername(updatedUser.getUsername());
        newAuthMetadata.setPassword(updatedUser.getPassword());
        newAuthMetadata.setRole(updatedUser.getRole());
        newAuthMetadata.setBlocked(updatedUser.isBlocked());
        newAuthMetadata.setProfileImageUrl(updatedUser.getProfileImageUrl());

        Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();

        Authentication newAuth = new UsernamePasswordAuthenticationToken(
                newAuthMetadata,
                currentAuth.getCredentials(),
                newAuthMetadata.getAuthorities()
        );

        SecurityContextHolder.getContext().setAuthentication(newAuth);

        redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_ATTR, "Profile updated successfully!");

        return "redirect:/profile";
    }
}
