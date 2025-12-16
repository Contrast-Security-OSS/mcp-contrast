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
package com.contrast.labs.ai.mcp.contrast.tool.adr.params;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Unit tests for AttackFilterParams validation. */
class AttackFilterParamsTest {

  // ========== Valid Input Tests ==========

  @Test
  void of_should_accept_null_filters_with_defaults() {
    var params = AttackFilterParams.of(null, null, null, null, null, null, null);

    assertThat(params.isValid()).isTrue();
    assertThat(params.errors()).isEmpty();
    assertThat(params.getQuickFilter()).isEqualTo("ALL");
    assertThat(params.getIncludeSuppressed()).isFalse();
    assertThat(params.warnings())
        .anyMatch(w -> w.contains("No quickFilter applied"))
        .anyMatch(w -> w.contains("Excluding suppressed attacks by default"));
  }

  @Test
  void of_should_accept_valid_quickFilter() {
    var params = AttackFilterParams.of("ACTIVE", null, null, null, null, null, null);

    assertThat(params.isValid()).isTrue();
    assertThat(params.getQuickFilter()).isEqualTo("ACTIVE");
  }

  @Test
  void of_should_normalize_quickFilter_to_uppercase() {
    var params = AttackFilterParams.of("active", null, null, null, null, null, null);

    assertThat(params.isValid()).isTrue();
    assertThat(params.getQuickFilter()).isEqualTo("ACTIVE");
  }

  @Test
  void of_should_accept_all_valid_quickFilter_values() {
    for (String filter : AttackFilterParams.VALID_QUICK_FILTERS) {
      var params = AttackFilterParams.of(filter, null, null, null, null, null, null);
      assertThat(params.isValid()).as("QuickFilter '%s' should be valid", filter).isTrue();
      assertThat(params.getQuickFilter()).isEqualTo(filter);
    }
  }

  @Test
  void of_should_accept_valid_statusFilter() {
    var params = AttackFilterParams.of(null, "EXPLOITED", null, null, null, null, null);

    assertThat(params.isValid()).isTrue();
    assertThat(params.getStatusFilters()).containsExactly("EXPLOITED");
  }

  @Test
  void of_should_normalize_statusFilter_to_uppercase() {
    var params = AttackFilterParams.of(null, "exploited", null, null, null, null, null);

    assertThat(params.isValid()).isTrue();
    assertThat(params.getStatusFilters()).containsExactly("EXPLOITED");
  }

  @Test
  void of_should_accept_all_valid_statusFilter_values() {
    for (String status : AttackFilterParams.VALID_STATUS_FILTERS) {
      var params = AttackFilterParams.of(null, status, null, null, null, null, null);
      assertThat(params.isValid()).as("StatusFilter '%s' should be valid", status).isTrue();
      assertThat(params.getStatusFilters()).containsExactly(status);
    }
  }

  @Test
  void of_should_accept_keyword() {
    var params = AttackFilterParams.of(null, null, "sql injection", null, null, null, null);

    assertThat(params.isValid()).isTrue();
    assertThat(params.getKeyword()).isEqualTo("sql injection");
  }

  @Test
  void of_should_accept_boolean_filters() {
    var params = AttackFilterParams.of(null, null, null, true, false, true, null);

    assertThat(params.isValid()).isTrue();
    assertThat(params.getIncludeSuppressed()).isTrue();
    assertThat(params.getIncludeBotBlockers()).isFalse();
    assertThat(params.getIncludeIpBlacklist()).isTrue();
    // When includeSuppressed is explicitly set, no default warning
    assertThat(params.warnings()).noneMatch(w -> w.contains("Excluding suppressed"));
  }

  @Test
  void of_should_accept_valid_sort_ascending() {
    var params = AttackFilterParams.of(null, null, null, null, null, null, "severity");

    assertThat(params.isValid()).isTrue();
    assertThat(params.getSort()).isEqualTo("severity");
  }

  @Test
  void of_should_accept_valid_sort_descending() {
    var params = AttackFilterParams.of(null, null, null, null, null, null, "-startTime");

    assertThat(params.isValid()).isTrue();
    assertThat(params.getSort()).isEqualTo("-startTime");
  }

