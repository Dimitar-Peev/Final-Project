package com.exam.eventhub.web;

import com.exam.eventhub.config.TestMvcConfig;
import com.exam.eventhub.config.TestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static com.exam.eventhub.common.Constants.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TestExceptionController.class)
@Import({TestMvcConfig.class, TestSecurityConfig.class, GlobalExceptionHandler.class})
class GlobalExceptionHandlerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void whenUsernameAlreadyExists_thenRedirectToRegister() throws Exception {

        mockMvc.perform(get("/test/username-exists"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/register"))
                .andExpect(flash().attribute("usernameAlreadyExistMessage", "Username taken"));
    }

    @Test
    void whenEmailAlreadyExists_thenRedirectToRegister() throws Exception {

        mockMvc.perform(get("/test/email-exists"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/register"))
                .andExpect(flash().attribute("emailAlreadyExistMessage", "Email taken"));
    }

    @Test
    void whenCategoryAlreadyExists_thenRedirectToAdminCategoryNew() throws Exception {

        mockMvc.perform(get("/test/category-exists"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/categories/new"))
                .andExpect(flash().attribute(ERROR_MESSAGE_ATTR, "Category exists"));
    }

    @Test
    void whenVenueAlreadyExists_thenRedirectToAdminVenueNew() throws Exception {

        mockMvc.perform(get("/test/venue-exists"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/venues/new"))
                .andExpect(flash().attribute(ERROR_MESSAGE_ATTR, "Venue exists"));
    }

    @Test
    void whenEmailDuplicate_thenRedirectToProfileEdit() throws Exception {

        mockMvc.perform(get("/test/email-duplicate"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile/edit"))
                .andExpect(flash().attribute("duplicateEmail", "Duplicate email"));
    }

    @Test
    void whenVenueDuplicate_thenRedirectToVenueById() throws Exception {
        UUID venueId = UUID.fromString("7c120bb1-ddd6-4284-9f31-38ac9a067490");

        mockMvc.perform(get("/test/venue-duplicate"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/venues/" + venueId))
                .andExpect(flash().attribute(ERROR_MESSAGE_ATTR, "Duplicate venue"));
    }

    @Test
    void whenEntityNotFound_thenReturnNotFoundView() throws Exception {

        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("not-found"))
                .andExpect(model().attribute(ERROR_MESSAGE_ATTR, "Event missing"));
    }

    @Test
    void whenMicroserviceFails_thenReturn503() throws Exception {

        mockMvc.perform(get("/test/microservice"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("status", 503))
                .andExpect(model().attribute("error", "Service Unavailable"))
                .andExpect(model().attribute(ERROR_MESSAGE_ATTR,MICROSERVICE_ERROR));
    }

    @Test
    void whenBadRequest_thenReturn400() throws Exception {

        mockMvc.perform(get("/test/bad-request"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error"))
                .andExpect(model().attribute(ERROR_MESSAGE_ATTR,
                        "Invalid request: Invalid input"));
    }

    @Test
    void whenUnexpectedException_thenReturn500() throws Exception {

        mockMvc.perform(get("/test/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(view().name("error"))
                .andExpect(model().attribute(ERROR_MESSAGE_ATTR, UNEXPECTED_ERROR))
                .andExpect(model().attributeExists("exceptionType"));
    }

    @Test
    void whenAccessDenied_thenReturn403() throws Exception {

        mockMvc.perform(get("/test/forbidden"))
                .andExpect(status().isForbidden())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("status", 403))
                .andExpect(model().attribute("error", "Forbidden"))
                .andExpect(model().attribute(ERROR_MESSAGE_ATTR, "You do not have permission to access this page."));
    }
}
