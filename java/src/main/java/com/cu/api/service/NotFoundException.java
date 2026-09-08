package com.cu.api.service;

/** The caller asked for something that does not exist. Becomes a 404. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
