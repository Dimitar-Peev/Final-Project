package com.exam.eventhub.web;

import com.exam.eventhub.booking.model.Booking;
import com.exam.eventhub.booking.service.BookingService;
import com.exam.eventhub.exception.UnauthorizedException;
import lombok.AllArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.UUID;

import static com.exam.eventhub.common.Constants.SUCCESS_MESSAGE_ATTR;

@Controller
@RequestMapping("/payments")
@AllArgsConstructor
@PreAuthorize("isAuthenticated()")
public class PaymentController {

    private final BookingService bookingService;

    @PostMapping("/bookings/{bookingId}")
    public String processPayment(@PathVariable UUID bookingId, Principal principal, RedirectAttributes redirectAttributes) {

        bookingService.markAsPaid(bookingId, principal.getName());

        redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_ATTR, "Payment successful! Your booking is confirmed.");
        return "redirect:/payments/bookings/%s/status?success=true".formatted(bookingId);
    }

    @GetMapping("/bookings/{bookingId}/status")
    public String paymentStatus(@PathVariable UUID bookingId, @RequestParam boolean success, Principal principal, Model model) {
        Booking booking = bookingService.getById(bookingId);

        if (!booking.getUser().getUsername().equals(principal.getName())) {
            throw new UnauthorizedException("You are not authorized to view this payment status");
        }

        model.addAttribute("booking", booking);
        model.addAttribute("success", success);
        return "payment-status";
    }
}