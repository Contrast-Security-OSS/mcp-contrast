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

/**
 * Intermediate result from tool execution, before final response building. Base classes use this to
 * build consistent PaginatedResponse or ToolResponse.
 *
 * @param <T> the type of items in the result
 * @param items the result items (never null, empty list if no results)
 * @param totalItems total count across all pages (null if unavailable from API)
 */
public record ExecutionResult<T>(List<T> items, Integer totalItems) {

  /** Compact constructor ensures non-null, immutable items list. */
  public ExecutionResult {
    items = items != null ? List.copyOf(items) : List.of();
  }

  /**
   * Creates an execution result with items and total count.
   *
   * @param items the result items
   * @param total total count (may be null if API doesn't provide it)
   * @param <T> the item type
   * @return execution result
   */
  public static <T> ExecutionResult<T> of(List<T> items, Integer total) {
    return new ExecutionResult<>(items, total);
  }

  /**
   * Creates an execution result with items but no total count.
   *
   * @param items the result items
   * @param <T> the item type
   * @return execution result with null totalItems
   */
  public static <T> ExecutionResult<T> of(List<T> items) {
    return new ExecutionResult<>(items, null);
  }

  /**
   * Creates an empty execution result.
   *
   * @param <T> the item type
   * @return empty execution result with zero total
   */
  public static <T> ExecutionResult<T> empty() {
    return new ExecutionResult<>(List.of(), 0);
  }
}
