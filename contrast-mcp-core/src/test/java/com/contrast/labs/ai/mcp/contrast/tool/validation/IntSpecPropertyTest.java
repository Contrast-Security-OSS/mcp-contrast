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
    Assume.that(min <= max);
    var ctx = new ToolValidationContext();
    ctx.intParam(value, "x").range(min, max).get();

    assertThat(ctx.notices()).hasSizeLessThanOrEqualTo(1);
  }

  @Property
  void no_notice_when_value_in_range(@ForAll @IntRange(min = 1, max = 100) int value) {
    var ctx = new ToolValidationContext();
    ctx.intParam(value, "x").range(1, 100).get();

    assertThat(ctx.notices()).isEmpty();
  }

  @Property
  void notice_emitted_when_value_below_min(
      @ForAll @IntRange(min = Integer.MIN_VALUE, max = 0) int value) {
    var ctx = new ToolValidationContext();
    ctx.intParam(value, "x").range(1, 100).get();

    assertThat(ctx.notices()).hasSize(1);
    assertThat(ctx.notices().getFirst()).contains("clamped");
  }

  @Property
  void notice_emitted_when_value_above_max(@ForAll @IntRange(min = 101) int value) {
    var ctx = new ToolValidationContext();
    ctx.intParam(value, "x").range(1, 100).get();

    assertThat(ctx.notices()).hasSize(1);
    assertThat(ctx.notices().getFirst()).contains("clamped");
  }

  @Property
  void default_used_when_null(@ForAll int defaultVal) {
    var ctx = new ToolValidationContext();
    var result = ctx.intParam(null, "x").defaultTo(defaultVal, "fallback").get();

    assertThat(result).isEqualTo(defaultVal);
    assertThat(ctx.notices()).containsExactly("fallback");
  }

  @Property
  void null_when_no_value_and_no_default() {
    var ctx = new ToolValidationContext();
    var result = ctx.intParam(null, "x").get();

    assertThat(result).isNull();
    assertThat(ctx.notices()).isEmpty();
  }

  @Property
  void value_returned_unchanged_when_no_range(@ForAll int value) {
    var ctx = new ToolValidationContext();
    var result = ctx.intParam(value, "x").get();

    assertThat(result).isEqualTo(value);
    assertThat(ctx.notices()).isEmpty();
  }

  @Property
  void no_errors_added_for_any_input(
      @ForAll Integer value,
      @ForAll @IntRange(min = -1000, max = 1000) int min,
      @ForAll @IntRange(min = -1000, max = 1000) int max) {
    Assume.that(min <= max);
    var ctx = new ToolValidationContext();
    ctx.intParam(value, "x").range(min, max).get();

    assertThat(ctx.isValid()).isTrue();
  }
}
