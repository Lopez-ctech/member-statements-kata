package com.cu.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Part C: build a monthly member statement and deliver it.
 *
 * <p>The job has three steps:
 *
 * <ol>
 *   <li>fetch: call the API for the account and its transactions (written for you)
 *   <li>transform: {@link StatementBuilder#buildStatement} (you write this)
 *   <li>deliver: POST the document to {@code /statements} (written for you)
 * </ol>
 *
 * <p>Run the whole job, with the API already running:
 *
 * <pre>
 *   ./mvnw -q compile exec:java -Dexec.args="ACC-1001 2025-01-01 2025-01-31"
 * </pre>
 */
public final class StatementRunner {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    private StatementRunner() {
    }

    /** A statement and the id the statement service gave it. */
    public record Delivered(Map<String, Object> statement, String statementId) {
    }

    public static String defaultBaseUrl() {
        String configured = System.getenv("API_BASE_URL");
        if (configured != null && !configured.isBlank()) {
            return configured;
        }
        String port = System.getenv("PORT");
        return "http://localhost:" + (port == null || port.isBlank() ? "8080" : port);
    }

    /** Step 1: fetch. */
    public static Map<String, Object> fetchAccount(String baseUrl, String accountId)
            throws Exception {
        return MAPPER.readValue(get(baseUrl + "/accounts/" + accountId), new TypeReference<>() {});
    }

    /** Step 1: fetch. */
    public static List<Map<String, Object>> fetchTransactions(
            String baseUrl, String accountId, Map<String, String> period) throws Exception {
        String url = baseUrl + "/accounts/" + accountId + "/transactions"
                + "?from=" + period.get("from") + "&to=" + period.get("to");
        return MAPPER.readValue(get(url), new TypeReference<>() {});
    }

    /** Step 3: deliver. Returns the id the service assigned to the statement. */
    public static String deliver(String baseUrl, Map<String, Object> statement) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/statements"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(statement)))
                .build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 201) {
            throw new IllegalStateException(
                    "POST /statements returned " + response.statusCode() + ": " + response.body());
        }
        return MAPPER.readTree(response.body()).get("statementId").asText();
    }

    /** Fetch, transform, deliver. */
    public static Delivered runStatement(
            String accountId, String periodFrom, String periodTo, String baseUrl) throws Exception {
        Map<String, String> period = Map.of("from", periodFrom, "to", periodTo);
        Map<String, Object> account = fetchAccount(baseUrl, accountId);
        List<Map<String, Object>> transactions = fetchTransactions(baseUrl, accountId, period);
        Map<String, Object> statement = StatementBuilder.buildStatement(account, transactions, period);
        return new Delivered(statement, deliver(baseUrl, statement));
    }

    private static String get(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
        return HTTP.send(request, HttpResponse.BodyHandlers.ofString()).body();
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.err.println("usage: StatementRunner <accountId> <from> <to>"
                    + " [--base-url URL] [--out FILE] [--expect FILE]");
            System.err.println("   e.g. ACC-1001 2025-01-01 2025-01-31");
            System.exit(2);
        }
        String baseUrl = defaultBaseUrl();
        String out = null;
        String expect = null;
        for (int index = 3; index + 1 < args.length; index += 2) {
            switch (args[index]) {
                case "--base-url" -> baseUrl = args[index + 1];
                case "--out" -> out = args[index + 1];
                case "--expect" -> expect = args[index + 1];
                default -> System.err.println("ignoring unknown option " + args[index]);
            }
        }

        Delivered delivered = runStatement(args[0], args[1], args[2], baseUrl);
        JsonNode produced = MAPPER.readTree(MAPPER.writeValueAsString(delivered.statement()));

        if (out != null) {
            Files.writeString(
                    Path.of(out),
                    MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(produced)
                            + System.lineSeparator(),
                    StandardCharsets.UTF_8);
        }
        System.out.println("statementId: " + delivered.statementId());

        if (expect != null) {
            JsonNode expected = MAPPER.readTree(Files.readString(Path.of(expect)));
            if (!expected.equals(produced)) {
                System.err.println("MISMATCH: the statement does not match " + expect);
                reportDifferences(expected, produced);
                System.exit(1);
            }
            System.out.println("match: the statement equals " + expect);
        }
    }

    private static void reportDifferences(JsonNode expected, JsonNode actual) {
        Set<String> keys = new TreeSet<>();
        expected.fieldNames().forEachRemaining(keys::add);
        actual.fieldNames().forEachRemaining(keys::add);
        for (String key : keys) {
            JsonNode expectedValue = expected.get(key);
            JsonNode actualValue = actual.get(key);
            if (!java.util.Objects.equals(expectedValue, actualValue)) {
                System.err.println("  " + key + ":");
                System.err.println("    expected " + shorten(expectedValue));
                System.err.println("    got      " + shorten(actualValue));
            }
        }
    }

    private static String shorten(JsonNode value) {
        String text = String.valueOf(value);
        return text.length() <= 300 ? text : text.substring(0, 300) + " ...";
    }
}
