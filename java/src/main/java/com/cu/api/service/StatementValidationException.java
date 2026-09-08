package com.cu.api.service;

import java.util.List;

/** A payload was well-formed JSON but broke the contract. Becomes a 422. */
public class StatementValidationException extends RuntimeException {

    private final transient List<FieldError> fieldErrors;

    public StatementValidationException(List<FieldError> fieldErrors) {
        super("Statement failed validation");
        this.fieldErrors = List.copyOf(fieldErrors);
    }

    public List<FieldError> fieldErrors() {
        return fieldErrors;
    }
}
