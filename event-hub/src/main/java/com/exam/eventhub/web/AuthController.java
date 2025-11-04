package com.exam.eventhub.web;

import com.exam.eventhub.user.service.UserService;
import com.exam.eventhub.web.dto.LoginRequest;
import com.exam.eventhub.web.dto.RegisterRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import static com.exam.eventhub.common.Constants.*;

@Slf4j
@Controller
@AllArgsConstructor
public class AuthController {

    private final UserService userService;

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        if (!model.containsAttribute("registerRequest")) {
            model.addAttribute("registerRequest", new RegisterRequest());
        }
        return "register";
    }

    @PostMapping("/register")
    public String processRegistration(@Valid @ModelAttribute("registerRequest") RegisterRequest registerRequest,
                                      BindingResult bindingResult,
                                      RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("registerRequest", registerRequest);
            redirectAttributes.addFlashAttribute(BINDING_MODEL + "registerRequest", bindingResult);
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR, ERROR_MESSAGE);
            return "redirect:/register";
        }

        userService.register(registerRequest);

        return "redirect:/login?registered";
    }

    @GetMapping("/login")
    public String showLoginPage(Model model) {
        if (!model.containsAttribute("loginRequest")) {
            model.addAttribute("loginRequest", new LoginRequest());
        }
        return "login";
    }
}