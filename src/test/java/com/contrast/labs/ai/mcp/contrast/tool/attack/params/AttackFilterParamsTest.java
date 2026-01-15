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
package com.contrast.labs.ai.mcp.contrast.tool.attack.params;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for AttackFilterParams validation. */
class AttackFilterParamsTest {

  // ========== Valid Input Tests ==========

  @Test
  void of_should_accept_null_filters_with_defaults() {
    var params = AttackFilterParams.of(null, null, null, null, null, null, null, null);

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
    var params = AttackFilterParams.of("ACTIVE", null, null, null, null, null, null, null);

    assertThat(params.isValid()).isTrue();
    assertThat(params.getQuickFilter()).isEqualTo("ACTIVE");
  }

  @Test
  void of_should_normalize_quickFilter_to_uppercase() {
    var params = AttackFilterParams.of("active", null, null, null, null, null, null, null);

    assertThat(params.isValid()).isTrue();
    assertThat(params.getQuickFilter()).isEqualTo("ACTIVE");
  }

  @Test
  void of_should_accept_all_valid_quickFilter_values() {
    for (String filter : AttackFilterParams.VALID_QUICK_FILTERS) {
      var params = AttackFilterParams.of(filter, null, null, null, null, null, null, null);
      assertThat(params.isValid()).as("QuickFilter '%s' should be valid", filter).isTrue();
      assertThat(params.getQuickFilter()).isEqualTo(filter);
    }
  }

  @Test
  void of_should_accept_valid_statusFilter() {
    var params = AttackFilterParams.of(null, "EXPLOITED", null, null, null, null, null, null);

    assertThat(params.isValid()).isTrue();
    assertThat(params.getStatusFilters()).containsExactly("EXPLOITED");
  }

  @Test
  void of_should_normalize_statusFilter_to_uppercase() {
    var params = AttackFilterParams.of(null, "exploited", null, null, null, null, null, null);

    assertThat(params.isValid()).isTrue();
    assertThat(params.getStatusFilters()).containsExactly("EXPLOITED");
  }

  @Test
  void of_should_accept_all_valid_statusFilter_values() {
    for (String status : AttackFilterParams.VALID_STATUS_FILTERS) {
      var params = AttackFilterParams.of(null, status, null, null, null, null, null, null);
      assertThat(params.isValid()).as("StatusFilter '%s' should be valid", status).isTrue();
      assertThat(params.getStatusFilters()).containsExactly(status);
    }
  }

  @Test
  void of_should_accept_keyword() {
    var params = AttackFilterParams.of(null, null, "sql injection", null, null, null, null, null);

    assertThat(params.isValid()).isTrue();
    assertThat(params.getKeyword()).isEqualTo("sql injection");
  }

  @Test
  void of_should_accept_boolean_filters() {
    var params = AttackFilterParams.of(null, null, null, true, false, true, null, null);

    assertThat(params.isValid()).isTrue();
    assertThat(params.getIncludeSuppressed()).isTrue();
    assertThat(params.getIncludeBotBlockers()).isFalse();
    assertThat(params.getIncludeIpBlacklist()).isTrue();
    // When includeSuppressed is explicitly set, no default warning
    assertThat(params.warnings()).noneMatch(w -> w.contains("Excluding suppressed"));
  }

  @Nested
  @DisplayName("Sort Field Allowlist Validation")
  class SortFieldValidationTests {

    @Test
    void of_should_accept_all_valid_sort_fields() {
      for (String field : AttackFilterParams.VALID_SORT_FIELDS) {
        var params = AttackFilterParams.of(null, null, null, null, null, null, field, null);
        assertThat(params.isValid()).as("Sort field '%s' should be valid", field).isTrue();
        assertThat(params.getSort()).isEqualTo(field);
      }
    }

    @Test
    void of_should_accept_all_valid_sort_fields_with_descending_prefix() {
      for (String field : AttackFilterParams.VALID_SORT_FIELDS) {
        String descending = "-" + field;
        var params = AttackFilterParams.of(null, null, null, null, null, null, descending, null);
        assertThat(params.isValid()).as("Sort field '%s' should be valid", descending).isTrue();
        assertThat(params.getSort()).isEqualTo(descending);
      }
    }

