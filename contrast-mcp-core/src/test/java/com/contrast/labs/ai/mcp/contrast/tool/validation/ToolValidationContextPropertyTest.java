package com.contrast.labs.ai.mcp.contrast.tool.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Assume;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.IntRange;

class ToolValidationContextPropertyTest {

  @Property
  void isValid_should_be_true_iff_errors_empty(
      @ForAll @IntRange(min = 0, max = 5) int errorCount,
      @ForAll @IntRange(min = 0, max = 5) int noticeCount) {
    var ctx = new ToolValidationContext();
    for (int i = 0; i < errorCount; i++) {
      ctx.addError("error-" + i);
    }
    for (int i = 0; i < noticeCount; i++) {
      ctx.addNotice("notice-" + i);
    }

    assertThat(ctx.isValid()).isEqualTo(errorCount == 0);
  }

  @Property
  void errors_should_preserve_insertion_order(@ForAll @IntRange(min = 1, max = 10) int count) {
    var ctx = new ToolValidationContext();
    var expected = new ArrayList<String>();
    for (int i = 0; i < count; i++) {
      var msg = "error-" + i;
      ctx.addError(msg);
      expected.add(msg);
    }

    assertThat(ctx.errors()).containsExactlyElementsOf(expected);
  }

  @Property
  void notices_should_preserve_insertion_order(@ForAll @IntRange(min = 1, max = 10) int count) {
    var ctx = new ToolValidationContext();
    var expected = new ArrayList<String>();
    for (int i = 0; i < count; i++) {
      var msg = "notice-" + i;
      ctx.addNotice(msg);
      expected.add(msg);
    }

    assertThat(ctx.notices()).containsExactlyElementsOf(expected);
  }

  @Property
  void requireUuid_should_accept_uuid_toString(@ForAll("randomUuid") String uuid) {
    var ctx = new ToolValidationContext();
    ctx.requireUuid(uuid, "id");

    assertThat(ctx.isValid()).isTrue();
    assertThat(ctx.errors()).isEmpty();
  }

  @Property
  void requireUuid_should_reject_non_uuid(@ForAll("nonUuid") String value) {
    var ctx = new ToolValidationContext();
    ctx.requireUuid(value, "id");

    assertThat(ctx.isValid()).isFalse();
    assertThat(ctx.errors()).hasSize(1);
  }

  @Property
  void requireUuid_should_reject_blank(@ForAll("blankString") String value) {
    var ctx = new ToolValidationContext();
    ctx.requireUuid(value, "id");

    assertThat(ctx.isValid()).isFalse();
    assertThat(ctx.errors()).hasSize(1);
    assertThat(ctx.errors().getFirst()).contains("is required");
  }

  @Property
  void errors_list_should_be_immutable(@ForAll @IntRange(min = 0, max = 3) int count) {
    var ctx = new ToolValidationContext();
    for (int i = 0; i < count; i++) {
      ctx.addError("e" + i);
    }

    assertThatThrownBy(() -> ctx.errors().add("injected"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Property
  void notices_list_should_be_immutable(@ForAll @IntRange(min = 0, max = 3) int count) {
    var ctx = new ToolValidationContext();
    for (int i = 0; i < count; i++) {
      ctx.addNotice("n" + i);
    }

    assertThatThrownBy(() -> ctx.notices().add("injected"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Property
  void require_should_reject_blank_or_null(@ForAll("blankOrNull") String value) {
    var ctx = new ToolValidationContext();
    ctx.require(value, "field");

    assertThat(ctx.isValid()).isFalse();
    assertThat(ctx.errors()).hasSize(1);
    assertThat(ctx.errors().getFirst()).contains("is required");
  }

  @Property
  void require_should_accept_non_blank(@ForAll("nonBlankString") String value) {
    var ctx = new ToolValidationContext();
    ctx.require(value, "field");

    assertThat(ctx.isValid()).isTrue();
  }

  @Property
  void noticeIf_should_add_notice_only_when_true(@ForAll boolean condition) {
    var ctx = new ToolValidationContext();
    ctx.noticeIf(condition, "conditional notice");

    if (condition) {
      assertThat(ctx.notices()).containsExactly("conditional notice");
    } else {
      assertThat(ctx.notices()).isEmpty();
    }
  }

  @Property
  void errorIf_should_add_error_only_when_true(@ForAll boolean condition) {
    var ctx = new ToolValidationContext();
    ctx.errorIf(condition, "conditional error");

    assertThat(ctx.isValid()).isEqualTo(!condition);
  }

  @Property
  void validateDateRange_should_add_error_when_start_after_end(
      @ForAll int endMillis, @ForAll @IntRange(min = 1) int gap) {
    var end = new Date(endMillis);
    var start = new Date((long) endMillis + gap);
    var ctx = new ToolValidationContext();
    ctx.validateDateRange(start, end, "startDate", "endDate");

    assertThat(ctx.isValid()).isFalse();
    assertThat(ctx.errors()).hasSize(1);
    assertThat(ctx.errors().getFirst()).contains("startDate").contains("endDate");
  }

  @Property
  void validateDateRange_should_accept_valid_range(
      @ForAll int startMillis, @ForAll @IntRange(min = 0) int gap) {
    var start = new Date(startMillis);
    var end = new Date((long) startMillis + gap);
    var ctx = new ToolValidationContext();
    ctx.validateDateRange(start, end, "startDate", "endDate");

    assertThat(ctx.isValid()).isTrue();
    assertThat(ctx.errors()).isEmpty();
  }

  @Property
  void validateDateRange_should_accept_nulls(@ForAll boolean startNull, @ForAll boolean endNull) {
    Assume.that(startNull || endNull);
    var start = startNull ? null : new Date(0);
    var end = endNull ? null : new Date(0);
    var ctx = new ToolValidationContext();
    ctx.validateDateRange(start, end, "startDate", "endDate");

    assertThat(ctx.isValid()).isTrue();
  }

  @Property
  void validateTimestampRange_should_add_error_when_start_after_end(
      @ForAll int endMillis, @ForAll @IntRange(min = 1) int gap) {
    var end = new Date(endMillis);
    var start = new Date((long) endMillis + gap);
    var ctx = new ToolValidationContext();
    ctx.validateTimestampRange(start, end, "startTs", "endTs");

    assertThat(ctx.isValid()).isFalse();
    assertThat(ctx.errors()).hasSize(1);
    assertThat(ctx.errors().getFirst()).contains("startTs").contains("endTs");
  }

  @Property
  void validateTimestampRange_should_accept_valid_range(
      @ForAll int startMillis, @ForAll @IntRange(min = 0) int gap) {
    var start = new Date(startMillis);
    var end = new Date((long) startMillis + gap);
    var ctx = new ToolValidationContext();
    ctx.validateTimestampRange(start, end, "startTs", "endTs");

    assertThat(ctx.isValid()).isTrue();
    assertThat(ctx.errors()).isEmpty();
  }

  @Provide
  Arbitrary<String> randomUuid() {
    return Arbitraries.longs().tuple2().map(t -> new UUID(t.get1(), t.get2()).toString());
  }

  @Provide
  Arbitrary<String> nonUuid() {
    return Arbitraries.of("not-a-uuid", "12345", "abc", "550e8400-ZZZZ-41d4-a716-446655440000");
  }

  @Provide
  Arbitrary<String> blankString() {
    return Arbitraries.of("", "   ", " \t\n ");
  }

  @Provide
  Arbitrary<String> blankOrNull() {
    return Arbitraries.of(null, "", "   ", " \t ");
  }

  @Provide
  Arbitrary<String> nonBlankString() {
    return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(20);
  }
}
