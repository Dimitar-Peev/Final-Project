package com.exam.eventhub.contact.service;

import com.exam.eventhub.contact.model.Contact;
import com.exam.eventhub.contact.repository.ContactRepository;
import com.exam.eventhub.web.dto.ContactMessageBinding;
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
    public Contact add(ContactMessageBinding contactRequest) {
        String name = contactRequest.getName();
        log.info("Creating contact: {}", name);

        Contact contact = create(contactRequest);

        Contact saved = contactRepository.save(contact);

        log.info("Contact [{}] (ID: [{}]) was successfully added.", saved.getName(), saved.getId());

        return saved;
    }

    private Contact create(ContactMessageBinding contactMessageBinding) {
        Contact contact = new Contact();

        contact.setName(contactMessageBinding.getName());
        contact.setEmail(contactMessageBinding.getEmail());
        contact.setMessage(contactMessageBinding.getMessage());

        return contact;
    }

    @Cacheable("contacts")
    public List<Contact> getAll() {
        return contactRepository.findAll();
    }
}
