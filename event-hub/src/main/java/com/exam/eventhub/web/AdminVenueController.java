package com.exam.eventhub.web;

import com.exam.eventhub.venue.model.Venue;
import com.exam.eventhub.venue.service.VenueService;
import com.exam.eventhub.web.dto.VenueCreateRequest;
import com.exam.eventhub.web.dto.VenueEditRequest;
import com.exam.eventhub.web.mapper.DtoMapper;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.UUID;

import static com.exam.eventhub.common.Constants.*;

@Controller
@RequestMapping("/admin/venues")
@AllArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminVenueController {

    private static final String ENTITY_NAME = "Venue";

    private final VenueService venueService;

    @GetMapping
    public String manageVenues(Model model) {
        List<Venue> allVenues = venueService.getAll();
        model.addAttribute("venues", allVenues);
        return "admin/manage-venues";
    }

    @GetMapping("/new")
    public String addVenues(Model model) {
        if (!model.containsAttribute("venueCreateRequest")) {
            model.addAttribute("venueCreateRequest", new VenueCreateRequest());
        }

        return "admin/venue-add";
    }

    @PostMapping
    public String addVenue(@Valid @ModelAttribute("venueCreateRequest") VenueCreateRequest venueCreateRequest,
                           BindingResult bindingResult,
                           RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("venueCreateRequest", venueCreateRequest);
            redirectAttributes.addFlashAttribute(BINDING_MODEL + "venueCreateRequest", bindingResult);
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR, ERROR_MESSAGE);
            return "redirect:/admin/venues/new";
        }

        venueService.add(venueCreateRequest);
        redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_ATTR, ADD_SUCCESSFUL.formatted(ENTITY_NAME));

        return "redirect:/admin/venues";
    }

    @GetMapping("/{id}")
    public String editVenueForm(@PathVariable UUID id, Model model) {
        Venue venue = venueService.getById(id);

        if (!model.containsAttribute("venueEditRequest")) {
            model.addAttribute("venueEditRequest", DtoMapper.mapVenueToVenueEditRequest(venue));
        }

        return "admin/venue-edit";
    }

    @PutMapping("/{id}")
    public String updateVenue(@PathVariable UUID id,
                              @Valid @ModelAttribute("venueEditRequest") VenueEditRequest venueEditRequest,
                              BindingResult bindingResult,
                              RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("venueEditRequest", venueEditRequest);
            redirectAttributes.addFlashAttribute(BINDING_MODEL + "venueEditRequest", bindingResult);
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR, ERROR_MESSAGE);
            return "redirect:/admin/venues/" + id;
        }

        venueService.update(id, venueEditRequest);
        redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_ATTR, UPDATE_SUCCESSFUL.formatted(ENTITY_NAME));

        return "redirect:/admin/venues";
    }

    @DeleteMapping("/{id}")
    public String deleteVenue(@PathVariable UUID id, RedirectAttributes redirectAttributes) {

        venueService.delete(id);
        redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_ATTR, DELETE_SUCCESSFUL.formatted(ENTITY_NAME));

        return "redirect:/admin/venues";
    }
}
