package com.cu.api.repository;

import com.cu.api.model.Transaction;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** Data access for the transactions table. SQL lives here and nowhere else. */
@Repository
public class TransactionRepository {

    private static final String COLUMNS =
            "id, account_id, posted_at, type, amount_cents, description";

    private static final RowMapper<Transaction> ROW_MAPPER = (rs, rowNum) -> new Transaction(
            rs.getLong("id"),
            rs.getString("account_id"),
            rs.getString("posted_at"),
            rs.getString("type"),
            rs.getLong("amount_cents"),
            rs.getString("description"));

    private final JdbcTemplate jdbc;

    TransactionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Signed sum of every transaction on the account.
     *
     * <p>Credits count positive, debits negative. There is no date filter here
     * on purpose: the current balance is the balance today, not the balance
     * over some period.
     */
    public long sumSignedAmountCents(String accountId) {
        Long total = jdbc.queryForObject(
                """
                SELECT COALESCE(SUM(CASE WHEN type = 'CREDIT' THEN amount_cents
                                         ELSE -amount_cents END), 0)
                  FROM transactions
                 WHERE account_id = ?
                """,
                Long.class,
                accountId);
        return total == null ? 0L : total;
    }

    /**
     * Transactions posted in the period, inclusive of both end dates.
     *
     * <p>{@code fromDate} and {@code toDate} are calendar dates (YYYY-MM-DD);
     * posted_at is a full ISO-8601 UTC timestamp. Ordered by posted_at then id,
     * so two transactions with the same timestamp always come back in the same
     * order.
     */
    public List<Transaction> findInRange(String accountId, String fromDate, String toDate) {
        return jdbc.query(
                """
                SELECT id, account_id, posted_at, type, amount_cents, description
                  FROM transactions
                 WHERE account_id = ?
                   AND posted_at >= ?
                   AND posted_at < ?
                 ORDER BY posted_at, id
                """,
                ROW_MAPPER,
                accountId,
                fromDate,
                toDate);
    }
}
