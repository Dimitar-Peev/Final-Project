package com.exam.eventhub.web;

import com.exam.eventhub.booking.service.BookingService;
import com.exam.eventhub.category.service.CategoryService;
import com.exam.eventhub.event.model.Event;
import com.exam.eventhub.event.service.EventService;
import com.exam.eventhub.user.model.Role;
import com.exam.eventhub.user.service.UserService;
import com.exam.eventhub.venue.service.VenueService;
import com.exam.eventhub.web.dto.EventCreateRequest;
import com.exam.eventhub.web.dto.EventEditRequest;
import com.exam.eventhub.web.mapper.DtoMapper;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.UUID;

import static com.exam.eventhub.common.Constants.*;

@Slf4j
@Controller
@RequestMapping("/events")
@AllArgsConstructor
public class EventController {

    private static final String ENTITY_NAME = "Event";

    private final EventService eventService;
    private final UserService userService;
    private final CategoryService categoryService;
    private final VenueService venueService;
    private final BookingService bookingService;

    @GetMapping("/search")
    public String searchEvents(@RequestParam(required = false) String keyword,
                               @RequestParam(required = false) String city,
                               @RequestParam(required = false) UUID categoryId,
                               @RequestParam(required = false) UUID venueId,
                               Model model) {
        model.addAttribute("events", eventService.searchEvents(keyword, city, categoryId, venueId));
        model.addAttribute("categories", categoryService.getAll());
        model.addAttribute("venues", venueService.getAll());
        return "events";
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('EVENT_ORGANIZER','ADMIN')")
    public String manageEvents(Model model, Principal principal) {
        boolean isAdmin = userService.hasRole(principal.getName(), Role.ADMIN);

        if (isAdmin) {
            model.addAttribute("myEvents", eventService.getAllWithDetails());
        } else {
            model.addAttribute("myEvents", eventService.getEventsByOrganizer(principal.getName()));
        }

        return "event/manage-events";
    }

    @GetMapping("/new")
    @PreAuthorize("hasAnyRole('EVENT_ORGANIZER','ADMIN')")
    public String showCreateForm(Model model) {
        if (!model.containsAttribute("eventCreateRequest")) {
            model.addAttribute("eventCreateRequest", new EventCreateRequest());
        }
        model.addAttribute("categories", categoryService.getAll());
        model.addAttribute("venues", venueService.getAll());
        return "event/event-add";
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('EVENT_ORGANIZER','ADMIN')")
    public String createEvent(@Valid @ModelAttribute("eventCreateRequest") EventCreateRequest eventCreateRequest,
                              BindingResult bindingResult,
                              RedirectAttributes redirectAttributes,
                              Principal principal) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("eventCreateRequest", eventCreateRequest);
            redirectAttributes.addFlashAttribute(BINDING_MODEL + "eventCreateRequest", bindingResult);
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR, ERROR_MESSAGE);
            return "redirect:/events/new";
        }

        eventService.add(eventCreateRequest, principal.getName());
        redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_ATTR, ADD_SUCCESSFUL.formatted(ENTITY_NAME));

        return "redirect:/events/my";
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAnyRole('EVENT_ORGANIZER','ADMIN')")
    public String showEditForm(@PathVariable UUID id, Model model, Principal principal, RedirectAttributes redirectAttributes) {
        Event event = eventService.getByIdWithDetails(id);

        if (!isAuthorizedToModifyEvent(event, principal, redirectAttributes, "edit")) {
            return "redirect:/events/my";
        }

        if (!model.containsAttribute("eventEditRequest")) {
            model.addAttribute("eventEditRequest", DtoMapper.mapEventToEventEditRequest(event));
        }

        model.addAttribute("categories", categoryService.getAll());
        model.addAttribute("venues", venueService.getAll());
        return "event/event-edit";
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('EVENT_ORGANIZER','ADMIN')")
    public String updateEvent(@PathVariable UUID id,
                              @Valid @ModelAttribute("eventEditRequest") EventEditRequest eventEditRequest,
                              BindingResult bindingResult,
                              RedirectAttributes redirectAttributes,
                              Principal principal) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("eventEditRequest", eventEditRequest);
            redirectAttributes.addFlashAttribute(BINDING_MODEL + "eventEditRequest", bindingResult);
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR, ERROR_MESSAGE);
            return "redirect:/events/%s/edit".formatted(id);
        }

        eventService.updateEvent(id, eventEditRequest, principal.getName());
        redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_ATTR, UPDATE_SUCCESSFUL.formatted(ENTITY_NAME));

        return "redirect:/events/my";
    }

    @GetMapping("/{id}")
    public String eventDetails(@PathVariable UUID id, Model model, Principal principal) {
        Event event = eventService.getById(id);

        boolean alreadyBooked = false;
        if (principal != null) {
            String username = principal.getName();
            alreadyBooked = bookingService.hasUserBookedEvent(username, id);
        }

        int confirmedBookings = bookingService.getCountTicketsByEventId(id);

        model.addAttribute("event", event);
        model.addAttribute("alreadyBooked", alreadyBooked);
        model.addAttribute("availableTickets", event.getAvailableTicketsSafe(confirmedBookings));
        return "event/event-details";
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('EVENT_ORGANIZER','ADMIN')")
    public String deleteEvent(@PathVariable UUID id, Principal principal, RedirectAttributes redirectAttributes) {
        Event event = eventService.getByIdWithDetails(id);

        if (!isAuthorizedToModifyEvent(event, principal, redirectAttributes, "delete")) {
            return "redirect:/events/my";
        }

        eventService.deleteEvent(id);
        redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_ATTR, DELETE_SUCCESSFUL.formatted(ENTITY_NAME));

        return "redirect:/events/my";
    }

    private boolean isAuthorizedToModifyEvent(Event event, Principal principal, RedirectAttributes redirectAttributes, String action) {
        if (!userService.hasRole(principal.getName(), Role.ADMIN) &&
                !event.getOrganizer().getUsername().equals(principal.getName())) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR, NOT_ALLOWED.formatted(action));
            return false;
        }
        return true;
    }
}
