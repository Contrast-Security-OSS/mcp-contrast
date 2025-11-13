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

import static org.junit.jupiter.api.Assertions.*;

import com.contrast.labs.ai.mcp.contrast.PaginationParams;
import com.contrast.labs.ai.mcp.contrast.data.PaginatedResponse;
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
    List<String> items = List.of("item1", "item2", "item3");
    PaginationParams params = PaginationParams.of(1, 3);

    PaginatedResponse<String> response = handler.createPaginatedResponse(items, params, 10);

    assertEquals(3, response.items().size());
    assertEquals(1, response.page());
    assertEquals(3, response.pageSize());
    assertEquals(10, response.totalItems());
    assertTrue(response.hasMorePages(), "Should have more pages when 3 items fetched out of 10");
    assertNull(response.message(), "No message for successful page");
  }

  @Test
  void testCreatePaginatedResponse_withTotalCount_lastPage() {
    List<String> items = List.of("item8", "item9", "item10");
    PaginationParams params = PaginationParams.of(4, 3);

    PaginatedResponse<String> response = handler.createPaginatedResponse(items, params, 10);

    assertEquals(3, response.items().size());
    assertEquals(4, response.page());
    assertEquals(3, response.pageSize());
    assertEquals(10, response.totalItems());
    assertFalse(
        response.hasMorePages(), "Should not have more pages (page 4 * pageSize 3 = 12 >= 10)");
    assertNull(response.message());
  }

  @Test
  void testCreatePaginatedResponse_withoutTotalCount_fullPage() {
    List<String> items = List.of("item1", "item2", "item3");
    PaginationParams params = PaginationParams.of(1, 3);

    PaginatedResponse<String> response = handler.createPaginatedResponse(items, params, null);

    assertEquals(3, response.items().size());
    assertNull(response.totalItems());
    assertTrue(
        response.hasMorePages(),
        "Full page without totalCount suggests more pages exist (heuristic)");
  }

  @Test
  void testCreatePaginatedResponse_withoutTotalCount_partialPage() {
    List<String> items = List.of("item1", "item2");
    PaginationParams params = PaginationParams.of(1, 3);

    PaginatedResponse<String> response = handler.createPaginatedResponse(items, params, null);

    assertEquals(2, response.items().size());
    assertNull(response.totalItems());
    assertFalse(response.hasMorePages(), "Partial page suggests no more pages (heuristic)");
  }

  @Test
  void testCreatePaginatedResponse_emptyFirstPage() {
    List<String> items = List.of();
    PaginationParams params = PaginationParams.of(1, 10);

    PaginatedResponse<String> response = handler.createPaginatedResponse(items, params, 0);

    assertTrue(response.items().isEmpty());
    assertEquals(0, response.totalItems());
    assertFalse(response.hasMorePages());
    assertEquals("No items found.", response.message());
  }

  @Test
  void testCreatePaginatedResponse_emptySecondPage_withTotalCount() {
    List<String> items = List.of();
    PaginationParams params = PaginationParams.of(2, 10);

    PaginatedResponse<String> response = handler.createPaginatedResponse(items, params, 5);

    assertTrue(response.items().isEmpty());
    assertEquals(5, response.totalItems());
    assertFalse(response.hasMorePages());
    assertEquals("Requested page 2 exceeds available pages (total: 1).", response.message());
  }

  @Test
  void testCreatePaginatedResponse_emptySecondPage_withoutTotalCount() {
    List<String> items = List.of();
    PaginationParams params = PaginationParams.of(2, 10);

    PaginatedResponse<String> response = handler.createPaginatedResponse(items, params, null);

    assertTrue(response.items().isEmpty());
    assertNull(response.totalItems());
    assertFalse(response.hasMorePages());
    assertEquals("Requested page 2 returned no results.", response.message());
  }

  @Test
  void testCreatePaginatedResponse_mergesWarnings() {
    List<String> items = List.of("item1");
    // Create params with warning (invalid page clamped to 1)
    PaginationParams params = PaginationParams.of(-5, 10);

    PaginatedResponse<String> response = handler.createPaginatedResponse(items, params, 1);

    assertNotNull(response.message());
    assertTrue(
        response.message().contains("Invalid page number -5"),
        "Should include pagination warning in message");
  }

  @Test
  void testCreatePaginatedResponse_mergesWarningsWithEmptyMessage() {
    List<String> items = List.of();
    // Create params with warning (invalid pageSize)
    PaginationParams params = PaginationParams.of(2, 200);

    PaginatedResponse<String> response = handler.createPaginatedResponse(items, params, 5);

    assertNotNull(response.message());
    assertTrue(
        response.message().contains("Requested pageSize 200 exceeds maximum 100"),
        "Should include pageSize warning");
    assertTrue(
        response.message().contains("Requested page 2 exceeds available pages"),
        "Should include empty page message");
  }

  // ========== Edge Cases ==========

  @Test
  void testHasMorePages_boundaryCase() {
    // Exactly 20 items, page 2, pageSize 10
    // Page 2 * 10 = 20, which equals totalItems, so no more pages
    List<String> items = List.of("item10", "item11");
    PaginationParams params = PaginationParams.of(2, 10);

    PaginatedResponse<String> response = handler.createPaginatedResponse(items, params, 20);

    assertFalse(response.hasMorePages(), "No more pages when page*pageSize == totalItems");
  }

  @Test
  void testHasMorePages_justOverBoundary() {
    // 21 items, page 2, pageSize 10
    // Page 2 * 10 = 20 < 21, so more pages exist
    List<String> items = List.of("item10", "item11");
    PaginationParams params = PaginationParams.of(2, 10);

    PaginatedResponse<String> response = handler.createPaginatedResponse(items, params, 21);

    assertTrue(response.hasMorePages(), "More pages when page*pageSize < totalItems");
  }

  @Test
  void testMessagePriority_warningAndEmpty() {
    // Both warning and empty message should appear
    List<String> items = List.of();
    PaginationParams params = PaginationParams.of(-1, 10); // Invalid page

    PaginatedResponse<String> response = handler.createPaginatedResponse(items, params, 0);

    assertNotNull(response.message());
    assertTrue(response.message().contains("Invalid page number"));
    assertTrue(response.message().contains("No items found"));
  }

  // ========== Helper Methods ==========

  private List<String> createItems(int count) {
    return IntStream.range(0, count).mapToObj(i -> "item" + i).collect(Collectors.toList());
  }
}
