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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IntSpecTest {

  private ToolValidationContext ctx;

  @BeforeEach
  void setUp() {
    ctx = new ToolValidationContext();
  }

  @Test
  void get_should_return_value_when_provided() {
    var result = ctx.intParam(42, "count").get();

    assertThat(result).isEqualTo(42);
    assertThat(ctx.isValid()).isTrue();
    assertThat(ctx.warnings()).isEmpty();
  }

  @Test
  void get_should_return_null_when_null_and_no_default() {
    var result = ctx.intParam(null, "count").get();

    assertThat(result).isNull();
    assertThat(ctx.isValid()).isTrue();
    assertThat(ctx.warnings()).isEmpty();
  }

  @Test
  void get_should_return_default_when_null() {
    var result = ctx.intParam(null, "page").defaultTo(1, "Using default page").get();

    assertThat(result).isEqualTo(1);
    assertThat(ctx.isValid()).isTrue();
    assertThat(ctx.warnings()).containsExactly("Using default page");
  }

  @Test
  void get_should_clamp_to_minimum_when_below_range() {
    var result = ctx.intParam(0, "page").range(1, 100).get();

    assertThat(result).isEqualTo(1);
    assertThat(ctx.isValid()).isTrue();
    assertThat(ctx.warnings()).containsExactly("page clamped from 0 to minimum 1");
  }

  @Test
  void get_should_clamp_to_maximum_when_above_range() {
    var result = ctx.intParam(200, "pageSize").range(1, 100).get();

    assertThat(result).isEqualTo(100);
    assertThat(ctx.isValid()).isTrue();
    assertThat(ctx.warnings()).containsExactly("pageSize clamped from 200 to maximum 100");
  }

  @Test
  void get_should_return_value_when_within_range() {
    var result = ctx.intParam(50, "page").range(1, 100).get();

    assertThat(result).isEqualTo(50);
    assertThat(ctx.isValid()).isTrue();
    assertThat(ctx.warnings()).isEmpty();
  }

  @Test
  void get_should_return_value_at_range_boundaries() {
    var resultMin = ctx.intParam(1, "page").range(1, 100).get();
    var resultMax = ctx.intParam(100, "pageSize").range(1, 100).get();

    assertThat(resultMin).isEqualTo(1);
    assertThat(resultMax).isEqualTo(100);
    assertThat(ctx.warnings()).isEmpty();
  }

  @Test
  void get_should_apply_default_before_range_check() {
    var result = ctx.intParam(null, "page").defaultTo(1, "Using default page").range(1, 100).get();

    assertThat(result).isEqualTo(1);
    assertThat(ctx.warnings()).containsExactly("Using default page");
  }

  @Test
  void get_should_clamp_negative_value() {
    var result = ctx.intParam(-5, "page").range(1, 100).get();

    assertThat(result).isEqualTo(1);
    assertThat(ctx.warnings()).containsExactly("page clamped from -5 to minimum 1");
  }
}
