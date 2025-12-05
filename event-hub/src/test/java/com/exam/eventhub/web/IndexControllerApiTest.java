package com.exam.eventhub.web;

import com.exam.eventhub.category.model.Category;
import com.exam.eventhub.category.service.CategoryService;
import com.exam.eventhub.config.TestMvcConfig;
import com.exam.eventhub.config.TestSecurityConfig;
import com.exam.eventhub.contact.service.ContactService;
import com.exam.eventhub.event.model.Event;
import com.exam.eventhub.event.service.EventService;
import com.exam.eventhub.user.model.User;
import com.exam.eventhub.user.service.UserService;
import com.exam.eventhub.venue.model.Venue;
import com.exam.eventhub.venue.service.VenueService;
import com.exam.eventhub.web.dto.ContactCreateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;

import static com.exam.eventhub.common.Constants.*;
import static com.exam.eventhub.util.TestBuilder.createEvent;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.isA;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(IndexController.class)
@Import({TestMvcConfig.class, TestSecurityConfig.class})
public class IndexControllerApiTest {

    @MockitoBean
    private CategoryService categoryService;
    @MockitoBean
    private EventService eventService;
    @MockitoBean
    private ContactService contactService;
    @MockitoBean
    private VenueService venueService;
    @MockitoBean
    private UserService userService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void index_shouldReturnIndexView() throws Exception {

        MockHttpServletRequestBuilder request = get("/");

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isOk())
                .andExpect(view().name("index"));
    }

    @Test
    void allEvents_shouldReturnEventsViewWithCategoriesAndEvents() throws Exception {

        Category music = new Category("Music", "Music description", "#ff6b6b");
        Category technology = new Category("Technology", "Technology description", "#4ecdc4");
        Category business = new Category("Business", "Business description", "#45b7d1");

        User admin = new User();
        admin.setUsername("admin");
        User organizer = new User();
        organizer.setUsername("organizer");

        Venue centralPark = new Venue();
        centralPark.setName("Central Park Amphitheater");
        Venue gallery = new Venue();
        gallery.setName("Gallery Modern");

        given(userService.getByUsername("admin")).willReturn(admin);
        given(userService.getByUsername("organizer")).willReturn(organizer);

        given(venueService.getByName("Central Park Amphitheater")).willReturn(centralPark);
        given(venueService.getByName("Gallery Modern")).willReturn(gallery);

        Event musicFestival = createEvent("Summer Music Festival 2026", centralPark, admin, music);
        Event jazzNight = createEvent("Jazz Night at the Gallery", gallery, organizer, music);

        List<Category> categories = List.of(music, technology, business);
        List<Event> events = List.of(musicFestival, jazzNight);

        given(categoryService.getAll()).willReturn(categories);
        given(eventService.getAll()).willReturn(events);

        MockHttpServletRequestBuilder request = get("/events");

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isOk())
                .andExpect(view().name("events"))
                .andExpect(model().attribute("categories", categories))
                .andExpect(model().attribute("events", events));
    }

    @Test
    void about_shouldReturnAboutView() throws Exception {

        MockHttpServletRequestBuilder request = get("/about");

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isOk())
                .andExpect(view().name("about"));
    }

    @Test
    void showContactForm_shouldReturnContactViewWithEmptyForm() throws Exception {

        MockHttpServletRequestBuilder request = get("/contact");

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isOk())
                .andExpect(view().name("contact"))
                .andExpect(model().attributeExists("contactMessage"))
                .andExpect(model().attribute("contactMessage", isA(ContactCreateRequest.class)));
    }

    @Test
    void showContactForm_whenModelContainsContactMessage_shouldNotAddNewAttribute() throws Exception {

        ContactCreateRequest existingContactMessage = new ContactCreateRequest();
        existingContactMessage.setName("John Doe");
        existingContactMessage.setEmail("john@example.com");

        MockHttpServletRequestBuilder request = get("/contact")
                .flashAttr("contactMessage", existingContactMessage);

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isOk())
                .andExpect(view().name("contact"))
                .andExpect(model().attributeExists("contactMessage"))
                .andExpect(model().attribute("contactMessage", existingContactMessage));
    }

    @Test
    void submitContactForm_sithValidData_shouldRedirectWithSuccessMessage() throws Exception {

        MockHttpServletRequestBuilder request = post("/contact")
                .param("name", "John Doe")
                .param("email", "john@example.com")
                .param("subject", "Test Subject")
                .param("message", "Test message content")
                .with(csrf());

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/contact"))
                .andExpect(flash().attribute(SUCCESS_MESSAGE_ATTR, "Your message has been sent!"));

        verify(contactService, times(1)).add(any(ContactCreateRequest.class));
    }

    @Test
    void submitContactForm_withInvalidData_shouldRedirectWithErrors() throws Exception {

        MockHttpServletRequestBuilder request = post("/contact")
                .param("name", "")
                .param("email", "john@example.com")
                .param("subject", "Test Subject")
                .param("message", "Test message")
                .with(csrf());

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/contact"))
                .andExpect(flash().attributeExists("contactMessage"))
                .andExpect(flash().attributeExists(BINDING_MODEL + "contactMessage"))
                .andExpect(flash().attribute(ERROR_MESSAGE_ATTR, ERROR_MESSAGE));

        verify(contactService, never()).add(any(ContactCreateRequest.class));
    }

    @Test
    void allEvents_whenServicesReturnEmptyLists_shouldStillReturnView() throws Exception {

        when(categoryService.getAll()).thenReturn(List.of());
        when(eventService.getAll()).thenReturn(List.of());

        MockHttpServletRequestBuilder request = get("/events");

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isOk())
                .andExpect(view().name("events"))
                .andExpect(model().attribute("categories", empty()))
                .andExpect(model().attribute("events", empty()));
    }
}