    @Test
    void of_should_accept_startTime_ascending() {
      var params = AttackFilterParams.of(null, null, null, null, null, null, "startTime", null);

      assertThat(params.isValid()).isTrue();
      assertThat(params.getSort()).isEqualTo("startTime");
    }

    @Test
    void of_should_accept_startTime_descending() {
      var params = AttackFilterParams.of(null, null, null, null, null, null, "-startTime", null);

      assertThat(params.isValid()).isTrue();
      assertThat(params.getSort()).isEqualTo("-startTime");
    }

    @Test
    void of_should_accept_endTime() {
      var params = AttackFilterParams.of(null, null, null, null, null, null, "endTime", null);

      assertThat(params.isValid()).isTrue();
      assertThat(params.getSort()).isEqualTo("endTime");
    }

    @Test
    void of_should_accept_status() {
      var params = AttackFilterParams.of(null, null, null, null, null, null, "status", null);

      assertThat(params.isValid()).isTrue();
      assertThat(params.getSort()).isEqualTo("status");
    }

    @Test
    void of_should_accept_sourceIP() {
      var params = AttackFilterParams.of(null, null, null, null, null, null, "sourceIP", null);

      assertThat(params.isValid()).isTrue();
      assertThat(params.getSort()).isEqualTo("sourceIP");
    }

    @Test
    void of_should_accept_type() {
      var params = AttackFilterParams.of(null, null, null, null, null, null, "type", null);

      assertThat(params.isValid()).isTrue();
      assertThat(params.getSort()).isEqualTo("type");
    }

    @Test
    void of_should_reject_severity_not_valid_sort_field() {
      var params = AttackFilterParams.of(null, null, null, null, null, null, "severity", null);

      assertThat(params.isValid()).isFalse();
      assertThat(params.errors().get(0)).contains("Invalid sort field").contains("severity");
    }

    @Test
    void of_should_reject_probes_not_valid_sort_field() {
      var params = AttackFilterParams.of(null, null, null, null, null, null, "probes", null);

      assertThat(params.isValid()).isFalse();
      assertThat(params.errors()).anyMatch(e -> e.contains("Invalid sort field"));
    }

    @Test
    void of_should_reject_invalid_field_with_descending_prefix() {
      var params = AttackFilterParams.of(null, null, null, null, null, null, "-invalidField", null);

      assertThat(params.isValid()).isFalse();
      assertThat(params.errors().get(0)).contains("Invalid sort field").contains("invalidField");
    }

    @Test
    void of_should_be_case_sensitive_starttime_lowercase_invalid() {
      // API is case-sensitive - must match exact camelCase
      var params = AttackFilterParams.of(null, null, null, null, null, null, "starttime", null);

      assertThat(params.isValid()).isFalse();
      assertThat(params.errors()).anyMatch(e -> e.contains("Invalid sort field"));
    }

    @Test
    void of_should_be_case_sensitive_STARTTIME_uppercase_invalid() {
      var params = AttackFilterParams.of(null, null, null, null, null, null, "STARTTIME", null);

      assertThat(params.isValid()).isFalse();
      assertThat(params.errors()).anyMatch(e -> e.contains("Invalid sort field"));
    }

    @Test
    void of_should_include_valid_fields_in_error_message() {
      var params = AttackFilterParams.of(null, null, null, null, null, null, "badField", null);

      assertThat(params.isValid()).isFalse();
      assertThat(params.errors().get(0))
          .contains("Valid fields:")
          .contains("startTime")
          .contains("endTime")
          .contains("status")
          .contains("sourceIP")
          .contains("type");
    }

    @Test
    void of_should_list_valid_fields_in_alphabetical_order() {
      var params = AttackFilterParams.of(null, null, null, null, null, null, "badField", null);

      assertThat(params.isValid()).isFalse();
      // Verify fields are in consistent alphabetical order for predictable UX
      assertThat(params.errors().get(0)).contains("[endTime, sourceIP, startTime, status, type]");
    }

