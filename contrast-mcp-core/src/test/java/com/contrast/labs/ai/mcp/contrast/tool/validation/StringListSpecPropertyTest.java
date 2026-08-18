package com.contrast.labs.ai.mcp.contrast.tool.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Assume;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

class StringListSpecPropertyTest {

  private static final Set<String> ALLOWED = Set.of("Reported", "Confirmed", "Fixed");

  @Property
  void output_should_be_subset_of_allowed_values(@ForAll("csvOfAllowed") String csv) {
    var ctx = new ToolValidationContext();
    var result = ctx.stringListParam(csv, "statuses").allowedValues(ALLOWED).get();

    assertThat(result).as("valid CSV input should parse successfully").isNotNull();
    assertThat(result).allSatisfy(item -> assertThat(ALLOWED).contains(item));
  }

  @Property
  void case_insensitive_matching_should_normalize_to_canonical(
      @ForAll("allowedValueInRandomCase") String item) {
    var ctx = new ToolValidationContext();
    var result = ctx.stringListParam(item, "statuses").allowedValues(ALLOWED).get();

    assertThat(result).isNotNull().hasSize(1);
    assertThat(ALLOWED).contains(result.getFirst());
  }

  @Property
  void unknown_tokens_should_produce_error_and_null_result(@ForAll("unknownToken") String token) {
    var ctx = new ToolValidationContext();
    var result = ctx.stringListParam(token, "statuses").allowedValues(ALLOWED).get();

    assertThat(ctx.isValid()).isFalse();
    assertThat(result).isNull();
    assertThat(ctx.errors()).isNotEmpty();
  }

  @Property
  void null_input_with_default_should_return_default() {
    var defaultVal = List.of("Reported", "Confirmed");
    var ctx = new ToolValidationContext();
    var result =
        ctx.stringListParam(null, "statuses")
            .allowedValues(ALLOWED)
            .defaultTo(defaultVal, "using defaults")
            .get();

    assertThat(result).isEqualTo(defaultVal);
    assertThat(ctx.notices()).containsExactly("using defaults");
  }

  @Property
  void null_input_without_default_should_return_null() {
    var ctx = new ToolValidationContext();
    var result = ctx.stringListParam(null, "statuses").get();

    assertThat(result).isNull();
    assertThat(ctx.isValid()).isTrue();
  }

  @Property
  void normalization_should_be_idempotent(@ForAll("csvOfAllowed") String csv) {
    var ctx1 = new ToolValidationContext();
    var first = ctx1.stringListParam(csv, "s").allowedValues(ALLOWED).get();
    Assume.that(first != null);

    var recombined = String.join(",", first);
    var ctx2 = new ToolValidationContext();
    var second = ctx2.stringListParam(recombined, "s").allowedValues(ALLOWED).get();

    assertThat(second).isEqualTo(first);
  }

  @Property
  void toUpperCase_should_uppercase_all_items(@ForAll("asciiAlpha") String csv) {
    var ctx = new ToolValidationContext();
    var result = ctx.stringListParam(csv, "tags").toUpperCase().get();

    assertThat(result).as("valid CSV input should parse successfully").isNotNull();
    assertThat(result).allSatisfy(item -> assertThat(item).isEqualTo(item.toUpperCase()));
  }

  @Provide
  Arbitrary<String> csvOfAllowed() {
    return Arbitraries.of(ALLOWED.toArray(String[]::new))
        .list()
        .ofMinSize(1)
        .ofMaxSize(3)
        .map(list -> String.join(",", list));
  }

  @Provide
  Arbitrary<String> allowedValueInRandomCase() {
    return Arbitraries.of(ALLOWED.toArray(String[]::new))
        .map(
            s -> {
              var chars = s.toCharArray();
              var sb = new StringBuilder();
              for (int i = 0; i < chars.length; i++) {
                sb.append(
                    i % 2 == 0 ? Character.toUpperCase(chars[i]) : Character.toLowerCase(chars[i]));
              }
              return sb.toString();
            });
  }

  @Provide
  Arbitrary<String> unknownToken() {
    return Arbitraries.of("INVALID", "Unknown", "bad", "NotAStatus", "PENDING");
  }

  @Provide
  Arbitrary<String> asciiAlpha() {
    return Arbitraries.strings()
        .alpha()
        .ofMinLength(1)
        .ofMaxLength(5)
        .list()
        .ofMinSize(1)
        .ofMaxSize(3)
        .map(list -> String.join(",", list));
  }
}
