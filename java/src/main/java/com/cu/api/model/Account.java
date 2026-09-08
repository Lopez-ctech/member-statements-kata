package com.cu.api.model;

/**
 * An account as it is stored.
 *
 * <p>{@code openingBalanceCents} is an integer number of cents, never a
 * {@code double}: binary floating point cannot hold most decimal fractions
 * exactly, and a ledger has to be exact.
 */
public record Account(
        String id,
        String memberId,
        String accountNumber,
        String type,
        String currency,
        String status,
        long openingBalanceCents) {
}
