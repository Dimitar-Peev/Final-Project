package com.exam.eventhub.contact.service;

import com.exam.eventhub.contact.model.Contact;
import com.exam.eventhub.contact.repository.ContactRepository;
import com.exam.eventhub.web.dto.ContactCreateRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@AllArgsConstructor
public class ContactService {

    private final ContactRepository contactRepository;

    @CacheEvict(value = "contacts", allEntries = true)
    public Contact add(ContactCreateRequest contactCreateRequest) {
        String name = contactCreateRequest.getName();
        log.info("Creating contact: {}", name);

        Contact contact = create(contactCreateRequest);

        Contact saved = contactRepository.save(contact);

        log.info("Contact [{}] (ID: [{}]) was successfully added.", saved.getName(), saved.getId());

        return saved;
    }

    private Contact create(ContactCreateRequest contactCreateRequest) {
        Contact contact = new Contact();

        contact.setName(contactCreateRequest.getName());
        contact.setEmail(contactCreateRequest.getEmail());
        contact.setMessage(contactCreateRequest.getMessage());

        return contact;
    }

    @Cacheable("contacts")
    public List<Contact> getAll() {
        return contactRepository.findAll();
    }
}
