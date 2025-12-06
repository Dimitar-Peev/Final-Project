package com.exam.eventhub.web;

import com.exam.eventhub.booking.model.Booking;
import com.exam.eventhub.booking.service.BookingService;
import com.exam.eventhub.web.dto.BookingCreateRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.UUID;

import static com.exam.eventhub.common.Constants.*;

@Controller
@RequestMapping("/bookings")
@AllArgsConstructor
@PreAuthorize("isAuthenticated()")
public class BookingController {

    private final BookingService bookingService;

    @GetMapping("/my")
    public String myBookings(Model model, Principal principal) {
        if (!model.containsAttribute("bookings")) {
            model.addAttribute("bookings", bookingService.getBookingsForUser(principal.getName()));
        }

        return "my-bookings";
    }

    @PostMapping
    public String createBooking(@Valid BookingCreateRequest bookingCreateRequest, BindingResult bindingResult,
                                Principal principal, RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("bookingCreateRequest", bookingCreateRequest);
            redirectAttributes.addFlashAttribute(BINDING_MODEL + "bookingCreateRequest", bindingResult);
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR, ERROR_MESSAGE);
            return "redirect:/events/" + bookingCreateRequest.getEventId();
        }

        Booking booking = bookingService.add(bookingCreateRequest, principal.getName());
        redirectAttributes.addFlashAttribute("booking", booking);
        return "redirect:/bookings/confirmation/" + booking.getId();
    }

    @GetMapping("/confirmation/{id}")
    public String showBookingConfirmation(@PathVariable UUID id, Model model) {
        Booking booking = bookingService.getById(id);
        model.addAttribute("booking", booking);
        return "booking-confirmation";
    }

    @PostMapping("/cancel/{id}")
    public String cancelBooking(@PathVariable UUID id, Principal principal, RedirectAttributes redirectAttributes) {
        bookingService.cancelBooking(id, principal.getName());
        redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_ATTR, "Booking cancelled successfully!");
        return "redirect:/bookings/my";
    }
}
