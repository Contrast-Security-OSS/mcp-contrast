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

import java.util.ArrayList;
import java.util.List;

/**
 * Response wrapper for non-paginated tools (get_* tools). Separates errors from warnings like
 * PaginatedResponse.
 *
 * @param <T> the type of data in the response
 * @param data the response data (null if not found or error)
 * @param errors validation or execution errors (empty if success)
 * @param warnings non-fatal warnings (e.g., applied defaults)
 * @param found true if the requested item was found, false if not found or error
 */
public record ToolResponse<T>(T data, List<String> errors, List<String> warnings, boolean found) {

  /** Compact constructor ensures non-null, immutable error and warning lists. */
  public ToolResponse {
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
   * Creates a successful response with data and warnings.
   *
   * @param data the response data
   * @param warnings non-fatal warnings
   * @param <T> the data type
   * @return successful response
   */
  public static <T> ToolResponse<T> success(T data, List<String> warnings) {
    return new ToolResponse<>(data, List.of(), warnings, true);
  }

  /**
   * Creates a successful response with data and no warnings.
   *
   * @param data the response data
   * @param <T> the data type
   * @return successful response
   */
  public static <T> ToolResponse<T> success(T data) {
    return success(data, List.of());
  }

  /**
   * Creates a not-found response. The message is added to warnings to inform the AI.
   *
   * @param message description of what was not found
   * @param warnings existing warnings to include
   * @param <T> the data type
   * @return not-found response with found=false
   */
  public static <T> ToolResponse<T> notFound(String message, List<String> warnings) {
    var allWarnings = new ArrayList<>(warnings);
    allWarnings.add(message);
    return new ToolResponse<>(null, List.of(), allWarnings, false);
  }

  /**
   * Creates an error response with a single error message.
   *
   * @param error the error message
   * @param <T> the data type
   * @return error response
   */
  public static <T> ToolResponse<T> error(String error) {
    return new ToolResponse<>(null, List.of(error), List.of(), false);
  }

  /**
   * Creates an error response with multiple error messages.
   *
   * @param errors the error messages
   * @param <T> the data type
   * @return error response
   */
  public static <T> ToolResponse<T> error(List<String> errors) {
    return new ToolResponse<>(null, errors, List.of(), false);
  }
}
