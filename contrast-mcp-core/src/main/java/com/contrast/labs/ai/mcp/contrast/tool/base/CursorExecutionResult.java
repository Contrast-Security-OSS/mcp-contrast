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
 * Intermediate result from cursor-backed tool execution before final response building.
 *
 * @param <T> item type
 * @param items page items
 * @param nextCursor opaque continuation token returned by the backend, or null
 * @param hasMore true when another page can be fetched
 */
public record CursorExecutionResult<T>(
    List<T> items, @Nullable String nextCursor, boolean hasMore) {

  public CursorExecutionResult {
    items = items != null ? List.copyOf(items) : List.of();
  }

  public static <T> CursorExecutionResult<T> of(
      List<T> items, @Nullable String nextCursor, boolean hasMore) {
    return new CursorExecutionResult<>(items, nextCursor, hasMore);
  }

  public static <T> CursorExecutionResult<T> empty() {
    return new CursorExecutionResult<>(List.of(), null, false);
  }
}