  @Test
  void of_should_accept_combined_valid_filters() {
    var params =
        AttackFilterParams.of("EFFECTIVE", "EXPLOITED", "xss", true, true, false, "-severity");

    assertThat(params.isValid()).isTrue();
    assertThat(params.getQuickFilter()).isEqualTo("EFFECTIVE");
    assertThat(params.getStatusFilters()).containsExactly("EXPLOITED");
    assertThat(params.getKeyword()).isEqualTo("xss");
    assertThat(params.getIncludeSuppressed()).isTrue();
    assertThat(params.getIncludeBotBlockers()).isTrue();
    assertThat(params.getIncludeIpBlacklist()).isFalse();
    assertThat(params.getSort()).isEqualTo("-severity");
  }

  // ========== Invalid Input Tests ==========

  @Test
  void of_should_reject_invalid_quickFilter() {
    var params = AttackFilterParams.of("INVALID", null, null, null, null, null, null);

    assertThat(params.isValid()).isFalse();
    assertThat(params.errors())
        .anyMatch(e -> e.contains("Invalid quickFilter") && e.contains("INVALID"));
  }

  @Test
  void of_should_reject_invalid_statusFilter() {
    var params = AttackFilterParams.of(null, "INVALID_STATUS", null, null, null, null, null);

    assertThat(params.isValid()).isFalse();
    assertThat(params.errors())
        .anyMatch(e -> e.contains("Invalid statusFilter") && e.contains("INVALID_STATUS"));
  }

  @Test
  void of_should_reject_invalid_sort_format() {
    var params = AttackFilterParams.of(null, null, null, null, null, null, "invalid sort!");

    assertThat(params.isValid()).isFalse();
    assertThat(params.errors())
        .anyMatch(e -> e.contains("Invalid sort format"))
        .anyMatch(e -> e.contains("Must be a field name with optional '-' prefix"));
  }

  @Test
  void of_should_reject_sort_with_spaces() {
    var params = AttackFilterParams.of(null, null, null, null, null, null, "start time");

    assertThat(params.isValid()).isFalse();
    assertThat(params.errors()).anyMatch(e -> e.contains("Invalid sort format"));
  }

  @Test
  void of_should_collect_multiple_errors() {
    var params = AttackFilterParams.of("BAD_FILTER", "BAD_STATUS", null, null, null, null, "bad!");

    assertThat(params.isValid()).isFalse();
    assertThat(params.errors()).hasSize(3);
    assertThat(params.errors())
        .anyMatch(e -> e.contains("Invalid quickFilter"))
        .anyMatch(e -> e.contains("Invalid statusFilter"))
        .anyMatch(e -> e.contains("Invalid sort format"));
  }

  // ========== SDK Conversion Tests ==========

  @Test
  void toAttacksFilterBody_should_convert_all_filters() {
    var params =
        AttackFilterParams.of("EFFECTIVE", "BLOCKED", "sql", false, true, false, "-severity");

    var filterBody = params.toAttacksFilterBody();

    assertThat(filterBody.getQuickFilter()).isEqualTo("EFFECTIVE");
    assertThat(filterBody.getStatusFilter()).containsExactly("BLOCKED");
    assertThat(filterBody.getKeyword()).isEqualTo("sql");
    assertThat(filterBody.isIncludeSuppressed()).isFalse();
    assertThat(filterBody.isIncludeBotBlockers()).isTrue();
    assertThat(filterBody.isIncludeIpBlacklist()).isFalse();
  }

  @Test
  void toAttacksFilterBody_should_handle_null_filters() {
    var params = AttackFilterParams.of(null, null, null, null, null, null, null);

    var filterBody = params.toAttacksFilterBody();

    assertThat(filterBody).isNotNull();
    assertThat(filterBody.getQuickFilter()).isEqualTo("ALL"); // Default
    assertThat(filterBody.isIncludeSuppressed()).isFalse(); // Default
  }
}
