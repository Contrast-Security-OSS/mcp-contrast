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
package com.contrast.labs.ai.mcp.contrast.utils;

import static org.assertj.core.api.Assertions.assertThat;

import com.contrastsecurity.http.RuleSeverity;
import com.contrastsecurity.http.ServerEnvironment;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/** Tests for ValidationHelper fluent builder. */
class ValidationHelperTest {

  // ========== Pagination Tests ==========

  @Test
  void pagination_should_use_defaults_when_null() {
    var v = ValidationHelper.builder().pagination(null, null);

    assertThat(v.isValid()).isTrue();
    assertThat(v.page()).isEqualTo(1);
    assertThat(v.pageSize()).isEqualTo(50);
    assertThat(v.offset()).isEqualTo(0);
    assertThat(v.warnings()).isEmpty();
  }

  @Test
  void pagination_should_accept_valid_values() {
    var v = ValidationHelper.builder().pagination(3, 25);

    assertThat(v.isValid()).isTrue();
    assertThat(v.page()).isEqualTo(3);
    assertThat(v.pageSize()).isEqualTo(25);
    assertThat(v.offset()).isEqualTo(50); // (3-1) * 25
    assertThat(v.warnings()).isEmpty();
  }

  @Test
  void pagination_should_clamp_negative_page_with_warning() {
    var v = ValidationHelper.builder().pagination(-5, 50);

    assertThat(v.isValid()).isTrue();
    assertThat(v.page()).isEqualTo(1);
    assertThat(v.warnings()).hasSize(1);
    assertThat(v.warnings().get(0)).contains("Invalid page -5");
  }

  @Test
  void pagination_should_clamp_zero_page_with_warning() {
    var v = ValidationHelper.builder().pagination(0, 50);

    assertThat(v.isValid()).isTrue();
    assertThat(v.page()).isEqualTo(1);
    assertThat(v.warnings()).hasSize(1);
    assertThat(v.warnings().get(0)).contains("Invalid page 0");
  }

  @Test
  void pagination_should_clamp_negative_pageSize_with_warning() {
    var v = ValidationHelper.builder().pagination(1, -10);

    assertThat(v.isValid()).isTrue();
    assertThat(v.pageSize()).isEqualTo(50);
    assertThat(v.warnings()).hasSize(1);
    assertThat(v.warnings().get(0)).contains("Invalid pageSize -10");
  }

  @Test
  void pagination_should_clamp_excessive_pageSize_with_warning() {
    var v = ValidationHelper.builder().pagination(1, 200);

    assertThat(v.isValid()).isTrue();
    assertThat(v.pageSize()).isEqualTo(100);
    assertThat(v.warnings()).hasSize(1);
    assertThat(v.warnings().get(0)).contains("pageSize 200 exceeds max 100");
  }

  @Test
  void pagination_should_accept_boundary_pageSize_100() {
    var v = ValidationHelper.builder().pagination(1, 100);

    assertThat(v.isValid()).isTrue();
    assertThat(v.pageSize()).isEqualTo(100);
    assertThat(v.warnings()).isEmpty();
  }

  // ========== EnumSet Tests ==========

  @Test
  void enumSet_should_parse_single_value() {
    AtomicReference<EnumSet<RuleSeverity>> result = new AtomicReference<>();

    var v =
        ValidationHelper.builder()
            .enumSet("CRITICAL", RuleSeverity.class, "severities", result::set);

    assertThat(v.isValid()).isTrue();
    assertThat(result.get()).containsExactly(RuleSeverity.CRITICAL);
    assertThat(v.errors()).isEmpty();
  }

  @Test
  void enumSet_should_parse_multiple_values() {
    AtomicReference<EnumSet<RuleSeverity>> result = new AtomicReference<>();

    var v =
        ValidationHelper.builder()
            .enumSet("CRITICAL, HIGH, MEDIUM", RuleSeverity.class, "severities", result::set);

    assertThat(v.isValid()).isTrue();
    assertThat(result.get())
        .containsExactlyInAnyOrder(RuleSeverity.CRITICAL, RuleSeverity.HIGH, RuleSeverity.MEDIUM);
  }

  @Test
  void enumSet_should_handle_case_insensitivity() {
    AtomicReference<EnumSet<RuleSeverity>> result = new AtomicReference<>();

    var v =
        ValidationHelper.builder()
            .enumSet("critical, High, MEDIUM", RuleSeverity.class, "severities", result::set);

    assertThat(v.isValid()).isTrue();
    assertThat(result.get())
        .containsExactlyInAnyOrder(RuleSeverity.CRITICAL, RuleSeverity.HIGH, RuleSeverity.MEDIUM);
  }

