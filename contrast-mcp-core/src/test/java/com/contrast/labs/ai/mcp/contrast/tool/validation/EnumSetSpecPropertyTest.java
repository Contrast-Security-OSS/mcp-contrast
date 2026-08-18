package com.contrast.labs.ai.mcp.contrast.tool.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

class EnumSetSpecPropertyTest {

  enum Color {
    RED,
    GREEN,
    BLUE,
    YELLOW
  }

  @Property
  void round_trip_should_preserve_identity(@ForAll("colorSubset") Set<Color> subset) {
    var csv = subset.stream().map(Enum::name).collect(Collectors.joining(","));
    var ctx = new ToolValidationContext();
    var result = ctx.enumSetParam(csv, Color.class, "colors").get();

    assertThat(result).containsExactlyInAnyOrderElementsOf(subset);
    assertThat(ctx.isValid()).isTrue();
  }

  @Property
  void parsing_should_be_case_insensitive(@ForAll("colorInRandomCase") String csv) {
    var ctx = new ToolValidationContext();
    var result = ctx.enumSetParam(csv, Color.class, "colors").get();

    assertThat(result).isNotNull().hasSize(1);
    assertThat(result.iterator().next().name()).isEqualToIgnoringCase(csv);
    assertThat(ctx.isValid()).isTrue();
  }

  @Property
  void unknown_tokens_should_produce_error_and_null(@ForAll("unknownColorToken") String token) {
    var ctx = new ToolValidationContext();
    var result = ctx.enumSetParam(token, Color.class, "colors").get();

    assertThat(ctx.isValid()).isFalse();
    assertThat(result).isNull();
    assertThat(ctx.errors()).isNotEmpty();
    assertThat(ctx.errors().getFirst())
        .contains("Invalid colors")
        .contains("RED, GREEN, BLUE, YELLOW");
  }

  @Property
  void null_input_without_default_should_return_null() {
    var ctx = new ToolValidationContext();
    var result = ctx.enumSetParam(null, Color.class, "colors").get();

    assertThat(result).isNull();
    assertThat(ctx.isValid()).isTrue();
  }

  @Property
  void null_input_with_default_should_return_default(@ForAll("colorSubset") Set<Color> subset) {
    var defaultSet = EnumSet.copyOf(subset);
    var ctx = new ToolValidationContext();
    var result =
        ctx.enumSetParam(null, Color.class, "colors").defaultTo(defaultSet, "using defaults").get();

    assertThat(result).containsExactlyInAnyOrderElementsOf(subset);
    assertThat(ctx.notices()).containsExactly("using defaults");
  }

  @Property
  void deduplication_should_produce_set_semantics(@ForAll("colorSubset") Set<Color> subset) {
    var duplicated =
        subset.stream()
            .flatMap(c -> java.util.stream.Stream.of(c.name(), c.name()))
            .collect(Collectors.joining(","));
    var ctx = new ToolValidationContext();
    var result = ctx.enumSetParam(duplicated, Color.class, "colors").get();

    assertThat(result).containsExactlyInAnyOrderElementsOf(subset);
  }

  @Property
  void blank_input_without_default_should_return_null(@ForAll("blankString") String blank) {
    var ctx = new ToolValidationContext();
    var result = ctx.enumSetParam(blank, Color.class, "colors").get();

    assertThat(result).isNull();
    assertThat(ctx.isValid()).isTrue();
  }

  @Provide
  Arbitrary<Set<Color>> colorSubset() {
    return Arbitraries.of(Color.class).set().ofMinSize(1);
  }

  @Provide
  Arbitrary<String> colorInRandomCase() {
    return Arbitraries.of(Color.class)
        .map(
            c -> {
              var name = c.name();
              var sb = new StringBuilder();
              for (int i = 0; i < name.length(); i++) {
                sb.append(
                    i % 2 == 0
                        ? Character.toLowerCase(name.charAt(i))
                        : Character.toUpperCase(name.charAt(i)));
              }
              return sb.toString();
            });
  }

  @Provide
  Arbitrary<String> unknownColorToken() {
    return Arbitraries.of("PURPLE", "orange", "PINK", "Magenta");
  }

  @Provide
  Arbitrary<String> blankString() {
    return Arbitraries.of("", "   ", " \t ");
  }
}
