/*
 * Copyright 2025 Contrast Security
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.contrast.labs.ai.mcp.contrast.tool.attack;

import static org.assertj.core.api.Assertions.assertThat;

import com.contrast.labs.ai.mcp.contrast.config.IntegrationTestConfig;
import com.contrast.labs.ai.mcp.contrast.result.AttackSummary;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Integration tests for SearchAttacksTool.
 *
 * <p>These tests require Contrast credentials to be set in environment variables. Run: source
 * .env.integration-test && mvn verify
 */
@Slf4j
@SpringBootTest
@Import(IntegrationTestConfig.class)
@EnabledIfEnvironmentVariable(named = "CONTRAST_HOST_NAME", matches = ".+")
class SearchAttacksToolIT {

  @Autowired private SearchAttacksTool searchAttacksTool;

  @Test
  void searchAttacks_should_return_response_with_no_filters() {
    var response =
        searchAttacksTool.searchAttacks(1, 10, null, null, null, null, null, null, null, null);

    assertThat(response).as("Response should not be null").isNotNull();
    assertThat(response.items()).as("Items should not be null").isNotNull();
    assertThat(response.page()).as("Page should be 1").isEqualTo(1);
    assertThat(response.pageSize()).as("Page size should be 10").isEqualTo(10);

    if (!response.items().isEmpty()) {
      var firstAttack = response.items().get(0);
      assertThat(firstAttack.attackId()).as("Attack ID should not be null").isNotNull();
      assertThat(firstAttack.status()).as("Status should not be null").isNotNull();
    }
  }

  @Test
  void searchAttacks_should_filter_by_quickFilter() {
    var response =
        searchAttacksTool.searchAttacks(
            1, 50, "EFFECTIVE", null, null, null, null, null, null, null);

    assertThat(response).as("Response should not be null").isNotNull();
    assertThat(response.errors()).as("Should have no errors").isEmpty();
    assertThat(response.items())
        .as("requires seeded EFFECTIVE attacks in test org — see INTEGRATION_TESTS.md")
        .isNotEmpty();

    // EFFECTIVE excludes probed attacks per SearchAttacksTool description (line 89).
    assertThat(response.items())
        .as("EFFECTIVE filter must exclude PROBED attacks")
        .allSatisfy(
            attack ->
                assertThat(attack.status())
                    .as("attack %s status", attack.attackId())
                    .isNotEqualTo("PROBED"));
  }

  @Test
  void searchAttacks_should_filter_by_statusFilter() {
    var response =
        searchAttacksTool.searchAttacks(
            1, 50, null, "EXPLOITED", null, null, null, null, null, null);

    assertThat(response).as("Response should not be null").isNotNull();
    assertThat(response.errors()).as("Should have no errors").isEmpty();
    assertThat(response.items())
        .as("requires seeded EXPLOITED attacks in test org — see INTEGRATION_TESTS.md")
        .isNotEmpty();
    assertThat(response.items())
        .as("statusFilter=EXPLOITED must return only EXPLOITED attacks")
        .allSatisfy(
            attack ->
                assertThat(attack.status())
                    .as("attack %s status", attack.attackId())
                    .isEqualTo("EXPLOITED"));
  }

  @Test
  void searchAttacks_should_filter_by_keyword() {
    var response =
        searchAttacksTool.searchAttacks(1, 50, null, null, "sql", null, null, null, null, null);

    assertThat(response).as("Response should not be null").isNotNull();
    assertThat(response.errors()).as("Should have no errors").isEmpty();
    assertThat(response.items())
        .as("requires seeded attacks matching keyword 'sql' — see INTEGRATION_TESTS.md")
        .isNotEmpty();

    // Keyword performs substring match across source IP, server name, application name, rule name,
    // attack UUID, forwarded IP/path, and attack tags (OR logic). Of those, only source IP,
    // application name, rule name, and attack UUID are exposed on AttackSummary. A match on a
    // non-exposed field (server name, forwarded path, tags) would fail this assertion — the
    // production-seeded SQL-injection data is expected to include at least one visible-field match
    // (typically rule names like "sql-injection").
    assertThat(response.items())
        .as("keyword 'sql' must appear in at least one visible searchable field per result")
        .allSatisfy(
            attack ->
                assertThat(visibleSearchableStrings(attack))
                    .as("attack %s searchable fields", attack.attackId())
                    .anyMatch(s -> s.toLowerCase().contains("sql")));
  }