    @Test
    void of_should_handle_null_sort() {
      var params = AttackFilterParams.of(null, null, null, null, null, null, null, null);

      assertThat(params.isValid()).isTrue();
      assertThat(params.getSort()).isNull();
    }

    @Test
    void of_should_handle_empty_sort() {
      var params = AttackFilterParams.of(null, null, null, null, null, null, "", null);

      assertThat(params.isValid()).isTrue();
      assertThat(params.getSort()).isNull();
    }

    @Test
    void of_should_handle_whitespace_only_sort() {
      var params = AttackFilterParams.of(null, null, null, null, null, null, "   ", null);

      assertThat(params.isValid()).isTrue();
      assertThat(params.getSort()).isNull();
    }

    @Test
    void of_should_trim_whitespace_from_valid_sort() {
      var params = AttackFilterParams.of(null, null, null, null, null, null, "  startTime  ", null);

      assertThat(params.isValid()).isTrue();
      assertThat(params.getSort()).isEqualTo("startTime");
    }
  }

  @Test
  void of_should_accept_combined_valid_filters() {
    var params =
        AttackFilterParams.of("EFFECTIVE", "EXPLOITED", "xss", true, true, false, "-status", null);

    assertThat(params.isValid()).isTrue();
    assertThat(params.getQuickFilter()).isEqualTo("EFFECTIVE");
    assertThat(params.getStatusFilters()).containsExactly("EXPLOITED");
    assertThat(params.getKeyword()).isEqualTo("xss");
    assertThat(params.getIncludeSuppressed()).isTrue();
    assertThat(params.getIncludeBotBlockers()).isTrue();
    assertThat(params.getIncludeIpBlacklist()).isFalse();
    assertThat(params.getSort()).isEqualTo("-status");
  }

  // ========== Invalid Input Tests ==========

  @Test
  void of_should_reject_invalid_quickFilter() {
    var params = AttackFilterParams.of("INVALID", null, null, null, null, null, null, null);

    assertThat(params.isValid()).isFalse();
    assertThat(params.errors())
        .anyMatch(e -> e.contains("Invalid quickFilter") && e.contains("INVALID"));
  }

  @Test
  void of_should_reject_invalid_statusFilter() {
    var params = AttackFilterParams.of(null, "INVALID_STATUS", null, null, null, null, null, null);

    assertThat(params.isValid()).isFalse();
    assertThat(params.errors())
        .anyMatch(e -> e.contains("Invalid statusFilter") && e.contains("INVALID_STATUS"));
  }

  @Test
  void of_should_collect_multiple_errors() {
    var params =
        AttackFilterParams.of(
            "BAD_FILTER", "BAD_STATUS", null, null, null, null, "invalidField", null);

    assertThat(params.isValid()).isFalse();
    assertThat(params.errors()).hasSize(3);
    assertThat(params.errors())
        .anyMatch(e -> e.contains("Invalid quickFilter"))
        .anyMatch(e -> e.contains("Invalid statusFilter"))
        .anyMatch(e -> e.contains("Invalid sort field"));
  }

  // ========== SDK Conversion Tests ==========

  @Test
  void toAttacksFilterBody_should_convert_all_filters() {
    var params =
        AttackFilterParams.of("EFFECTIVE", "BLOCKED", "sql", false, true, false, "-status", null);

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
    var params = AttackFilterParams.of(null, null, null, null, null, null, null, null);

    var filterBody = params.toAttacksFilterBody();

    assertThat(filterBody).isNotNull();
    assertThat(filterBody.getQuickFilter()).isEqualTo("ALL"); // Default
    assertThat(filterBody.isIncludeSuppressed()).isFalse(); // Default
  }

  // ========== Keyword Encoding Tests ==========

  @Nested
  @DisplayName("toAttacksFilterBody keyword handling")
  class KeywordEncodingTests {

