package com.exam.app.web;

import com.exam.app.exception.NotificationNotFoundException;
import com.exam.app.web.dto.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
public class ExceptionAdviceUTest {

    @InjectMocks
    private ExceptionAdvice exceptionAdvice;

    @Test
    void handleNotFoundEndpoint_shouldReturnNotFoundResponse() {

        ResponseEntity<ErrorResponse> response = exceptionAdvice.handleNotFoundEndpoint();

        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.NOT_FOUND.value(), response.getBody().getStatus());
        assertEquals("Not supported application endpoint.", response.getBody().getMessage());
    }

    @Test
    void handleNotificationNotFound_shouldReturnNotFoundResponseWithCustomMessage() {

        String errorMessage = "Notification with ID 123 not found";
        NotificationNotFoundException exception = new NotificationNotFoundException(errorMessage);

        ResponseEntity<ErrorResponse> response = exceptionAdvice.handleNotificationNotFound(exception);

        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.NOT_FOUND.value(), response.getBody().getStatus());
        assertEquals(errorMessage, response.getBody().getMessage());
    }

    @Test
    void handleException_shouldReturnInternalServerErrorResponse() {

        String errorMessage = "Unexpected error occurred";
        Exception exception = new Exception(errorMessage);

        ResponseEntity<ErrorResponse> response = exceptionAdvice.handleException(exception);

        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getBody().getStatus());
        assertEquals(errorMessage, response.getBody().getMessage());
    }

    @Test
    void handleException_shouldHandleRuntimeException() {

        String errorMessage = "Runtime error";
        RuntimeException exception = new RuntimeException(errorMessage);

        ResponseEntity<ErrorResponse> response = exceptionAdvice.handleException(exception);

        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(errorMessage, response.getBody().getMessage());
    }
}
