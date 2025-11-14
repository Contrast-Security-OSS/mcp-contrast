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
package com.contrast.labs.ai.mcp.contrast;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class AttackFilterParamsTest {

  @Test
  void testValidFiltersAllProvided() {
    var params = AttackFilterParams.of("EXPLOITED", "xss", true, true, false, "severity");

    assertTrue(params.isValid());
    assertTrue(params.errors().isEmpty());

    var filterBody = params.toAttacksFilterBody();
    assertEquals("EXPLOITED", filterBody.getQuickFilter());
    assertEquals("xss", filterBody.getKeyword());
    assertTrue(filterBody.isIncludeSuppressed());
    assertTrue(filterBody.isIncludeBotBlockers());
    assertFalse(filterBody.isIncludeIpBlacklist());
  }

  @Test
  void testNoFiltersProvided() {
    var params = AttackFilterParams.of(null, null, null, null, null, null);

    assertTrue(params.isValid());
    assertFalse(params.messages().isEmpty()); // Should have smart defaults messages
    assertTrue(params.errors().isEmpty());

    // Smart defaults should be applied
    var filterBody = params.toAttacksFilterBody();
    assertEquals("ALL", filterBody.getQuickFilter());
    assertFalse(filterBody.isIncludeSuppressed()); // Smart default: exclude suppressed
  }

  @Test
  void testSmartDefaultForIncludeSuppressed() {
    var params = AttackFilterParams.of(null, null, null, null, null, null);

    assertTrue(params.isValid());
    assertTrue(params.errors().isEmpty());

    // Should have two messages: quickFilter default and includeSuppressed default
    assertEquals(2, params.messages().size());
    assertTrue(params.messages().stream().anyMatch(m -> m.contains("No quickFilter applied")));
    assertTrue(
        params.messages().stream()
            .anyMatch(m -> m.contains("Excluding suppressed attacks by default")));

    var filterBody = params.toAttacksFilterBody();
    assertFalse(filterBody.isIncludeSuppressed());
  }

  @Test
  void testExplicitIncludeSuppressedNoMessage() {
    var params = AttackFilterParams.of("EXPLOITED", null, true, null, null, null);

    assertTrue(params.isValid());
    assertTrue(params.errors().isEmpty());
    // Should not have includeSuppressed message when explicitly provided
    assertFalse(params.messages().stream().anyMatch(m -> m.contains("Excluding suppressed")));

    var filterBody = params.toAttacksFilterBody();
    assertTrue(filterBody.isIncludeSuppressed());
  }

  @Test
  void testInvalidQuickFilterHardFailure() {
    var params = AttackFilterParams.of("INVALID", null, null, null, null, null);

    assertFalse(params.isValid());
    assertEquals(1, params.errors().size());
    assertTrue(params.errors().get(0).contains("Invalid quickFilter 'INVALID'"));
    assertTrue(
        params.errors().get(0).contains("Valid: EXPLOITED, PROBED, BLOCKED, INEFFECTIVE, ALL"));
  }

  @Test
  void testValidQuickFilterValues() {
    String[] validFilters = {"EXPLOITED", "PROBED", "BLOCKED", "INEFFECTIVE", "ALL"};

    for (String filter : validFilters) {
      var params = AttackFilterParams.of(filter, null, false, null, null, null);
      assertTrue(params.isValid(), "Filter " + filter + " should be valid");
      assertTrue(params.errors().isEmpty());
    }
  }

  @Test
  void testQuickFilterCaseInsensitive() {
    // Test lowercase and mixed case
    var params1 = AttackFilterParams.of("exploited", null, false, null, null, null);
    assertTrue(params1.isValid());
    assertEquals("EXPLOITED", params1.toAttacksFilterBody().getQuickFilter());

    var params2 = AttackFilterParams.of("PrObEd", null, false, null, null, null);
    assertTrue(params2.isValid());
    assertEquals("PROBED", params2.toAttacksFilterBody().getQuickFilter());
  }

  @Test
  void testQuickFilterWithWhitespace() {
    var params = AttackFilterParams.of("  EXPLOITED  ", null, false, null, null, null);

    assertTrue(params.isValid());
    assertEquals("EXPLOITED", params.toAttacksFilterBody().getQuickFilter());
  }

  @Test
  void testKeywordPassThrough() {
    var params = AttackFilterParams.of("EXPLOITED", "sql injection test", false, null, null, null);

    assertTrue(params.isValid());
    assertEquals("sql injection test", params.toAttacksFilterBody().getKeyword());
  }

  @Test
  void testValidSortFormat() {
    var params = AttackFilterParams.of("EXPLOITED", null, false, null, null, "severity");

    assertTrue(params.isValid());
    assertTrue(params.errors().isEmpty());
  }

  @Test
  void testValidDescendingSortFormat() {
    var params = AttackFilterParams.of("EXPLOITED", null, false, null, null, "-severity");

    assertTrue(params.isValid());
    assertTrue(params.errors().isEmpty());
  }

  @Test
  void testInvalidSortFormatHardFailure() {
    var params = AttackFilterParams.of("EXPLOITED", null, false, null, null, "invalid sort!");

    assertFalse(params.isValid());
    assertEquals(1, params.errors().size());
    assertTrue(params.errors().get(0).contains("Invalid sort format 'invalid sort!'"));
    assertTrue(params.errors().get(0).contains("Must be a field name with optional '-' prefix"));
  }

  @Test
  void testValidSortWithUnderscores() {
    var params = AttackFilterParams.of("EXPLOITED", null, false, null, null, "field_name");

    assertTrue(params.isValid());
    assertTrue(params.errors().isEmpty());
  }

  @Test
  void testAllBooleanFlagsExplicitlySet() {
    var params = AttackFilterParams.of("BLOCKED", "keyword", true, true, true, null);

    assertTrue(params.isValid());

    var filterBody = params.toAttacksFilterBody();
    assertTrue(filterBody.isIncludeSuppressed());
    assertTrue(filterBody.isIncludeBotBlockers());
    assertTrue(filterBody.isIncludeIpBlacklist());
  }

  @Test
  void testMultipleErrorsAccumulate() {
    var params =
        AttackFilterParams.of("INVALID_FILTER", null, null, null, null, "bad-sort-format!");

    assertFalse(params.isValid());
    assertEquals(2, params.errors().size()); // quickFilter and sort errors
  }

  @Test
  void testMessagesAreImmutable() {
    var params = AttackFilterParams.of(null, null, null, null, null, null);

    assertThrows(
        UnsupportedOperationException.class,
        () -> {
          params.messages().add("Should fail");
        });
  }

  @Test
  void testErrorsAreImmutable() {
    var params = AttackFilterParams.of("INVALID", null, null, null, null, null);

    assertThrows(
        UnsupportedOperationException.class,
        () -> {
          params.errors().add("Should fail");
        });
  }

  @Test
  void testQuickFilterDefaultMessage() {
    var params = AttackFilterParams.of(null, null, false, null, null, null);

    assertTrue(params.isValid());
    assertTrue(
        params.messages().stream()
            .anyMatch(m -> m.contains("No quickFilter applied - showing all attack types")));
  }

  @Test
  void testNoQuickFilterMessageWhenProvided() {
    var params = AttackFilterParams.of("EXPLOITED", null, false, null, null, null);

    assertTrue(params.isValid());
    assertFalse(params.messages().stream().anyMatch(m -> m.contains("No quickFilter applied")));
  }

  @Test
  void testEmptyStringQuickFilterTreatedAsNull() {
    var params = AttackFilterParams.of("   ", null, false, null, null, null);

    assertTrue(params.isValid());
    // Empty/whitespace should be treated as null and use default
    assertEquals("ALL", params.toAttacksFilterBody().getQuickFilter());
    assertTrue(params.messages().stream().anyMatch(m -> m.contains("No quickFilter applied")));
  }

  @Test
  void testEmptyStringKeywordHandled() {
    var params = AttackFilterParams.of("EXPLOITED", "   ", false, null, null, null);

    assertTrue(params.isValid());
    // Empty keyword shouldn't cause issues
  }

  @Test
  void testEmptyStringSortTreatedAsNull() {
    var params = AttackFilterParams.of("EXPLOITED", null, false, null, null, "   ");

    assertTrue(params.isValid());
    assertTrue(params.errors().isEmpty());
  }
}
