/*
 * Copyright 2026 Contrast Security
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

import com.contrast.labs.ai.mcp.contrast.util.TimestampFormatter;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

/**
 * Utility class for parsing filter parameters in MCP tools. Provides reusable methods for parsing
 * comma-separated lists, dates, and other common filter formats. Returns validation messages for AI
 * feedback when input is invalid.
 */
@Slf4j
@UtilityClass
public class FilterHelper {
  static final long MIN_EPOCH_MILLIS = 0L;
  static final long MAX_EPOCH_MILLIS = 253402300799999L;

  /** Result of parsing with optional validation message for AI feedback */
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
      return StringUtils.hasText(validationMessage);
    }
  }

  /**
   * Parse comma-separated string into list with robust whitespace handling. Handles edge cases like
   * extra whitespace, empty values, and null input.
   *
   * @param input Comma-separated string (e.g., "CRITICAL, HIGH" or "sql-injection,reflected-xss")
   * @return List of trimmed, non-empty strings, or null if input is null/empty
   * @example parseCommaSeparated("CRITICAL, HIGH, ") → ["CRITICAL", "HIGH"]
   * @example parseCommaSeparated("sql-injection,,xss") → ["sql-injection", "xss"]
   * @example parseCommaSeparated(null) → null
   * @example parseCommaSeparated(" ") → null
   */
  public static List<String> parseCommaSeparated(String input) {
    if (!StringUtils.hasText(input)) {
      return null;
    }
    List<String> result =
        Arrays.stream(input.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();

    return result.isEmpty() ? null : result;
  }

  /**
   * Parse date string in ISO format (YYYY-MM-DD) or epoch timestamp (milliseconds). Tries epoch
   * timestamp first, then falls back to ISO date format. Values outside the supported range
   * (1970-01-01 through 9999-12-31) are rejected in both formats. Returns validation message if
   * format is invalid.
   *
   * @param dateStr Date string in ISO format or epoch timestamp
   * @param paramName Parameter name for error messages (e.g., "lastSeenAfter")
   * @return ParseResult with Date object and optional validation message
   * @example parseDateWithValidation("2025-01-15", "lastSeenAfter") → ParseResult(Date, null)
   * @example parseDateWithValidation("1704067200000", "lastSeenAfter") → ParseResult(Date, null)
   * @example parseDateWithValidation("invalid", "lastSeenAfter") → ParseResult(null, "Invalid
   *     date...")
   * @example parseDateWithValidation(null, "lastSeenAfter") → ParseResult(null, null)
   */
  public static ParseResult<Date> parseDateWithValidation(String dateStr, String paramName) {
    if (!StringUtils.hasText(dateStr)) {
      return new ParseResult<>(null);
    }
    try {
      // Try parsing as epoch timestamp first
      long timestamp = Long.parseLong(dateStr.trim());
      if (!isSupportedTimestampMillis(timestamp)) {
        return invalidDateResult(dateStr, paramName);
      }
      return new ParseResult<>(new Date(timestamp));
    } catch (NumberFormatException e) {
      // Try ISO date format
      try {
        LocalDate localDate = LocalDate.parse(dateStr.trim());
        Date parsed = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        if (!isSupportedTimestampMillis(parsed.getTime())) {
          return invalidDateResult(dateStr, paramName);
        }
        return new ParseResult<>(parsed);
      } catch (DateTimeParseException ex) {
        return invalidDateResult(dateStr, paramName);
      }
    }
  }

  private static ParseResult<Date> invalidDateResult(String dateStr, String paramName) {
    String message =
        String.format(
            "Invalid %s date '%s'. Expected ISO format (YYYY-MM-DD) like '2025-01-15' or epoch"
                + " timestamp between %d and %d millis like '1705276800000'.",
            paramName, sanitizeForMessage(dateStr), MIN_EPOCH_MILLIS, MAX_EPOCH_MILLIS);
    log.warn(message);
    return new ParseResult<>(null, message);
  }

  /**
   * Parse timestamp string in ISO datetime format or epoch timestamp (milliseconds). Returns
   * validation message if format is invalid.
   *
   * @param timestampStr timestamp string in ISO datetime or epoch timestamp format
   * @param paramName parameter name for error messages (e.g., "startTime")
   * @return ParseResult with Date object and optional validation message
   */
  public static ParseResult<Date> parseTimestampWithValidation(
      String timestampStr, String paramName) {
    if (!StringUtils.hasText(timestampStr)) {
      return new ParseResult<>(null);
    }
    var trimmed = timestampStr.trim();
    try {
      long timestamp = Long.parseLong(trimmed);
      if (!isSupportedTimestampMillis(timestamp)) {
        return invalidTimestampResult(timestampStr, paramName);
      }
      return new ParseResult<>(new Date(timestamp));
    } catch (NumberFormatException e) {
      Date parsed = parseIsoTimestamp(trimmed);
      if (parsed != null) {
        return new ParseResult<>(parsed);
      }
      return invalidTimestampResult(timestampStr, paramName);
    }
  }

  private static Date parseIsoTimestamp(String timestampStr) {
    try {
      Instant instant = OffsetDateTime.parse(timestampStr).toInstant();
      long epochMillis = instant.toEpochMilli();
      if (!isSupportedTimestampMillis(epochMillis)) {
        return null;
      }
      return Date.from(instant);
    } catch (DateTimeException | ArithmeticException e) {
      return null;
    }
  }

  private static boolean isSupportedTimestampMillis(long epochMillis) {
    return epochMillis >= MIN_EPOCH_MILLIS && epochMillis <= MAX_EPOCH_MILLIS;
  }

  private static ParseResult<Date> invalidTimestampResult(String timestampStr, String paramName) {
    String message =
        String.format(
            "Invalid %s timestamp '%s'. Expected ISO timestamp with date, time, and timezone offset"
                + " like '2025-01-15T10:30:00Z', or epoch timestamp between %d and %d millis"
                + " like '1705276800000'.",
            paramName, sanitizeForMessage(timestampStr), MIN_EPOCH_MILLIS, MAX_EPOCH_MILLIS);
    log.warn(message);
    return new ParseResult<>(null, message);
  }

  private static String sanitizeForMessage(String value) {
    return value.replace("\r", "\\r").replace("\n", "\\n");
  }

  /**
   * Parse comma-separated list and convert to case-insensitive list. Useful for status, severity,
   * and other case-insensitive filters.
   *
   * @param input Comma-separated string
   * @return List of trimmed, uppercase strings, or null if input is null/empty
   * @example parseCommaSeparatedUpperCase("critical, high") → ["CRITICAL", "HIGH"]
   */
  public static List<String> parseCommaSeparatedUpperCase(String input) {
    List<String> parsed = parseCommaSeparated(input);
    if (parsed == null) {
      return null;
    }
    return parsed.stream().map(String::toUpperCase).toList();
  }

  /**
   * @see TimestampFormatter#formatTimestamp(Long)
   */
  public static String formatTimestamp(Long epochMillis) {
    return TimestampFormatter.formatTimestamp(epochMillis);
  }

  /**
   * @see TimestampFormatter#formatTimestampWithMillis(Long)
   */
  public static String formatTimestampWithMillis(Long epochMillis) {
    return TimestampFormatter.formatTimestampWithMillis(epochMillis);
  }
}
