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

import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StringSpecTest {

  private ToolValidationContext ctx;

  @BeforeEach
  void setUp() {
    ctx = new ToolValidationContext();
  }

  @Test
  void get_should_return_value_when_provided() {
    var result = ctx.stringParam("test-value", "name").get();

    assertThat(result).isEqualTo("test-value");
    assertThat(ctx.isValid()).isTrue();
  }

  @Test
  void get_should_return_null_when_null_and_no_default() {
    var result = ctx.stringParam(null, "name").get();

    assertThat(result).isNull();
    assertThat(ctx.isValid()).isTrue();
  }

  @Test
  void get_should_return_null_when_blank_and_no_default() {
    var result = ctx.stringParam("   ", "name").get();

    assertThat(result).isNull();
    assertThat(ctx.isValid()).isTrue();
  }

  @Test
  void get_should_trim_value() {
    var result = ctx.stringParam("  test  ", "name").get();

    assertThat(result).isEqualTo("test");
  }

  @Test
  void get_should_return_default_when_null() {
    var result = ctx.stringParam(null, "status").defaultTo("active", "Using default status").get();

    assertThat(result).isEqualTo("active");
    assertThat(ctx.warnings()).containsExactly("Using default status");
  }

  @Test
  void get_should_return_default_when_blank() {
    var result = ctx.stringParam("  ", "status").defaultTo("active", "Using default status").get();

    assertThat(result).isEqualTo("active");
    assertThat(ctx.warnings()).containsExactly("Using default status");
  }

  @Test
  void get_should_add_error_when_required_and_null() {
    var result = ctx.stringParam(null, "appId").required().get();

    assertThat(result).isNull();
    assertThat(ctx.isValid()).isFalse();
    assertThat(ctx.errors()).containsExactly("appId is required");
  }

  @Test
  void get_should_add_error_when_required_and_blank() {
    var result = ctx.stringParam("   ", "appId").required().get();

    assertThat(result).isNull();
    assertThat(ctx.isValid()).isFalse();
    assertThat(ctx.errors()).containsExactly("appId is required");
  }

  @Test
  void get_should_validate_allowed_values() {
    var result =
        ctx.stringParam("invalid", "status").allowedValues(Set.of("active", "inactive")).get();

    assertThat(result).isNull();
    assertThat(ctx.isValid()).isFalse();
    assertThat(ctx.errors()).hasSize(1);
    assertThat(ctx.errors().get(0)).contains("Invalid status: 'invalid'");
  }

  @Test
  void get_should_accept_allowed_value() {
    var result =
        ctx.stringParam("active", "status").allowedValues(Set.of("active", "inactive")).get();

    assertThat(result).isEqualTo("active");
    assertThat(ctx.isValid()).isTrue();
  }

  @Test
  void get_should_apply_default_before_checking_required() {
    var result =
        ctx.stringParam(null, "status")
            .defaultTo("active", "Using default")
            .required()
            .allowedValues(Set.of("active", "inactive"))
            .get();

    assertThat(result).isEqualTo("active");
    assertThat(ctx.isValid()).isTrue();
  }

  @Test
  void get_should_skip_allowed_check_when_null() {
    var result = ctx.stringParam(null, "status").allowedValues(Set.of("active", "inactive")).get();

    assertThat(result).isNull();
    assertThat(ctx.isValid()).isTrue();
  }
}
