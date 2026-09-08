package com.cu.integration;

import java.util.List;
import java.util.Map;

/**
 * Part C, step 2: the transformation.
 *
 * <p>This is a pure function: same inputs, same output, no network and no files.
 * That is what lets {@code StatementBuilderTest} run with the API switched off.
 *
 * <p>The rules are in the README, section "Part C", and the shape is in
 * {@code docs/statement-schema.json}.
 */
public final class StatementBuilder {

    private StatementBuilder() {
    }

    /**
     * Turns one account and its transactions into a statement document.
     *
     * @param account the body of {@code GET /accounts/{id}}, e.g.
     *     {@code {"id": "ACC-1001", "accountNumber": "483920174821",
     *     "currency": "USD", "openingBalanceCents": 152000, ...}}
     * @param transactions the body of {@code GET /accounts/{id}/transactions},
     *     entries like {@code {"id": 17, "postedAt": "2025-01-03T09:12:00Z",
     *     "type": "DEBIT", "amountCents": 4500, "description": "POS DEBIT COFFEE CO"}}
     * @param period {@code {"from": "2025-01-01", "to": "2025-01-31"}}
     * @return the statement document described in the README
     */
    public static Map<String, Object> buildStatement(
            Map<String, Object> account,
            List<Map<String, Object>> transactions,
            Map<String, String> period) {
        throw new UnsupportedOperationException(
                "Part C: implement buildStatement. See the README, section 'Part C'.");
    }
}
