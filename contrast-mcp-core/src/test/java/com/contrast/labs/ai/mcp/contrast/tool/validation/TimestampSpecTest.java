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
package com.contrast.labs.ai.mcp.contrast.tool.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TimestampSpecTest {

  private ToolValidationContext ctx;

  @BeforeEach
  void setUp() {
    ctx = new ToolValidationContext();
  }

  @Test
  void get_should_parse_iso_instant() {
    var result = ctx.timestampParam("2025-01-01T00:00:00Z", "startTime").get();

    assertThat(result.toInstant()).isEqualTo(Instant.parse("2025-01-01T00:00:00Z"));
    assertThat(ctx.isValid()).isTrue();
  }

  @Test
  void get_should_parse_iso_timestamp_with_numeric_offset() {
    var result = ctx.timestampParam("2025-01-01T00:00:00-05:00", "startTime").get();

    assertThat(result.toInstant()).isEqualTo(Instant.parse("2025-01-01T05:00:00Z"));
    assertThat(ctx.isValid()).isTrue();
  }

  @Test
  void get_should_reject_iso_date_without_time() {
    var result = ctx.timestampParam("2025-01-15", "startTime").get();

    assertThat(result).isNull();
    assertThat(ctx.isValid()).isFalse();
    assertThat(ctx.errors()).hasSize(1);
    assertThat(ctx.errors().get(0)).contains("Invalid startTime timestamp");
    assertThat(ctx.errors().get(0)).contains("date, time, and timezone offset");
  }

  @Test
  void get_should_reject_iso_timestamp_without_timezone_offset() {
    var result = ctx.timestampParam("2025-01-15T10:30:00", "startTime").get();

    assertThat(result).isNull();
    assertThat(ctx.isValid()).isFalse();
    assertThat(ctx.errors()).hasSize(1);
    assertThat(ctx.errors().get(0)).contains("Invalid startTime timestamp");
    assertThat(ctx.errors().get(0)).contains("date, time, and timezone offset");
  }

  @Test
  void get_should_parse_epoch_timestamp() {
    long epochMillis = 1705276800000L;
    var result = ctx.timestampParam(String.valueOf(epochMillis), "startTime").get();

    assertThat(result.getTime()).isEqualTo(epochMillis);
    assertThat(ctx.isValid()).isTrue();
  }

  @Test
  void get_should_reject_negative_epoch_timestamp() {
    var result = ctx.timestampParam("-1", "startTime").get();

    assertThat(result).isNull();
    assertThat(ctx.isValid()).isFalse();
    assertThat(ctx.errors()).hasSize(1);
    assertThat(ctx.errors().get(0)).contains("Invalid startTime timestamp");
    assertThat(ctx.errors().get(0)).contains("between 0 and 253402300799999");
  }

  @Test
  void get_should_reject_epoch_timestamp_after_supported_iso_year_range() {
    var result = ctx.timestampParam(String.valueOf(Long.MAX_VALUE), "startTime").get();

    assertThat(result).isNull();
    assertThat(ctx.isValid()).isFalse();
    assertThat(ctx.errors()).hasSize(1);
    assertThat(ctx.errors().get(0)).contains("Invalid startTime timestamp");
    assertThat(ctx.errors().get(0)).contains("between 0 and 253402300799999");
  }

  @Test
  void get_should_return_null_when_null() {
    var result = ctx.timestampParam(null, "startTime").get();

    assertThat(result).isNull();
    assertThat(ctx.isValid()).isTrue();
  }

  @Test
  void get_should_return_null_when_blank() {
    var result = ctx.timestampParam("   ", "startTime").get();

    assertThat(result).isNull();
    assertThat(ctx.isValid()).isTrue();
  }

  @Test
  void get_should_add_error_for_invalid_format() {
    var result = ctx.timestampParam("not-a-timestamp", "startTime").get();

    assertThat(result).isNull();
    assertThat(ctx.isValid()).isFalse();
    assertThat(ctx.errors()).hasSize(1);
    assertThat(ctx.errors().get(0)).contains("Invalid startTime timestamp");
    assertThat(ctx.errors().get(0)).contains("ISO timestamp");
    assertThat(ctx.errors().get(0)).contains("date, time, and timezone offset");
    assertThat(ctx.errors().get(0)).contains("epoch timestamp");
  }

  @Test
  void get_should_sanitize_control_characters_in_validation_message() {
    var result = ctx.timestampParam("bad\nvalue\rnext", "startTime").get();

    assertThat(result).isNull();
    assertThat(ctx.isValid()).isFalse();
    assertThat(ctx.errors()).hasSize(1);
    assertThat(ctx.errors().get(0)).doesNotContain("\n");
    assertThat(ctx.errors().get(0)).doesNotContain("\r");
    assertThat(ctx.errors().get(0)).contains("bad\\nvalue\\rnext");
  }
}
