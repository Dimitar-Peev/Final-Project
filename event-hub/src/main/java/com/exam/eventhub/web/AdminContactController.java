package com.exam.eventhub.web;

import com.exam.eventhub.contact.model.Contact;
import com.exam.eventhub.contact.service.ContactService;
import lombok.AllArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/admin/contacts")
@AllArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminContactController {

    private final ContactService contactService;

    @GetMapping
    public String manageContacts(Model model) {
        List<Contact> allContacts = contactService.getAll();
        model.addAttribute("allContacts", allContacts);
        return "admin/manage-contacts";
    }
}
