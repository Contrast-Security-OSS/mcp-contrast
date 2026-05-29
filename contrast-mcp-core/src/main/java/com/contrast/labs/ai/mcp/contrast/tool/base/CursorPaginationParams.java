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

import static com.contrast.labs.ai.mcp.contrast.tool.validation.ValidationConstants.DEFAULT_PAGE_SIZE;
import static com.contrast.labs.ai.mcp.contrast.tool.validation.ValidationConstants.MAX_PAGE_SIZE;

import java.util.ArrayList;
import java.util.List;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * Cursor pagination parameters for keyset-backed tools. Cursor values are opaque continuation
 * tokens and must be passed through without parsing, encoding, decoding, or logging.
 *
 * @param cursor opaque continuation token, or null for the first page
 * @param pageSize validated page size
 * @param limit same as pageSize, for downstream client clarity
 * @param warnings validation warnings
 */
public record CursorPaginationParams(
    @Nullable String cursor, int pageSize, int limit, List<String> warnings) {

  private static final String CURSOR_PRESENT = "present";
  private static final String CURSOR_ABSENT = "absent";

  public CursorPaginationParams {
    cursor = StringUtils.hasText(cursor) ? cursor : null;
    warnings = warnings != null ? List.copyOf(warnings) : List.of();
  }

  /**
   * Parse and validate cursor pagination parameters with default max page size.
   *
   * @param cursor opaque cursor, null or blank for first page
   * @param pageSize requested page size, null defaults to 50
   * @return validated cursor pagination params
   */
  public static CursorPaginationParams of(@Nullable String cursor, @Nullable Integer pageSize) {
    return of(cursor, pageSize, MAX_PAGE_SIZE);
  }

  /**
   * Parse and validate cursor pagination parameters with a tool-specific max page size.
   *
   * @param cursor opaque cursor, null or blank for first page
   * @param pageSize requested page size, null defaults to 50 unless endpoint max is narrower
   * @param maxPageSize tool-specific maximum page size
   * @return validated cursor pagination params
   */
  public static CursorPaginationParams of(
      @Nullable String cursor, @Nullable Integer pageSize, int maxPageSize) {
    List<String> warnings = new ArrayList<>();
    int defaultSize = Math.min(DEFAULT_PAGE_SIZE, maxPageSize);
    int actualSize = pageSize != null && pageSize > 0 ? pageSize : defaultSize;

    if (pageSize != null && pageSize < 1) {
      warnings.add(String.format("Invalid pageSize %d, using default %d", pageSize, defaultSize));
      actualSize = defaultSize;
    } else if (pageSize != null && pageSize > maxPageSize) {
      warnings.add(
          String.format(
              "Requested pageSize %d exceeds maximum %d, capped to %d",
              pageSize, maxPageSize, maxPageSize));
      actualSize = maxPageSize;
    }

    return new CursorPaginationParams(cursor, actualSize, actualSize, warnings);
  }

  /**
   * Cursor pagination uses soft validation only; invalid page sizes are corrected with warnings.
   *
   * @return true always
   */
  public boolean isValid() {
    return true;
  }

  /**
   * Returns a sanitized cursor presence marker suitable for logs and metrics.
   *
   * @return "present" when a cursor was supplied, otherwise "absent"
   */
  public String cursorPresence() {
    return cursor == null ? CURSOR_ABSENT : CURSOR_PRESENT;
  }
}