  @Test
  void enumSet_should_return_error_for_invalid_value() {
    AtomicReference<EnumSet<RuleSeverity>> result = new AtomicReference<>();

    var v =
        ValidationHelper.builder()
            .enumSet("CRITICAL, INVALID, HIGH", RuleSeverity.class, "severities", result::set);

    assertThat(v.isValid()).isFalse();
    assertThat(result.get()).isNull();
    assertThat(v.errors()).hasSize(1);
    assertThat(v.errors().get(0)).contains("Invalid severities");
    assertThat(v.errors().get(0)).contains("INVALID");
  }

  @Test
  void enumSet_should_set_null_for_empty_input() {
    AtomicReference<EnumSet<RuleSeverity>> result = new AtomicReference<>();

    var v = ValidationHelper.builder().enumSet("", RuleSeverity.class, "severities", result::set);

    assertThat(v.isValid()).isTrue();
    assertThat(result.get()).isNull();
  }

  @Test
  void enumSet_should_set_null_for_null_input() {
    AtomicReference<EnumSet<RuleSeverity>> result = new AtomicReference<>();

    var v = ValidationHelper.builder().enumSet(null, RuleSeverity.class, "severities", result::set);

    assertThat(v.isValid()).isTrue();
    assertThat(result.get()).isNull();
  }

  @Test
  void enumSet_should_work_with_ServerEnvironment() {
    AtomicReference<EnumSet<ServerEnvironment>> result = new AtomicReference<>();

    var v =
        ValidationHelper.builder()
            .enumSet("PRODUCTION, QA", ServerEnvironment.class, "environments", result::set);

    assertThat(v.isValid()).isTrue();
    assertThat(result.get())
        .containsExactlyInAnyOrder(ServerEnvironment.PRODUCTION, ServerEnvironment.QA);
  }

  // ========== StringList Tests ==========

  @Test
  void stringList_should_parse_valid_values() {
    AtomicReference<List<String>> result = new AtomicReference<>();
    Set<String> allowed = Set.of("Reported", "Confirmed", "Fixed");

    var v =
        ValidationHelper.builder()
            .stringList("Reported, Confirmed", allowed, null, "statuses", result::set);

    assertThat(v.isValid()).isTrue();
    assertThat(result.get()).containsExactly("Reported", "Confirmed");
    assertThat(v.warnings()).isEmpty();
  }

  @Test
  void stringList_should_return_error_for_invalid_value() {
    AtomicReference<List<String>> result = new AtomicReference<>();
    Set<String> allowed = Set.of("Reported", "Confirmed", "Fixed");

    var v =
        ValidationHelper.builder()
            .stringList("Reported, Invalid, Confirmed", allowed, null, "statuses", result::set);

    assertThat(v.isValid()).isFalse();
    assertThat(result.get()).isNull();
    assertThat(v.errors()).hasSize(1);
    assertThat(v.errors().get(0)).contains("Invalid statuses");
    assertThat(v.errors().get(0)).contains("Invalid");
  }

  @Test
  void stringList_should_use_defaults_when_empty() {
    AtomicReference<List<String>> result = new AtomicReference<>();
    Set<String> allowed = Set.of("Reported", "Confirmed", "Fixed");
    List<String> defaults = List.of("Reported", "Confirmed");

    var v = ValidationHelper.builder().stringList("", allowed, defaults, "statuses", result::set);

    assertThat(v.isValid()).isTrue();
    assertThat(result.get()).containsExactly("Reported", "Confirmed");
    assertThat(v.warnings()).hasSize(1);
    assertThat(v.warnings().get(0)).contains("Using default statuses");
  }

  @Test
  void stringList_should_set_null_when_empty_and_no_defaults() {
    AtomicReference<List<String>> result = new AtomicReference<>();
    Set<String> allowed = Set.of("Reported", "Confirmed", "Fixed");

    var v = ValidationHelper.builder().stringList("", allowed, null, "statuses", result::set);

    assertThat(v.isValid()).isTrue();
    assertThat(result.get()).isNull();
    assertThat(v.warnings()).isEmpty();
  }

  @Test
  void stringList_should_be_case_sensitive() {
    AtomicReference<List<String>> result = new AtomicReference<>();
    Set<String> allowed = Set.of("Reported", "Confirmed", "Fixed");

    var v =
        ValidationHelper.builder().stringList("reported", allowed, null, "statuses", result::set);

    assertThat(v.isValid()).isFalse();
    assertThat(v.errors().get(0)).contains("Invalid statuses");
    assertThat(v.errors().get(0)).contains("reported");
  }

  // ========== Cross-Field Validation Tests ==========

  @Test
  void requireWith_should_pass_when_both_present() {
    var v = ValidationHelper.builder().requireWith("value1", "param1", "value2", "param2");

    assertThat(v.isValid()).isTrue();
    assertThat(v.errors()).isEmpty();
  }

