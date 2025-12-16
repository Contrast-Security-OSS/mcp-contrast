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

import com.contrast.labs.ai.mcp.contrast.FilterHelper;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Fluent validation spec for comma-separated enum set parameters. Parses comma-separated enum names
 * (case-insensitive) into an EnumSet with type-safe validation.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * var severities = ctx.enumSetParam(sevParam, RuleSeverity.class, "severities")
 *     .defaultTo(EnumSet.allOf(RuleSeverity.class), "Including all severities")
 *     .get();
 * }</pre>
 *
 * @param <E> the enum type
 */
public class EnumSetSpec<E extends Enum<E>> {

  private final ToolValidationContext ctx;
  private final String value;
  private final Class<E> enumClass;
  private final String name;
  private EnumSet<E> defaultValue;
  private String defaultReason;

  EnumSetSpec(ToolValidationContext ctx, String value, Class<E> enumClass, String name) {
    this.ctx = ctx;
    this.value = value;
    this.enumClass = enumClass;
    this.name = name;
  }

  /**
   * Sets a default value to use when the parameter is null or blank.
   *
   * @param val the default EnumSet value
   * @param reason explanation for AI feedback (added as warning when default is used)
   * @return this for fluent chaining
   */
  public EnumSetSpec<E> defaultTo(EnumSet<E> val, String reason) {
    this.defaultValue = val;
    this.defaultReason = reason;
    return this;
  }

  /**
   * Validates and returns the parsed EnumSet.
   *
   * @return validated EnumSet, or null if no value and no default
   */
  public EnumSet<E> get() {
    List<String> items = FilterHelper.parseCommaSeparatedUpperCase(value);

    if (items == null) {
      if (defaultValue != null) {
        ctx.addWarning(defaultReason);
        return EnumSet.copyOf(defaultValue);
      }
      return null;
    }

    EnumSet<E> result = EnumSet.noneOf(enumClass);
    String validValues =
        Arrays.stream(enumClass.getEnumConstants())
            .map(Enum::name)
            .collect(Collectors.joining(", "));

    for (String item : items) {
      try {
        result.add(Enum.valueOf(enumClass, item));
      } catch (IllegalArgumentException e) {
        ctx.addError(String.format("Invalid %s: '%s'. Valid values: %s", name, item, validValues));
      }
    }

    return ctx.isValid() ? result : null;
  }
}
