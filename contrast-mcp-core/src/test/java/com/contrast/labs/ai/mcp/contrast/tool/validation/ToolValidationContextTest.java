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
package com.contrast.labs.ai.mcp.contrast.tool.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ToolValidationContextTest {

  private ToolValidationContext ctx;

  @BeforeEach
  void setUp() {
    ctx = new ToolValidationContext();
  }

  // -- Basic state tests --

  @Test
  void isValid_should_return_true_when_no_errors() {
    assertThat(ctx.isValid()).isTrue();
  }

  @Test
  void isValid_should_return_false_when_errors_exist() {
    ctx.require(null, "appId");

    assertThat(ctx.isValid()).isFalse();
  }

  @Test
  void errors_should_return_immutable_list() {
    ctx.require(null, "appId");

    assertThatThrownBy(() -> ctx.errors().add("new error"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void warnings_should_return_immutable_list() {
    ctx.warnIf(true, "A warning");

    assertThatThrownBy(() -> ctx.warnings().add("new warning"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  // -- validateDateRange tests --

  @Test
  void validateDateRange_should_add_error_when_start_after_end() {
    Date start = new Date(1705363200000L); // 2024-01-16
    Date end = new Date(1705276800000L); // 2024-01-15

    ctx.validateDateRange(start, end, "startDate", "endDate");

    assertThat(ctx.isValid()).isFalse();
    assertThat(ctx.errors()).hasSize(1);
    assertThat(ctx.errors().get(0)).contains("Invalid date range");
    assertThat(ctx.errors().get(0)).contains("startDate must be before endDate");
  }

  @Test
  void validateDateRange_should_pass_when_start_before_end() {
    Date start = new Date(1705276800000L); // 2024-01-15
    Date end = new Date(1705363200000L); // 2024-01-16

    ctx.validateDateRange(start, end, "startDate", "endDate");

    assertThat(ctx.isValid()).isTrue();
    assertThat(ctx.errors()).isEmpty();
  }

  @Test
  void validateDateRange_should_pass_when_start_is_null() {
    Date end = new Date(1705363200000L);

    ctx.validateDateRange(null, end, "startDate", "endDate");

    assertThat(ctx.isValid()).isTrue();
  }

  @Test
  void validateDateRange_should_pass_when_end_is_null() {
    Date start = new Date(1705276800000L);

    ctx.validateDateRange(start, null, "startDate", "endDate");

    assertThat(ctx.isValid()).isTrue();
  }

  @Test
  void validateDateRange_should_pass_when_both_null() {
    ctx.validateDateRange(null, null, "startDate", "endDate");

    assertThat(ctx.isValid()).isTrue();
  }

  // -- requireIfPresent tests --

  @Test
  void requireIfPresent_should_add_error_when_dependent_present_but_required_missing() {
    ctx.requireIfPresent("some value", "fieldA", null, "fieldB");

    assertThat(ctx.isValid()).isFalse();
    assertThat(ctx.errors()).containsExactly("fieldA requires fieldB to be specified");
  }

  @Test
  void requireIfPresent_should_add_error_when_required_is_blank() {
    ctx.requireIfPresent("some value", "fieldA", "   ", "fieldB");

    assertThat(ctx.isValid()).isFalse();
    assertThat(ctx.errors()).containsExactly("fieldA requires fieldB to be specified");
  }

  @Test
  void requireIfPresent_should_pass_when_both_present() {
    ctx.requireIfPresent("value1", "fieldA", "value2", "fieldB");

    assertThat(ctx.isValid()).isTrue();
  }

  @Test
  void requireIfPresent_should_pass_when_dependent_is_null() {
    ctx.requireIfPresent(null, "fieldA", null, "fieldB");

    assertThat(ctx.isValid()).isTrue();
  }

  @Test
  void requireIfPresent_should_pass_when_dependent_is_blank() {
    ctx.requireIfPresent("   ", "fieldA", null, "fieldB");

    assertThat(ctx.isValid()).isTrue();
  }

  // -- require tests --

  @Test
  void require_should_add_error_when_null() {
    ctx.require(null, "appId");

    assertThat(ctx.isValid()).isFalse();
    assertThat(ctx.errors()).containsExactly("appId is required");
  }

  @Test
  void require_should_add_error_when_blank() {
    ctx.require("   ", "appId");

    assertThat(ctx.isValid()).isFalse();
    assertThat(ctx.errors()).containsExactly("appId is required");
  }

  @Test
  void require_should_pass_when_value_present() {
    ctx.require("abc-123", "appId");

    assertThat(ctx.isValid()).isTrue();
    assertThat(ctx.errors()).isEmpty();
  }

  // -- requireUuid tests --

  @Test
  void requireUuid_should_add_error_when_null() {
    ctx.requireUuid(null, "vulnId");

    assertThat(ctx.isValid()).isFalse();
    assertThat(ctx.errors()).containsExactly("vulnId is required");
  }

  @Test
  void requireUuid_should_add_error_when_blank() {
    ctx.requireUuid("   ", "vulnId");

    assertThat(ctx.isValid()).isFalse();
    assertThat(ctx.errors()).containsExactly("vulnId is required");
  }

  @Test
  void requireUuid_should_add_error_when_invalid_format() {
    ctx.requireUuid("not-a-uuid", "vulnId");

    assertThat(ctx.isValid()).isFalse();
    assertThat(ctx.errors()).hasSize(1);
    assertThat(ctx.errors().get(0)).contains("UUID format");
  }

  @Test
  void requireUuid_should_pass_when_valid_uuid() {
    ctx.requireUuid("550e8400-e29b-41d4-a716-446655440000", "vulnId");

    assertThat(ctx.isValid()).isTrue();
    assertThat(ctx.errors()).isEmpty();
  }

  @Test
  void requireUuid_should_accept_uppercase_uuid() {
    ctx.requireUuid("550E8400-E29B-41D4-A716-446655440000", "vulnId");

    assertThat(ctx.isValid()).isTrue();
  }

  // -- requireAtLeastOne tests --

  @Test
  void requireAtLeastOne_should_add_error_when_all_null() {
    ctx.requireAtLeastOne("At least one filter required", null, null, null);

    assertThat(ctx.isValid()).isFalse();
    assertThat(ctx.errors()).containsExactly("At least one filter required");
  }

  @Test
  void requireAtLeastOne_should_add_error_when_all_blank() {
    ctx.requireAtLeastOne("At least one filter required", "", "   ", null);

    assertThat(ctx.isValid()).isFalse();
    assertThat(ctx.errors()).containsExactly("At least one filter required");
  }

  @Test
  void requireAtLeastOne_should_pass_when_one_present() {
    ctx.requireAtLeastOne("At least one filter required", null, "value", null);

    assertThat(ctx.isValid()).isTrue();
  }

  @Test
  void requireAtLeastOne_should_pass_when_multiple_present() {
    ctx.requireAtLeastOne("At least one filter required", "a", "b", "c");

    assertThat(ctx.isValid()).isTrue();
  }

  // -- warnIf tests --

  @Test
  void warnIf_should_add_warning_when_condition_true() {
    ctx.warnIf(true, "This is a warning");

    assertThat(ctx.isValid()).isTrue();
    assertThat(ctx.warnings()).containsExactly("This is a warning");
  }

  @Test
  void warnIf_should_not_add_warning_when_condition_false() {
    ctx.warnIf(false, "This is a warning");

    assertThat(ctx.warnings()).isEmpty();
  }

  // -- Multiple validations --

  @Test
  void should_collect_multiple_errors() {
    ctx.require(null, "appId");
    ctx.require(null, "vulnId");
    ctx.requireAtLeastOne("Need something", null, null);

    assertThat(ctx.isValid()).isFalse();
    assertThat(ctx.errors()).hasSize(3);
  }

  @Test
  void should_collect_multiple_warnings() {
    ctx.warnIf(true, "Warning 1");
    ctx.warnIf(true, "Warning 2");

    assertThat(ctx.isValid()).isTrue();
    assertThat(ctx.warnings()).containsExactly("Warning 1", "Warning 2");
  }

  @Test
  void should_collect_both_errors_and_warnings() {
    ctx.warnIf(true, "A warning");
    ctx.require(null, "appId");

    assertThat(ctx.isValid()).isFalse();
    assertThat(ctx.errors()).hasSize(1);
    assertThat(ctx.warnings()).hasSize(1);
  }
}
