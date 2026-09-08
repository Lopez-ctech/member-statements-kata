package com.cu.api.controller;

import com.cu.api.service.BadRequestException;
import com.cu.api.service.FieldError;
import com.cu.api.service.NotFoundException;
import com.cu.api.service.StatementValidationException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** The one place where a domain error becomes an HTTP status code. */
@RestControllerAdvice
public class ApiExceptionHandler {

    public record ApiError(String error, String message) {
    }

    public record ValidationError(String error, String message, List<FieldError> fieldErrors) {
    }

    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleBadRequest(BadRequestException error) {
        return new ApiError("BAD_REQUEST", error.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleNotFound(NotFoundException error) {
        return new ApiError("NOT_FOUND", error.getMessage());
    }

    @ExceptionHandler(StatementValidationException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ValidationError handleStatementValidation(StatementValidationException error) {
        return new ValidationError(
                "UNPROCESSABLE_ENTITY", error.getMessage(), error.fieldErrors());
    }

    /** A body that is not JSON at all, or is missing entirely. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ValidationError handleUnreadableBody(HttpMessageNotReadableException error) {
        return new ValidationError(
                "UNPROCESSABLE_ENTITY",
                "Statement failed validation",
                List.of(new FieldError("", "body must be valid JSON")));
    }
}
