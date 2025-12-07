package com.exam.eventhub.web;

import com.exam.eventhub.exception.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.ResourceAccessException;

import java.util.UUID;

@RestController
@RequestMapping("/test")
class TestExceptionController {

    @GetMapping("/username-exists")
    void usernameExists() {
        throw new UsernameAlreadyExistsException("Username taken");
    }

    @GetMapping("/email-exists")
    void emailExists() {
        throw new EmailAlreadyExistsException("Email taken");
    }

    @GetMapping("/category-exists")
    void categoryExists() {
        throw new CategoryAlreadyExistException("Category exists");
    }

    @GetMapping("/venue-exists")
    void venueExists() {
        throw new VenueAlreadyExistException("Venue exists");
    }

    @GetMapping("/email-duplicate")
    void emailDuplicate() {
        throw new EmailDuplicateException("Duplicate email");
    }

    @GetMapping("/venue-duplicate")
    void venueDuplicate() {
        throw new VenueDuplicateException("Duplicate venue", UUID.fromString("7c120bb1-ddd6-4284-9f31-38ac9a067490"));
    }

    @GetMapping("/not-found")
    void notFound() {
        throw new EventNotFoundException("Event missing");
    }

    @GetMapping("/bad-request")
    void badRequest() {
        throw new IllegalArgumentException("Invalid input");
    }

    @GetMapping("/forbidden")
    void forbidden() {
        throw new AccessDeniedException("Forbidden");
    }

    @GetMapping("/microservice")
    void microservice() {
        throw new ResourceAccessException("Service down");
    }

    @GetMapping("/unexpected")
    void unexpected() {
        throw new RuntimeException("Boom");
    }
}
