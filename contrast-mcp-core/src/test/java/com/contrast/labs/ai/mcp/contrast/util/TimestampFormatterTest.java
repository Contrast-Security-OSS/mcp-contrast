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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TimestampFormatterTest {

  @Test
  void formatTimestamp_should_return_iso8601_with_timezone_offset() {
    var result = TimestampFormatter.formatTimestamp(1705328400000L);

    assertThat(result).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[+-]\\d{2}:\\d{2}");
  }

  @Test
  void formatTimestamp_should_return_null_for_null_input() {
    assertThat(TimestampFormatter.formatTimestamp(null)).isNull();
  }

  @Test
  void formatTimestampWithMillis_should_return_utc_with_millisecond_precision() {
    var result = TimestampFormatter.formatTimestampWithMillis(1705328400123L);

    assertThat(result).isEqualTo("2024-01-15T14:20:00.123+00:00");
  }

  @Test
  void formatTimestampWithMillis_should_return_null_for_null_input() {
    assertThat(TimestampFormatter.formatTimestampWithMillis(null)).isNull();
  }

  @Test
  void formatTimestampWithMillis_should_handle_zero_epoch() {
    assertThat(TimestampFormatter.formatTimestampWithMillis(0L))
        .isEqualTo("1970-01-01T00:00:00.000+00:00");
  }

  @Test
  void formatTimestampWithMillis_should_handle_negative_epoch() {
    assertThat(TimestampFormatter.formatTimestampWithMillis(-1L))
        .isEqualTo("1969-12-31T23:59:59.999+00:00");
  }
}
