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

package com.contrast.labs.ai.mcp.contrast.tool.validation;

import static org.assertj.core.api.Assertions.assertThat;

import net.jqwik.api.Assume;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;

class IntSpecPropertyTest {

  @Property
  void output_should_be_in_range_when_both_bounds_set(
      @ForAll int value,
      @ForAll @IntRange(min = -1000, max = 1000) int min,
      @ForAll @IntRange(min = -1000, max = 1000) int max) {
    // min <= max is a caller precondition: IntSpec.range() does not define behavior
    // for inverted bounds. See bead for the inverted-bounds edge case.
    Assume.that(min <= max);
    var ctx = new ToolValidationContext();
    var result = ctx.intParam(value, "x").range(min, max).get();

    assertThat(result).isBetween(min, max);
  }

  @Property
  void notice_count_should_be_zero_or_one(
      @ForAll Integer value,
      @ForAll @IntRange(min = -1000, max = 1000) int min,
      @ForAll @IntRange(min = -1000, max = 1000) int max) {
    Assume.that(min <= max); // see output_should_be_in_range WHY comment
    var ctx = new ToolValidationContext();
    ctx.intParam(value, "x").range(min, max).get();

    assertThat(ctx.notices()).hasSizeLessThanOrEqualTo(1);
  }

  @Property
  void notice_should_be_empty_when_value_in_range(@ForAll @IntRange(min = 1, max = 100) int value) {
    var ctx = new ToolValidationContext();
    ctx.intParam(value, "x").range(1, 100).get();

    assertThat(ctx.notices()).isEmpty();
  }

  @Property
  void notice_should_be_emitted_when_value_below_min(
      @ForAll @IntRange(min = Integer.MIN_VALUE, max = 0) int value) {
    var ctx = new ToolValidationContext();
    ctx.intParam(value, "x").range(1, 100).get();

    assertThat(ctx.notices()).hasSize(1);
    assertThat(ctx.notices().getFirst()).contains("clamped");
  }

  @Property
  void notice_should_be_emitted_when_value_above_max(@ForAll @IntRange(min = 101) int value) {
    var ctx = new ToolValidationContext();
    ctx.intParam(value, "x").range(1, 100).get();

    assertThat(ctx.notices()).hasSize(1);
    assertThat(ctx.notices().getFirst()).contains("clamped");
  }

  @Property
  void default_should_be_used_when_null(@ForAll int defaultVal) {
    var ctx = new ToolValidationContext();
    var result = ctx.intParam(null, "x").defaultTo(defaultVal, "fallback").get();

    assertThat(result).isEqualTo(defaultVal);
    assertThat(ctx.notices()).containsExactly("fallback");
  }

  @Property
  void result_should_be_null_when_no_value_and_no_default() {
    var ctx = new ToolValidationContext();
    var result = ctx.intParam(null, "x").get();

    assertThat(result).isNull();
    assertThat(ctx.notices()).isEmpty();
  }

  @Property
  void value_should_be_returned_unchanged_when_no_range(@ForAll int value) {
    var ctx = new ToolValidationContext();
    var result = ctx.intParam(value, "x").get();

    assertThat(result).isEqualTo(value);
    assertThat(ctx.notices()).isEmpty();
  }

  @Property
  void errors_should_be_empty_for_any_input(
      @ForAll Integer value,
      @ForAll @IntRange(min = -1000, max = 1000) int min,
      @ForAll @IntRange(min = -1000, max = 1000) int max) {
    Assume.that(min <= max); // see output_should_be_in_range WHY comment
    var ctx = new ToolValidationContext();
    ctx.intParam(value, "x").range(min, max).get();

    assertThat(ctx.isValid()).isTrue();
  }
}
