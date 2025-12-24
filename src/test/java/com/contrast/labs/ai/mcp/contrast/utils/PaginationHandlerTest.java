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

import com.contrast.labs.ai.mcp.contrast.PaginationParams;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests for PaginationHandler component. */
class PaginationHandlerTest {

  private PaginationHandler handler;

  @BeforeEach
  void setUp() {
    handler = new PaginationHandler();
  }

  // ========== createPaginatedResponse Tests ==========

  @Test
  void testCreatePaginatedResponse_withTotalCount_firstPage() {
    var items = List.of("item1", "item2", "item3");
    var params = PaginationParams.of(1, 3);

    var response = handler.createPaginatedResponse(items, params, 10);

    assertThat(response.items().size()).isEqualTo(3);
    assertThat(response.page()).isEqualTo(1);
    assertThat(response.pageSize()).isEqualTo(3);
    assertThat(response.totalItems()).isEqualTo(10);
    assertThat(response.hasMorePages())
        .as("Should have more pages when 3 items fetched out of 10")
        .isTrue();
    assertThat(response.errors()).as("No errors for successful page").isEmpty();
    assertThat(response.warnings()).as("No warnings for successful page").isEmpty();
  }

  @Test
  void testCreatePaginatedResponse_withTotalCount_lastPage() {
    var items = List.of("item8", "item9", "item10");
    var params = PaginationParams.of(4, 3);

    var response = handler.createPaginatedResponse(items, params, 10);

    assertThat(response.items().size()).isEqualTo(3);
    assertThat(response.page()).isEqualTo(4);
    assertThat(response.pageSize()).isEqualTo(3);
    assertThat(response.totalItems()).isEqualTo(10);
    assertThat(response.hasMorePages())
        .as("Should not have more pages (page 4 * pageSize 3 = 12 >= 10)")
        .isFalse();
    assertThat(response.errors()).isEmpty();
    assertThat(response.warnings()).isEmpty();
  }

  @Test
  void testCreatePaginatedResponse_withoutTotalCount_fullPage() {
    var items = List.of("item1", "item2", "item3");
    var params = PaginationParams.of(1, 3);

    var response = handler.createPaginatedResponse(items, params, null);

    assertThat(response.items().size()).isEqualTo(3);
    assertThat(response.totalItems()).isNull();
    assertThat(response.hasMorePages())
        .as("Full page without totalCount suggests more pages exist (heuristic)")
        .isTrue();
  }

  @Test
  void testCreatePaginatedResponse_withoutTotalCount_partialPage() {
    var items = List.of("item1", "item2");
    var params = PaginationParams.of(1, 3);

    var response = handler.createPaginatedResponse(items, params, null);

    assertThat(response.items().size()).isEqualTo(2);
    assertThat(response.totalItems()).isNull();
    assertThat(response.hasMorePages())
        .as("Partial page suggests no more pages (heuristic)")
        .isFalse();
  }

  @Test
  void testCreatePaginatedResponse_emptyFirstPage() {
    var items = List.of();
    var params = PaginationParams.of(1, 10);

    var response = handler.createPaginatedResponse(items, params, 0);

    assertThat(response.items()).isEmpty();
    assertThat(response.totalItems()).isEqualTo(0);
    assertThat(response.hasMorePages()).isFalse();
    assertThat(response.warnings()).containsExactly("No items found.");
  }

  @Test
  void testCreatePaginatedResponse_emptySecondPage_withTotalCount() {
    var items = List.of();
    var params = PaginationParams.of(2, 10);

    var response = handler.createPaginatedResponse(items, params, 5);

    assertThat(response.items()).isEmpty();
    assertThat(response.totalItems()).isEqualTo(5);
    assertThat(response.hasMorePages()).isFalse();
    assertThat(response.warnings())
        .anyMatch(w -> w.contains("Requested page 2 exceeds available pages (total: 1)"));
  }

  @Test
  void testCreatePaginatedResponse_emptySecondPage_withoutTotalCount() {
    var items = List.of();
    var params = PaginationParams.of(2, 10);

    var response = handler.createPaginatedResponse(items, params, null);

    assertThat(response.items()).isEmpty();
    assertThat(response.totalItems()).isNull();
    assertThat(response.hasMorePages()).isFalse();
    assertThat(response.warnings())
        .anyMatch(w -> w.contains("Requested page 2 returned no results"));
  }

  @Test
  void testCreatePaginatedResponse_mergesWarnings() {
    var items = List.of("item1");
    // Create params with warning (invalid page clamped to 1)
    var params = PaginationParams.of(-5, 10);

    var response = handler.createPaginatedResponse(items, params, 1);

    assertThat(response.warnings()).isNotEmpty();
    assertThat(response.warnings())
        .as("Should include pagination warning")
        .anyMatch(w -> w.contains("Invalid page number -5"));
  }

  @Test
  void testCreatePaginatedResponse_mergesWarningsWithEmptyMessage() {
    var items = List.of();
    // Create params with warning (invalid pageSize)
    var params = PaginationParams.of(2, 200);

    var response = handler.createPaginatedResponse(items, params, 5);

    assertThat(response.warnings()).isNotEmpty();
    assertThat(response.warnings())
        .as("Should include pageSize warning")
        .anyMatch(w -> w.contains("Requested pageSize 200 exceeds maximum 100"));
    assertThat(response.warnings())
        .as("Should include empty page message")
        .anyMatch(w -> w.contains("Requested page 2 exceeds available pages"));
  }

  // ========== Edge Cases ==========

  @Test
  void testHasMorePages_boundaryCase() {
    // Exactly 20 items, page 2, pageSize 10
    // Page 2 * 10 = 20, which equals totalItems, so no more pages
    var items = List.of("item10", "item11");
    var params = PaginationParams.of(2, 10);

    var response = handler.createPaginatedResponse(items, params, 20);

    assertThat(response.hasMorePages())
        .as("No more pages when page*pageSize == totalItems")
        .isFalse();
  }

  @Test
  void testHasMorePages_justOverBoundary() {
    // 21 items, page 2, pageSize 10
    // Page 2 * 10 = 20 < 21, so more pages exist
    var items = List.of("item10", "item11");
    var params = PaginationParams.of(2, 10);

    var response = handler.createPaginatedResponse(items, params, 21);

    assertThat(response.hasMorePages()).as("More pages when page*pageSize < totalItems").isTrue();
  }

  @Test
  void testMessagePriority_warningAndEmpty() {
    // Both warning and empty message should appear
    var items = List.of();
    var params = PaginationParams.of(-1, 10); // Invalid page

    var response = handler.createPaginatedResponse(items, params, 0);

    assertThat(response.warnings()).isNotEmpty();
    assertThat(response.warnings()).anyMatch(w -> w.contains("Invalid page number"));
    assertThat(response.warnings()).anyMatch(w -> w.contains("No items found"));
  }

  // ========== Helper Methods ==========

  private List<String> createItems(int count) {
    return IntStream.range(0, count).mapToObj(i -> "item" + i).collect(Collectors.toList());
  }
}
