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

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DateSpecTest {

  private ToolValidationContext ctx;

  @BeforeEach
  void setUp() {
    ctx = new ToolValidationContext();
  }

  @Test
  void get_should_parse_iso_date() {
    var result = ctx.dateParam("2025-01-15", "startDate").get();

    assertThat(result).isNotNull();
    var localDate = result.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    assertThat(localDate).isEqualTo(LocalDate.of(2025, 1, 15));
    assertThat(ctx.isValid()).isTrue();
  }

  @Test
  void get_should_parse_epoch_timestamp() {
    long epochMillis = 1705276800000L;
    var result = ctx.dateParam(String.valueOf(epochMillis), "startDate").get();

    assertThat(result).isNotNull();
    assertThat(result.getTime()).isEqualTo(epochMillis);
    assertThat(ctx.isValid()).isTrue();
  }

  @Test
  void get_should_return_null_when_null() {
    var result = ctx.dateParam(null, "startDate").get();

    assertThat(result).isNull();
    assertThat(ctx.isValid()).isTrue();
  }

  @Test
  void get_should_return_null_when_blank() {
    var result = ctx.dateParam("   ", "startDate").get();

    assertThat(result).isNull();
    assertThat(ctx.isValid()).isTrue();
  }

  @Test
  void get_should_add_error_for_invalid_format() {
    var result = ctx.dateParam("not-a-date", "startDate").get();

    assertThat(result).isNull();
    assertThat(ctx.isValid()).isFalse();
    assertThat(ctx.errors()).hasSize(1);
    assertThat(ctx.errors().get(0)).contains("Invalid startDate date");
    assertThat(ctx.errors().get(0)).contains("YYYY-MM-DD");
    assertThat(ctx.errors().get(0)).contains("epoch timestamp");
  }

  @Test
  void get_should_add_error_for_invalid_date() {
    var result = ctx.dateParam("2025-13-45", "lastSeen").get();

    assertThat(result).isNull();
    assertThat(ctx.isValid()).isFalse();
    assertThat(ctx.errors()).hasSize(1);
  }

  @Test
  void get_should_trim_whitespace() {
    var result = ctx.dateParam("  2025-01-15  ", "startDate").get();

    assertThat(result).isNotNull();
    assertThat(ctx.isValid()).isTrue();
  }

  @Test
  void get_should_parse_different_dates() {
    Date startDate = ctx.dateParam("2025-01-01", "startDate").get();
    Date endDate = ctx.dateParam("2025-12-31", "endDate").get();

    assertThat(startDate).isBefore(endDate);
    assertThat(ctx.isValid()).isTrue();
  }
}
