package com.exam.eventhub.web;

import com.exam.eventhub.category.service.CategoryService;
import com.exam.eventhub.contact.service.ContactService;
import com.exam.eventhub.event.service.EventService;
import com.exam.eventhub.web.dto.ContactMessageBinding;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import static com.exam.eventhub.common.Constants.*;

@Controller
@AllArgsConstructor
public class IndexController {

    private final CategoryService categoryService;
    private final EventService eventService;
    private final ContactService contactService;

    @GetMapping(value = {"/", "index"})
    public String index() {
        return "index";
    }

    @GetMapping("/events")
    public String allEvents(Model model) {
        model.addAttribute("categories", categoryService.getAll());
        model.addAttribute("events", eventService.findAll());
        return "events";
    }

    @GetMapping("/about")
    public String about() {
        return "about";
    }

    @GetMapping("/contact")
    public String showContactForm(Model model) {
        if (!model.containsAttribute("contactMessage")) {
            model.addAttribute("contactMessage", new ContactMessageBinding());
        }
        return "contact";
    }

    @PostMapping("/contact")
    public String submitContactForm(@Valid @ModelAttribute("contactMessage") ContactMessageBinding contactMessage,
                                    BindingResult bindingResult,
                                    RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("contactMessage", contactMessage);
            redirectAttributes.addFlashAttribute(BINDING_MODEL + "contactMessage", bindingResult);
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR, ERROR_MESSAGE);
            return "redirect:/contact";
        }

        contactService.add(contactMessage);
        redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_ATTR, "Your message has been sent!");

        return "redirect:/contact";
    }

}
