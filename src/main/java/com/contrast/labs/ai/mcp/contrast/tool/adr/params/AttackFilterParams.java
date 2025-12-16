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
package com.contrast.labs.ai.mcp.contrast.tool.adr.params;

import com.contrast.labs.ai.mcp.contrast.sdkextension.data.adr.AttacksFilterBody;
import com.contrast.labs.ai.mcp.contrast.tool.validation.ToolValidationContext;
import java.util.List;
import java.util.Set;

/**
 * Attack filter parameters using fluent validation API. Demonstrates the ToolValidationContext
 * pattern for tool-per-class architecture.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * var params = AttackFilterParams.of("ACTIVE", "EXPLOITED", "sql", false, null, null, "-startTime");
 * if (!params.isValid()) {
 *   // Handle errors
 * }
 * var filterBody = params.toAttacksFilterBody();
 * }</pre>
 */
public class AttackFilterParams extends ToolValidationContext {

  /** Valid quickFilter values for validation (from AttackQuickFilterType). */
  public static final Set<String> VALID_QUICK_FILTERS =
      Set.of("ALL", "ACTIVE", "MANUAL", "AUTOMATED", "PRODUCTION", "EFFECTIVE");

  /** Valid statusFilter values (from AttackStatus enum). */
  public static final Set<String> VALID_STATUS_FILTERS =
      Set.of(
          "EXPLOITED", "PROBED", "BLOCKED", "BLOCKED_PERIMETER", "PROBED_PERIMETER", "SUSPICIOUS");

  private String quickFilter;
  private List<String> statusFilters;
  private String keyword;
  private Boolean includeSuppressed;
  private Boolean includeBotBlockers;
  private Boolean includeIpBlacklist;
  private String sort;

  /** Private constructor - use static factory method {@link #of}. */
  private AttackFilterParams() {}

  /**
   * Parse and validate attack filter parameters using fluent API.
   *
   * @param quickFilter Filter by attack categorization (e.g., "ACTIVE", "MANUAL", "AUTOMATED")
   * @param statusFilter Filter by attack outcome status (e.g., "EXPLOITED", "PROBED", "BLOCKED")
   * @param keyword Search keyword for filtering attacks
   * @param includeSuppressed Include suppressed attacks (null = use smart default of false)
   * @param includeBotBlockers Include bot blocker attacks
   * @param includeIpBlacklist Include IP blacklist attacks
   * @param sort Sort field (e.g., "severity", "-severity" for descending)
   * @return AttackFilterParams with validation state
   */
  public static AttackFilterParams of(
      String quickFilter,
      String statusFilter,
      String keyword,
      Boolean includeSuppressed,
      Boolean includeBotBlockers,
      Boolean includeIpBlacklist,
      String sort) {

    var params = new AttackFilterParams();

    // Parse quickFilter with validation
    params.quickFilter =
        params
            .stringParam(quickFilter, "quickFilter")
            .toUpperCase()
            .allowedValues(VALID_QUICK_FILTERS)
            .defaultTo("ALL", "No quickFilter applied - showing all attack types")
            .get();

    // Parse statusFilter with validation
    params.statusFilters =
        params
            .stringListParam(statusFilter, "statusFilter")
            .toUpperCase()
            .allowedValues(VALID_STATUS_FILTERS)
            .get();

    // Parse keyword (no validation - pass through)
    params.keyword = params.stringParam(keyword, "keyword").get();

    // Parse includeSuppressed with smart default
    if (includeSuppressed == null) {
      params.includeSuppressed = false;
      params.addWarning(
          "Excluding suppressed attacks by default. "
              + "To see all attacks including suppressed, set includeSuppressed=true.");
    } else {
      params.includeSuppressed = includeSuppressed;
    }

    // Parse other boolean filters (no defaults needed)
    params.includeBotBlockers = includeBotBlockers;
    params.includeIpBlacklist = includeIpBlacklist;

    // Parse sort with basic format validation
    if (sort != null && !sort.trim().isEmpty()) {
      String trimmedSort = sort.trim();
      if (trimmedSort.matches("^-?[a-zA-Z][a-zA-Z0-9_]*$")) {
        params.sort = trimmedSort;
      } else {
        params.addError(
            String.format(
                "Invalid sort format '%s'. Must be a field name with optional '-' prefix for"
                    + " descending. Example: 'severity' or '-severity'",
                sort));
      }
    }

    return params;
  }

  /**
   * Convert to SDK AttacksFilterBody for API calls.
   *
   * @return AttacksFilterBody configured with all filters
   */
  public AttacksFilterBody toAttacksFilterBody() {
    var builder = AttacksFilterBody.builder();

    if (quickFilter != null) {
      builder.quickFilter(quickFilter);
    }
    if (statusFilters != null && !statusFilters.isEmpty()) {
      builder.statusFilter(statusFilters);
    }
    if (keyword != null) {
      builder.keyword(keyword);
    }
    if (includeSuppressed != null) {
      builder.includeSuppressed(includeSuppressed);
    }
    if (includeBotBlockers != null) {
      builder.includeBotBlockers(includeBotBlockers);
    }
    if (includeIpBlacklist != null) {
      builder.includeIpBlacklist(includeIpBlacklist);
    }

    return builder.build();
  }

  public String getQuickFilter() {
    return quickFilter;
  }

  public List<String> getStatusFilters() {
    return statusFilters;
  }

  public String getKeyword() {
    return keyword;
  }

  public Boolean getIncludeSuppressed() {
    return includeSuppressed;
  }

  public Boolean getIncludeBotBlockers() {
    return includeBotBlockers;
  }

  public Boolean getIncludeIpBlacklist() {
    return includeIpBlacklist;
  }

  public String getSort() {
    return sort;
  }
}
