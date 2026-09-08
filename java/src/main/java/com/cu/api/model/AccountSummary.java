package com.cu.api.model;

/** The shape returned by {@code GET /accounts}. Deliberately carries no balances. */
public record AccountSummary(
        String id,
        String memberId,
        String accountNumber,
        String type,
        String currency,
        String status) {

    public static AccountSummary of(Account account) {
        return new AccountSummary(
                account.id(),
                account.memberId(),
                account.accountNumber(),
                account.type(),
                account.currency(),
                account.status());
    }
}
