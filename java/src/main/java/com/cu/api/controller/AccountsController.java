package com.cu.api.controller;

import com.cu.api.model.Account;
import com.cu.api.model.AccountDetail;
import com.cu.api.model.AccountSummary;
import com.cu.api.model.Transaction;
import com.cu.api.service.AccountService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP layer.
 *
 * <p>Reads parameters off the request, hands them to the service, and shapes the
 * response. No SQL, no arithmetic. The date parameters are declared optional
 * here on purpose: the service decides what a missing or malformed date means,
 * so the error message is ours rather than the framework's.
 */
@RestController
public class AccountsController {

    private final AccountService accounts;

    AccountsController(AccountService accounts) {
        this.accounts = accounts;
    }

    @GetMapping("/accounts")
    public List<AccountSummary> listAccounts() {
        return accounts.listAccounts().stream().map(AccountSummary::of).toList();
    }

    @GetMapping("/accounts/{accountId}")
    public AccountDetail getAccount(@PathVariable String accountId) {
        Account account = accounts.getAccount(accountId);
        return AccountDetail.of(account, accounts.currentBalanceCents(account));
    }

    @GetMapping("/accounts/{accountId}/transactions")
    public List<Transaction> getTransactions(
            @PathVariable String accountId,
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "to", required = false) String to) {
        return accounts.transactionsInRange(accountId, from, to);
    }
}
