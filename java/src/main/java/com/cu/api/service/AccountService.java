package com.cu.api.service;

import com.cu.api.model.Account;
import com.cu.api.model.Transaction;
import com.cu.api.repository.AccountRepository;
import com.cu.api.repository.TransactionRepository;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Business rules for accounts.
 *
 * <p>Everything that is not "how do I speak HTTP" and not "how do I speak SQL"
 * lives here: balance arithmetic, date-range validation, and the
 * does-this-account-exist check.
 *
 * <p>The exceptions thrown here are about the problem, not about HTTP.
 * {@code ApiExceptionHandler} is the only place that decides which status code
 * each one becomes.
 */
@Service
public class AccountService {

    private final AccountRepository accounts;
    private final TransactionRepository transactions;

    AccountService(AccountRepository accounts, TransactionRepository transactions) {
        this.accounts = accounts;
        this.transactions = transactions;
    }

    public List<Account> listAccounts() {
        return accounts.findAll();
    }

    public Account getAccount(String accountId) {
        return accounts
                .findById(accountId)
                .orElseThrow(() -> new NotFoundException("Unknown account '" + accountId + "'"));
    }

    /**
     * Opening balance plus every credit, minus every debit.
     *
     * <p>Computed on every read rather than stored, so it can never drift out of
     * step with the transactions it is derived from.
     */
    public long currentBalanceCents(Account account) {
        return account.openingBalanceCents() + transactions.sumSignedAmountCents(account.id());
    }

    /**
     * Transactions for an account over an inclusive date range.
     *
     * <p>Parameters are validated before the account is looked up, so a request
     * with no dates gets a 400 whether or not the account exists.
     */
    public List<Transaction> transactionsInRange(String accountId, String from, String to) {
        LocalDate fromDate = parseDate("from", from);
        LocalDate toDate = parseDate("to", to);
        if (fromDate.isAfter(toDate)) {
            throw new BadRequestException(
                    "'from' (" + fromDate + ") must not be after 'to' (" + toDate + ")");
        }
        getAccount(accountId);
        return transactions.findInRange(accountId, fromDate.toString(), toDate.toString());
    }

    private static LocalDate parseDate(String name, String value) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(
                    "Query parameter '" + name + "' is required (YYYY-MM-DD)");
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException malformed) {
            throw new BadRequestException("Query parameter '" + name
                    + "' must be a date in YYYY-MM-DD format, got '" + value + "'");
        }
    }
}
