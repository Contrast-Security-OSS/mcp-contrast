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
package com.contrast.labs.ai.mcp.contrast.data;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Tests for PaginatedResponse record. */
class PaginatedResponseTest {

  // ========== Compact Constructor Tests ==========

  @Test
  void compactConstructor_should_ensure_non_null_items() {
    var response = new PaginatedResponse<String>(null, 1, 10, 0, false, List.of(), List.of());

    assertThat(response.items()).isNotNull();
    assertThat(response.items()).isEmpty();
  }

  @Test
  void compactConstructor_should_ensure_non_null_errors() {
    var response = new PaginatedResponse<>(List.of("item"), 1, 10, 1, false, null, List.of());

    assertThat(response.errors()).isNotNull();
    assertThat(response.errors()).isEmpty();
  }

  @Test
  void compactConstructor_should_ensure_non_null_warnings() {
    var response = new PaginatedResponse<>(List.of("item"), 1, 10, 1, false, List.of(), null);

    assertThat(response.warnings()).isNotNull();
    assertThat(response.warnings()).isEmpty();
  }

  // ========== Factory Method Tests ==========

  @Test
  void success_should_create_response_with_items_and_warnings() {
    var items = List.of("item1", "item2");
    var warnings = List.of("Using defaults");

    var response = PaginatedResponse.success(items, 1, 10, 50, warnings);

    assertThat(response.items()).isEqualTo(items);
    assertThat(response.page()).isEqualTo(1);
    assertThat(response.pageSize()).isEqualTo(10);
    assertThat(response.totalItems()).isEqualTo(50);
    assertThat(response.hasMorePages()).isTrue();
    assertThat(response.errors()).isEmpty();
    assertThat(response.warnings()).isEqualTo(warnings);
  }

  @Test
  void success_should_calculate_hasMorePages_correctly() {
    // Total 30 items, page 1, pageSize 10: (1*10) < 30 = true
    var response1 = PaginatedResponse.success(List.of("item"), 1, 10, 30, List.of());
    assertThat(response1.hasMorePages()).isTrue();

    // Total 30 items, page 3, pageSize 10: (3*10) < 30 = false
    var response2 = PaginatedResponse.success(List.of("item"), 3, 10, 30, List.of());
    assertThat(response2.hasMorePages()).isFalse();

    // Total null: hasMorePages = false (can't calculate)
    var response3 = PaginatedResponse.success(List.of("item"), 1, 10, null, List.of());
    assertThat(response3.hasMorePages()).isFalse();
  }

  @Test
  void errors_should_create_response_with_error_list() {
    var errorList = List.of("Invalid severity: INVALID", "Missing required field");

    var response = PaginatedResponse.<String>errors(2, 25, errorList);

    assertThat(response.items()).isEmpty();
    assertThat(response.page()).isEqualTo(2);
    assertThat(response.pageSize()).isEqualTo(25);
    assertThat(response.totalItems()).isEqualTo(0);
    assertThat(response.hasMorePages()).isFalse();
    assertThat(response.errors()).isEqualTo(errorList);
    assertThat(response.warnings()).isEmpty();
  }

  @Test
  void empty_should_create_response_with_warning_reason() {
    var response = PaginatedResponse.<String>empty(1, 50, "No vulnerabilities found");

    assertThat(response.items()).isEmpty();
    assertThat(response.page()).isEqualTo(1);
    assertThat(response.pageSize()).isEqualTo(50);
    assertThat(response.totalItems()).isEqualTo(0);
    assertThat(response.hasMorePages()).isFalse();
    assertThat(response.errors()).isEmpty();
    assertThat(response.warnings()).containsExactly("No vulnerabilities found");
  }

  @Test
  @SuppressWarnings("deprecation")
  void error_should_delegate_to_errors() {
    var response = PaginatedResponse.<String>error(1, 10, "Something went wrong");

    assertThat(response.errors()).containsExactly("Something went wrong");
    assertThat(response.warnings()).isEmpty();
  }

  // ========== Deprecated message() Tests ==========

  @Test
  @SuppressWarnings("deprecation")
  void message_should_return_joined_errors_when_present() {
    var response =
        new PaginatedResponse<>(
            List.of(), 1, 10, 0, false, List.of("Error 1", "Error 2"), List.of("Warning 1"));

    assertThat(response.message()).isEqualTo("Error 1; Error 2");
  }

  @Test
  @SuppressWarnings("deprecation")
  void message_should_return_joined_warnings_when_no_errors() {
    var response =
        new PaginatedResponse<>(List.of(), 1, 10, 0, false, List.of(), List.of("Warn 1", "Warn 2"));

    assertThat(response.message()).isEqualTo("Warn 1; Warn 2");
  }

  @Test
  @SuppressWarnings("deprecation")
  void message_should_return_null_when_no_errors_or_warnings() {
    var response = new PaginatedResponse<>(List.of("item"), 1, 10, 1, false, List.of(), List.of());

    assertThat(response.message()).isNull();
  }

  // ========== Record Equality Tests ==========

  @Test
  void equality_should_compare_all_fields() {
    var response1 = new PaginatedResponse<>(List.of("a"), 1, 10, 1, false, List.of(), List.of("w"));
    var response2 = new PaginatedResponse<>(List.of("a"), 1, 10, 1, false, List.of(), List.of("w"));
    var response3 = new PaginatedResponse<>(List.of("b"), 1, 10, 1, false, List.of(), List.of("w"));

    assertThat(response1).isEqualTo(response2);
    assertThat(response1).isNotEqualTo(response3);
  }
}
