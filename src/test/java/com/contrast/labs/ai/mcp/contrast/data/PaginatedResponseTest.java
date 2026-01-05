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

class PaginatedResponseTest {

  // ========== success() factory method tests ==========

  @Test
  void success_should_createResponseWithEmptyErrors() {
    var response = PaginatedResponse.success(List.of("item1"), 1, 50, 1, false, List.of(), 100L);

    assertThat(response.items()).containsExactly("item1");
    assertThat(response.errors()).isEmpty();
    assertThat(response.warnings()).isEmpty();
    assertThat(response.isSuccess()).isTrue();
    assertThat(response.durationMs()).isEqualTo(100L);
  }

  @Test
  void success_should_includeWarnings() {
    var response =
        PaginatedResponse.success(
            List.of("item1"), 1, 50, 1, false, List.of("Applied default status"), 100L);

    assertThat(response.items()).containsExactly("item1");
    assertThat(response.errors()).isEmpty();
    assertThat(response.warnings()).containsExactly("Applied default status");
    assertThat(response.isSuccess()).isTrue();
  }

  // ========== validationError() factory method tests ==========

  @Test
  void validationError_should_createResponseWithErrors() {
    var response =
        PaginatedResponse.validationError(
            1, 50, List.of("Invalid severity: CRIT", "Invalid status: UNKNOWN"));

    assertThat(response.items()).isEmpty();
    assertThat(response.errors())
        .containsExactly("Invalid severity: CRIT", "Invalid status: UNKNOWN");
    assertThat(response.warnings()).isEmpty();
    assertThat(response.isSuccess()).isFalse();
    assertThat(response.totalItems()).isEqualTo(0);
    assertThat(response.durationMs()).isNull();
  }

  // ========== error() single-error convenience method tests ==========

  @Test
  void error_should_createSingleErrorResponse() {
    var response = PaginatedResponse.error(2, 25, "Page 2 exceeds available pages");

    assertThat(response.items()).isEmpty();
    assertThat(response.errors()).containsExactly("Page 2 exceeds available pages");
    assertThat(response.warnings()).isEmpty();
    assertThat(response.isSuccess()).isFalse();
    assertThat(response.page()).isEqualTo(2);
    assertThat(response.pageSize()).isEqualTo(25);
  }

  // ========== empty() factory method tests ==========

  @Test
  void empty_should_createEmptyResponseWithWarning() {
    var response = PaginatedResponse.empty(1, 50, "No items found.");

    assertThat(response.items()).isEmpty();
    assertThat(response.errors()).isEmpty();
    assertThat(response.warnings()).containsExactly("No items found.");
    assertThat(response.isSuccess()).isTrue();
  }

  // ========== isSuccess() tests ==========

  @Test
  void isSuccess_should_returnFalseWhenErrorsPresent() {
    var response = PaginatedResponse.error(1, 50, "Error occurred");

    assertThat(response.isSuccess()).isFalse();
  }

  @Test
  void isSuccess_should_returnTrueWhenNoErrors() {
    var response = PaginatedResponse.success(List.of(), 1, 50, 0, false, List.of("warning"), null);

    assertThat(response.isSuccess()).isTrue();
  }

  // ========== Deprecated message() method tests ==========

  @Test
  @SuppressWarnings("deprecation")
  void message_deprecated_should_returnErrors() {
    var response = PaginatedResponse.validationError(1, 50, List.of("Error 1", "Error 2"));

    assertThat(response.message()).isEqualTo("Error 1 Error 2");
  }

  @Test
  @SuppressWarnings("deprecation")
  void message_deprecated_should_returnWarningsWhenNoErrors() {
    var response =
        PaginatedResponse.success(
            List.of(), 1, 50, 0, false, List.of("Warning 1", "Warning 2"), null);

    assertThat(response.message()).isEqualTo("Warning 1 Warning 2");
  }

  @Test
  @SuppressWarnings("deprecation")
  void message_deprecated_should_returnNullWhenNoMessagesExist() {
    var response = PaginatedResponse.success(List.of("item"), 1, 50, 1, false, List.of(), 100L);

    assertThat(response.message()).isNull();
  }

  // ========== Compact constructor tests ==========

  @Test
  void compactConstructor_should_ensureNonNullLists_whenItemsNull() {
    var response = new PaginatedResponse<String>(null, 1, 50, 0, false, List.of(), List.of(), null);

    assertThat(response.items()).isNotNull().isEmpty();
  }

