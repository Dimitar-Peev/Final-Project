package com.exam.eventhub.web;

import com.exam.eventhub.booking.model.Booking;
import com.exam.eventhub.booking.service.BookingService;
import com.exam.eventhub.exception.UnauthorizedException;
import com.exam.eventhub.web.dto.BookingCreateRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
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
    public String createBooking(@Valid @ModelAttribute("bookingCreateRequest") BookingCreateRequest bookingCreateRequest,
                                BindingResult bindingResult, Principal principal, RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("bookingCreateRequest", bookingCreateRequest);
            redirectAttributes.addFlashAttribute(BINDING_MODEL + "bookingCreateRequest", bindingResult);
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR, ERROR_MESSAGE);
            return "redirect:/events/" + bookingCreateRequest.getEventId();
        }

        Booking booking = bookingService.add(bookingCreateRequest, principal.getName());
        redirectAttributes.addFlashAttribute("booking", booking);

        return "redirect:/bookings/%s/confirmation".formatted(booking.getId());
    }

    @GetMapping("/{id}/confirmation")
    public String showBookingConfirmation(@PathVariable UUID id, Model model, Principal principal) {
        Booking booking = bookingService.getById(id);

        if (!booking.getUser().getUsername().equals(principal.getName())) {
            throw new UnauthorizedException("You are not authorized to view this booking");
        }

        model.addAttribute("booking", booking);
        return "booking-confirmation";
    }

    @DeleteMapping("/{id}")
    public String cancelBooking(@PathVariable UUID id, Principal principal, RedirectAttributes redirectAttributes) {
        bookingService.cancelBooking(id, principal.getName());
        redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_ATTR, "Booking cancelled successfully!");
        return "redirect:/bookings/my";
    }
}
