package com.cu.api.service;

/** The caller sent something we cannot act on. Becomes a 400. */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
