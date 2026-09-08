package com.cu.api.service;

/** One broken rule in a submitted statement, e.g. {@code totals.credits}. */
public record FieldError(String field, String message) {
}