  @Test
  void searchAttacks_should_handle_pagination() {
    var page1 =
        searchAttacksTool.searchAttacks(1, 5, null, null, null, null, null, null, null, null);

    assertThat(page1).as("Page 1 response should not be null").isNotNull();
    assertThat(page1.page()).as("Should be page 1").isEqualTo(1);
    assertThat(page1.pageSize()).as("Page size should be 5").isEqualTo(5);

    if (page1.hasMorePages()) {
      var page2 =
          searchAttacksTool.searchAttacks(2, 5, null, null, null, null, null, null, null, null);

      assertThat(page2).as("Page 2 response should not be null").isNotNull();
      assertThat(page2.page()).as("Should be page 2").isEqualTo(2);

      var page1Ids = page1.items().stream().map(AttackSummary::attackId).toList();
      var page2Ids = page2.items().stream().map(AttackSummary::attackId).toList();
      assertThat(page2Ids)
          .as("Page 2 items should be disjoint from page 1 items")
          .doesNotContainAnyElementsOf(page1Ids);
    }
  }

  @Test
  void searchAttacks_should_handle_sort() {
    var response =
        searchAttacksTool.searchAttacks(
            1, 10, null, null, null, null, null, null, "-startTime", null);

    assertThat(response).as("Response should not be null").isNotNull();
    assertThat(response.errors()).as("Should have no errors").isEmpty();
    assertThat(response.items())
        .as("requires seeded attacks to verify sort order — see INTEGRATION_TESTS.md")
        .isNotEmpty();
    assertThat(response.items())
        .as("sort=-startTime must return items ordered by startTimeMs descending")
        .isSortedAccordingTo(Comparator.comparingLong(AttackSummary::startTimeMs).reversed());
  }

  @Test
  void searchAttacks_should_return_error_for_invalid_quickFilter() {
    var response =
        searchAttacksTool.searchAttacks(1, 10, "INVALID", null, null, null, null, null, null, null);

    assertThat(response).as("Response should not be null").isNotNull();
    assertThat(response.errors()).as("Should have errors").isNotEmpty();
    assertThat(response.errors())
        .as("Should explain invalid filter")
        .anyMatch(e -> e.contains("Invalid quickFilter"));
  }

  @Test
  void searchAttacks_should_return_error_for_invalid_statusFilter() {
    var response =
        searchAttacksTool.searchAttacks(1, 10, null, "INVALID", null, null, null, null, null, null);

    assertThat(response).as("Response should not be null").isNotNull();
    assertThat(response.errors()).as("Should have errors").isNotEmpty();
    assertThat(response.errors())
        .as("Should explain invalid filter")
        .anyMatch(e -> e.contains("Invalid statusFilter"));
  }

  @Test
  void searchAttacks_should_handle_boolean_filters() {
    // Compare with and without suppressed attacks to verify includeSuppressed takes effect.
    // AttackSummary does not expose the `suppressed` field, so we can only assert the count
    // relationship: including suppressed must return at least as many results as excluding them.
    var withSuppressed =
        searchAttacksTool.searchAttacks(1, 100, null, null, null, true, null, null, null, null);
    var withoutSuppressed =
        searchAttacksTool.searchAttacks(1, 100, null, null, null, false, null, null, null, null);

    assertThat(withSuppressed.errors())
        .as("includeSuppressed=true should have no errors")
        .isEmpty();
    assertThat(withoutSuppressed.errors())
        .as("includeSuppressed=false should have no errors")
        .isEmpty();

    assertThat(totalOrZero(withSuppressed.totalItems()))
        .as("includeSuppressed=true must return >= total items than includeSuppressed=false")
        .isGreaterThanOrEqualTo(totalOrZero(withoutSuppressed.totalItems()));
  }

