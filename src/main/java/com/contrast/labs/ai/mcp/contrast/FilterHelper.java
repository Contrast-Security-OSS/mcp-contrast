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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class for parsing filter parameters in MCP tools.
 * Provides reusable methods for parsing comma-separated lists, dates, and other common filter formats.
 * Returns validation messages for AI feedback when input is invalid.
 */
public class FilterHelper {

    private static final Logger logger = LoggerFactory.getLogger(FilterHelper.class);

    /**
     * Result of parsing with optional validation message for AI feedback
     */
    public static class ParseResult<T> {
        private final T value;
        private final String validationMessage;

        public ParseResult(T value, String validationMessage) {
            this.value = value;
            this.validationMessage = validationMessage;
        }

        public ParseResult(T value) {
            this(value, null);
        }

        public T getValue() {
            return value;
        }

        public String getValidationMessage() {
            return validationMessage;
        }

        public boolean hasValidationMessage() {
            return validationMessage != null && !validationMessage.isEmpty();
        }
    }

    /**
     * Parse comma-separated string into list with robust whitespace handling.
     * Handles edge cases like extra whitespace, empty values, and null input.
     *
     * @param input Comma-separated string (e.g., "CRITICAL, HIGH" or "sql-injection,xss-reflected")
     * @return List of trimmed, non-empty strings, or null if input is null/empty
     *
     * @example parseCommaSeparated("CRITICAL, HIGH, ") → ["CRITICAL", "HIGH"]
     * @example parseCommaSeparated("sql-injection,,xss") → ["sql-injection", "xss"]
     * @example parseCommaSeparated(null) → null
     * @example parseCommaSeparated("  ") → null
     */
    public static List<String> parseCommaSeparated(String input) {
        if (input == null || input.trim().isEmpty()) {
            return null;
        }
        List<String> result = Arrays.stream(input.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        return result.isEmpty() ? null : result;
    }

    /**
     * Parse date string in ISO format (YYYY-MM-DD) or epoch timestamp (milliseconds).
     * Tries epoch timestamp first, then falls back to ISO date format.
     * Returns validation message if format is invalid.
     *
     * @param dateStr Date string in ISO format or epoch timestamp
     * @param paramName Parameter name for error messages (e.g., "lastSeenAfter")
     * @return ParseResult with Date object and optional validation message
     *
     * @example parseDate("2025-01-15", "lastSeenAfter") → ParseResult(Date, null)
     * @example parseDate("1704067200000", "lastSeenAfter") → ParseResult(Date, null)
     * @example parseDate("invalid", "lastSeenAfter") → ParseResult(null, "Invalid date...")
     * @example parseDate(null, "lastSeenAfter") → ParseResult(null, null)
     */
    public static ParseResult<Date> parseDateWithValidation(String dateStr, String paramName) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return new ParseResult<>(null);
        }
        try {
            // Try parsing as epoch timestamp first
            long timestamp = Long.parseLong(dateStr.trim());
            return new ParseResult<>(new Date(timestamp));
        } catch (NumberFormatException e) {
            // Try ISO date format
            try {
                LocalDate localDate = LocalDate.parse(dateStr.trim());
                Date parsed = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
                return new ParseResult<>(parsed);
            } catch (DateTimeParseException ex) {
                String message = String.format(
                    "Invalid %s date '%s'. Expected ISO format (YYYY-MM-DD) like '2025-01-15' or epoch timestamp like '1705276800000'.",
                    paramName, dateStr
                );
                logger.warn(message);
                return new ParseResult<>(null, message);
            }
        }
    }

    /**
     * Parse date string (legacy method for backward compatibility).
     * Use parseDateWithValidation() for new code to get validation messages.
     */
    public static Date parseDate(String dateStr) {
        return parseDateWithValidation(dateStr, "date").getValue();
    }

    /**
     * Parse comma-separated list and convert to case-insensitive list.
     * Useful for status, severity, and other case-insensitive filters.
     *
     * @param input Comma-separated string
     * @return List of trimmed, uppercase strings, or null if input is null/empty
     *
     * @example parseCommaSeparatedUpperCase("critical, high") → ["CRITICAL", "HIGH"]
     */
    public static List<String> parseCommaSeparatedUpperCase(String input) {
        List<String> parsed = parseCommaSeparated(input);
        if (parsed == null) {
            return null;
        }
        return parsed.stream()
                .map(String::toUpperCase)
                .collect(Collectors.toList());
    }

    /**
     * Parse comma-separated list and convert to lowercase list.
     * Useful for vulnerability types and other lowercase filters.
     *
     * @param input Comma-separated string
     * @return List of trimmed, lowercase strings, or null if input is null/empty
     *
     * @example parseCommaSeparatedLowerCase("SQL-Injection, XSS") → ["sql-injection", "xss"]
     */
    public static List<String> parseCommaSeparatedLowerCase(String input) {
        List<String> parsed = parseCommaSeparated(input);
        if (parsed == null) {
            return null;
        }
        return parsed.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toList());
    }
}
