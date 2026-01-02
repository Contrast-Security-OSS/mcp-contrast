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
package com.contrast.labs.ai.mcp.contrast;

import static com.contrast.labs.ai.mcp.contrast.tool.validation.ValidationConstants.DEFAULT_PAGE_SIZE;
import static com.contrast.labs.ai.mcp.contrast.tool.validation.ValidationConstants.MAX_PAGE_SIZE;

import java.util.ArrayList;
import java.util.List;

/**
 * Pagination parameters with validation and SDK conversion. Handles page/pageSize validation with
 * graceful degradation (soft failures only).
 *
 * @param page Validated 1-based page number (min: 1)
 * @param pageSize Validated page size (range: 1-100, default: 50)
 * @param offset Calculated 0-based offset for SDK
 * @param limit Same as pageSize, for SDK clarity
 * @param warnings Validation warnings (soft failures - execution continues with corrected values)
 */
public record PaginationParams(
    int page, int pageSize, int offset, int limit, List<String> warnings) {

  /**
   * Parse and validate pagination parameters. Invalid values are clamped to acceptable defaults
   * with warnings.
   *
   * @param page Requested page number (1-based), null defaults to 1
   * @param pageSize Requested page size, null defaults to 50
   * @return PaginationParams with validated values and warnings
   */
  public static PaginationParams of(Integer page, Integer pageSize) {
    List<String> warnings = new ArrayList<>();

    // Soft failure: invalid page → clamp to 1
    int actualPage = page != null && page > 0 ? page : 1;
    if (page != null && page < 1) {
      warnings.add(String.format("Invalid page number %d, using page 1", page));
    }

    // Soft failure: invalid pageSize → clamp to range
    int actualSize = pageSize != null && pageSize > 0 ? pageSize : DEFAULT_PAGE_SIZE;
    if (pageSize != null && pageSize < 1) {
      warnings.add(
          String.format("Invalid pageSize %d, using default %d", pageSize, DEFAULT_PAGE_SIZE));
      actualSize = DEFAULT_PAGE_SIZE;
    } else if (pageSize != null && pageSize > MAX_PAGE_SIZE) {
      warnings.add(
          String.format(
              "Requested pageSize %d exceeds maximum %d, capped to %d",
              pageSize, MAX_PAGE_SIZE, MAX_PAGE_SIZE));
      actualSize = MAX_PAGE_SIZE;
    }

    return new PaginationParams(
        actualPage,
        actualSize,
        (actualPage - 1) * actualSize, // 0-based offset
        actualSize, // limit
        List.copyOf(warnings));
  }

  /**
   * Pagination params are always valid (soft failures only). Invalid values are clamped to
   * acceptable defaults.
   *
   * @return true always
   */
  public boolean isValid() {
    return true; // Always valid - uses graceful degradation
  }
}
