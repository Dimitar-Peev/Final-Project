package com.exam.eventhub.common;

public class Constants {

    public static final String BINDING_MODEL = "org.springframework.validation.BindingResult.";
    public static final String ERROR_MESSAGE_ATTR = "errorMessage";
    public static final String SUCCESS_MESSAGE_ATTR = "successMessage";
    public static final String INFO_MESSAGE_ATTR = "infoMessage";

    public static final String ERROR_MESSAGE = "Please correct the errors below.";
    public static final String ADD_SUCCESSFUL = "%s added successfully!";
    public static final String UPDATE_SUCCESSFUL = "%s updated successfully!";
    public static final String DELETE_SUCCESSFUL = "%s deleted successfully!";
    public static final String NOT_ALLOWED = "You are not allowed to %s this event.";
    public static final String EMAIL_EXIST = "There is an existing account associated with this email";
    public static final String USERNAME_EXIST = "The username '%s' already exists.";

    public static final String ID_DELETED_SUCCESSFUL = "%s with ID [%s] was successfully deleted.";
    public static final String ID_NOT_FOUND = "%s with ID [%s] was not found.";
    public static final String NAME_NOT_FOUND = "%s with name [%s] was not found.";

    public static final String MICROSERVICE_ERROR = "The service is temporarily unavailable. Please try again later.";
    public static final String UNEXPECTED_ERROR = "An unexpected error occurred. Please try again.";
    public static final String DELETE_UNSUCCESSFUL = "Error deleting notification. Please try again later!";

}
