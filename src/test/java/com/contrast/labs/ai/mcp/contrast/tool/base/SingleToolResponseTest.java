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
package com.contrast.labs.ai.mcp.contrast.tool.base;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SingleToolResponseTest {

  @Test
  void success_should_create_response_with_data_and_warnings() {
    var warnings = List.of("warning1", "warning2");

    var response = SingleToolResponse.success("data", warnings);

    assertThat(response.data()).isEqualTo("data");
    assertThat(response.warnings()).containsExactly("warning1", "warning2");
    assertThat(response.errors()).isEmpty();
    assertThat(response.found()).isTrue();
    assertThat(response.isSuccess()).isTrue();
  }

  @Test
  void success_should_create_response_with_data_only() {
    var response = SingleToolResponse.success("data");

    assertThat(response.data()).isEqualTo("data");
    assertThat(response.warnings()).isEmpty();
    assertThat(response.errors()).isEmpty();
    assertThat(response.found()).isTrue();
    assertThat(response.isSuccess()).isTrue();
  }

  @Test
  void notFound_should_create_response_with_message_in_warnings() {
    var existingWarnings = List.of("existing");

    var response = SingleToolResponse.<String>notFound("Item not found", existingWarnings);

    assertThat(response.data()).isNull();
    assertThat(response.warnings()).containsExactly("existing", "Item not found");
    assertThat(response.errors()).isEmpty();
    assertThat(response.found()).isFalse();
    assertThat(response.isSuccess()).isTrue(); // No errors = success
  }

  @Test
  void notFound_should_work_with_empty_warnings() {
    var response = SingleToolResponse.<String>notFound("Not found", List.of());

    assertThat(response.warnings()).containsExactly("Not found");
    assertThat(response.found()).isFalse();
  }

  @Test
  void notFound_should_handle_null_warnings() {
    var response = SingleToolResponse.<String>notFound("Not found", null);

    assertThat(response.warnings()).containsExactly("Not found");
    assertThat(response.found()).isFalse();
    assertThat(response.isSuccess()).isTrue();
  }

  @Test
  void error_should_create_response_with_single_error() {
    var response = SingleToolResponse.<String>error("Something went wrong");

    assertThat(response.data()).isNull();
    assertThat(response.errors()).containsExactly("Something went wrong");
    assertThat(response.warnings()).isEmpty();
    assertThat(response.found()).isFalse();
    assertThat(response.isSuccess()).isFalse();
  }

  @Test
  void error_should_create_response_with_multiple_errors() {
    var errors = List.of("error1", "error2");

    var response = SingleToolResponse.<String>error(errors);

    assertThat(response.data()).isNull();
    assertThat(response.errors()).containsExactly("error1", "error2");
    assertThat(response.warnings()).isEmpty();
    assertThat(response.found()).isFalse();
    assertThat(response.isSuccess()).isFalse();
  }

  @Test
  void constructor_should_handle_null_errors() {
    var response = new SingleToolResponse<>("data", null, List.of("warn"), true);

    assertThat(response.errors()).isEmpty();
    assertThat(response.warnings()).containsExactly("warn");
  }

  @Test
  void constructor_should_handle_null_warnings() {
    var response = new SingleToolResponse<>("data", List.of("err"), null, true);

    assertThat(response.errors()).containsExactly("err");
    assertThat(response.warnings()).isEmpty();
  }

  @Test
  void errors_should_be_immutable() {
    var response = SingleToolResponse.<String>error("error");

    assertThatThrownBy(() -> response.errors().add("new error"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void warnings_should_be_immutable() {
    var response = SingleToolResponse.success("data", List.of("warning"));

    assertThatThrownBy(() -> response.warnings().add("new warning"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void errors_should_be_defensive_copy() {
    var mutableErrors = new ArrayList<>(List.of("error1"));
    var response = new SingleToolResponse<>("data", mutableErrors, List.of(), true);

    mutableErrors.add("error2");

    assertThat(response.errors()).containsExactly("error1");
  }

  @Test
  void warnings_should_be_defensive_copy() {
    var mutableWarnings = new ArrayList<>(List.of("warning1"));
    var response = new SingleToolResponse<>("data", List.of(), mutableWarnings, true);

    mutableWarnings.add("warning2");

    assertThat(response.warnings()).containsExactly("warning1");
  }

  @Test
  void isSuccess_should_return_true_when_no_errors() {
    var response = new SingleToolResponse<>("data", List.of(), List.of("warning"), true);

    assertThat(response.isSuccess()).isTrue();
  }

  @Test
  void isSuccess_should_return_false_when_errors_exist() {
    var response = new SingleToolResponse<>("data", List.of("error"), List.of(), true);

    assertThat(response.isSuccess()).isFalse();
  }

  @Test
  void found_should_distinguish_not_found_from_error() {
    var notFoundResponse = SingleToolResponse.<String>notFound("Not found", List.of());
    var errorResponse = SingleToolResponse.<String>error("Error occurred");

    // Both have found=false, but different semantics
    assertThat(notFoundResponse.found()).isFalse();
    assertThat(errorResponse.found()).isFalse();

    // Not found is still "success" (no errors), error is not
    assertThat(notFoundResponse.isSuccess()).isTrue();
    assertThat(errorResponse.isSuccess()).isFalse();
  }
}