  @Test
  void compactConstructor_should_ensureNonNullLists_whenErrorsNull() {
    var response = new PaginatedResponse<>(List.of(), 1, 50, 0, false, null, List.of(), null);

    assertThat(response.errors()).isNotNull().isEmpty();
  }

  @Test
  void compactConstructor_should_ensureNonNullLists_whenWarningsNull() {
    var response = new PaginatedResponse<>(List.of(), 1, 50, 0, false, List.of(), null, null);

    assertThat(response.warnings()).isNotNull().isEmpty();
  }

  @Test
  void compactConstructor_should_makeListsImmutable() {
    var mutableItems = new java.util.ArrayList<String>();
    mutableItems.add("item1");

    var response =
        new PaginatedResponse<>(mutableItems, 1, 50, 1, false, List.of(), List.of(), null);

    // Modify original list
    mutableItems.add("item2");

    // Response should not be affected
    assertThat(response.items()).containsExactly("item1");
  }

  // ========== Pagination metadata tests ==========

  @Test
  void should_preservePaginationMetadata() {
    var response =
        PaginatedResponse.success(List.of("a", "b", "c"), 3, 25, 100, true, List.of(), 150L);

    assertThat(response.page()).isEqualTo(3);
    assertThat(response.pageSize()).isEqualTo(25);
    assertThat(response.totalItems()).isEqualTo(100);
    assertThat(response.hasMorePages()).isTrue();
    assertThat(response.durationMs()).isEqualTo(150L);
  }

  // ========== Conditional defensive copying optimization tests ==========

  @Test
  void compactConstructor_should_reuseImmutableList_whenItemsFromListOf() {
    var immutableItems = List.of("item1", "item2");
    var response =
        new PaginatedResponse<>(immutableItems, 1, 50, 2, false, List.of(), List.of(), null);

    // Same instance should be reused (no defensive copy needed)
    assertThat(response.items()).isSameAs(immutableItems);
  }

  @Test
  void compactConstructor_should_reuseImmutableList_whenErrorsFromListOf() {
    var immutableErrors = List.of("error1", "error2");
    var response =
        new PaginatedResponse<>(List.of(), 1, 50, 0, false, immutableErrors, List.of(), null);

    assertThat(response.errors()).isSameAs(immutableErrors);
  }

  @Test
  void compactConstructor_should_reuseImmutableList_whenWarningsFromListOf() {
    var immutableWarnings = List.of("warning1", "warning2");
    var response =
        new PaginatedResponse<>(List.of(), 1, 50, 0, false, List.of(), immutableWarnings, null);

    assertThat(response.warnings()).isSameAs(immutableWarnings);
  }

  @Test
  void compactConstructor_should_copyArrayList_forItems() {
    var mutableItems = new java.util.ArrayList<String>();
    mutableItems.add("item1");

    var response =
        new PaginatedResponse<>(mutableItems, 1, 50, 1, false, List.of(), List.of(), null);

    // Different instance (defensive copy made)
    assertThat(response.items()).isNotSameAs(mutableItems);
    // But same contents
    assertThat(response.items()).containsExactly("item1");
  }

  @Test
  void compactConstructor_should_copyArrayList_forErrors() {
    var mutableErrors = new java.util.ArrayList<String>();
    mutableErrors.add("error1");

    var response =
        new PaginatedResponse<>(List.of(), 1, 50, 0, false, mutableErrors, List.of(), null);

    assertThat(response.errors()).isNotSameAs(mutableErrors);
    assertThat(response.errors()).containsExactly("error1");
  }

  @Test
  void compactConstructor_should_copyArrayList_forWarnings() {
    var mutableWarnings = new java.util.ArrayList<String>();
    mutableWarnings.add("warning1");

    var response =
        new PaginatedResponse<>(List.of(), 1, 50, 0, false, List.of(), mutableWarnings, null);

    assertThat(response.warnings()).isNotSameAs(mutableWarnings);
    assertThat(response.warnings()).containsExactly("warning1");
  }

  @Test
  void compactConstructor_should_reuseList_fromStreamToList() {
    var streamList = java.util.stream.Stream.of("a", "b", "c").toList();
    var response = new PaginatedResponse<>(streamList, 1, 50, 3, false, List.of(), List.of(), null);

    // toList() returns immutable list, should be reused
    assertThat(response.items()).isSameAs(streamList);
  }
}