  @Test
  void searchAttacks_should_handle_combined_filters() {
    var response =
        searchAttacksTool.searchAttacks(
            1, 50, "EFFECTIVE", "EXPLOITED", null, false, null, null, null, null);

    assertThat(response).as("Response should not be null").isNotNull();
    assertThat(response.errors()).as("Should have no errors").isEmpty();
    assertThat(response.items())
        .as("requires seeded EFFECTIVE+EXPLOITED attacks — see INTEGRATION_TESTS.md")
        .isNotEmpty();

    // Intersection of EFFECTIVE (excludes PROBED) and statusFilter=EXPLOITED must hold per result.
    // includeSuppressed=false cannot be verified from AttackSummary (field not exposed).
    assertThat(response.items())
        .as("every result must satisfy both quickFilter=EFFECTIVE and statusFilter=EXPLOITED")
        .allSatisfy(
            attack -> {
              assertThat(attack.status())
                  .as("attack %s status must be EXPLOITED (statusFilter)", attack.attackId())
                  .isEqualTo("EXPLOITED");
              assertThat(attack.status())
                  .as("attack %s status must not be PROBED (EFFECTIVE)", attack.attackId())
                  .isNotEqualTo("PROBED");
            });
  }

  // ========== Sort Field Validation Tests (Bug Fix: AIML-345) ==========

  @Test
  void searchAttacks_should_return_validation_error_for_invalid_sort_field() {
    // This test verifies the bug fix: invalid sort fields should return a helpful
    // validation error listing valid options, NOT a generic "Contrast API error"
    var response =
        searchAttacksTool.searchAttacks(
            1, 10, null, null, null, null, null, null, "severity", null);

    assertThat(response).as("Response should not be null").isNotNull();
    assertThat(response.errors()).as("Should have validation errors").isNotEmpty();
    assertThat(response.errors())
        .as("Should contain 'Invalid sort field' not generic API error")
        .anyMatch(e -> e.contains("Invalid sort field"));
    assertThat(response.errors())
        .as("Should list valid sort fields")
        .anyMatch(e -> e.contains("Valid fields:"));
    assertThat(response.errors())
        .as("Error message should NOT be generic API error")
        .noneMatch(e -> e.contains("Contrast API error"));
  }

  @ParameterizedTest(name = "valid sort field: {0}")
  @ValueSource(strings = {"sourceIP", "status", "startTime", "endTime", "type"})
  void searchAttacks_should_accept_all_valid_sort_fields(String sortField) {
    var response =
        searchAttacksTool.searchAttacks(1, 10, null, null, null, null, null, null, sortField, null);

    assertThat(response).as("Response should not be null").isNotNull();
    assertThat(response.errors()).as("Should have no errors for sort: " + sortField).isEmpty();
    assertThat(response.items()).as("Items should not be null").isNotNull();
  }

  @ParameterizedTest(name = "valid sort field descending: -{0}")
  @ValueSource(strings = {"sourceIP", "status", "startTime", "endTime", "type"})
  void searchAttacks_should_accept_all_valid_sort_fields_with_descending_prefix(String sortField) {
    String descending = "-" + sortField;
    var response =
        searchAttacksTool.searchAttacks(
            1, 10, null, null, null, null, null, null, descending, null);

    assertThat(response).as("Response should not be null").isNotNull();
    assertThat(response.errors()).as("Should have no errors for sort: " + descending).isEmpty();
    assertThat(response.items()).as("Items should not be null").isNotNull();
  }

  @ParameterizedTest(name = "invalid sort field: {0}")
  @ValueSource(strings = {"severity", "probes", "NEWEST", "OLDEST", "invalidField"})
  void searchAttacks_should_reject_invalid_sort_fields(String sortField) {
    var response =
        searchAttacksTool.searchAttacks(1, 10, null, null, null, null, null, null, sortField, null);

    assertThat(response).as("Response should not be null").isNotNull();
    assertThat(response.errors())
        .as("Should have validation errors for: " + sortField)
        .isNotEmpty();
    assertThat(response.errors())
        .as("Should be validation error not API error")
        .anyMatch(e -> e.contains("Invalid sort field"));
  }

  // ========== Keyword and Rules Parameter Tests (Bug Fix: AIML-385) ==========

  @Test
  void searchAttacks_should_accept_keyword_with_spaces_without_error() {
    // Narrow contract: multi-word keywords must be URL-encoded safely and accepted by the API.
    // We do NOT assert results are non-empty because the seeded test org may not contain attacks
    // whose visible fields match "SQL Injection" as a literal substring — keyword also matches
    // rule display names which aren't exposed on AttackSummary.
    var response =
        searchAttacksTool.searchAttacks(
            1, 10, null, null, "SQL Injection", null, null, null, null, null);

    assertThat(response).as("Response should not be null").isNotNull();
    assertThat(response.errors())
        .as("keyword with spaces must be URL-encoded and accepted without error")
        .isEmpty();
    assertThat(response.items()).as("Items should not be null").isNotNull();
  }