    @Test
    void toAttacksFilterBody_should_preserve_spaces_in_keyword() {
      var params = AttackFilterParams.of(null, null, "SQL Injection", null, null, null, null, null);

      var filterBody = params.toAttacksFilterBody();

      // Spaces should be preserved, not URL-encoded to +
      assertThat(filterBody.getKeyword()).isEqualTo("SQL Injection");
    }

    @Test
    void toAttacksFilterBody_should_preserve_specialCharacters() {
      var params = AttackFilterParams.of(null, null, "<script>", null, null, null, null, null);

      var filterBody = params.toAttacksFilterBody();

      // Special characters should be preserved (not URL encoded)
      // The keyword is sent in a JSON POST body, not a URL query string
      assertThat(filterBody.getKeyword()).isEqualTo("<script>");
    }

    @Test
    void toAttacksFilterBody_should_preserve_angleBrackets() {
      var params =
          AttackFilterParams.of(
              null, null, "<script>alert('xss')</script>", null, null, null, null, null);

      var filterBody = params.toAttacksFilterBody();

      // Keywords are preserved as-is (sent in JSON body, not URL)
      assertThat(filterBody.getKeyword()).isEqualTo("<script>alert('xss')</script>");
    }

    @Test
    void toAttacksFilterBody_should_preserve_sqlInjectionPattern() {
      var params =
          AttackFilterParams.of(null, null, "'; DROP TABLE users;--", null, null, null, null, null);

      var filterBody = params.toAttacksFilterBody();

      // Keywords are preserved as-is (sent in JSON body, not URL)
      assertThat(filterBody.getKeyword()).isEqualTo("'; DROP TABLE users;--");
    }

    @Test
    void toAttacksFilterBody_should_preserve_pathTraversalPattern() {
      var params =
          AttackFilterParams.of(null, null, "../../../etc/passwd", null, null, null, null, null);

      var filterBody = params.toAttacksFilterBody();

      // Keywords are preserved as-is (sent in JSON body, not URL)
      assertThat(filterBody.getKeyword()).isEqualTo("../../../etc/passwd");
    }

    @Test
    void toAttacksFilterBody_should_preserve_emailAddress() {
      var params =
          AttackFilterParams.of(null, null, "user@domain.com", null, null, null, null, null);

      var filterBody = params.toAttacksFilterBody();

      // Keywords are preserved as-is (sent in JSON body, not URL)
      assertThat(filterBody.getKeyword()).isEqualTo("user@domain.com");
    }

    @Test
    void toAttacksFilterBody_should_preserve_alphanumericKeyword() {
      var params = AttackFilterParams.of(null, null, "sql-injection", null, null, null, null, null);

      var filterBody = params.toAttacksFilterBody();

      assertThat(filterBody.getKeyword()).isEqualTo("sql-injection");
    }

    @Test
    void toAttacksFilterBody_should_preserve_spaces() {
      var params =
          AttackFilterParams.of(null, null, "SELECT * FROM users", null, null, null, null, null);

      var filterBody = params.toAttacksFilterBody();

      // Spaces are preserved (sent in JSON body, not URL)
      assertThat(filterBody.getKeyword()).isEqualTo("SELECT * FROM users");
    }

    @Test
    void toAttacksFilterBody_should_handleNullKeyword() {
      var params = AttackFilterParams.of(null, null, null, null, null, null, null, null);

      var filterBody = params.toAttacksFilterBody();

      // Null keyword should result in empty string from builder
      assertThat(filterBody.getKeyword()).isEmpty();
    }

    @Test
    void toAttacksFilterBody_should_handleEmptyKeyword() {
      var params = AttackFilterParams.of(null, null, "", null, null, null, null, null);

      var filterBody = params.toAttacksFilterBody();

      assertThat(filterBody.getKeyword()).isEmpty();
    }

    @Test
    void toAttacksFilterBody_should_preserve_unicodeCharacters() {
      var params = AttackFilterParams.of(null, null, "攻撃", null, null, null, null, null);

      var filterBody = params.toAttacksFilterBody();

      // Unicode characters are preserved (sent in JSON body, not URL)
      assertThat(filterBody.getKeyword()).isEqualTo("攻撃");
    }
  }
}
