package com.cu.api.model;

/** The shape returned by {@code GET /accounts/{id}}. The current balance is computed. */
public record AccountDetail(
        String id,
        String memberId,
        String accountNumber,
        String type,
        String currency,
        String status,
        long openingBalanceCents,
        long currentBalanceCents) {

    public static AccountDetail of(Account account, long currentBalanceCents) {
        return new AccountDetail(
                account.id(),
                account.memberId(),
                account.accountNumber(),
                account.type(),
                account.currency(),
                account.status(),
                account.openingBalanceCents(),
                currentBalanceCents);
    }
}
