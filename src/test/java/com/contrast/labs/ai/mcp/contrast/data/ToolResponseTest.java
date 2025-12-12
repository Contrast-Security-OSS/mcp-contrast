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

/** Tests for ToolResponse record. */
class ToolResponseTest {

  // ========== Compact Constructor Tests ==========

  @Test
  void compactConstructor_should_ensure_non_null_errors() {
    var response = new ToolResponse<>("data", null, List.of());

    assertThat(response.errors()).isNotNull();
    assertThat(response.errors()).isEmpty();
  }

  @Test
  void compactConstructor_should_ensure_non_null_warnings() {
    var response = new ToolResponse<>("data", List.of(), null);

    assertThat(response.warnings()).isNotNull();
    assertThat(response.warnings()).isEmpty();
  }

  @Test
  void compactConstructor_should_allow_null_data() {
    var response = new ToolResponse<String>(null, List.of("error"), List.of());

    assertThat(response.data()).isNull();
    assertThat(response.errors()).containsExactly("error");
  }

  // ========== isSuccess Tests ==========

  @Test
  void isSuccess_should_return_true_when_no_errors() {
    var response = new ToolResponse<>("data", List.of(), List.of("warning"));

    assertThat(response.isSuccess()).isTrue();
  }

  @Test
  void isSuccess_should_return_false_when_errors_present() {
    var response = new ToolResponse<>("data", List.of("error"), List.of());

    assertThat(response.isSuccess()).isFalse();
  }

  // ========== Factory Method Tests ==========

  @Test
  void success_should_create_response_with_data_and_warnings() {
    var data = "result";
    var warnings = List.of("Using defaults");

    var response = ToolResponse.success(data, warnings);

    assertThat(response.data()).isEqualTo(data);
    assertThat(response.errors()).isEmpty();
    assertThat(response.warnings()).isEqualTo(warnings);
    assertThat(response.isSuccess()).isTrue();
  }

  @Test
  void success_should_create_response_with_data_only() {
    var data = 42;

    var response = ToolResponse.success(data);

    assertThat(response.data()).isEqualTo(data);
    assertThat(response.errors()).isEmpty();
    assertThat(response.warnings()).isEmpty();
    assertThat(response.isSuccess()).isTrue();
  }

  @Test
  void error_should_create_response_with_error_list() {
    var errorList = List.of("Invalid ID", "Resource not found");

    var response = ToolResponse.<String>error(errorList);

    assertThat(response.data()).isNull();
    assertThat(response.errors()).isEqualTo(errorList);
    assertThat(response.warnings()).isEmpty();
    assertThat(response.isSuccess()).isFalse();
  }

  @Test
  void error_should_create_response_with_single_error() {
    var response = ToolResponse.<String>error("Something went wrong");

    assertThat(response.data()).isNull();
    assertThat(response.errors()).containsExactly("Something went wrong");
    assertThat(response.warnings()).isEmpty();
    assertThat(response.isSuccess()).isFalse();
  }

  // ========== Record Equality Tests ==========

  @Test
  void equality_should_compare_all_fields() {
    var response1 = new ToolResponse<>("data", List.of(), List.of("w"));
    var response2 = new ToolResponse<>("data", List.of(), List.of("w"));
    var response3 = new ToolResponse<>("other", List.of(), List.of("w"));

    assertThat(response1).isEqualTo(response2);
    assertThat(response1).isNotEqualTo(response3);
  }

  // ========== Generic Type Tests ==========

  @Test
  void should_work_with_complex_types() {
    record VulnDetail(String id, String title) {}

    var detail = new VulnDetail("vuln-123", "SQL Injection");
    var response = ToolResponse.success(detail);

    assertThat(response.data()).isEqualTo(detail);
    assertThat(response.data().id()).isEqualTo("vuln-123");
    assertThat(response.isSuccess()).isTrue();
  }
}
