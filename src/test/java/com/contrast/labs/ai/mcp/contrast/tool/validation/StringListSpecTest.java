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

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StringListSpecTest {

  private ToolValidationContext ctx;

  @BeforeEach
  void setUp() {
    ctx = new ToolValidationContext();
  }

  @Test
  void get_should_parse_comma_separated_values() {
    var result = ctx.stringListParam("a, b, c", "items").get();

    assertThat(result).containsExactly("a", "b", "c");
    assertThat(ctx.isValid()).isTrue();
  }

  @Test
  void get_should_return_null_when_null_and_no_default() {
    var result = ctx.stringListParam(null, "items").get();

    assertThat(result).isNull();
    assertThat(ctx.isValid()).isTrue();
  }

  @Test
  void get_should_return_null_when_blank_and_no_default() {
    var result = ctx.stringListParam("   ", "items").get();

    assertThat(result).isNull();
    assertThat(ctx.isValid()).isTrue();
  }

  @Test
  void get_should_return_default_when_null() {
    var result =
        ctx.stringListParam(null, "statuses")
            .defaultTo(List.of("Reported", "Confirmed"), "Using default statuses")
            .get();

    assertThat(result).containsExactly("Reported", "Confirmed");
    assertThat(ctx.warnings()).containsExactly("Using default statuses");
  }

  @Test
  void get_should_validate_allowed_values() {
    var result =
        ctx.stringListParam("Reported, Invalid", "statuses")
            .allowedValues(Set.of("Reported", "Confirmed", "Fixed"))
            .get();

    assertThat(result).isNull();
    assertThat(ctx.isValid()).isFalse();
    assertThat(ctx.errors()).hasSize(1);
    assertThat(ctx.errors().get(0)).contains("Invalid statuses: 'Invalid'");
  }

  @Test
  void get_should_accept_all_allowed_values() {
    var result =
        ctx.stringListParam("Reported, Confirmed", "statuses")
            .allowedValues(Set.of("Reported", "Confirmed", "Fixed"))
            .get();

    assertThat(result).containsExactly("Reported", "Confirmed");
    assertThat(ctx.isValid()).isTrue();
  }

  @Test
  void get_should_handle_empty_items_in_list() {
    var result = ctx.stringListParam("a,,b,  ,c", "items").get();

    assertThat(result).containsExactly("a", "b", "c");
  }

  @Test
  void get_should_trim_items() {
    var result = ctx.stringListParam("  a  ,  b  ", "items").get();

    assertThat(result).containsExactly("a", "b");
  }

  @Test
  void get_should_report_multiple_invalid_values() {
    var result =
        ctx.stringListParam("Bad1, Good, Bad2", "items")
            .allowedValues(Set.of("Good", "Also Good"))
            .get();

    assertThat(result).isNull();
    assertThat(ctx.isValid()).isFalse();
    assertThat(ctx.errors()).hasSize(2);
  }

  @Test
  void get_should_return_immutable_list() {
    var result = ctx.stringListParam("a, b", "items").get();

    assertThat(result).isUnmodifiable();
  }

  @Test
  void get_should_return_immutable_default() {
    var result =
        ctx.stringListParam(null, "items").defaultTo(List.of("a", "b"), "Using default").get();

    assertThat(result).isUnmodifiable();
  }
}
