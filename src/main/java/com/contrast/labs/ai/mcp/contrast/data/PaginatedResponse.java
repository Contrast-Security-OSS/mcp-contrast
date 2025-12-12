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

import java.util.List;

/**
 * Generic paginated response wrapper for all list-returning MCP tools. Provides consistent
 * pagination metadata and messaging across all endpoints.
 *
 * <p>Separates errors (hard failures preventing results) from warnings (informational messages
 * about defaults, clamped values, etc.). This enables AI clients to distinguish between failures
 * requiring correction and advisory information.
 *
 * @param <T> The type of items in the paginated response
 * @param items The data for the current page (never null, empty list if no results)
 * @param page The page number returned (1-based, always ≥ 1)
 * @param pageSize Items per page used for this response (1-100)
 * @param totalItems Total count across all pages (null if unavailable or expensive to compute)
 * @param hasMorePages true if additional pages exist beyond this page
 * @param errors Validation or execution errors (empty list if successful)
 * @param warnings Informational messages about defaults, adjustments, empty results, etc.
 */
public record PaginatedResponse<T>(
    List<T> items,
    int page,
    int pageSize,
    Integer totalItems,
    boolean hasMorePages,
    List<String> errors,
    List<String> warnings) {

  /** Compact constructor ensuring non-null lists. */
  public PaginatedResponse {
    items = items != null ? items : List.of();
    errors = errors != null ? errors : List.of();
    warnings = warnings != null ? warnings : List.of();
  }

  /**
   * Backward compatibility method. Returns errors joined by semicolon, or warnings if no errors.
   *
   * @return Combined message string or null if no messages
   * @deprecated Use {@link #errors()} and {@link #warnings()} separately for clearer semantics
   */
  @Deprecated
  public String message() {
    if (!errors.isEmpty()) {
      return String.join("; ", errors);
    }
    if (!warnings.isEmpty()) {
      return String.join("; ", warnings);
    }
    return null;
  }

  /**
   * Creates a successful paginated response with items and optional warnings.
   *
   * @param items The items for this page
   * @param page Page number (1-based)
   * @param pageSize Items per page
   * @param totalItems Total count (null if unknown)
   * @param warnings Informational messages (empty list if none)
   * @param <T> Item type
   * @return New PaginatedResponse with empty errors
   */
  public static <T> PaginatedResponse<T> success(
      List<T> items, int page, int pageSize, Integer totalItems, List<String> warnings) {
    boolean hasMore = totalItems != null && (page * pageSize) < totalItems;
    return new PaginatedResponse<>(items, page, pageSize, totalItems, hasMore, List.of(), warnings);
  }

  /**
   * Creates an error response for validation or execution failures.
   *
   * @param page Page number
   * @param pageSize Page size
   * @param errors List of error messages
   * @param <T> Item type
   * @return New PaginatedResponse with empty items and errors
   */
  public static <T> PaginatedResponse<T> errors(int page, int pageSize, List<String> errors) {
    return new PaginatedResponse<>(List.of(), page, pageSize, 0, false, errors, List.of());
  }

  /**
   * Creates an empty response with a warning explaining why no results were found.
   *
   * @param page Page number
   * @param pageSize Page size
   * @param reason Explanation for empty results
   * @param <T> Item type
   * @return New PaginatedResponse with empty items and reason as warning
   */
  public static <T> PaginatedResponse<T> empty(int page, int pageSize, String reason) {
    return new PaginatedResponse<>(List.of(), page, pageSize, 0, false, List.of(), List.of(reason));
  }

  /**
   * Legacy factory method for backward compatibility.
   *
   * @deprecated Use {@link #errors(int, int, List)} instead
   */
  @Deprecated
  public static <T> PaginatedResponse<T> error(int page, int pageSize, String errorMessage) {
    return errors(page, pageSize, List.of(errorMessage));
  }
}
