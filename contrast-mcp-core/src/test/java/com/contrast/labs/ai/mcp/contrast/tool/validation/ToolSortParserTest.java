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

import java.util.Map;
import org.junit.jupiter.api.Test;

class ToolSortParserTest {

  private static final Map<String, String> FIELDS =
      Map.of("lastActivity", "last_activity", "name", "server_name");

  @Test
  void parse_should_translate_case_insensitive_property_and_direction() {
    var context = new ToolValidationContext();

    var result = ToolSortParser.parse(context, "LASTACTIVITY,desc", FIELDS, false, null);

    assertThat(result).isEqualTo("-last_activity");
    assertThat(context.isValid()).isTrue();
  }

  @Test
  void parse_should_preserve_case_sensitive_property_contract() {
    var context = new ToolValidationContext();

    var result = ToolSortParser.parse(context, "NAME,ASC", FIELDS, true, null);

    assertThat(result).isNull();
    assertThat(context.errors())
        .containsExactly(
            "Invalid sort: 'NAME,ASC'. Expected format: property,DIRECTION. Valid properties:"
                + " lastActivity, name. Valid directions: ASC, DESC.");
  }

  @Test
  void parse_should_return_default_for_absent_or_invalid_sort() {
    var absentContext = new ToolValidationContext();
    var invalidContext = new ToolValidationContext();

    var absent = ToolSortParser.parse(absentContext, null, FIELDS, false, "-last_activity");
    var invalid = ToolSortParser.parse(invalidContext, "name", FIELDS, false, "-last_activity");

    assertThat(absent).isEqualTo("-last_activity");
    assertThat(absentContext.isValid()).isTrue();
    assertThat(invalid).isEqualTo("-last_activity");
    assertThat(invalidContext.isValid()).isFalse();
  }
}
