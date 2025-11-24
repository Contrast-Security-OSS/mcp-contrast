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

import com.contrast.labs.ai.mcp.contrast.sdkextension.data.adr.AttacksFilterBody;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

/**
 * Attack filter parameters with validation and SDK conversion. Hard failures (errors) stop
 * execution with descriptive messages for AI self-correction. Informational messages provide
 * context about defaults and filter interpretations.
 *
 * @param filterBody SDK AttacksFilterBody with all filters applied
 * @param messages Informational context for AI (smart defaults applied, filter interpretations)
 * @param errors Validation errors (hard failures - execution must stop)
 */
@Slf4j
public record AttackFilterParams(
    AttacksFilterBody filterBody, List<String> messages, List<String> errors) {
  // Valid quickFilter values for validation (from AttackQuickFilterType)
  // ALL: no filtering, ACTIVE: ongoing attacks, MANUAL: human-initiated,
  // AUTOMATED: bot attacks, PRODUCTION: prod environment, EFFECTIVE: non-probed attacks
  private static final Set<String> VALID_QUICK_FILTERS =
      Set.of("ALL", "ACTIVE", "MANUAL", "AUTOMATED", "PRODUCTION", "EFFECTIVE");

  // Valid statusFilter values (from AttackStatus enum)
  // EXPLOITED: successfully exploited, PROBED: detected but not exploited,
  // BLOCKED: blocked by Protect, BLOCKED_PERIMETER: blocked at perimeter,
  // PROBED_PERIMETER: probed at perimeter, SUSPICIOUS: suspicious attack
  private static final Set<String> VALID_STATUS_FILTERS =
      Set.of(
          "EXPLOITED", "PROBED", "BLOCKED", "BLOCKED_PERIMETER", "PROBED_PERIMETER", "SUSPICIOUS");

  /**
   * Parse and validate attack filter parameters. Returns object with validation status
   * (messages/errors) and configured AttacksFilterBody.
   *
   * @param quickFilter Filter by attack categorization (e.g., "ACTIVE", "MANUAL", "AUTOMATED",
   *     "PRODUCTION", "EFFECTIVE", "ALL")
   * @param statusFilter Filter by attack outcome status (e.g., "EXPLOITED", "PROBED", "BLOCKED",
   *     "SUSPICIOUS")
   * @param keyword Search keyword for filtering attacks
   * @param includeSuppressed Include suppressed attacks (null = use smart default of false)
   * @param includeBotBlockers Include bot blocker attacks
   * @param includeIpBlacklist Include IP blacklist attacks
   * @param sort Sort field (e.g., "severity", "-severity" for descending)
   * @return AttackFilterParams with validated filters and feedback messages
   */
  public static AttackFilterParams of(
      String quickFilter,
      String statusFilter,
      String keyword,
      Boolean includeSuppressed,
      Boolean includeBotBlockers,
      Boolean includeIpBlacklist,
      String sort) {
    List<String> messages = new ArrayList<>();
    List<String> errors = new ArrayList<>();
    AttacksFilterBody.AttacksFilterBodyBuilder filterBuilder = AttacksFilterBody.builder();

    // Parse quickFilter (HARD FAILURE - invalid values are errors)
    if (quickFilter != null && !quickFilter.trim().isEmpty()) {
      String normalizedFilter = quickFilter.trim().toUpperCase();
      if (VALID_QUICK_FILTERS.contains(normalizedFilter)) {
        filterBuilder.quickFilter(normalizedFilter);
        log.debug("QuickFilter set to: {}", normalizedFilter);
      } else {
        log.warn("Invalid quickFilter value: {}", quickFilter);
        errors.add(
            String.format(
                "Invalid quickFilter '%s'. Valid: ALL, ACTIVE, MANUAL, AUTOMATED, PRODUCTION,"
                    + " EFFECTIVE. Example: 'ACTIVE'",
                quickFilter));
      }
    } else {
      // No quickFilter provided - use default "ALL"
      filterBuilder.quickFilter("ALL");
      messages.add("No quickFilter applied - showing all attack types");
      log.debug("Using default quickFilter: ALL");
    }

    // Parse statusFilter (HARD FAILURE - invalid values are errors)
    if (statusFilter != null && !statusFilter.trim().isEmpty()) {
      String normalizedStatus = statusFilter.trim().toUpperCase();
      if (VALID_STATUS_FILTERS.contains(normalizedStatus)) {
        // Add to statusFilter list in filter body
        filterBuilder.statusFilter(List.of(normalizedStatus));
        log.debug("StatusFilter set to: {}", normalizedStatus);
      } else {
        log.warn("Invalid statusFilter value: {}", statusFilter);
        errors.add(
            String.format(
                "Invalid statusFilter '%s'. Valid: EXPLOITED, PROBED, BLOCKED,"
                    + " BLOCKED_PERIMETER, PROBED_PERIMETER, SUSPICIOUS. Example: 'EXPLOITED'",
                statusFilter));
      }
    }

    // Parse keyword (no validation - pass through)
    if (keyword != null && !keyword.trim().isEmpty()) {
      filterBuilder.keyword(keyword.trim());
      log.debug("Keyword set to: {}", keyword);
    }

    // Parse includeSuppressed with smart default
    boolean usingDefaultSuppressed = false;
    if (includeSuppressed == null) {
      // Smart default: exclude suppressed attacks
      filterBuilder.includeSuppressed(false);
      usingDefaultSuppressed = true;
      log.debug("Using smart default for includeSuppressed: false");
    } else {
      filterBuilder.includeSuppressed(includeSuppressed);
      log.debug("includeSuppressed set to: {}", includeSuppressed);
    }

    if (usingDefaultSuppressed) {
      messages.add(
          "Excluding suppressed attacks by default. "
              + "To see all attacks including suppressed, set includeSuppressed=true.");
    }

    // Parse includeBotBlockers (no default needed - builder handles it)
    if (includeBotBlockers != null) {
      filterBuilder.includeBotBlockers(includeBotBlockers);
      log.debug("includeBotBlockers set to: {}", includeBotBlockers);
    }

    // Parse includeIpBlacklist (no default needed - builder handles it)
    if (includeIpBlacklist != null) {
      filterBuilder.includeIpBlacklist(includeIpBlacklist);
      log.debug("includeIpBlacklist set to: {}", includeIpBlacklist);
    }

    // Parse sort (basic validation for format)
    if (sort != null && !sort.trim().isEmpty()) {
      String trimmedSort = sort.trim();
      // Basic validation: sort should be a field name, optionally prefixed with '-' for descending
      if (trimmedSort.matches("^-?[a-zA-Z][a-zA-Z0-9_]*$")) {
        // Sort looks valid - will be passed to SDK/API
        log.debug("Sort parameter: {}", trimmedSort);
      } else {
        log.warn("Invalid sort format: {}", sort);
        errors.add(
            String.format(
                "Invalid sort format '%s'. Must be a field name with optional '-' prefix for"
                    + " descending. Example: 'severity' or '-severity'",
                sort));
      }
    }

    AttacksFilterBody filterBody = filterBuilder.build();

    return new AttackFilterParams(filterBody, List.copyOf(messages), List.copyOf(errors));
  }

  /**
   * Returns true if filters are valid enough to execute query. Check errors() for details if false.
   *
   * @return true if no errors, false if errors exist
   */
  public boolean isValid() {
    return errors.isEmpty();
  }

  /**
   * Convert to SDK AttacksFilterBody. Explicitly marks SDK boundary crossing.
   *
   * @return AttacksFilterBody configured with all filters
   */
  public AttacksFilterBody toAttacksFilterBody() {
    return filterBody;
  }
}
