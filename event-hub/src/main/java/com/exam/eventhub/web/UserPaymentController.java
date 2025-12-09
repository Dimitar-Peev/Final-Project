package com.exam.eventhub.web;

import com.exam.eventhub.payment.client.dto.PaymentResponse;
import com.exam.eventhub.payment.service.PaymentService;
import com.exam.eventhub.user.service.UserService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Controller
@RequestMapping("/profile/payments")
@AllArgsConstructor
@PreAuthorize("isAuthenticated()")
public class UserPaymentController {

    private final PaymentService paymentService;
    private final UserService userService;

    @GetMapping
    public String viewPaymentHistory(Model model, Principal principal) {
        UUID userId = userService.getByUsername(principal.getName()).getId();
        List<PaymentResponse> payments = paymentService.getPaymentsByUser(userId);

        model.addAttribute("payments", payments);
        return "user/payment-history";
    }
}
