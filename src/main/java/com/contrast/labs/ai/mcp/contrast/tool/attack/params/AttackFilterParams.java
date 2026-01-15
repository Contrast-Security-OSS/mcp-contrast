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
package com.contrast.labs.ai.mcp.contrast.tool.attack.params;

import com.contrast.labs.ai.mcp.contrast.sdkextension.data.adr.AttacksFilterBody;
import com.contrast.labs.ai.mcp.contrast.tool.base.BaseToolParams;
import com.contrast.labs.ai.mcp.contrast.tool.validation.ToolValidationContext;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import org.springframework.util.StringUtils;

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
@Getter
public class AttackFilterParams extends BaseToolParams {

  /** Valid quickFilter values for validation (from AttackQuickFilterType). */
  public static final Set<String> VALID_QUICK_FILTERS =
      Set.of("ALL", "ACTIVE", "MANUAL", "AUTOMATED", "PRODUCTION", "EFFECTIVE");

  /** Valid statusFilter values (from AttackStatus enum). */
  public static final Set<String> VALID_STATUS_FILTERS =
      Set.of(
          "EXPLOITED", "PROBED", "BLOCKED", "BLOCKED_PERIMETER", "PROBED_PERIMETER", "SUSPICIOUS");

  /**
   * Valid sort fields for attacks API (from TeamServer NgAttackRestController). Use '-' prefix for
   * descending order. Default is -startTime when not specified.
   */
  public static final Set<String> VALID_SORT_FIELDS =
      Set.of("sourceIP", "status", "startTime", "endTime", "type");

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
   * @param sort Sort field (e.g., "startTime", "-startTime" for descending)
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
    var ctx = new ToolValidationContext();

    // Parse quickFilter with validation
    params.quickFilter =
        ctx.stringParam(quickFilter, "quickFilter")
            .toUpperCase()
            .allowedValues(VALID_QUICK_FILTERS)
            .defaultTo("ALL", "No quickFilter applied - showing all attack types")
            .get();

    // Parse statusFilter with validation
    params.statusFilters =
        ctx.stringListParam(statusFilter, "statusFilter")
            .toUpperCase()
            .allowedValues(VALID_STATUS_FILTERS)
            .get();

    // Parse keyword (no validation - pass through)
    params.keyword = ctx.stringParam(keyword, "keyword").get();

    // Parse includeSuppressed with smart default
    if (includeSuppressed == null) {
      params.includeSuppressed = false;
      ctx.warnIf(
          true,
          "Excluding suppressed attacks by default. "
              + "To see all attacks including suppressed, set includeSuppressed=true.");
    } else {
      params.includeSuppressed = includeSuppressed;
    }

    // Parse other boolean filters (no defaults needed)
    params.includeBotBlockers = includeBotBlockers;
    params.includeIpBlacklist = includeIpBlacklist;

    // Parse sort with allowlist validation (case-sensitive to match API)
    if (StringUtils.hasText(sort)) {
      String trimmedSort = sort.trim();
      // Extract base field (strip '-' prefix for validation)
      String baseField = trimmedSort.startsWith("-") ? trimmedSort.substring(1) : trimmedSort;

      if (VALID_SORT_FIELDS.contains(baseField)) {
        params.sort = trimmedSort;
      } else {
        ctx.errorIf(
            true,
            String.format(
                "Invalid sort field '%s'. Valid fields: %s. Use '-' prefix for descending order"
                    + " (e.g., '-startTime'). Default: -startTime",
                baseField, VALID_SORT_FIELDS.stream().sorted().toList()));
      }
    }

    params.setValidationResult(ctx);
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
    if (StringUtils.hasText(keyword)) {
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
}
