package com.exam.eventhub.web;

import com.exam.eventhub.config.TestMvcConfig;
import com.exam.eventhub.config.TestSecurityConfig;
import com.exam.eventhub.security.AuthenticationMetadata;
import com.exam.eventhub.user.model.Role;
import com.exam.eventhub.venue.model.Venue;
import com.exam.eventhub.venue.service.VenueService;
import com.exam.eventhub.web.dto.VenueCreateRequest;
import com.exam.eventhub.web.dto.VenueEditRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.exam.eventhub.common.Constants.*;
import static com.exam.eventhub.util.ApiHelper.createMockVenue;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminVenueController.class)
@Import({TestMvcConfig.class, TestSecurityConfig.class})
public class AdminVenueControllerApiTest {

    @MockitoBean
    private VenueService venueService;

    @Autowired
    private MockMvc mockMvc;

    private AuthenticationMetadata adminPrincipal;
    private AuthenticationMetadata principal;

    @BeforeEach
    void setup() {
        adminPrincipal = new AuthenticationMetadata
                (UUID.randomUUID(), "adminUser", "password", Role.ADMIN, false, null);
        principal = new AuthenticationMetadata
                (UUID.randomUUID(), "testUser", "password", Role.USER, false, null);
    }

    @Test
    void getAuthenticatedAdminRequestToManageVenues_returnsManageVenuesView() throws Exception {

        Venue venue1 = createMockVenue("Venue One", "Description One");
        Venue venue2 = createMockVenue("Venue Two", "Description Two");
        Venue venue3 = createMockVenue("Venue Three", "Description Three");

        List<Venue> mockVenues = Arrays.asList(venue1, venue2, venue3);
        when(venueService.getAll()).thenReturn(mockVenues);

        MockHttpServletRequestBuilder request = get("/admin/venues")
                .with(user(adminPrincipal));

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isOk())
                .andExpect(view().name("admin/manage-venues"))
                .andExpect(model().attributeExists("venues"))
                .andExpect(model().attribute("venues", mockVenues));

