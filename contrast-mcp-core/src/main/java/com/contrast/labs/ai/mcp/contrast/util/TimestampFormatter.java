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
package com.contrast.labs.ai.mcp.contrast.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Epoch-millis to ISO 8601 timestamp formatting. Extracted from FilterHelper so that result models
 * can format timestamps without depending on the tool layer.
 */
public final class TimestampFormatter {
  // Lowercase 'xxx' always outputs numeric offsets (e.g. "+00:00"), never "Z".
  // Uppercase 'XXX' would output "Z" for UTC; lowercase guarantees numeric format.
  private static final DateTimeFormatter TIMESTAMP_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx");
  private static final DateTimeFormatter TIMESTAMP_WITH_MILLIS_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSxxx");

  private TimestampFormatter() {}

  /**
   * Format epoch milliseconds as ISO 8601 timestamp with timezone offset. Uses system default
   * timezone (user's local timezone since MCP server runs locally). Always uses numeric timezone
   * offsets (e.g., "+00:00" instead of "Z"). Returns null for null input.
   *
   * @param epochMillis Epoch timestamp in milliseconds
   * @return ISO 8601 formatted timestamp (e.g., "2025-01-15T10:30:00-05:00"), or null if input is
   *     null
   */
  public static String formatTimestamp(Long epochMillis) {
    if (epochMillis == null) {
      return null;
    }
    return Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(TIMESTAMP_FORMATTER);
  }

  /**
   * Formats epoch milliseconds as a deterministic UTC ISO 8601 timestamp while retaining
   * millisecond precision. Use this for public contracts whose backend value is meaningful at
   * sub-second precision.
   *
   * @param epochMillis Epoch timestamp in milliseconds
   * @return ISO 8601 timestamp with milliseconds and a numeric UTC offset, or null for null input
   */
  public static String formatTimestampWithMillis(Long epochMillis) {
    if (epochMillis == null) {
      return null;
    }
    return Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneOffset.UTC)
        .format(TIMESTAMP_WITH_MILLIS_FORMATTER);
  }
}
