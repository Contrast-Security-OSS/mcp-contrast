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
 * Generic response wrapper for non-paginated MCP tools (get_vulnerability, get_application, etc.).
 * Provides consistent error and warning handling across single-item retrievals.
 *
 * <p>Separates errors (hard failures preventing results) from warnings (informational messages).
 *
 * @param <T> The type of the returned data
 * @param data The result data (null if errors occurred)
 * @param errors Validation or execution errors (empty list if successful)
 * @param warnings Informational messages about defaults, adjustments, etc.
 */
public record ToolResponse<T>(T data, List<String> errors, List<String> warnings) {

  /** Compact constructor ensuring non-null lists. */
  public ToolResponse {
    errors = errors != null ? errors : List.of();
    warnings = warnings != null ? warnings : List.of();
  }

  /**
   * Check if the response represents a successful operation.
   *
   * @return true if no errors, false otherwise
   */
  public boolean isSuccess() {
    return errors.isEmpty();
  }

  /**
   * Creates a successful response with data and optional warnings.
   *
   * @param data The result data
   * @param warnings Informational messages (empty list if none)
   * @param <T> Data type
   * @return New ToolResponse with empty errors
   */
  public static <T> ToolResponse<T> success(T data, List<String> warnings) {
    return new ToolResponse<>(data, List.of(), warnings);
  }

  /**
   * Creates a successful response with data and no warnings.
   *
   * @param data The result data
   * @param <T> Data type
   * @return New ToolResponse with empty errors and warnings
   */
  public static <T> ToolResponse<T> success(T data) {
    return new ToolResponse<>(data, List.of(), List.of());
  }

  /**
   * Creates an error response with no data.
   *
   * @param errors List of error messages
   * @param <T> Data type
   * @return New ToolResponse with null data and errors
   */
  public static <T> ToolResponse<T> error(List<String> errors) {
    return new ToolResponse<>(null, errors, List.of());
  }

  /**
   * Creates an error response with a single error message.
   *
   * @param errorMessage The error message
   * @param <T> Data type
   * @return New ToolResponse with null data and single error
   */
  public static <T> ToolResponse<T> error(String errorMessage) {
    return new ToolResponse<>(null, List.of(errorMessage), List.of());
  }
}