  @Test
  void searchAttacks_should_find_results_with_rules_filter() {
    var response =
        searchAttacksTool.searchAttacks(
            1, 10, null, null, null, null, null, null, null, "sql-injection");

    assertThat(response).as("Response should not be null").isNotNull();
    assertThat(response.errors()).as("Should have no errors").isEmpty();

    // If there are results, verify they have the expected rule
    if (!response.items().isEmpty()) {
      assertThat(response.items())
          .allSatisfy(
              attack ->
                  assertThat(attack.rules()).anyMatch(rule -> rule.toLowerCase().contains("sql")));
    }
  }

  @Test
  void searchAttacks_should_find_results_with_multiple_rules() {
    var response =
        searchAttacksTool.searchAttacks(
            1, 50, null, null, null, null, null, null, null, "sql-injection,xss-reflected");

    assertThat(response).as("Response should not be null").isNotNull();
    assertThat(response.errors()).as("Should have no errors").isEmpty();
    assertThat(response.items())
        .as("requires seeded attacks matching sql-injection or xss-reflected rules")
        .isNotEmpty();

    // Every attack must have at least one rule matching one of the two requested rule families.
    assertThat(response.items())
        .as("every result must match at least one of the requested rules (sql or xss)")
        .allSatisfy(
            attack ->
                assertThat(attack.rules())
                    .as("attack %s rules", attack.attackId())
                    .anyMatch(
                        rule -> {
                          var lower = rule.toLowerCase();
                          return lower.contains("sql") || lower.contains("xss");
                        }));
  }

  // ========== Special Character Keyword Tests (Test Cases 3.5 and 13.4) ==========
  // Note: Full XSS payloads like "<script>alert('xss')</script>" are blocked by Contrast Protect
  // on the API server, so we test with patterns that demonstrate URL encoding works without
  // triggering attack detection.

  @ParameterizedTest(name = "keyword with special chars: {0}")
  @ValueSource(
      strings = {
        "'; DROP TABLE users;--", // SQL injection
        "../../../etc/passwd", // Path traversal
        "<>", // Angle brackets (HTML/XML)
        "attacker@malicious.com", // Email address
        "/admin/login?user=admin", // URL path with query string
        "%00null-byte" // Null byte pattern (tests double-encoding of %)
      })
  void searchAttacks_should_handle_specialCharacterKeywords(String keyword) {
    // Narrow contract: keyword is URL-encoded safely without triggering API-side attack
    // detection. Result content is irrelevant — seeded test data is not expected to match these
    // adversarial keywords. We only assert the request round-trips without error.
    var response =
        searchAttacksTool.searchAttacks(1, 10, null, null, keyword, null, null, null, null, null);

    assertThat(response).as("Response should not be null").isNotNull();
    assertThat(response.errors()).as("Should have no errors for keyword: " + keyword).isEmpty();
    assertThat(response.items()).as("Items should not be null").isNotNull();
  }

  /**
   * Collects all string fields on an {@link AttackSummary} that participate in keyword substring
   * matching and are exposed on the summary. Server name, forwarded IP/path, and attack tags are
   * also keyword-searchable on the server side but are not exposed on AttackSummary.
   */
  private static List<String> visibleSearchableStrings(AttackSummary attack) {
    var appStrings =
        Optional.ofNullable(attack.applications()).orElse(List.of()).stream()
            .flatMap(
                app ->
                    Stream.of(app.applicationName(), app.applicationId())
                        .filter(s -> s != null && !s.isBlank()));
    var ruleStrings =
        Optional.ofNullable(attack.rules()).orElse(List.of()).stream()
            .filter(s -> s != null && !s.isBlank());
    var topLevel =
        Stream.of(attack.source(), attack.attackId()).filter(s -> s != null && !s.isBlank());
    return Stream.concat(Stream.concat(topLevel, ruleStrings), appStrings).toList();
  }

  private static int totalOrZero(Integer totalItems) {
    return totalItems != null ? totalItems : 0;
  }
}
