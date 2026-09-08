package com.cu.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * The golden test for Part C.
 *
 * <p>It reads fixtures, calls {@code buildStatement}, and compares the result
 * with a statement document that is known to be correct. It never touches the
 * API, so it runs with the server switched off.
 *
 * <p>This test fails on a fresh clone because {@code buildStatement} is not
 * written yet. Do not change this test.
 */
class StatementBuilderTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void build_statement_matches_the_golden_statement() throws Exception {
        JsonNode given = Fixtures.load("acc-1001-input.json");
        JsonNode expected = Fixtures.load("acc-1001-statement.json");

        Map<String, Object> account =
                MAPPER.convertValue(given.get("account"), new TypeReference<>() {});
        List<Map<String, Object>> transactions =
                MAPPER.convertValue(given.get("transactions"), new TypeReference<>() {});
        Map<String, String> period =
                MAPPER.convertValue(given.get("period"), new TypeReference<>() {});

        Map<String, Object> actual = StatementBuilder.buildStatement(account, transactions, period);

        // Round-tripped through text so that, say, a Long 1 and an int 1 compare equal.
        assertEquals(expected, MAPPER.readTree(MAPPER.writeValueAsString(actual)));
    }
}
