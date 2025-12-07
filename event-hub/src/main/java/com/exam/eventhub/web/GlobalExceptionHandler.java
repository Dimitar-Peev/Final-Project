package com.exam.eventhub.web;

import com.exam.eventhub.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MissingRequestValueException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.ConnectException;
import java.net.SocketTimeoutException;

import static com.exam.eventhub.common.Constants.*;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final String ERROR_VIEW = "error";
    private static final String NOT_FOUND_VIEW = "not-found";

    @ExceptionHandler(UsernameAlreadyExistsException.class)
    public String handleUsernameAlreadyExist(UsernameAlreadyExistsException ex, RedirectAttributes redirectAttributes) {
        log.warn("Username already used: {}", ex.getMessage());
        redirectAttributes.addFlashAttribute("usernameAlreadyExistMessage", ex.getMessage());
        return "redirect:/register";
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public String handleEmailAlreadyUsed(EmailAlreadyExistsException ex, RedirectAttributes redirectAttributes) {
        log.warn("Email already used: {}", ex.getMessage());
        redirectAttributes.addFlashAttribute("emailAlreadyExistMessage", ex.getMessage());
        return "redirect:/register";
    }

    @ExceptionHandler(CategoryAlreadyExistException.class)
    public String handleCategoryAlreadyExist(CategoryAlreadyExistException ex, RedirectAttributes redirectAttributes) {
        log.warn("Category already used: {}", ex.getMessage());
        redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR, ex.getMessage());
        return "redirect:/admin/categories/new";
    }

    @ExceptionHandler(VenueAlreadyExistException.class)
    public String handleVenueAlreadyExist(VenueAlreadyExistException ex, RedirectAttributes redirectAttributes) {
        log.warn("Venue already used: {}", ex.getMessage());
        redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR, ex.getMessage());
        return "redirect:/admin/venues/new";
    }

    @ExceptionHandler(EmailDuplicateException.class)
    public String handleEmailDuplicate(EmailDuplicateException ex, RedirectAttributes redirectAttributes) {
        log.warn("Duplicate email attempt: {}", ex.getMessage());
        redirectAttributes.addFlashAttribute("duplicateEmail", ex.getMessage());
        return "redirect:/profile/edit";
    }

    @ExceptionHandler(VenueDuplicateException.class)
    public String handleVenueDuplicate(VenueDuplicateException ex, RedirectAttributes redirectAttributes) {
        log.warn("Duplicate venue attempt: {}", ex.getMessage());
        redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR, ex.getMessage());
        return "redirect:/admin/venues/" + ex.getVenueId();
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler({
            BookingNotFoundException.class,
            CategoryNotFoundException.class,
            EventNotFoundException.class,
            VenueNotFoundException.class,
            UserNotFoundException.class
    })
    public ModelAndView handleNotFoundExceptions(Exception ex) {
        log.warn("Resource not found: {}", ex.getMessage());

        ModelAndView modelAndView = new ModelAndView(NOT_FOUND_VIEW);
        modelAndView.addObject(ERROR_MESSAGE_ATTR, ex.getMessage());
        return modelAndView;
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler({
            NoResourceFoundException.class,
            MethodArgumentTypeMismatchException.class,
            MissingRequestValueException.class
    })
    public ModelAndView handleHttpNotFound(Exception ex, HttpServletRequest request) {

        String path = request.getRequestURI();
        if (path.contains(".well-known") || path.contains("favicon.ico") || path.endsWith(".map")) {

            ModelAndView modelAndView = new ModelAndView();
            modelAndView.setStatus(HttpStatus.NO_CONTENT);
            return modelAndView;
        }

        log.warn("HTTP not found: {}", ex.getMessage());

        ModelAndView modelAndView = new ModelAndView(NOT_FOUND_VIEW);
        modelAndView.addObject(ERROR_MESSAGE_ATTR, "Page not found");
        return modelAndView;
    }

    @ExceptionHandler({
            ResourceAccessException.class,
            RestClientException.class,
            ConnectException.class,
            SocketTimeoutException.class,
            feign.RetryableException.class,
            feign.FeignException.class
    })
    public ModelAndView handleMicroserviceErrors(Exception ex, HttpServletResponse response) {
        log.error("Microservice communication error: {}", ex.getMessage(), ex);
        response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());

        ModelAndView modelAndView = new ModelAndView(ERROR_VIEW);
        modelAndView.addObject("status", 503);
        modelAndView.addObject("error", "Service Unavailable");
        modelAndView.addObject(ERROR_MESSAGE_ATTR, MICROSERVICE_ERROR);
        return modelAndView;
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({
            IllegalArgumentException.class,
            IllegalStateException.class
    })
    public ModelAndView handleBadRequest(RuntimeException ex) {
        log.error("Bad request error: ", ex);

        ModelAndView modelAndView = new ModelAndView(ERROR_VIEW);
        modelAndView.addObject(ERROR_MESSAGE_ATTR, "Invalid request: " + ex.getMessage());
        return modelAndView;
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public ModelAndView handleException(Exception ex) {
        log.error("Unexpected error: ", ex);

        ModelAndView modelAndView = new ModelAndView(ERROR_VIEW);
        modelAndView.addObject(ERROR_MESSAGE_ATTR, UNEXPECTED_ERROR);
        modelAndView.addObject("exceptionType", ex.getClass().getSimpleName());
        return modelAndView;
    }

    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(AccessDeniedException.class)
    public ModelAndView handleAccessDenied(Exception ex) {
        log.warn("Access denied: {}", ex.getMessage());

        ModelAndView modelAndView = new ModelAndView("error");
        modelAndView.addObject("status", 403);
        modelAndView.addObject("error", "Forbidden");
        modelAndView.addObject(ERROR_MESSAGE_ATTR, "You do not have permission to access this page.");
        return modelAndView;
    }

}