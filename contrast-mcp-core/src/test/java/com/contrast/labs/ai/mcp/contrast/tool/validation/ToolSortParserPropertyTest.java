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

import java.util.Map;
import java.util.Set;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

class ToolSortParserPropertyTest {

  private static final Map<String, String> FIELDS =
      Map.of("severity", "severityValue", "title", "vulnTitle", "lastSeen", "last_time_seen");
  private static final String DEFAULT_SORT = "severityValue";
  private static final Set<String> WIRE_VALUES = Set.copyOf(FIELDS.values());

  @Property
  void valid_asc_sort_should_return_wire_name(@ForAll("validProperty") String property) {
    var ctx = new ToolValidationContext();
    var sort = property + ",ASC";
    var result = ToolSortParser.parse(ctx, sort, FIELDS, DEFAULT_SORT);

    assertThat(result).isIn(WIRE_VALUES);
    assertThat(result).isEqualTo(FIELDS.get(property));
    assertThat(ctx.isValid()).isTrue();
  }

  @Property
  void valid_desc_sort_should_return_prefixed_wire_name(@ForAll("validProperty") String property) {
    var ctx = new ToolValidationContext();
    var sort = property + ",DESC";
    var result = ToolSortParser.parse(ctx, sort, FIELDS, DEFAULT_SORT);

    assertThat(result).startsWith("-");
    assertThat(result.substring(1)).isIn(WIRE_VALUES);
    assertThat(ctx.isValid()).isTrue();
  }

  @Property
  void property_should_be_case_insensitive(@ForAll("validPropertyRandomCase") String property) {
    var ctx = new ToolValidationContext();
    var sort = property + ",ASC";
    var result = ToolSortParser.parse(ctx, sort, FIELDS, DEFAULT_SORT);

    assertThat(result).isNotNull().isIn(WIRE_VALUES);
    assertThat(ctx.isValid()).isTrue();
  }

  @Property
  void direction_should_be_case_insensitive(
      @ForAll("validProperty") String property, @ForAll("directionCase") String direction) {
    var ctx = new ToolValidationContext();
    var sort = property + "," + direction;
    var result = ToolSortParser.parse(ctx, sort, FIELDS, DEFAULT_SORT);

    assertThat(result).isNotNull();
    assertThat(ctx.isValid()).isTrue();
  }

  @Property
  void malformed_input_should_return_default_sort(@ForAll("malformedSort") String sort) {
    var ctx = new ToolValidationContext();
    var result = ToolSortParser.parse(ctx, sort, FIELDS, DEFAULT_SORT);

    assertThat(result).isEqualTo(DEFAULT_SORT);
    assertThat(ctx.isValid()).isFalse();
    assertThat(ctx.errors()).isNotEmpty();
  }

  @Property
  void null_or_blank_input_should_return_default_sort(@ForAll("blankOrNull") String sort) {
    var ctx = new ToolValidationContext();
    var result = ToolSortParser.parse(ctx, sort, FIELDS, DEFAULT_SORT);

    assertThat(result).isEqualTo(DEFAULT_SORT);
    assertThat(ctx.isValid()).isTrue();
  }

  @Property
  void invalid_property_should_add_error(@ForAll("invalidProperty") String property) {
    var ctx = new ToolValidationContext();
    var sort = property + ",ASC";
    ToolSortParser.parse(ctx, sort, FIELDS, DEFAULT_SORT);

    assertThat(ctx.isValid()).isFalse();
    assertThat(ctx.errors()).isNotEmpty();
    assertThat(ctx.errors().getFirst()).contains("Invalid sort");
  }

  @Property
  void output_wire_name_should_always_come_from_fields_values(
      @ForAll("validSortString") String sort) {
    var ctx = new ToolValidationContext();
    var result = ToolSortParser.parse(ctx, sort, FIELDS, DEFAULT_SORT);

    assertThat(ctx.isValid()).isTrue();
    assertThat(result).isNotNull();
    var bare = result.startsWith("-") ? result.substring(1) : result;
    assertThat(bare).isIn(WIRE_VALUES);
  }

  @Provide
  Arbitrary<String> validProperty() {
    return Arbitraries.of(FIELDS.keySet().toArray(String[]::new));
  }

  @Provide
  Arbitrary<String> validPropertyRandomCase() {
    return validProperty()
        .map(
            s -> {
              var sb = new StringBuilder();
              for (int i = 0; i < s.length(); i++) {
                sb.append(
                    i % 2 == 0
                        ? Character.toUpperCase(s.charAt(i))
                        : Character.toLowerCase(s.charAt(i)));
              }
              return sb.toString();
            });
  }

  @Provide
  Arbitrary<String> directionCase() {
    return Arbitraries.of("asc", "ASC", "Asc", "desc", "DESC", "Desc", "aSc", "dEsC");
  }

  @Provide
  Arbitrary<String> malformedSort() {
    return Arbitraries.of(
        "severity", "severity,ASC,extra", ",ASC", "severity,INVALID", ",,", "unknown,ASC");
  }

  @Provide
  Arbitrary<String> blankOrNull() {
    return Arbitraries.of(null, "", "   ", " \t ");
  }

  @Provide
  Arbitrary<String> invalidProperty() {
    return Arbitraries.of("unknown", "SEVERITY_WRONG", "foobar", "name");
  }

  @Provide
  Arbitrary<String> validSortString() {
    return Combinators.combine(validProperty(), Arbitraries.of("ASC", "DESC"))
        .as((prop, dir) -> prop + "," + dir);
  }
}
