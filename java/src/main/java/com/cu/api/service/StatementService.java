package com.cu.api.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/**
 * The statement receiver.
 *
 * <p>A stub. A real one would hand the document to a delivery system; this one
 * checks the contract and keeps it in memory so the integration exercise has
 * something to POST to. Nothing survives a restart.
 */
@Service
public class StatementService {

    /** Money in a statement is a string with exactly two decimals, e.g. "-45.00". */
    public static final Pattern AMOUNT_PATTERN = Pattern.compile("^-?\\d+\\.\\d{2}$");

    /** Four asterisks and the last four digits, e.g. "****4821". */
    public static final Pattern MASKED_ACCOUNT_NUMBER_PATTERN = Pattern.compile("^\\*{4}\\d{4}$");

    private final Map<String, Object> statements = new ConcurrentHashMap<>();
    private final AtomicInteger statementNumbers = new AtomicInteger();

    public String recordStatement(Object payload) {
        List<FieldError> errors = validateStatement(payload);
        if (!errors.isEmpty()) {
            throw new StatementValidationException(errors);
        }
        String statementId = String.format("STMT-%04d", statementNumbers.incrementAndGet());
        statements.put(statementId, payload);
        return statementId;
    }

    public Object findStatement(String statementId) {
        return statements.get(statementId);
    }

    /** Returns one entry per broken rule. An empty list means the payload is good. */
    public List<FieldError> validateStatement(Object payload) {
        if (!(payload instanceof Map<?, ?> statement)) {
            return List.of(new FieldError("", "statement must be a JSON object"));
        }

        List<FieldError> errors = new ArrayList<>();
        requireText(statement, "accountId", "", errors);
        requireMatch(statement, "maskedAccountNumber", MASKED_ACCOUNT_NUMBER_PATTERN, "", errors);
        requireText(statement, "currency", "", errors);
        requireAmount(statement, "openingBalance", "", errors);
        requireAmount(statement, "closingBalance", "", errors);
        requireInteger(statement, "largeTransactionCount", "", errors);

        if (statement.get("period") instanceof Map<?, ?> period) {
            requireText(period, "from", "period.", errors);
            requireText(period, "to", "period.", errors);
        } else {
            errors.add(new FieldError("period", "is required and must be an object"));
        }

        if (statement.get("totals") instanceof Map<?, ?> totals) {
            requireInteger(totals, "creditCount", "totals.", errors);
            requireInteger(totals, "debitCount", "totals.", errors);
            requireAmount(totals, "credits", "totals.", errors);
            requireAmount(totals, "debits", "totals.", errors);
        } else {
            errors.add(new FieldError("totals", "is required and must be an object"));
        }

        if (statement.get("transactions") instanceof List<?> lines) {
            for (int index = 0; index < lines.size(); index++) {
                String at = "transactions[" + index + "]";
                if (!(lines.get(index) instanceof Map<?, ?> line)) {
                    errors.add(new FieldError(at, "must be an object"));
                    continue;
                }
                requireInteger(line, "id", at + ".", errors);
                requireText(line, "date", at + ".", errors);
                requireText(line, "description", at + ".", errors);
                requireAmount(line, "amount", at + ".", errors);
                requireAmount(line, "runningBalance", at + ".", errors);
                requireBoolean(line, "largeTransaction", at + ".", errors);
            }
        } else {
            errors.add(new FieldError("transactions", "is required and must be an array"));
        }

        return errors;
    }

    private static void require(Map<?, ?> container, String name, String prefix,
            Predicate<Object> isValid, String message, List<FieldError> errors) {
        if (!container.containsKey(name)) {
            errors.add(new FieldError(prefix + name, "is required"));
        } else if (!isValid.test(container.get(name))) {
            errors.add(new FieldError(prefix + name, message));
        }
    }

    private static void requireText(
            Map<?, ?> container, String name, String prefix, List<FieldError> errors) {
        require(container, name, prefix,
                value -> value instanceof String text && !text.isEmpty(),
                "must be a non-empty string", errors);
    }

    private static void requireAmount(
            Map<?, ?> container, String name, String prefix, List<FieldError> errors) {
        requireMatch(container, name, AMOUNT_PATTERN, prefix, errors);
    }

    private static void requireMatch(Map<?, ?> container, String name, Pattern pattern,
            String prefix, List<FieldError> errors) {
        require(container, name, prefix,
                value -> value instanceof String text && pattern.matcher(text).matches(),
                "must be a string matching " + pattern.pattern(), errors);
    }

    private static void requireInteger(
            Map<?, ?> container, String name, String prefix, List<FieldError> errors) {
        require(container, name, prefix,
                value -> value instanceof Integer || value instanceof Long,
                "must be an integer", errors);
    }

    private static void requireBoolean(
            Map<?, ?> container, String name, String prefix, List<FieldError> errors) {
        require(container, name, prefix,
                value -> value instanceof Boolean, "must be a boolean", errors);
    }
}
