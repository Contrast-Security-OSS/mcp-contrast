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

import java.util.Set;
import org.springframework.util.StringUtils;

/**
 * Fluent validation spec for string parameters. Supports required validation, default values, and
 * allowed value constraints.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * var status = ctx.stringParam(statusParam, "status")
 *     .allowedValues(Set.of("active", "inactive"))
 *     .get();
 *
 * var appId = ctx.stringParam(appIdParam, "applicationId")
 *     .required()
 *     .get();
 * }</pre>
 */
public class StringSpec {

  private final ToolValidationContext ctx;
  private String value;
  private final String name;
  private String defaultValue;
  private String defaultReason;
  private Set<String> allowedValues;
  private boolean required;

  StringSpec(ToolValidationContext ctx, String value, String name) {
    this.ctx = ctx;
    this.value = value;
    this.name = name;
  }

  /**
   * Sets a default value to use when the parameter is null or blank.
   *
   * @param val the default value
   * @param reason explanation for AI feedback (added as warning when default is used)
   * @return this for fluent chaining
   */
  public StringSpec defaultTo(String val, String reason) {
    this.defaultValue = val;
    this.defaultReason = reason;
    return this;
  }

  /**
   * Sets allowed values. If value is not in set, adds an error.
   *
   * @param values set of valid values
   * @return this for fluent chaining
   */
  public StringSpec allowedValues(Set<String> values) {
    this.allowedValues = values;
    return this;
  }

  /**
   * Marks parameter as required. If null or blank, adds an error.
   *
   * @return this for fluent chaining
   */
  public StringSpec required() {
    this.required = true;
    return this;
  }

  /**
   * Converts the value to uppercase. Useful for case-insensitive enum matching.
   *
   * @return this for fluent chaining
   */
  public StringSpec toUpperCase() {
    if (this.value != null) {
      this.value = this.value.toUpperCase();
    }
    return this;
  }

  /**
   * Validates and returns the final value.
   *
   * @return validated string value, or null if no value and no default
   */
  public String get() {
    String result = StringUtils.hasText(value) ? value.trim() : null;

    if (result == null && defaultValue != null) {
      ctx.addWarning(defaultReason);
      result = defaultValue;
    }

    if (required && result == null) {
      ctx.addError(String.format("%s is required", name));
      return null;
    }

    if (result != null && allowedValues != null && !allowedValues.contains(result)) {
      ctx.addError(
          String.format(
              "Invalid %s: '%s'. Valid values: %s",
              name, result, String.join(", ", allowedValues)));
      return null;
    }

    return result;
  }
}
