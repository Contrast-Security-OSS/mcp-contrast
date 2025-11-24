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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AttackFilterParamsTest {

  @Test
  void testValidFiltersAllProvided() {
    var params = AttackFilterParams.of("EFFECTIVE", null, "xss", true, true, false, "severity");

    assertThat(params.isValid()).isTrue();
    assertThat(params.errors()).isEmpty();

    var filterBody = params.toAttacksFilterBody();
    assertThat(filterBody.getQuickFilter()).isEqualTo("EFFECTIVE");
    assertThat(filterBody.getKeyword()).isEqualTo("xss");
    assertThat(filterBody.isIncludeSuppressed()).isTrue();
    assertThat(filterBody.isIncludeBotBlockers()).isTrue();
    assertThat(filterBody.isIncludeIpBlacklist()).isFalse();
  }

  @Test
  void testNoFiltersProvided() {
    var params = AttackFilterParams.of(null, null, null, null, null, null, null);

    assertThat(params.isValid()).isTrue();
    assertThat(params.messages()).isNotEmpty(); // Should have smart defaults messages
    assertThat(params.errors()).isEmpty();

    // Smart defaults should be applied
    var filterBody = params.toAttacksFilterBody();
    assertThat(filterBody.getQuickFilter()).isEqualTo("ALL");
    assertThat(filterBody.isIncludeSuppressed()).isFalse(); // Smart default: exclude suppressed
  }

  @Test
  void testSmartDefaultForIncludeSuppressed() {
    var params = AttackFilterParams.of(null, null, null, null, null, null, null);

    assertThat(params.isValid()).isTrue();
    assertThat(params.errors()).isEmpty();

    // Should have two messages: quickFilter default and includeSuppressed default
    assertThat(params.messages()).hasSize(2);
    assertThat(params.messages().stream().anyMatch(m -> m.contains("No quickFilter applied")))
        .isTrue();
    assertThat(
            params.messages().stream()
                .anyMatch(m -> m.contains("Excluding suppressed attacks by default")))
        .isTrue();

    var filterBody = params.toAttacksFilterBody();
    assertThat(filterBody.isIncludeSuppressed()).isFalse();
  }

  @Test
  void testExplicitIncludeSuppressedNoMessage() {
    var params = AttackFilterParams.of("EFFECTIVE", null, null, true, null, null, null);

    assertThat(params.isValid()).isTrue();
    assertThat(params.errors()).isEmpty();
    // Should not have includeSuppressed message when explicitly provided
    assertThat(params.messages().stream().anyMatch(m -> m.contains("Excluding suppressed")))
        .isFalse();

    var filterBody = params.toAttacksFilterBody();
    assertThat(filterBody.isIncludeSuppressed()).isTrue();
  }

  @Test
  void testInvalidQuickFilterHardFailure() {
    var params = AttackFilterParams.of("INVALID", null, null, null, null, null, null);

    assertThat(params.isValid()).isFalse();
    assertThat(params.errors()).hasSize(1);
    assertThat(params.errors().get(0)).contains("Invalid quickFilter 'INVALID'");
    assertThat(params.errors().get(0))
        .contains("Valid: ALL, ACTIVE, MANUAL, AUTOMATED, PRODUCTION, EFFECTIVE");
  }

  @Test
  void testValidQuickFilterValues() {
    String[] validFilters = {"ALL", "ACTIVE", "MANUAL", "AUTOMATED", "PRODUCTION", "EFFECTIVE"};

    for (String filter : validFilters) {
      var params = AttackFilterParams.of(filter, null, null, false, null, null, null);
      assertThat(params.isValid()).as("Filter " + filter + " should be valid").isTrue();
      assertThat(params.errors()).isEmpty();
    }
  }

  @Test
  void testQuickFilterCaseInsensitive() {
    // Test lowercase and mixed case
    var params1 = AttackFilterParams.of("active", null, null, false, null, null, null);
    assertThat(params1.isValid()).isTrue();
    assertThat(params1.toAttacksFilterBody().getQuickFilter()).isEqualTo("ACTIVE");

    var params2 = AttackFilterParams.of("MaNuAl", null, null, false, null, null, null);
    assertThat(params2.isValid()).isTrue();
    assertThat(params2.toAttacksFilterBody().getQuickFilter()).isEqualTo("MANUAL");
  }

  @Test
  void testQuickFilterWithWhitespace() {
    var params = AttackFilterParams.of("  ACTIVE  ", null, null, false, null, null, null);

    assertThat(params.isValid()).isTrue();
    assertThat(params.toAttacksFilterBody().getQuickFilter()).isEqualTo("ACTIVE");
  }

  @Test
  void testKeywordPassThrough() {
    var params =
        AttackFilterParams.of("EFFECTIVE", null, "sql injection test", false, null, null, null);

    assertThat(params.isValid()).isTrue();
    assertThat(params.toAttacksFilterBody().getKeyword()).isEqualTo("sql injection test");
  }

  @Test
  void testValidSortFormat() {
    var params = AttackFilterParams.of("EFFECTIVE", null, null, false, null, null, "severity");

    assertThat(params.isValid()).isTrue();
    assertThat(params.errors()).isEmpty();
  }

  @Test
  void testValidDescendingSortFormat() {
    var params = AttackFilterParams.of("EFFECTIVE", null, null, false, null, null, "-severity");

    assertThat(params.isValid()).isTrue();
    assertThat(params.errors()).isEmpty();
  }

  @Test
  void testInvalidSortFormatHardFailure() {
    var params = AttackFilterParams.of("EFFECTIVE", null, null, false, null, null, "invalid sort!");

    assertThat(params.isValid()).isFalse();
    assertThat(params.errors()).hasSize(1);
    assertThat(params.errors().get(0)).contains("Invalid sort format 'invalid sort!'");
    assertThat(params.errors().get(0)).contains("Must be a field name with optional '-' prefix");
  }

  @Test
  void testValidSortWithUnderscores() {
    var params = AttackFilterParams.of("EFFECTIVE", null, null, false, null, null, "field_name");

    assertThat(params.isValid()).isTrue();
    assertThat(params.errors()).isEmpty();
  }

  @Test
  void testAllBooleanFlagsExplicitlySet() {
    var params = AttackFilterParams.of("EFFECTIVE", null, "keyword", true, true, true, null);

    assertThat(params.isValid()).isTrue();

    var filterBody = params.toAttacksFilterBody();
    assertThat(filterBody.isIncludeSuppressed()).isTrue();
    assertThat(filterBody.isIncludeBotBlockers()).isTrue();
    assertThat(filterBody.isIncludeIpBlacklist()).isTrue();
  }

  @Test
  void testMultipleErrorsAccumulate() {
    var params =
        AttackFilterParams.of("INVALID_FILTER", null, null, null, null, null, "bad-sort-format!");

    assertThat(params.isValid()).isFalse();
    assertThat(params.errors()).hasSize(2); // quickFilter and sort errors
  }

  @Test
  void testMessagesAreImmutable() {
    var params = AttackFilterParams.of(null, null, null, null, null, null, null);

    assertThatThrownBy(
            () -> {
              params.messages().add("Should fail");
            })
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void testErrorsAreImmutable() {
    var params = AttackFilterParams.of("INVALID", null, null, null, null, null, null);

    assertThatThrownBy(
            () -> {
              params.errors().add("Should fail");
            })
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void testQuickFilterDefaultMessage() {
    var params = AttackFilterParams.of(null, null, null, false, null, null, null);

    assertThat(params.isValid()).isTrue();
    assertThat(
            params.messages().stream()
                .anyMatch(m -> m.contains("No quickFilter applied - showing all attack types")))
        .isTrue();
  }

  @Test
  void testNoQuickFilterMessageWhenProvided() {
    var params = AttackFilterParams.of("EFFECTIVE", null, null, false, null, null, null);

    assertThat(params.isValid()).isTrue();
    assertThat(params.messages().stream().anyMatch(m -> m.contains("No quickFilter applied")))
        .isFalse();
  }

  @Test
  void testEmptyStringQuickFilterTreatedAsNull() {
    var params = AttackFilterParams.of("   ", null, null, false, null, null, null);

    assertThat(params.isValid()).isTrue();
    // Empty/whitespace should be treated as null and use default
    assertThat(params.toAttacksFilterBody().getQuickFilter()).isEqualTo("ALL");
    assertThat(params.messages().stream().anyMatch(m -> m.contains("No quickFilter applied")))
        .isTrue();
  }

  @Test
  void testEmptyStringKeywordHandled() {
    var params = AttackFilterParams.of("EFFECTIVE", null, "   ", false, null, null, null);

    assertThat(params.isValid()).isTrue();
    // Empty keyword shouldn't cause issues
  }

  @Test
  void testEmptyStringSortTreatedAsNull() {
    var params = AttackFilterParams.of("EFFECTIVE", null, null, false, null, null, "   ");

    assertThat(params.isValid()).isTrue();
    assertThat(params.errors()).isEmpty();
  }
}
