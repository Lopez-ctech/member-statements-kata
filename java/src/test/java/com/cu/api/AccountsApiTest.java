package com.cu.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

/**
 * Tests for the accounts API.
 *
 * <p>One of these fails on a fresh clone. That failure is Part B of the
 * exercise: it is a real defect in the application code, not a broken test.
 * Fix the code, not the test.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AccountsApiTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** A minimal statement that satisfies every rule POST /statements checks. */
    private static final String VALID_STATEMENT = """
            {
              "accountId": "ACC-1001",
              "maskedAccountNumber": "****4821",
              "currency": "USD",
              "period": { "from": "2025-01-01", "to": "2025-01-31" },
              "openingBalance": "1520.00",
              "closingBalance": "1475.00",
              "totals": {
                "creditCount": 0, "debitCount": 1,
                "credits": "0.00", "debits": "45.00"
              },
              "transactions": [
                { "id": 17, "date": "2025-01-03", "description": "POS DEBIT COFFEE CO",
                  "amount": "-45.00", "runningBalance": "1475.00", "largeTransaction": false }
              ],
              "largeTransactionCount": 0
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void health_reports_up() throws Exception {
        JsonNode body = getJson("/health", status().isOk());
        assertEquals("UP", body.get("status").asText());
    }

    @Test
    void accounts_are_listed_without_balances() throws Exception {
        JsonNode accounts = getJson("/accounts", status().isOk());

        List<String> ids = new ArrayList<>();
        accounts.forEach(account -> ids.add(account.get("id").asText()));
        assertEquals(List.of("ACC-1001", "ACC-1002", "ACC-1003", "ACC-1004", "ACC-1005"), ids);

        assertEquals("483920174821", accounts.get(0).get("accountNumber").asText());
        assertEquals("USD", accounts.get(0).get("currency").asText());
        accounts.forEach(account -> {
            assertFalse(account.has("openingBalanceCents"), "list must not carry balances");
            assertFalse(account.has("currentBalanceCents"), "list must not carry balances");
        });
    }

    @Test
    void account_detail_includes_opening_and_current_balance() throws Exception {
        JsonNode account = getJson("/accounts/ACC-1001", status().isOk());

        assertEquals("ACC-1001", account.get("id").asText());
        assertEquals("MEM-1", account.get("memberId").asText());
        assertEquals("CHECKING", account.get("type").asText());
        assertEquals("OPEN", account.get("status").asText());
        // Opening balance is stored; the current balance is computed from every
        // transaction on the account, with no date filter.
        assertEquals(152000L, account.get("openingBalanceCents").asLong());
        assertEquals(1395700L, account.get("currentBalanceCents").asLong());
    }

    @Test
    void unknown_account_returns_404() throws Exception {
        JsonNode body = getJson("/accounts/ACC-9999", status().isNotFound());
        assertEquals("NOT_FOUND", body.get("error").asText());

        mockMvc.perform(get("/accounts/ACC-9999/transactions")
                        .param("from", "2025-01-01")
                        .param("to", "2025-01-31"))
                .andExpect(status().isNotFound());
    }

    @Test
    void transactions_require_from_and_to() throws Exception {
        mockMvc.perform(get("/accounts/ACC-1001/transactions"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/accounts/ACC-1001/transactions").param("from", "2025-01-01"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/accounts/ACC-1001/transactions").param("to", "2025-01-31"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/accounts/ACC-1001/transactions")
                        .param("from", "01/01/2025")
                        .param("to", "2025-01-31"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void transactions_reject_from_after_to() throws Exception {
        JsonNode body = getJson(
                "/accounts/ACC-1001/transactions?from=2025-01-31&to=2025-01-01",
                status().isBadRequest());
        assertEquals("BAD_REQUEST", body.get("error").asText());
    }

    /**
     * The date range is inclusive on BOTH ends.
     *
     * <p>docs/api-contract.yaml and the README both say so. ACC-1001 has six
     * transactions in January 2025, the last of them posted on the 31st.
     */
    @Test
    void transactions_include_last_day_of_range() throws Exception {
        String body = mockMvc.perform(get("/accounts/ACC-1001/transactions")
                        .param("from", "2025-01-01")
                        .param("to", "2025-01-31"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<Integer> returnedIds = new ArrayList<>();
        MAPPER.readTree(body).forEach(transaction -> returnedIds.add(transaction.get("id").asInt()));
        // Ordered by postedAt, then id.
        List<Integer> expectedIds = List.of(17, 37, 29, 26, 8, 3);
        List<Integer> missing =
                expectedIds.stream().filter(id -> !returnedIds.contains(id)).toList();

        assertEquals(expectedIds, returnedIds,
                () -> "ACC-1001 from 2025-01-01 to 2025-01-31: expected " + expectedIds.size()
                        + " transactions, got " + returnedIds.size() + "; missing id(s) " + missing
                        + ". Transaction id 3 is posted at 2025-01-31T14:30:00Z, "
                        + "CREDIT 1250000, 'ACH CREDIT PAYROLL ACME CORP'.");
    }

    @Test
    void statement_is_accepted_and_returns_an_id() throws Exception {
        String body = mockMvc.perform(post("/statements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_STATEMENT))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertTrue(MAPPER.readTree(body).get("statementId").asText().startsWith("STMT-"));
    }

    @Test
    void statement_with_bad_fields_returns_422() throws Exception {
        Map<String, Object> broken =
                MAPPER.readValue(VALID_STATEMENT, new TypeReference<>() {});
        broken.put("maskedAccountNumber", "4821");   // missing the four asterisks
        broken.put("openingBalance", 1520.0);        // a number, not a "0.00" string
        broken.remove("currency");
        @SuppressWarnings("unchecked")
        Map<String, Object> totals = (Map<String, Object>) broken.get("totals");
        totals.put("credits", "12512.5");            // one decimal place, not two

        String body = mockMvc.perform(post("/statements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MAPPER.writeValueAsString(broken)))
                .andExpect(status().isUnprocessableEntity())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode response = MAPPER.readTree(body);
        assertEquals("UNPROCESSABLE_ENTITY", response.get("error").asText());

        Set<String> fields = new HashSet<>();
        response.get("fieldErrors").forEach(error -> fields.add(error.get("field").asText()));
        assertEquals(
                Set.of("maskedAccountNumber", "openingBalance", "totals.credits", "currency"),
                fields);
    }

    private JsonNode getJson(String url, ResultMatcher expected) throws Exception {
        String body = mockMvc.perform(get(url))
                .andExpect(expected)
                .andReturn()
                .getResponse()
                .getContentAsString();
        return MAPPER.readTree(body);
    }
}
