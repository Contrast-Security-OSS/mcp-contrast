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

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.Test;

/** Test suite for FilterHelper utility methods, focusing on timestamp formatting. */
class FilterHelperTest {

  // Named constants for test timestamps
  private static final long JAN_15_2025_10_30_UTC =
      LocalDateTime.of(2025, 1, 15, 10, 30)
          .toInstant(ZoneOffset.UTC)
          .toEpochMilli(); // 1736938200000L

  private static final long JAN_1_2030_00_00_UTC =
      LocalDateTime.of(2030, 1, 1, 0, 0).toInstant(ZoneOffset.UTC).toEpochMilli(); // 1893456000000L

  private static final long SEP_9_2001_01_46_UTC =
      LocalDateTime.of(2001, 9, 9, 1, 46, 40)
          .toInstant(ZoneOffset.UTC)
          .toEpochMilli(); // 1000000000000L

  private static final long JUL_14_2017_02_40_UTC =
      LocalDateTime.of(2017, 7, 14, 2, 40)
          .toInstant(ZoneOffset.UTC)
          .toEpochMilli(); // 1500000000000L

  @Test
  void testFormatTimestamp_ValidTimestamp() {
    // Given: A known epoch timestamp (Jan 15, 2025 10:30:00 UTC)
    long epochMillis = JAN_15_2025_10_30_UTC;

    // When: Formatting the timestamp
    var result = FilterHelper.formatTimestamp(epochMillis);

    // Then: Should return ISO 8601 format with timezone offset
    assertThat(result).isNotNull();
    assertThat(result)
        .as("Timestamp should match ISO 8601 format with timezone offset: " + result)
        .matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[+-]\\d{2}:\\d{2}");

    // Verify it can be parsed back
    var parsed = ZonedDateTime.parse(result, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    assertThat(parsed.toInstant().toEpochMilli())
        .as("Parsed timestamp should match original epoch milliseconds")
        .isEqualTo(epochMillis);
  }

  @Test
  void testFormatTimestamp_NullInput() {
    // Given: Null input
    Long nullValue = null;

    // When: Formatting null
    var result = FilterHelper.formatTimestamp(nullValue);

    // Then: Should return null
    assertThat(result).as("Formatting null timestamp should return null").isNull();
  }

  @Test
  void testFormatTimestamp_EpochZero() {
    // Given: Epoch zero (Jan 1, 1970 00:00:00 UTC)
    long epochMillis = 0L;

    // When: Formatting epoch zero
    var result = FilterHelper.formatTimestamp(epochMillis);

    // Then: Should return valid ISO 8601 timestamp
    assertThat(result).isNotNull();
    assertThat(result)
        .as("Timestamp should match ISO 8601 format: " + result)
        .matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[+-]\\d{2}:\\d{2}");
    assertThat(result.startsWith("1970-01-01") || result.startsWith("1969-12-31"))
        .as(
            "Epoch zero should be Jan 1, 1970 UTC (or Dec 31, 1969 in negative timezone): "
                + result)
        .isTrue();
  }

  @Test
  void testFormatTimestamp_FutureDate() {
    // Given: A future timestamp (Jan 1, 2030 00:00:00 UTC)
    long epochMillis = JAN_1_2030_00_00_UTC;

    // When: Formatting the timestamp
    var result = FilterHelper.formatTimestamp(epochMillis);

    // Then: Should return valid ISO 8601 timestamp
    assertThat(result).isNotNull();
    assertThat(result)
        .as("Timestamp should match ISO 8601 format: " + result)
        .matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[+-]\\d{2}:\\d{2}");
    assertThat(result.startsWith("2029-12-31") || result.startsWith("2030-01-01"))
        .as("Should represent Jan 1, 2030 in local timezone: " + result)
        .isTrue();
  }

  @Test
  void testFormatTimestamp_UsesSystemDefaultTimezone() {
    // Given: A known timestamp
    long epochMillis = JAN_15_2025_10_30_UTC;

    // When: Formatting the timestamp
    var result = FilterHelper.formatTimestamp(epochMillis);

    // Then: Should use system default timezone
    var expected =
        ZonedDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
    // Use the same formatter pattern as production code (lowercase 'xxx' always uses numeric
    // offsets)
    var formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx");
    var expectedFormat = expected.format(formatter);

    assertThat(result)
        .as("Should format timestamp using system default timezone")
        .isEqualTo(expectedFormat);
  }

  @Test
  void testFormatTimestamp_PreservesTimezone() {
    // Given: A timestamp
    long epochMillis = JAN_15_2025_10_30_UTC;

    // When: Formatting the timestamp
    var result = FilterHelper.formatTimestamp(epochMillis);

    // Then: Should include timezone offset in the output
    assertThat(result.contains("+") || result.contains("-"))
        .as("Timestamp should include timezone offset (+ or -): " + result)
        .isTrue();

    // Extract and verify timezone offset format
    var timezoneOffset = result.substring(result.length() - 6);
    assertThat(timezoneOffset)
        .as("Timezone offset should be in format +/-HH:MM: " + timezoneOffset)
        .matches("[+-]\\d{2}:\\d{2}");
  }

  @Test
  void testFormatTimestamp_ConsistentFormat() {
    // Given: Multiple different timestamps
    long[] timestamps = {SEP_9_2001_01_46_UTC, JUL_14_2017_02_40_UTC, JAN_15_2025_10_30_UTC};

    // When: Formatting all timestamps
    for (long timestamp : timestamps) {
      var result = FilterHelper.formatTimestamp(timestamp);

      // Then: All should match ISO 8601 format with timezone
      assertThat(result).isNotNull();
      assertThat(result)
          .as("All timestamps should match ISO 8601 format: " + result)
          .matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[+-]\\d{2}:\\d{2}");
    }
  }
}
