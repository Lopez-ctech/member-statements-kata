package com.cu.api.repository;

import com.cu.api.model.Account;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** Data access for the accounts table. SQL lives here and nowhere else. */
@Repository
public class AccountRepository {

    private static final String COLUMNS =
            "id, member_id, account_number, type, currency, status, opening_balance_cents";

    private static final RowMapper<Account> ROW_MAPPER = (rs, rowNum) -> new Account(
            rs.getString("id"),
            rs.getString("member_id"),
            rs.getString("account_number"),
            rs.getString("type"),
            rs.getString("currency"),
            rs.getString("status"),
            rs.getLong("opening_balance_cents"));

    private final JdbcTemplate jdbc;

    AccountRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Account> findAll() {
        return jdbc.query("SELECT " + COLUMNS + " FROM accounts ORDER BY id", ROW_MAPPER);
    }

    public Optional<Account> findById(String accountId) {
        List<Account> found =
                jdbc.query("SELECT " + COLUMNS + " FROM accounts WHERE id = ?", ROW_MAPPER, accountId);
        return found.stream().findFirst();
    }
}
