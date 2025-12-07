package com.exam.eventhub.web;

import com.exam.eventhub.booking.service.BookingService;
import lombok.AllArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

import static com.exam.eventhub.common.Constants.SUCCESS_MESSAGE_ATTR;

@Controller
@RequestMapping("/admin/bookings")
@AllArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminBookingController {

    private final BookingService bookingService;

    @GetMapping
    public String listAllBookings(Model model) {
        model.addAttribute("bookings", bookingService.getAllBookings());
        return "admin/manage-bookings";
    }

    @DeleteMapping("/{id}")
    public String cancelBooking(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        bookingService.adminCancelBooking(id);
        redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_ATTR, "Booking cancelled!");
        return "redirect:/admin/bookings";
    }

    @PostMapping("/{id}/refunds")
    public String refundBooking(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        bookingService.refundBooking(id);
        redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_ATTR, "Booking refunded!");
        return "redirect:/admin/bookings";
    }
}
