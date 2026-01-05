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

import java.util.EnumSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EnumSetSpecTest {

  private ToolValidationContext ctx;

  enum Priority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
  }

  @BeforeEach
  void setUp() {
    ctx = new ToolValidationContext();
  }

  @Test
  void get_should_parse_comma_separated_enum_values() {
    var result = ctx.enumSetParam("LOW, HIGH", Priority.class, "priorities").get();

    assertThat(result).containsExactlyInAnyOrder(Priority.LOW, Priority.HIGH);
    assertThat(ctx.isValid()).isTrue();
  }

  @Test
  void get_should_be_case_insensitive() {
    var result = ctx.enumSetParam("low, High, MEDIUM", Priority.class, "priorities").get();

    assertThat(result).containsExactlyInAnyOrder(Priority.LOW, Priority.HIGH, Priority.MEDIUM);
    assertThat(ctx.isValid()).isTrue();
  }

  @Test
  void get_should_return_null_when_null_and_no_default() {
    var result = ctx.enumSetParam(null, Priority.class, "priorities").get();

    assertThat(result).isNull();
    assertThat(ctx.isValid()).isTrue();
  }

  @Test
  void get_should_return_null_when_blank_and_no_default() {
    var result = ctx.enumSetParam("   ", Priority.class, "priorities").get();

    assertThat(result).isNull();
    assertThat(ctx.isValid()).isTrue();
  }

  @Test
  void get_should_return_default_when_null() {
    var result =
        ctx.enumSetParam(null, Priority.class, "priorities")
            .defaultTo(EnumSet.of(Priority.HIGH, Priority.CRITICAL), "Using high priorities")
            .get();

    assertThat(result).containsExactlyInAnyOrder(Priority.HIGH, Priority.CRITICAL);
    assertThat(ctx.warnings()).containsExactly("Using high priorities");
  }

  @Test
  void get_should_add_error_for_invalid_value() {
    var result = ctx.enumSetParam("LOW, INVALID", Priority.class, "priorities").get();

    assertThat(result).isNull();
    assertThat(ctx.isValid()).isFalse();
    assertThat(ctx.errors()).hasSize(1);
    assertThat(ctx.errors().get(0)).contains("Invalid priorities: 'INVALID'");
    assertThat(ctx.errors().get(0)).contains("LOW, MEDIUM, HIGH, CRITICAL");
  }

  @Test
  void get_should_report_multiple_invalid_values() {
    var result = ctx.enumSetParam("BAD1, LOW, BAD2", Priority.class, "priorities").get();

    assertThat(result).isNull();
    assertThat(ctx.isValid()).isFalse();
    assertThat(ctx.errors()).hasSize(2);
  }

  @Test
  void get_should_handle_empty_items_in_list() {
    var result = ctx.enumSetParam("LOW,,HIGH,  ,MEDIUM", Priority.class, "priorities").get();

    assertThat(result).containsExactlyInAnyOrder(Priority.LOW, Priority.HIGH, Priority.MEDIUM);
    assertThat(ctx.isValid()).isTrue();
  }

  @Test
  void get_should_handle_all_enum_values() {
    var result =
        ctx.enumSetParam("LOW, MEDIUM, HIGH, CRITICAL", Priority.class, "priorities").get();

    assertThat(result).containsExactlyInAnyOrder(Priority.values());
  }

  @Test
  void get_should_deduplicate_values() {
    var result = ctx.enumSetParam("LOW, LOW, HIGH, LOW", Priority.class, "priorities").get();

    assertThat(result).containsExactlyInAnyOrder(Priority.LOW, Priority.HIGH);
  }

  @Test
  void get_should_return_copy_of_default() {
    var defaultSet = EnumSet.of(Priority.HIGH);
    var result =
        ctx.enumSetParam(null, Priority.class, "priorities")
            .defaultTo(defaultSet, "Using default")
            .get();

    defaultSet.add(Priority.LOW);

    assertThat(result).containsExactly(Priority.HIGH);
  }
}
