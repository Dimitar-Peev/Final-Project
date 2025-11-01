package com.exam.eventhub.web;

import com.exam.eventhub.category.service.CategoryService;
import com.exam.eventhub.venue.service.VenueService;
import lombok.AllArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@AllArgsConstructor
public class HomeController {

    private final CategoryService categoryService;
    private final VenueService venueService;

    @GetMapping("/categories")
    public String categories(Model model) {
        model.addAttribute("categories", categoryService.getAll());
        return "categories";
    }

    @GetMapping("/venues")
    public String venues(Model model) {
        model.addAttribute("venues", venueService.getAll());
        return "venues";
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public String dashboard() {
        return "admin/dashboard";
    }

}
