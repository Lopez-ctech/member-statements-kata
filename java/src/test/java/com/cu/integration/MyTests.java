package com.cu.integration;

/**
 * Your tests go here. Add at least two.
 *
 * <p>The golden test in {@code StatementBuilderTest} proves one account works for
 * one period. It does not prove much else. Pick cases you think are worth
 * pinning down and say why you chose them in SUBMISSION.md.
 *
 * <p>Some candidates worth considering (you do not have to use these):
 *
 * <ul>
 *   <li>an account with no transactions in the period
 *   <li>an amount of exactly 10,000.00 at the largeTransaction boundary
 *   <li>an account number shorter than four digits
 *   <li>two transactions with the same postedAt
 * </ul>
 *
 * <p>You can build inputs by hand; nothing here needs the API to be running.
 * A test looks like this:
 *
 * <pre>
 * &#64;Test
 * void empty_period_still_produces_a_statement() {
 *     Map&lt;String, Object&gt; account = Map.of(
 *             "id", "ACC-9001", "accountNumber", "999888777666", "currency", "USD",
 *             "openingBalanceCents", 50000, "currentBalanceCents", 50000);
 *
 *     Map&lt;String, Object&gt; statement = StatementBuilder.buildStatement(
 *             account, List.of(), Map.of("from", "2025-03-01", "to", "2025-03-31"));
 *
 *     assertEquals("500.00", statement.get("closingBalance"));
 *     assertEquals(List.of(), statement.get("transactions"));
 * }
 * </pre>
 */
class MyTests {
}
