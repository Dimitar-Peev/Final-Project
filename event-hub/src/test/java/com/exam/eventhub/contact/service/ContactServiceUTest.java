package com.exam.eventhub.contact.service;

import com.exam.eventhub.contact.model.Contact;
import com.exam.eventhub.contact.repository.ContactRepository;
import com.exam.eventhub.web.dto.ContactCreateRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static com.exam.eventhub.util.TestBuilder.createContact;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ContactServiceUTest {

    @Mock
    private ContactRepository contactRepository;

    @InjectMocks
    private ContactService contactService;

    @Test
    void add_shouldCreateAndReturnContact() {

        ContactCreateRequest request = new ContactCreateRequest();
        request.setName("John Doe");
        request.setEmail("john.doe@example.com");
        request.setMessage("This is a test message");

        Contact savedContact = createContact(UUID.randomUUID(), request.getName(), request.getEmail(), request.getMessage());
        when(contactRepository.save(any(Contact.class))).thenReturn(savedContact);

        Contact result = contactService.add(request);

        assertNotNull(result);
        assertEquals(savedContact.getName(), result.getName());
        assertEquals(savedContact.getEmail(), result.getEmail());
        assertEquals(savedContact.getMessage(), result.getMessage());
        verify(contactRepository, times(1)).save(any(Contact.class));
    }

    @Test
    void add_shouldSetAllFieldsCorrectly() {

        ContactCreateRequest request = new ContactCreateRequest();
        request.setName("Jane Smith");
        request.setEmail("jane.smith@example.com");
        request.setMessage("Another test message");

        Contact savedContact = createContact(UUID.randomUUID(), request.getName(), request.getEmail(), request.getMessage());
        when(contactRepository.save(any(Contact.class))).thenReturn(savedContact);

        Contact result = contactService.add(request);

        assertNotNull(result);
        assertEquals(request.getName(), result.getName());
        assertEquals(request.getEmail(), result.getEmail());
        assertEquals(request.getMessage(), result.getMessage());
        verify(contactRepository, times(1)).save(any(Contact.class));
    }

    @Test
    void getAll_shouldReturnAllContacts() {

        Contact contact1 = createContact(UUID.randomUUID(), "John Doe", "john@example.com", "Message 1");
        Contact contact2 = createContact(UUID.randomUUID(), "Jane Smith", "jane@example.com", "Message 2");
        Contact contact3 = createContact(UUID.randomUUID(), "Bob Johnson", "bob@example.com", "Message 3");
        List<Contact> expectedContacts = List.of(contact1, contact2, contact3);
        when(contactRepository.findAll()).thenReturn(expectedContacts);

        List<Contact> actualContacts = contactService.getAll();

        assertNotNull(actualContacts);
        assertEquals(3, actualContacts.size());
        assertEquals(expectedContacts, actualContacts);
        verify(contactRepository, times(1)).findAll();
    }

    @Test
    void getAll_whenNoContacts_shouldReturnEmptyList() {

        when(contactRepository.findAll()).thenReturn(List.of());

        List<Contact> actualContacts = contactService.getAll();

        assertNotNull(actualContacts);
        assertTrue(actualContacts.isEmpty());
        assertEquals(0, actualContacts.size());
        verify(contactRepository, times(1)).findAll();
    }
}