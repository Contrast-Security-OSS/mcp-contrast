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

/**
 * Fluent validation spec for integer parameters. Supports default values and range clamping with
 * warnings.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * var page = ctx.intParam(pageParam, "page")
 *     .defaultTo(1, "Using default page")
 *     .range(1, 1000)
 *     .get();
 * }</pre>
 */
public class IntSpec {

  private final ToolValidationContext ctx;
  private final Integer value;
  private final String name;
  private Integer defaultValue;
  private String defaultReason;
  private Integer min;
  private Integer max;

  IntSpec(ToolValidationContext ctx, Integer value, String name) {
    this.ctx = ctx;
    this.value = value;
    this.name = name;
  }

  /**
   * Sets a default value to use when the parameter is null.
   *
   * @param val the default value
   * @param reason explanation for AI feedback (added as warning when default is used)
   * @return this for fluent chaining
   */
  public IntSpec defaultTo(int val, String reason) {
    this.defaultValue = val;
    this.defaultReason = reason;
    return this;
  }

  /**
   * Sets valid range. Values outside range are clamped with a warning.
   *
   * @param min minimum value (inclusive)
   * @param max maximum value (inclusive)
   * @return this for fluent chaining
   */
  public IntSpec range(int min, int max) {
    this.min = min;
    this.max = max;
    return this;
  }

  /**
   * Validates and returns the final value.
   *
   * @return validated integer value, or null if no value and no default
   */
  public Integer get() {
    if (value == null) {
      if (defaultValue != null) {
        ctx.addWarning(defaultReason);
        return defaultValue;
      }
      return null;
    }

    int result = value;

    if (min != null && value < min) {
      ctx.addWarning(String.format("%s clamped from %d to minimum %d", name, value, min));
      result = min;
    } else if (max != null && value > max) {
      ctx.addWarning(String.format("%s clamped from %d to maximum %d", name, value, max));
      result = max;
    }

    return result;
  }
}
