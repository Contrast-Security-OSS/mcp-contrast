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
package com.contrast.labs.ai.mcp.contrast.tool.adr;

import static org.assertj.core.api.Assertions.assertThat;

import com.contrast.labs.ai.mcp.contrast.config.IntegrationTestConfig;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
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
    log.info("\n=== Integration Test: search_attacks (no filters) ===");

    var response = searchAttacksTool.searchAttacks(null, null, null, null, null, null, null, 1, 10);

    assertThat(response).as("Response should not be null").isNotNull();
    assertThat(response.items()).as("Items should not be null").isNotNull();
    assertThat(response.page()).as("Page should be 1").isEqualTo(1);
    assertThat(response.pageSize()).as("Page size should be 10").isEqualTo(10);

    log.info("✓ Retrieved {} attacks", response.items().size());
    log.info("  Total items: {}", response.totalItems());
    log.info("  Has more pages: {}", response.hasMorePages());

    if (!response.items().isEmpty()) {
      var firstAttack = response.items().get(0);
      log.info("  Sample attack:");
      log.info("    Attack ID: {}", firstAttack.attackId());
      log.info("    Status: {}", firstAttack.status());
      log.info("    Source: {}", firstAttack.source());
      log.info("    Rules: {}", firstAttack.rules());

      assertThat(firstAttack.attackId()).as("Attack ID should not be null").isNotNull();
      assertThat(firstAttack.status()).as("Status should not be null").isNotNull();
    }
  }

  @Test
  void searchAttacks_should_filter_by_quickFilter() {
    log.info("\n=== Integration Test: search_attacks (quickFilter=EFFECTIVE) ===");

    var response =
        searchAttacksTool.searchAttacks("EFFECTIVE", null, null, null, null, null, null, 1, 10);

    assertThat(response).as("Response should not be null").isNotNull();
    assertThat(response.items()).as("Items should not be null").isNotNull();

    log.info("✓ Retrieved {} EFFECTIVE attacks", response.items().size());
  }

  @Test
  void searchAttacks_should_filter_by_statusFilter() {
    log.info("\n=== Integration Test: search_attacks (statusFilter=EXPLOITED) ===");

    var response =
        searchAttacksTool.searchAttacks(null, "EXPLOITED", null, null, null, null, null, 1, 10);

    assertThat(response).as("Response should not be null").isNotNull();
    assertThat(response.items()).as("Items should not be null").isNotNull();

    log.info("✓ Retrieved {} EXPLOITED attacks", response.items().size());
  }

  @Test
  void searchAttacks_should_filter_by_keyword() {
    log.info("\n=== Integration Test: search_attacks (keyword=sql) ===");

    var response =
        searchAttacksTool.searchAttacks(null, null, "sql", null, null, null, null, 1, 10);

    assertThat(response).as("Response should not be null").isNotNull();
    assertThat(response.items()).as("Items should not be null").isNotNull();

    log.info("✓ Retrieved {} attacks matching keyword 'sql'", response.items().size());
  }

  @Test
  void searchAttacks_should_handle_pagination() {
    log.info("\n=== Integration Test: search_attacks (pagination) ===");

    var page1 = searchAttacksTool.searchAttacks(null, null, null, null, null, null, null, 1, 5);

    assertThat(page1).as("Page 1 response should not be null").isNotNull();
    assertThat(page1.page()).as("Should be page 1").isEqualTo(1);
    assertThat(page1.pageSize()).as("Page size should be 5").isEqualTo(5);

    log.info("✓ Page 1: {} attacks", page1.items().size());
    log.info("  Total items: {}", page1.totalItems());
    log.info("  Has more pages: {}", page1.hasMorePages());

    if (page1.hasMorePages()) {
      var page2 = searchAttacksTool.searchAttacks(null, null, null, null, null, null, null, 2, 5);

      assertThat(page2).as("Page 2 response should not be null").isNotNull();
      assertThat(page2.page()).as("Should be page 2").isEqualTo(2);

      log.info("✓ Page 2: {} attacks", page2.items().size());

      if (!page1.items().isEmpty() && !page2.items().isEmpty()) {
        assertThat(page1.items().get(0).attackId())
            .as("Page 1 and Page 2 should have different attacks")
            .isNotEqualTo(page2.items().get(0).attackId());
      }
    }
  }

  @Test
  void searchAttacks_should_handle_sort() {
    log.info("\n=== Integration Test: search_attacks (sort=-startTime) ===");

    var response =
        searchAttacksTool.searchAttacks(null, null, null, null, null, null, "-startTime", 1, 10);

    assertThat(response).as("Response should not be null").isNotNull();
    assertThat(response.items()).as("Items should not be null").isNotNull();

    log.info("✓ Retrieved {} attacks sorted by -startTime", response.items().size());
  }

  @Test
  void searchAttacks_should_return_error_for_invalid_quickFilter() {
    log.info("\n=== Integration Test: search_attacks (invalid quickFilter) ===");

    var response =
        searchAttacksTool.searchAttacks("INVALID", null, null, null, null, null, null, 1, 10);

    assertThat(response).as("Response should not be null").isNotNull();
    assertThat(response.message()).as("Should have error message").isNotNull();
    assertThat(response.message())
        .as("Should explain invalid filter")
        .contains("Invalid quickFilter");

    log.info("✓ Invalid filter correctly rejected");
    log.info("  Error message: {}", response.message());
  }

  @Test
  void searchAttacks_should_return_error_for_invalid_statusFilter() {
    log.info("\n=== Integration Test: search_attacks (invalid statusFilter) ===");

    var response =
        searchAttacksTool.searchAttacks(null, "INVALID", null, null, null, null, null, 1, 10);

    assertThat(response).as("Response should not be null").isNotNull();
    assertThat(response.message()).as("Should have error message").isNotNull();
    assertThat(response.message())
        .as("Should explain invalid filter")
        .contains("Invalid statusFilter");

    log.info("✓ Invalid status filter correctly rejected");
    log.info("  Error message: {}", response.message());
  }

  @Test
  void searchAttacks_should_handle_boolean_filters() {
    log.info("\n=== Integration Test: search_attacks (boolean filters) ===");

    var response = searchAttacksTool.searchAttacks(null, null, null, true, null, null, null, 1, 10);

    assertThat(response).as("Response should not be null").isNotNull();
    assertThat(response.items()).as("Items should not be null").isNotNull();

    log.info("✓ Retrieved {} attacks with includeSuppressed=true", response.items().size());
  }

  @Test
  void searchAttacks_should_handle_combined_filters() {
    log.info("\n=== Integration Test: search_attacks (combined filters) ===");

    var response =
        searchAttacksTool.searchAttacks(
            "EFFECTIVE", "EXPLOITED", null, false, null, null, null, 1, 10);

    assertThat(response).as("Response should not be null").isNotNull();
    assertThat(response.items()).as("Items should not be null").isNotNull();

    log.info("✓ Retrieved {} attacks with combined filters", response.items().size());
    log.info("  Filters: quickFilter=EFFECTIVE, statusFilter=EXPLOITED");
  }
}
