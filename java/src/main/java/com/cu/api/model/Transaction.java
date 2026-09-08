package com.cu.api.model;

/**
 * One posted transaction.
 *
 * <p>{@code amountCents} is always positive. The sign of the transaction comes
 * from {@code type}, which is either {@code DEBIT} or {@code CREDIT}.
 * {@code postedAt} is an ISO 8601 UTC timestamp, e.g. {@code 2025-01-31T14:30:00Z}.
 */
public record Transaction(
        long id,
        String accountId,
        String postedAt,
        String type,
        long amountCents,
        String description) {
}
