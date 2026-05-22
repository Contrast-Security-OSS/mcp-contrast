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

import java.util.List;
import org.springframework.lang.Nullable;

/**
 * Response wrapper for cursor-backed MCP list tools. It intentionally exposes only cursor
 * continuation metadata, not random-access pagination metadata.
 *
 * @param <T> item type
 * @param items page items
 * @param pageSize page size used
 * @param nextCursor opaque continuation token, or null when absent
 * @param hasMore true when another page can be fetched
 * @param errors validation or execution errors
 * @param warnings non-fatal warnings
 * @param durationMs execution duration in milliseconds
 */
public record CursorToolResponse<T>(
    List<T> items,
    int pageSize,
    @Nullable String nextCursor,
    boolean hasMore,
    List<String> errors,
    List<String> warnings,
    @Nullable Long durationMs) {

  public CursorToolResponse {
    items = items != null ? List.copyOf(items) : List.of();
    errors = errors != null ? List.copyOf(errors) : List.of();
    warnings = warnings != null ? List.copyOf(warnings) : List.of();
  }

  /**
   * Returns true if there are no errors.
   *
   * @return true if successful
   */
  public boolean isSuccess() {
    return errors.isEmpty();
  }

  public static <T> CursorToolResponse<T> success(
      List<T> items,
      int pageSize,
      @Nullable String nextCursor,
      boolean hasMore,
      List<String> warnings,
      @Nullable Long durationMs) {
    return new CursorToolResponse<>(
        items, pageSize, nextCursor, hasMore, List.of(), warnings, durationMs);
  }

  public static <T> CursorToolResponse<T> validationError(int pageSize, List<String> errors) {
    return new CursorToolResponse<>(List.of(), pageSize, null, false, errors, List.of(), null);
  }

  public static <T> CursorToolResponse<T> error(int pageSize, String errorMessage) {
    return new CursorToolResponse<>(
        List.of(), pageSize, null, false, List.of(errorMessage), List.of(), null);
  }
}
