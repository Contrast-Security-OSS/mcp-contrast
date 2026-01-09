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

import org.junit.jupiter.api.Test;

class MetadataJsonFilterSpecTest {

  @Test
  void get_should_parse_simple_key_value() {
    var ctx = new ToolValidationContext();
    var result = ctx.metadataJsonFilterParam("{\"key\":\"value\"}", "test").get();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).fieldName()).isEqualTo("key");
    assertThat(result.get(0).values()).containsExactly("value");
    assertThat(ctx.isValid()).isTrue();
  }

  @Test
  void get_should_parse_array_values() {
    var ctx = new ToolValidationContext();
    var result = ctx.metadataJsonFilterParam("{\"key\":[\"a\",\"b\"]}", "test").get();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).fieldName()).isEqualTo("key");
    assertThat(result.get(0).values()).containsExactly("a", "b");
    assertThat(ctx.isValid()).isTrue();
  }

  @Test
  void get_should_return_null_for_empty_input() {
    var ctx = new ToolValidationContext();
    assertThat(ctx.metadataJsonFilterParam("", "test").get()).isNull();
    assertThat(ctx.isValid()).isTrue();
  }

  @Test
  void get_should_return_null_for_null_input() {
    var ctx = new ToolValidationContext();
    assertThat(ctx.metadataJsonFilterParam(null, "test").get()).isNull();
    assertThat(ctx.isValid()).isTrue();
  }

  @Test
  void get_should_return_null_for_empty_json_object() {
    var ctx = new ToolValidationContext();
    assertThat(ctx.metadataJsonFilterParam("{}", "test").get()).isNull();
    assertThat(ctx.isValid()).isTrue();
  }

  @Test
  void get_should_add_error_for_invalid_json() {
    var ctx = new ToolValidationContext();
    assertThat(ctx.metadataJsonFilterParam("not json", "test").get()).isNull();
    assertThat(ctx.isValid()).isFalse();
    assertThat(ctx.errors()).anyMatch(e -> e.contains("Invalid JSON"));
  }

  @Test
  void get_should_convert_number_to_string() {
    var ctx = new ToolValidationContext();
    var result = ctx.metadataJsonFilterParam("{\"count\":42}", "test").get();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).fieldName()).isEqualTo("count");
    assertThat(result.get(0).values()).containsExactly("42");
    assertThat(ctx.isValid()).isTrue();
  }

  @Test
  void get_should_convert_number_in_array_to_string() {
    var ctx = new ToolValidationContext();
    var result = ctx.metadataJsonFilterParam("{\"ids\":[1,2,3]}", "test").get();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).fieldName()).isEqualTo("ids");
    assertThat(result.get(0).values()).containsExactly("1", "2", "3");
    assertThat(ctx.isValid()).isTrue();
  }

  @Test
  void get_should_reject_complex_object_value() {
    var ctx = new ToolValidationContext();
    var result = ctx.metadataJsonFilterParam("{\"nested\":{\"a\":\"b\"}}", "test").get();
    assertThat(result).isNull();
    assertThat(ctx.isValid()).isFalse();
    assertThat(ctx.errors())
        .anyMatch(e -> e.contains("nested") && e.contains("expected string or array"));
  }

  @Test
  void get_should_reject_complex_object_in_array() {
    var ctx = new ToolValidationContext();
    var result = ctx.metadataJsonFilterParam("{\"items\":[{\"a\":\"b\"}]}", "test").get();
    assertThat(result).isNull();
    assertThat(ctx.isValid()).isFalse();
    assertThat(ctx.errors()).anyMatch(e -> e.contains("items") && e.contains("non-string values"));
  }

  @Test
  void get_should_handle_mixed_valid_values() {
    var ctx = new ToolValidationContext();
    var result =
        ctx.metadataJsonFilterParam("{\"str\":\"val\",\"arr\":[\"a\",\"b\"],\"num\":123}", "test")
            .get();

    assertThat(result).hasSize(3);
    // Note: LinkedHashMap preserves insertion order
    assertThat(result.get(0).fieldName()).isEqualTo("str");
    assertThat(result.get(0).values()).containsExactly("val");
    assertThat(result.get(1).fieldName()).isEqualTo("arr");
    assertThat(result.get(1).values()).containsExactly("a", "b");
    assertThat(result.get(2).fieldName()).isEqualTo("num");
    assertThat(result.get(2).values()).containsExactly("123");
    assertThat(ctx.isValid()).isTrue();
  }

  @Test
  void get_should_preserve_insertion_order() {
    var ctx = new ToolValidationContext();
    var result =
        ctx.metadataJsonFilterParam("{\"branch\":\"main\",\"developer\":\"Ellen\"}", "test").get();

    assertThat(result).hasSize(2);
    assertThat(result.get(0).fieldName()).isEqualTo("branch");
    assertThat(result.get(1).fieldName()).isEqualTo("developer");
  }

  // Tests for mutuallyExclusive method on ToolValidationContext
  @Test
  void mutuallyExclusive_should_add_error_when_both_present() {
    var ctx = new ToolValidationContext();
    ctx.mutuallyExclusive(true, "paramA", true, "paramB", "They conflict");
    assertThat(ctx.isValid()).isFalse();
    assertThat(ctx.errors())
        .anyMatch(
            e -> e.contains("paramA") && e.contains("paramB") && e.contains("mutually exclusive"));
  }

  @Test
  void mutuallyExclusive_should_not_add_error_when_only_first_present() {
    var ctx = new ToolValidationContext();
    ctx.mutuallyExclusive(true, "paramA", false, "paramB", "They conflict");
    assertThat(ctx.isValid()).isTrue();
  }

  @Test
  void mutuallyExclusive_should_not_add_error_when_only_second_present() {
    var ctx = new ToolValidationContext();
    ctx.mutuallyExclusive(false, "paramA", true, "paramB", "They conflict");
    assertThat(ctx.isValid()).isTrue();
  }

  @Test
  void mutuallyExclusive_should_not_add_error_when_neither_present() {
    var ctx = new ToolValidationContext();
    ctx.mutuallyExclusive(false, "paramA", false, "paramB", "They conflict");
    assertThat(ctx.isValid()).isTrue();
  }
}
