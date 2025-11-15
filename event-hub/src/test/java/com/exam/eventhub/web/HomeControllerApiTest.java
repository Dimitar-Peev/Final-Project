package com.exam.eventhub.web;

import com.exam.eventhub.category.model.Category;
import com.exam.eventhub.category.service.CategoryService;
import com.exam.eventhub.venue.model.Venue;
import com.exam.eventhub.venue.service.VenueService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HomeController.class)
@AutoConfigureMockMvc(addFilters = false)
public class HomeControllerApiTest {

    @MockitoBean
    private CategoryService categoryService;
    @MockitoBean
    private VenueService venueService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getCategories_shouldReturnCategoriesView() throws Exception {

        Category category1 = new Category("Music", "Music events", "#FF0000");
        Category category2 = new Category("Sports", "Sports events", "#00FF00");
        List<Category> categories = List.of(category1, category2);

        when(categoryService.getAll()).thenReturn(categories);

        MockHttpServletRequestBuilder request = get("/categories");

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isOk())
                .andExpect(view().name("categories"))
                .andExpect(model().attributeExists("categories"))
                .andExpect(model().attribute("categories", categories));
    }

    @Test
    void getVenues_shouldReturnVenuesView() throws Exception {

        Venue venue1 = new Venue("Arena Sofia", "Sofia Center", "Sofia", 5000,
                BigDecimal.valueOf(500.00), "contact@arena.bg", "+359888111222", "Great venue");
        Venue venue2 = new Venue("Palace of Culture", "NDK", "Sofia", 3000,
                BigDecimal.valueOf(300.00), "ndk@sofia.bg", "+359888333444", "Cultural center");
        List<Venue> venues = List.of(venue1, venue2);

        when(venueService.getAll()).thenReturn(venues);

        MockHttpServletRequestBuilder request = get("/venues");

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isOk())
                .andExpect(view().name("venues"))
                .andExpect(model().attributeExists("venues"))
                .andExpect(model().attribute("venues", venues));
    }

    @Test
    void getAdminDashboard_shouldReturnDashboardView() throws Exception {

        MockHttpServletRequestBuilder request = get("/admin");

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"));
    }
}
