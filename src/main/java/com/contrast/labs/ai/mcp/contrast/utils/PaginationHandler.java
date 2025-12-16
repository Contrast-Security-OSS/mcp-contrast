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

import com.contrast.labs.ai.mcp.contrast.PaginationParams;
import com.contrast.labs.ai.mcp.contrast.data.PaginatedResponse;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Centralized pagination handler for all MCP Server endpoints. Provides consistent pagination logic
 * including: - hasMorePages calculation (based on totalCount or heuristic) - Empty result messaging
 * - Response construction with validation messages
 */
@Component
public class PaginationHandler {

  /**
   * Creates a PaginatedResponse from API-paginated items. Use this when the API has already
   * paginated the data (e.g., SDK returns page of results).
   *
   * @param items The items returned by the API for this page
   * @param params Validated pagination parameters
   * @param totalCount Total count from API (null if unavailable)
   * @param <T> Type of items
   * @return PaginatedResponse with calculated hasMorePages and messages
   */
  public <T> PaginatedResponse<T> createPaginatedResponse(
      List<T> items, PaginationParams params, Integer totalCount) {
    return createPaginatedResponse(items, params, totalCount, List.of());
  }

  /**
   * Creates a PaginatedResponse from API-paginated items with additional warnings. Use this when
   * the API has already paginated the data (e.g., SDK returns page of results).
   *
   * @param items The items returned by the API for this page
   * @param params Validated pagination parameters
   * @param totalCount Total count from API (null if unavailable)
   * @param additionalWarnings Extra warnings to include (e.g., filter validation warnings)
   * @param <T> Type of items
   * @return PaginatedResponse with calculated hasMorePages and messages
   */
  public <T> PaginatedResponse<T> createPaginatedResponse(
      List<T> items, PaginationParams params, Integer totalCount, List<String> additionalWarnings) {
    boolean hasMorePages = calculateHasMorePages(params, totalCount, items.size());
    var emptyResultWarning = buildEmptyResultMessage(items, params, totalCount);

    // Collect all warnings
    var warnings = new ArrayList<String>();
    warnings.addAll(params.warnings());
    if (!CollectionUtils.isEmpty(additionalWarnings)) {
      warnings.addAll(additionalWarnings);
    }
    if (StringUtils.hasText(emptyResultWarning)) {
      warnings.add(emptyResultWarning);
    }

    return PaginatedResponse.success(
        items, params.page(), params.pageSize(), totalCount, hasMorePages, warnings, null);
  }

  /**
   * Calculate whether more pages exist. Uses totalCount if available, otherwise uses heuristic
   * (full page = more exist).
   *
   * @param params Pagination parameters
   * @param totalCount Total count from API (null if unavailable)
   * @param itemsReturned Number of items in current page
   * @return true if more pages likely exist
   */
  private boolean calculateHasMorePages(
      PaginationParams params, Integer totalCount, int itemsReturned) {
    if (totalCount != null) {
      // Accurate calculation: more pages exist if we haven't fetched everything
      return (params.page() * params.pageSize()) < totalCount;
    } else {
      // Heuristic: if we got a full page, assume more exist
      return itemsReturned == params.pageSize();
    }
  }

  /**
   * Build helpful message for empty results or page-beyond-bounds scenarios.
   *
   * @param items Items in current page
   * @param params Pagination parameters
   * @param totalCount Total count (null if unavailable)
   * @return Message string or null if no message needed
   */
  private String buildEmptyResultMessage(
      List<?> items, PaginationParams params, Integer totalCount) {
    if (!items.isEmpty()) {
      return null; // No message needed for non-empty results
    }

    if (params.page() == 1) {
      return "No items found.";
    } else {
      // Page > 1 with empty results
      if (totalCount != null) {
        int totalPages = (int) Math.ceil((double) totalCount / params.pageSize());
        return String.format(
            "Requested page %d exceeds available pages (total: %d).", params.page(), totalPages);
      } else {
        return String.format("Requested page %d returned no results.", params.page());
      }
    }
  }
}