        verify(venueService, times(1)).getAll();
    }

    @Test
    void getAuthenticatedNonAdminRequestToManageVenues_returnsForbidden() throws Exception {

        MockHttpServletRequestBuilder request = get("/admin/venues")
                .with(user(principal));

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isForbidden());

        verify(venueService, never()).getAll();
    }

    @Test
    void getAuthenticatedAdminRequestToAddVenueForm_returnsVenueAddView() throws Exception {

        MockHttpServletRequestBuilder request = get("/admin/venues/new")
                .with(user(adminPrincipal));

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isOk())
                .andExpect(view().name("admin/venue-add"))
                .andExpect(model().attributeExists("venueCreateRequest"));
    }

    @Test
    void getAddVenueFormWithExistingModelAttribute_doesNotOverrideAttribute() throws Exception {

        VenueCreateRequest existingRequest = new VenueCreateRequest();
        existingRequest.setName("ExistingName");
        existingRequest.setAddress("Existing address");
        existingRequest.setCity("Existing City");
        existingRequest.setCapacity(200);
        existingRequest.setHourlyRate(new BigDecimal("150.00"));

        MockHttpServletRequestBuilder request = get("/admin/venues/new")
                .flashAttr("venueCreateRequest", existingRequest)
                .with(user(adminPrincipal));

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isOk())
                .andExpect(view().name("admin/venue-add"))
                .andExpect(model().attributeExists("venueCreateRequest"))
                .andExpect(model().attribute("venueCreateRequest", existingRequest));
    }

    @Test
    void getAuthenticatedNonAdminRequestToAddVenueForm_returnsForbidden() throws Exception {

        MockHttpServletRequestBuilder request = get("/admin/venues/new")
                .with(user(principal));

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isForbidden());
    }

    @Test
    void postValidVenueCreateRequest_addsVenueAndRedirects() throws Exception {

        MockHttpServletRequestBuilder request = post("/admin/venues")
                .param("name", "Tech Conference Center")
                .param("address", "123 Tech St")
                .param("city", "Techville")
                .param("capacity", "300")
                .param("hourlyRate", "100.00")
                .with(user(adminPrincipal))
                .with(csrf());

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/venues"))
                .andExpect(flash().attributeExists(SUCCESS_MESSAGE_ATTR));

        verify(venueService, times(1)).add(any(VenueCreateRequest.class));
    }

    @Test
    void postInvalidVenueCreateRequest_redirectsToFormWithErrors() throws Exception {

        MockHttpServletRequestBuilder request = post("/admin/venues")
                .param("name", "")
                .param("address", "")
                .param("city", "")
                .param("capacity", "")
                .param("hourlyRate", "100.00")
                .with(user(adminPrincipal))
                .with(csrf());

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/venues/new"))
                .andExpect(flash().attributeExists("venueCreateRequest"))
                .andExpect(flash().attributeExists(BINDING_MODEL + "venueCreateRequest"))
                .andExpect(flash().attributeExists(ERROR_MESSAGE_ATTR));

        verify(venueService, never()).add(any());
    }

    @Test
    void postAuthenticatedNonAdminVenueCreateRequest_returnsForbidden() throws Exception {

        MockHttpServletRequestBuilder request = post("/admin/venues")
                .param("name", "Tech Conference Center")
                .param("address", "123 Tech St")
                .param("city", "Techville")
                .param("capacity", "300")
                .param("hourlyRate", "100.00")
                .with(user(principal))
                .with(csrf());

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isForbidden());

        verify(venueService, never()).add(any());
    }

    @Test
    void postVenueWithoutCsrf_returnsForbidden() throws Exception {

        MockHttpServletRequestBuilder request = post("/admin/venues")
                .param("name", "Tech Conference Center")
                .param("address", "123 Tech St")
                .param("city", "Techville")
                .param("capacity", "300")
                .param("hourlyRate", "100.00")
                .with(user(adminPrincipal));

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isForbidden());

        verify(venueService, never()).add(any());
    }

    @Test
    void getAuthenticatedAdminRequestToEditVenueForm_returnsVenueEditView() throws Exception {

        UUID venueId = UUID.randomUUID();
        Venue mockVenue = createMockVenue("Grand Hall", "A large event venue");
        mockVenue.setId(venueId);
        when(venueService.getById(venueId)).thenReturn(mockVenue);

        MockHttpServletRequestBuilder request = get("/admin/venues/" + venueId)
                .with(user(adminPrincipal));

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isOk())
                .andExpect(view().name("admin/venue-edit"))
                .andExpect(model().attributeExists("venueEditRequest"));

        verify(venueService, times(1)).getById(venueId);
    }

    @Test
    void getEditCategoryFormWithExistingModelAttribute_doesNotOverrideAttribute() throws Exception {

        UUID venueId = UUID.randomUUID();
        Venue mockVenue = createMockVenue("Grand Hall", "A large event venue");
        when(venueService.getById(venueId)).thenReturn(mockVenue);

        VenueEditRequest existingRequest = new VenueEditRequest();
        existingRequest.setName("ExistingName");
        existingRequest.setAddress("Existing address");

        MockHttpServletRequestBuilder request = get("/admin/venues/" + venueId)
                .flashAttr("venueEditRequest", existingRequest)
                .with(user(adminPrincipal));

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isOk())
                .andExpect(view().name("admin/venue-edit"))
                .andExpect(model().attributeExists("venueEditRequest"))
                .andExpect(model().attribute("venueEditRequest", existingRequest));

        verify(venueService, times(1)).getById(venueId);
    }

    @Test
    void getAuthenticatedNonAdminRequestToEditVenueForm_returnsForbidden() throws Exception {


        UUID venueId = UUID.randomUUID();

        MockHttpServletRequestBuilder request = get("/admin/venues/" + venueId)
                .with(user(principal));

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isForbidden());

        verify(venueService, never()).getById(any());
    }

    @Test
    void putValidVenueEditRequest_updatesVenueAndRedirects() throws Exception {

        UUID venueId = UUID.randomUUID();

        MockHttpServletRequestBuilder request = put("/admin/venues/" + venueId)
                .param("name", "Updated Venue Name")
                .param("address", "Updated Venue Address")
                .param("city", "Updated City")
                .param("capacity", "500")
                .param("hourlyRate", "150.00")
                .with(user(adminPrincipal))
                .with(csrf());

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/venues"))
                .andExpect(flash().attributeExists(SUCCESS_MESSAGE_ATTR));

        verify(venueService, times(1)).update(eq(venueId), any(VenueEditRequest.class));
    }

    @Test
    void putInvalidVenueEditRequest_redirectsToFormWithErrors() throws Exception {

        UUID venueId = UUID.randomUUID();

        MockHttpServletRequestBuilder request = put("/admin/venues/" + venueId)
                .param("name", "")
                .param("address", "")
                .with(user(adminPrincipal))
                .with(csrf());

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/venues/" + venueId))
                .andExpect(flash().attributeExists("venueEditRequest"))
                .andExpect(flash().attributeExists(BINDING_MODEL + "venueEditRequest"))
                .andExpect(flash().attributeExists(ERROR_MESSAGE_ATTR));

        verify(venueService, never()).update(any(), any());
    }

    @Test
    void putAuthenticatedNonAdminVenueEditRequest_returnsForbidden() throws Exception {

        UUID venueId = UUID.randomUUID();

        MockHttpServletRequestBuilder request = put("/admin/venues/" + venueId)
                .param("name", "Updated Venue Name")
                .param("address", "Updated Venue Address")
                .param("city", "Updated City")
                .param("capacity", "500")
                .param("hourlyRate", "150.00")
                .with(user(principal))
                .with(csrf());

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isForbidden());

        verify(venueService, never()).update(any(), any());
    }

    @Test
    void deleteAuthenticatedAdminRequestToDeleteVenue_deletesVenueAndRedirects() throws Exception {

        UUID categoryId = UUID.randomUUID();

        MockHttpServletRequestBuilder request = delete("/admin/venues/" + categoryId)
                .with(user(adminPrincipal))
                .with(csrf());

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/venues"))
                .andExpect(flash().attributeExists(SUCCESS_MESSAGE_ATTR));

        verify(venueService, times(1)).delete(categoryId);
    }

    @Test
    void deleteVenueWithoutCsrf_returnsForbidden() throws Exception {

        UUID categoryId = UUID.randomUUID();

        MockHttpServletRequestBuilder request = delete("/admin/venues/" + categoryId)
                .with(user(adminPrincipal));

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isForbidden());

        verify(venueService, never()).delete(any());
    }
}