  @Test
  void requireWith_should_pass_when_first_empty() {
    var v = ValidationHelper.builder().requireWith("", "param1", "value2", "param2");

    assertThat(v.isValid()).isTrue();
    assertThat(v.errors()).isEmpty();
  }

  @Test
  void requireWith_should_fail_when_first_present_but_second_missing() {
    var v =
        ValidationHelper.builder()
            .requireWith("value1", "sessionMetadataValue", "", "sessionMetadataName");

    assertThat(v.isValid()).isFalse();
    assertThat(v.errors()).hasSize(1);
    assertThat(v.errors().get(0)).isEqualTo("sessionMetadataValue requires sessionMetadataName");
  }

  @Test
  void mutuallyExclusive_should_pass_when_only_first_present() {
    var v = ValidationHelper.builder().mutuallyExclusive("appId", "app-123", "appName", null);

    assertThat(v.isValid()).isTrue();
    assertThat(v.errors()).isEmpty();
  }

  @Test
  void mutuallyExclusive_should_pass_when_neither_present() {
    var v = ValidationHelper.builder().mutuallyExclusive("appId", null, "appName", null);

    assertThat(v.isValid()).isTrue();
    assertThat(v.errors()).isEmpty();
  }

  @Test
  void mutuallyExclusive_should_fail_when_both_present() {
    var v = ValidationHelper.builder().mutuallyExclusive("appId", "app-123", "appName", "MyApp");

    assertThat(v.isValid()).isFalse();
    assertThat(v.errors()).hasSize(1);
    assertThat(v.errors().get(0)).isEqualTo("appId and appName are mutually exclusive");
  }

  @Test
  void mutuallyExclusive_should_treat_empty_string_as_absent() {
    var v = ValidationHelper.builder().mutuallyExclusive("appId", "app-123", "appName", "");

    assertThat(v.isValid()).isTrue();
    assertThat(v.errors()).isEmpty();
  }

  // ========== Custom Error/Warning Tests ==========

  @Test
  void addError_should_mark_invalid() {
    var v = ValidationHelper.builder().addError("Custom error message");

    assertThat(v.isValid()).isFalse();
    assertThat(v.errors()).containsExactly("Custom error message");
  }

  @Test
  void addWarning_should_not_affect_validity() {
    var v = ValidationHelper.builder().addWarning("Custom warning message");

    assertThat(v.isValid()).isTrue();
    assertThat(v.warnings()).containsExactly("Custom warning message");
  }

  @Test
  void addError_should_ignore_empty_message() {
    var v = ValidationHelper.builder().addError("").addError(null);

    assertThat(v.isValid()).isTrue();
    assertThat(v.errors()).isEmpty();
  }

  // ========== Chaining Tests ==========

  @Test
  void should_support_full_validation_chain() {
    AtomicReference<EnumSet<RuleSeverity>> severities = new AtomicReference<>();
    AtomicReference<List<String>> statuses = new AtomicReference<>();
    Set<String> validStatuses = Set.of("Reported", "Confirmed", "Fixed");

    var v =
        ValidationHelper.builder()
            .pagination(2, 25)
            .enumSet("CRITICAL, HIGH", RuleSeverity.class, "severities", severities::set)
            .stringList("Reported, Confirmed", validStatuses, null, "statuses", statuses::set)
            .requireWith("value", "sessionValue", "name", "sessionName");

    assertThat(v.isValid()).isTrue();
    assertThat(v.page()).isEqualTo(2);
    assertThat(v.pageSize()).isEqualTo(25);
    assertThat(severities.get())
        .containsExactlyInAnyOrder(RuleSeverity.CRITICAL, RuleSeverity.HIGH);
    assertThat(statuses.get()).containsExactly("Reported", "Confirmed");
  }

  @Test
  void should_collect_multiple_errors() {
    AtomicReference<EnumSet<RuleSeverity>> severities = new AtomicReference<>();
    AtomicReference<List<String>> statuses = new AtomicReference<>();
    Set<String> validStatuses = Set.of("Reported", "Confirmed", "Fixed");

    var v =
        ValidationHelper.builder()
            .enumSet("INVALID_SEV", RuleSeverity.class, "severities", severities::set)
            .stringList("InvalidStatus", validStatuses, null, "statuses", statuses::set)
            .requireWith("value", "param1", "", "param2");

    assertThat(v.isValid()).isFalse();
    assertThat(v.errors()).hasSize(3);
  }

  // ========== Immutability Tests ==========

  @Test
  void errors_should_return_immutable_list() {
    var v = ValidationHelper.builder().addError("error");
    var errors = v.errors();

    assertThat(errors).isUnmodifiable();
  }

  @Test
  void warnings_should_return_immutable_list() {
    var v = ValidationHelper.builder().addWarning("warning");
    var warnings = v.warnings();

    assertThat(warnings).isUnmodifiable();
  }
}
