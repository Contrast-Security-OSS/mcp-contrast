package com.contrast.labs.ai.mcp.contrast.tool.base;

import static org.assertj.core.api.Assertions.assertThat;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.LongRange;

class FilterHelperPropertyTest {

  @Property
  void parseCommaSeparated_should_never_contain_blank_strings(
      @ForAll("commaSeparatedInput") String input) {
    var result = FilterHelper.parseCommaSeparated(input);

    if (result != null) {
      assertThat(result).allSatisfy(s -> assertThat(s).isNotBlank());
    }
  }

  @Property
  void parseCommaSeparated_should_return_null_for_blank_input(@ForAll("blankOrNull") String input) {
    var result = FilterHelper.parseCommaSeparated(input);

    assertThat(result).isNull();
  }

  @Property
  void parseCommaSeparated_should_round_trip_through_join(
      @ForAll("commaSeparatedWithContent") String input) {
    var first = FilterHelper.parseCommaSeparated(input);

    if (first != null) {
      var rejoined = String.join(",", first);
      var second = FilterHelper.parseCommaSeparated(rejoined);
      assertThat(second).isEqualTo(first);
    }
  }

  @Property
  void parseCommaSeparatedUpperCase_should_produce_all_uppercase(
      @ForAll("commaSeparatedWithContent") String input) {
    var result = FilterHelper.parseCommaSeparatedUpperCase(input);

    if (result != null) {
      assertThat(result).allSatisfy(s -> assertThat(s).isEqualTo(s.toUpperCase()));
    }
  }

  @Property
  void parseCommaSeparatedUpperCase_should_return_null_for_blank_input(
      @ForAll("blankOrNull") String input) {
    assertThat(FilterHelper.parseCommaSeparatedUpperCase(input)).isNull();
  }

  @Property
  void parseDateWithValidation_should_succeed_for_valid_epoch(
      @ForAll @LongRange(min = 0, max = 253402300799999L) long epoch) {
    var result = FilterHelper.parseDateWithValidation(String.valueOf(epoch), "param");

    assertThat(result.getValue()).isNotNull();
    assertThat(result.hasValidationMessage()).isFalse();
    assertThat(result.getValue().getTime()).isEqualTo(epoch);
  }

  @Property
  void parseDateWithValidation_should_fail_for_negative_epoch(
      @ForAll @LongRange(min = Long.MIN_VALUE, max = -1) long epoch) {
    var result = FilterHelper.parseDateWithValidation(String.valueOf(epoch), "param");

    assertThat(result.getValue()).isNull();
    assertThat(result.hasValidationMessage()).isTrue();
    assertThat(result.getValidationMessage()).contains("Invalid param date");
  }

  @Property
  void parseDateWithValidation_should_fail_for_above_max_epoch(
      @ForAll @LongRange(min = 253402300800000L) long epoch) {
    var result = FilterHelper.parseDateWithValidation(String.valueOf(epoch), "param");

    assertThat(result.getValue()).isNull();
    assertThat(result.hasValidationMessage()).isTrue();
  }

  @Property
  void parseDateWithValidation_should_never_return_value_for_invalid_string(
      @ForAll("invalidDateString") String input) {
    var result = FilterHelper.parseDateWithValidation(input, "param");

    assertThat(result.getValue()).isNull();
    assertThat(result.hasValidationMessage()).isTrue();
  }

  @Property
  void parseTimestampWithValidation_should_succeed_for_valid_epoch(
      @ForAll @LongRange(min = 0, max = 253402300799999L) long epoch) {
    var result = FilterHelper.parseTimestampWithValidation(String.valueOf(epoch), "param");

    assertThat(result.getValue()).isNotNull();
    assertThat(result.hasValidationMessage()).isFalse();
    assertThat(result.getValue().getTime()).isEqualTo(epoch);
  }

  @Property
  void parseTimestampWithValidation_should_fail_for_out_of_range(
      @ForAll("outOfRangeEpoch") long epoch) {
    var result = FilterHelper.parseTimestampWithValidation(String.valueOf(epoch), "param");

    assertThat(result.getValue()).isNull();
    assertThat(result.hasValidationMessage()).isTrue();
    assertThat(result.getValidationMessage()).contains("Invalid param timestamp");
  }

  @Property
  void parseTimestampWithValidation_should_never_return_value_for_invalid_string(
      @ForAll("invalidDateString") String input) {
    var result = FilterHelper.parseTimestampWithValidation(input, "param");

    assertThat(result.getValue()).isNull();
    assertThat(result.hasValidationMessage()).isTrue();
  }

  @Provide
  Arbitrary<String> commaSeparatedInput() {
    var token = Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(8);
    var padding = Arbitraries.of("", " ", "  ");
    var segment = Combinators.combine(padding, token, padding).as((pre, t, post) -> pre + t + post);
    var emptySegment = Arbitraries.of("", " ");
    var mixed = Arbitraries.oneOf(segment, emptySegment);
    return mixed.list().ofMinSize(1).ofMaxSize(6).map(parts -> String.join(",", parts));
  }

  @Provide
  Arbitrary<String> commaSeparatedWithContent() {
    return Arbitraries.strings()
        .alpha()
        .ofMinLength(1)
        .ofMaxLength(8)
        .list()
        .ofMinSize(1)
        .ofMaxSize(5)
        .map(tokens -> String.join(",", tokens));
  }

  @Provide
  Arbitrary<String> blankOrNull() {
    return Arbitraries.of(null, "", "   ", " \t ", "\n");
  }

  @Provide
  Arbitrary<String> invalidDateString() {
    return Arbitraries.oneOf(
        Arbitraries.strings().alpha().ofMinLength(2).ofMaxLength(12),
        Arbitraries.integers()
            .between(1, 12)
            .flatMap(
                m ->
                    Arbitraries.integers()
                        .between(1, 28)
                        .map(d -> String.format("%02d-%02d-2025", m, d))));
  }

  @Provide
  Arbitrary<Long> outOfRangeEpoch() {
    return Arbitraries.oneOf(
        Arbitraries.longs().between(Long.MIN_VALUE, -1),
        Arbitraries.longs().between(253402300800000L, Long.MAX_VALUE));
  }
}
