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

import com.contrast.labs.ai.mcp.contrast.sdkexstension.data.adr.AttacksFilterBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Attack filter parameters with validation and SDK conversion.
 * Hard failures (errors) stop execution with descriptive messages for AI self-correction.
 * Informational messages provide context about defaults and filter interpretations.
 *
 * @param filterBody SDK AttacksFilterBody with all filters applied
 * @param messages Informational context for AI (smart defaults applied, filter interpretations)
 * @param errors Validation errors (hard failures - execution must stop)
 */
public record AttackFilterParams(
    AttacksFilterBody filterBody,
    List<String> messages,
    List<String> errors
) {
    private static final Logger logger = LoggerFactory.getLogger(AttackFilterParams.class);

    // Valid quickFilter values for validation
    private static final Set<String> VALID_QUICK_FILTERS = Set.of(
        "ALL", "EXPLOITED", "PROBED", "BLOCKED", "INEFFECTIVE"
    );

    /**
     * Parse and validate attack filter parameters.
     * Returns object with validation status (messages/errors) and configured AttacksFilterBody.
     *
     * @param quickFilter Filter by attack effectiveness (e.g., "EXPLOITED", "PROBED", "BLOCKED", "INEFFECTIVE", "ALL")
     * @param keyword Search keyword for filtering attacks
     * @param includeSuppressed Include suppressed attacks (null = use smart default of false)
     * @param includeBotBlockers Include bot blocker attacks
     * @param includeIpBlacklist Include IP blacklist attacks
     * @param sort Sort field (e.g., "severity", "-severity" for descending)
     * @return AttackFilterParams with validated filters and feedback messages
     */
    public static AttackFilterParams of(
            String quickFilter,
            String keyword,
            Boolean includeSuppressed,
            Boolean includeBotBlockers,
            Boolean includeIpBlacklist,
            String sort
    ) {
        List<String> messages = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        AttacksFilterBody.Builder filterBuilder = new AttacksFilterBody.Builder();

        // Parse quickFilter (HARD FAILURE - invalid values are errors)
        if (quickFilter != null && !quickFilter.trim().isEmpty()) {
            String normalizedFilter = quickFilter.trim().toUpperCase();
            if (VALID_QUICK_FILTERS.contains(normalizedFilter)) {
                filterBuilder.quickFilter(normalizedFilter);
                logger.debug("QuickFilter set to: {}", normalizedFilter);
            } else {
                logger.warn("Invalid quickFilter value: {}", quickFilter);
                errors.add(String.format(
                    "Invalid quickFilter '%s'. Valid: EXPLOITED, PROBED, BLOCKED, INEFFECTIVE, ALL. Example: 'EXPLOITED'",
                    quickFilter
                ));
            }
        } else {
            // No quickFilter provided - use default "ALL"
            filterBuilder.quickFilter("ALL");
            messages.add("No quickFilter applied - showing all attack types");
            logger.debug("Using default quickFilter: ALL");
        }

        // Parse keyword (no validation - pass through)
        if (keyword != null && !keyword.trim().isEmpty()) {
            filterBuilder.keyword(keyword.trim());
            logger.debug("Keyword set to: {}", keyword);
        }

        // Parse includeSuppressed with smart default
        boolean usingDefaultSuppressed = false;
        if (includeSuppressed == null) {
            // Smart default: exclude suppressed attacks
            filterBuilder.includeSuppressed(false);
            usingDefaultSuppressed = true;
            logger.debug("Using smart default for includeSuppressed: false");
        } else {
            filterBuilder.includeSuppressed(includeSuppressed);
            logger.debug("includeSuppressed set to: {}", includeSuppressed);
        }

        if (usingDefaultSuppressed) {
            messages.add(
                "Excluding suppressed attacks by default. " +
                "To see all attacks including suppressed, set includeSuppressed=true."
            );
        }

        // Parse includeBotBlockers (no default needed - builder handles it)
        if (includeBotBlockers != null) {
            filterBuilder.includeBotBlockers(includeBotBlockers);
            logger.debug("includeBotBlockers set to: {}", includeBotBlockers);
        }

        // Parse includeIpBlacklist (no default needed - builder handles it)
        if (includeIpBlacklist != null) {
            filterBuilder.includeIpBlacklist(includeIpBlacklist);
            logger.debug("includeIpBlacklist set to: {}", includeIpBlacklist);
        }

        // Parse sort (basic validation for format)
        if (sort != null && !sort.trim().isEmpty()) {
            String trimmedSort = sort.trim();
            // Basic validation: sort should be a field name, optionally prefixed with '-' for descending
            if (trimmedSort.matches("^-?[a-zA-Z][a-zA-Z0-9_]*$")) {
                // Sort looks valid - will be passed to SDK/API
                logger.debug("Sort parameter: {}", trimmedSort);
            } else {
                logger.warn("Invalid sort format: {}", sort);
                errors.add(String.format(
                    "Invalid sort format '%s'. Must be a field name with optional '-' prefix for descending. Example: 'severity' or '-severity'",
                    sort
                ));
            }
        }

        AttacksFilterBody filterBody = filterBuilder.build();

        return new AttackFilterParams(
            filterBody,
            List.copyOf(messages),
            List.copyOf(errors)
        );
    }

    /**
     * Returns true if filters are valid enough to execute query.
     * Check errors() for details if false.
     *
     * @return true if no errors, false if errors exist
     */
    public boolean isValid() {
        return errors.isEmpty();
    }

    /**
     * Convert to SDK AttacksFilterBody.
     * Explicitly marks SDK boundary crossing.
     *
     * @return AttacksFilterBody configured with all filters
     */
    public AttacksFilterBody toAttacksFilterBody() {
        return filterBody;
    }
}
