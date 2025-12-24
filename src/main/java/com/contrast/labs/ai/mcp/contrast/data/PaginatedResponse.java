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
 * pagination metadata, error/warning separation, and timing across all endpoints.
 *
 * <p>AI agents can use {@link #isSuccess()} to determine if the request succeeded, and examine
 * {@link #errors()} for actionable problems vs {@link #warnings()} for informational messages.
 *
 * @param <T> The type of items in the paginated response
 * @param items The data for the current page (never null, empty list if no results)
 * @param page The page number returned (1-based, always ≥ 1)
 * @param pageSize Items per page used for this response (1-100)
 * @param totalItems Total count across all pages (null if unavailable or expensive to compute)
 * @param hasMorePages true if additional pages exist beyond this page
 * @param errors Validation or execution errors (empty if success) - actionable problems
 * @param warnings Non-fatal warnings (e.g., applied defaults, empty results) - informational
 * @param durationMs Execution duration in milliseconds (null for validation errors)
 */
public record PaginatedResponse<T>(
    List<T> items,
    int page,
    int pageSize,
    Integer totalItems,
    boolean hasMorePages,
    List<String> errors,
    List<String> warnings,
    Long durationMs) {

  /** Compact constructor ensures non-null, immutable lists. */
  public PaginatedResponse {
    items = items != null ? List.copyOf(items) : List.of();
    errors = errors != null ? List.copyOf(errors) : List.of();
    warnings = warnings != null ? List.copyOf(warnings) : List.of();
  }

  /**
   * Returns true if there are no errors.
   *
   * @return true if successful (no errors), false otherwise
   */
  public boolean isSuccess() {
    return errors.isEmpty();
  }

  /**
   * Creates a successful response with items and optional warnings.
   *
   * @param items the response items
   * @param page page number (1-based)
   * @param pageSize items per page
   * @param totalItems total count (null if unavailable)
   * @param hasMorePages true if more pages exist
   * @param warnings non-fatal warnings
   * @param durationMs execution time in milliseconds
   * @param <T> item type
   * @return successful paginated response
   */
  public static <T> PaginatedResponse<T> success(
      List<T> items,
      int page,
      int pageSize,
      Integer totalItems,
      boolean hasMorePages,
      List<String> warnings,
      Long durationMs) {
    return new PaginatedResponse<>(
        items, page, pageSize, totalItems, hasMorePages, List.of(), warnings, durationMs);
  }

  /**
   * Creates a validation error response with multiple errors.
   *
   * @param page page number (1-based)
   * @param pageSize items per page
   * @param errors validation error messages
   * @param <T> item type
   * @return error response with no items
   */
  public static <T> PaginatedResponse<T> validationError(
      int page, int pageSize, List<String> errors) {
    return new PaginatedResponse<>(List.of(), page, pageSize, 0, false, errors, List.of(), null);
  }

  /**
   * Creates an empty paginated response with a warning message.
   *
   * @param page page number (1-based)
   * @param pageSize items per page
   * @param warningMessage informational message about empty results
   * @param <T> item type
   * @return empty response with warning
   */
  public static <T> PaginatedResponse<T> empty(int page, int pageSize, String warningMessage) {
    return new PaginatedResponse<>(
        List.of(), page, pageSize, 0, false, List.of(), List.of(warningMessage), null);
  }

  /**
   * Creates an error response for validation failures. Returns empty items with error message.
   *
   * @param page page number (1-based)
   * @param pageSize items per page
   * @param errorMessage the error message
   * @param <T> item type
   * @return error response with no items
   */
  public static <T> PaginatedResponse<T> error(int page, int pageSize, String errorMessage) {
    return new PaginatedResponse<>(
        List.of(), page, pageSize, 0, false, List.of(errorMessage), List.of(), null);
  }

  /**
   * Returns a combined message string for backward compatibility.
   *
   * @return errors joined, or warnings joined, or null
   * @deprecated Use {@link #errors()} and {@link #warnings()} instead. This method will be removed
   *     in a future release.
   */
  @Deprecated(forRemoval = true)
  public String message() {
    if (!errors.isEmpty()) {
      return String.join(" ", errors);
    }
    if (!warnings.isEmpty()) {
      return String.join(" ", warnings);
    }
    return null;
  }
}
