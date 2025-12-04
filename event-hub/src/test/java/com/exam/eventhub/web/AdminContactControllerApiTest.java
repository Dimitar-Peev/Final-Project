package com.exam.eventhub.web;

import com.exam.eventhub.config.TestMvcConfig;
import com.exam.eventhub.config.TestSecurityConfig;
import com.exam.eventhub.contact.model.Contact;
import com.exam.eventhub.contact.service.ContactService;
import com.exam.eventhub.security.AuthenticationMetadata;
import com.exam.eventhub.user.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.exam.eventhub.util.ApiHelper.createMockContact;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminContactController.class)
@Import({TestMvcConfig.class, TestSecurityConfig.class})
class AdminContactControllerApiTest {

    @MockitoBean
    private ContactService contactService;

    @Autowired
    private MockMvc mockMvc;

    private AuthenticationMetadata adminPrincipal;

    @BeforeEach
    void setUp() {
        adminPrincipal = new AuthenticationMetadata
                (UUID.randomUUID(), "adminUser", "password", Role.ADMIN, false, null);
    }

    @Test
    void getAuthenticatedAdminRequestToManageContacts_returnsManageContactsView() throws Exception {

        Contact contact1 = createMockContact("John Doe", "john@example.com", "Question about events");
        Contact contact2 = createMockContact("Jane Smith", "jane@example.com", "Need help with booking");
        Contact contact3 = createMockContact("Bob Wilson", "bob@example.com", "Feedback about the platform");

        List<Contact> mockContacts = Arrays.asList(contact1, contact2, contact3);
        when(contactService.getAll()).thenReturn(mockContacts);

        MockHttpServletRequestBuilder request = get("/admin/contacts")
                .with(user(adminPrincipal));

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isOk())
                .andExpect(view().name("admin/manage-contacts"))
                .andExpect(model().attributeExists("allContacts"))
                .andExpect(model().attribute("allContacts", mockContacts));

        verify(contactService, times(1)).getAll();
    }

    @Test
    void getAuthenticatedAdminRequestToManageContactsWithEmptyList_returnsViewWithEmptyList() throws Exception {

        when(contactService.getAll()).thenReturn(Collections.emptyList());

        MockHttpServletRequestBuilder request = get("/admin/contacts")
                .with(user(adminPrincipal));

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isOk())
                .andExpect(view().name("admin/manage-contacts"))
                .andExpect(model().attributeExists("allContacts"))
                .andExpect(model().attribute("allContacts", Collections.emptyList()));

        verify(contactService, times(1)).getAll();
    }

    @Test
    void getAuthenticatedNonAdminRequestToManageContacts_returnsForbidden() throws Exception {

        UUID userId = UUID.randomUUID();
        AuthenticationMetadata userPrincipal = new AuthenticationMetadata
                (userId, "regularUser", "password", Role.USER, false, null);

        MockHttpServletRequestBuilder request = get("/admin/contacts")
                .with(user(userPrincipal));

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isForbidden());

        verify(contactService, never()).getAll();
    }

    @Test
    void getUnauthenticatedRequestToManageContacts_returnsForbidden() throws Exception {

        MockHttpServletRequestBuilder request = get("/admin/contacts");

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isForbidden());

        verify(contactService, never()).getAll();
    }

    @Test
    void getAuthenticatedAdminRequestToManageContactsWithMultipleContacts_verifiesCorrectOrder() throws Exception {

        Contact contact1 = createMockContact("Alice", "alice@example.com", "First message");
        Contact contact2 = createMockContact("Bob", "bob@example.com", "Second message");
        Contact contact3 = createMockContact("Charlie", "charlie@example.com", "Third message");

        List<Contact> mockContacts = Arrays.asList(contact1, contact2, contact3);
        when(contactService.getAll()).thenReturn(mockContacts);

        MockHttpServletRequestBuilder request = get("/admin/contacts")
                .with(user(adminPrincipal));

        ResultActions response = mockMvc.perform(request);

        response.andExpect(status().isOk())
                .andExpect(view().name("admin/manage-contacts"))
                .andExpect(model().attributeExists("allContacts"))
                .andExpect(model().attribute("allContacts", mockContacts));

        verify(contactService, times(1)).getAll();
    }
}