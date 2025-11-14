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

import com.contrast.labs.ai.mcp.contrast.data.PaginatedResponse;

/**
 * Reusable test utilities for validating paginated responses. Provides assertion methods to verify
 * pagination behavior across all endpoints.
 */
public class PaginationTestHelper {

  /** Asserts that a paginated response has valid structure and values */
  public static <T> void assertValidPaginatedResponse(
      PaginatedResponse<T> response, int expectedPage, int expectedPageSize) {
    assertNotNull(response, "Response should not be null");
    assertNotNull(response.items(), "Items list should not be null");
    assertEquals(expectedPage, response.page(), "Page mismatch");
    assertEquals(expectedPageSize, response.pageSize(), "Page size mismatch");
    assertTrue(response.page() >= 1, "Page should be 1-based");
    assertTrue(response.pageSize() >= 1 && response.pageSize() <= 100, "Page size should be 1-100");

    // Items should not exceed pageSize
    assertTrue(response.items().size() <= response.pageSize(), "Items count exceeds pageSize");

    // If totalItems is present, validate hasMorePages
    if (response.totalItems() != null) {
      boolean expectedHasMore = (response.page() * response.pageSize()) < response.totalItems();
      assertEquals(
          expectedHasMore, response.hasMorePages(), "hasMorePages inconsistent with totalItems");
    }

    // If empty, totalItems should be 0 or null
    if (response.items().isEmpty() && response.totalItems() != null) {
      assertTrue(response.totalItems() >= 0, "TotalItems should be non-negative");
    }
  }

  /** Asserts that response contains a validation message */
  public static <T> void assertHasValidationMessage(PaginatedResponse<T> response) {
    assertNotNull(response.message(), "Expected validation message");
    assertFalse(response.message().isEmpty(), "Message should not be empty");
  }

  /** Asserts that response represents an empty page */
  public static <T> void assertEmptyPage(PaginatedResponse<T> response) {
    assertTrue(response.items().isEmpty(), "Expected empty items");
    assertFalse(response.hasMorePages(), "Empty page should have no more pages");
  }

  /** Asserts that hasMorePages logic is correct */
  public static <T> void assertHasMorePagesLogic(PaginatedResponse<T> response) {
    if (response.totalItems() != null) {
      // When totalItems available, hasMorePages should be accurate
      int itemsFetched = response.page() * response.pageSize();
      boolean expectedHasMore = itemsFetched < response.totalItems();
      assertEquals(expectedHasMore, response.hasMorePages(), "hasMorePages logic incorrect");
    } else {
      // When totalItems null, hasMorePages uses heuristic
      // (can't validate without knowing actual total)
      assertNotNull(response.hasMorePages(), "hasMorePages should not be null");
    }
  }

  /** Asserts that response has no validation message (clean response) */
  public static <T> void assertNoValidationMessage(PaginatedResponse<T> response) {
    assertTrue(
        response.message() == null || response.message().isEmpty(),
        "Expected no validation message");
  }

  /** Asserts that the response contains the expected number of items */
  public static <T> void assertItemCount(PaginatedResponse<T> response, int expectedCount) {
    assertEquals(
        expectedCount,
        response.items().size(),
        String.format("Expected %d items but got %d", expectedCount, response.items().size()));
  }
}
