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

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for FilterHelper utility methods, focusing on timestamp formatting.
 */
class FilterHelperTest {

    // Named constants for test timestamps
    private static final long JAN_15_2025_10_30_UTC = LocalDateTime.of(2025, 1, 15, 10, 30)
            .toInstant(ZoneOffset.UTC).toEpochMilli(); // 1736938200000L

    private static final long JAN_1_2030_00_00_UTC = LocalDateTime.of(2030, 1, 1, 0, 0)
            .toInstant(ZoneOffset.UTC).toEpochMilli(); // 1893456000000L

    private static final long SEP_9_2001_01_46_UTC = LocalDateTime.of(2001, 9, 9, 1, 46, 40)
            .toInstant(ZoneOffset.UTC).toEpochMilli(); // 1000000000000L

    private static final long JUL_14_2017_02_40_UTC = LocalDateTime.of(2017, 7, 14, 2, 40)
            .toInstant(ZoneOffset.UTC).toEpochMilli(); // 1500000000000L

    @Test
    void testFormatTimestamp_ValidTimestamp() {
        // Given: A known epoch timestamp (Jan 15, 2025 10:30:00 UTC)
        long epochMillis = JAN_15_2025_10_30_UTC;

        // When: Formatting the timestamp
        String result = FilterHelper.formatTimestamp(epochMillis);

        // Then: Should return ISO 8601 format with timezone offset
        assertNotNull(result);
        assertTrue(result.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[+-]\\d{2}:\\d{2}"),
                "Timestamp should match ISO 8601 format with timezone offset: " + result);

        // Verify it can be parsed back
        ZonedDateTime parsed = ZonedDateTime.parse(result, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        assertEquals(epochMillis, parsed.toInstant().toEpochMilli(),
                "Parsed timestamp should match original epoch milliseconds");
    }

    @Test
    void testFormatTimestamp_NullInput() {
        // Given: Null input
        Long nullValue = null;

        // When: Formatting null
        String result = FilterHelper.formatTimestamp(nullValue);

        // Then: Should return null
        assertNull(result, "Formatting null timestamp should return null");
    }

    @Test
    void testFormatTimestamp_EpochZero() {
        // Given: Epoch zero (Jan 1, 1970 00:00:00 UTC)
        long epochMillis = 0L;

        // When: Formatting epoch zero
        String result = FilterHelper.formatTimestamp(epochMillis);

        // Then: Should return valid ISO 8601 timestamp
        assertNotNull(result);
        assertTrue(result.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[+-]\\d{2}:\\d{2}"),
                "Timestamp should match ISO 8601 format: " + result);
        assertTrue(result.startsWith("1970-01-01") || result.startsWith("1969-12-31"),
                "Epoch zero should be Jan 1, 1970 UTC (or Dec 31, 1969 in negative timezone): " + result);
    }

    @Test
    void testFormatTimestamp_FutureDate() {
        // Given: A future timestamp (Jan 1, 2030 00:00:00 UTC)
        long epochMillis = JAN_1_2030_00_00_UTC;

        // When: Formatting the timestamp
        String result = FilterHelper.formatTimestamp(epochMillis);

        // Then: Should return valid ISO 8601 timestamp
        assertNotNull(result);
        assertTrue(result.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[+-]\\d{2}:\\d{2}"),
                "Timestamp should match ISO 8601 format: " + result);
        assertTrue(result.startsWith("2029-12-31") || result.startsWith("2030-01-01"),
                "Should represent Jan 1, 2030 in local timezone: " + result);
    }

    @Test
    void testFormatTimestamp_UsesSystemDefaultTimezone() {
        // Given: A known timestamp
        long epochMillis = JAN_15_2025_10_30_UTC;

        // When: Formatting the timestamp
        String result = FilterHelper.formatTimestamp(epochMillis);

        // Then: Should use system default timezone
        ZonedDateTime expected = ZonedDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(epochMillis),
                ZoneId.systemDefault()
        );
        // Use the same formatter pattern as production code (lowercase 'xxx' always uses numeric offsets)
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx");
        String expectedFormat = expected.format(formatter);

        assertEquals(expectedFormat, result,
                "Should format timestamp using system default timezone");
    }

    @Test
    void testFormatTimestamp_PreservesTimezone() {
        // Given: A timestamp
        long epochMillis = JAN_15_2025_10_30_UTC;

        // When: Formatting the timestamp
        String result = FilterHelper.formatTimestamp(epochMillis);

        // Then: Should include timezone offset in the output
        assertTrue(result.contains("+") || result.contains("-"),
                "Timestamp should include timezone offset (+ or -): " + result);

        // Extract and verify timezone offset format
        String timezoneOffset = result.substring(result.length() - 6);
        assertTrue(timezoneOffset.matches("[+-]\\d{2}:\\d{2}"),
                "Timezone offset should be in format +/-HH:MM: " + timezoneOffset);
    }

    @Test
    void testFormatTimestamp_ConsistentFormat() {
        // Given: Multiple different timestamps
        long[] timestamps = {
                SEP_9_2001_01_46_UTC,
                JUL_14_2017_02_40_UTC,
                JAN_15_2025_10_30_UTC
        };

        // When: Formatting all timestamps
        for (long timestamp : timestamps) {
            String result = FilterHelper.formatTimestamp(timestamp);

            // Then: All should match ISO 8601 format with timezone
            assertNotNull(result);
            assertTrue(result.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[+-]\\d{2}:\\d{2}"),
                    "All timestamps should match ISO 8601 format: " + result);
        }
    }
}
