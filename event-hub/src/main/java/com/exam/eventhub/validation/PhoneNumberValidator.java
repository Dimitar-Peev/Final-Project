package com.exam.eventhub.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PhoneNumberValidator implements ConstraintValidator<PhoneNumber, String> {

    private boolean allowEmpty;

    @Override
    public void initialize(PhoneNumber constraintAnnotation) {
        this.allowEmpty = constraintAnnotation.allowEmpty();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {

        if ((value == null || value.isBlank()) && allowEmpty) {
            return true;
        }

        if (value == null || value.isBlank()) {
            return false;
        }

        boolean localPattern = value.matches("^\\d{10}$");

        boolean internationalPattern = value.matches("^\\+359\\d{9}$");

        return localPattern || internationalPattern;
    }
}