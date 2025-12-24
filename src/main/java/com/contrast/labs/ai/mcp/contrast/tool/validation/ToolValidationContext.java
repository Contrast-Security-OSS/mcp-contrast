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

import com.contrast.labs.ai.mcp.contrast.tool.base.ToolParams;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.springframework.util.StringUtils;

/**
 * Fluent validation context for MCP tool parameters. Collects errors and warnings during
 * validation, providing AI-friendly feedback messages.
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * var ctx = new ToolValidationContext();
 *
 * var severities = ctx.enumSetParam(sevParam, RuleSeverity.class, "severities")
 *     .defaultTo(EnumSet.allOf(RuleSeverity.class), "Including all severities")
 *     .get();
 *
 * var page = ctx.intParam(pageParam, "page")
 *     .defaultTo(1, "Using default page")
 *     .range(1, 1000)
 *     .get();
 *
 * if (!ctx.isValid()) {
 *   // Handle errors
 * }
 * }</pre>
 */
public class ToolValidationContext implements ToolParams {

  private final List<String> warnings = new ArrayList<>();
  private final List<String> errors = new ArrayList<>();

  /**
   * Creates a fluent spec for integer parameter validation.
   *
   * @param value the parameter value (may be null)
   * @param name the parameter name for error messages
   * @return IntSpec for fluent configuration
   */
  public IntSpec intParam(Integer value, String name) {
    return new IntSpec(this, value, name);
  }

  /**
   * Creates a fluent spec for string parameter validation.
   *
   * @param value the parameter value (may be null or empty)
   * @param name the parameter name for error messages
   * @return StringSpec for fluent configuration
   */
  public StringSpec stringParam(String value, String name) {
    return new StringSpec(this, value, name);
  }

  /**
   * Creates a fluent spec for comma-separated string list parameter validation.
   *
   * @param value comma-separated string (may be null or empty)
   * @param name the parameter name for error messages
   * @return StringListSpec for fluent configuration
   */
  public StringListSpec stringListParam(String value, String name) {
    return new StringListSpec(this, value, name);
  }

  /**
   * Creates a fluent spec for enum set parameter validation (comma-separated enum values).
   *
   * @param value comma-separated enum names (may be null or empty)
   * @param enumClass the enum class for type-safe validation
   * @param name the parameter name for error messages
   * @param <E> the enum type
   * @return EnumSetSpec for fluent configuration
   */
  public <E extends Enum<E>> EnumSetSpec<E> enumSetParam(
      String value, Class<E> enumClass, String name) {
    return new EnumSetSpec<>(this, value, enumClass, name);
  }

  /**
   * Creates a fluent spec for date parameter validation.
   *
   * @param value date string in ISO format (YYYY-MM-DD) or epoch milliseconds
   * @param name the parameter name for error messages
   * @return DateSpec for fluent configuration
   */
  public DateSpec dateParam(String value, String name) {
    return new DateSpec(this, value, name);
  }

  /**
   * Validates that a date range is logically consistent (start before end).
   *
   * @param start the start date (may be null)
   * @param end the end date (may be null)
   * @param startName the start parameter name for error messages
   * @param endName the end parameter name for error messages
   */
  public void validateDateRange(Date start, Date end, String startName, String endName) {
    if (start != null && end != null && start.after(end)) {
      errors.add(
          String.format(
              "Invalid date range: %s must be before %s. "
                  + "Example: %s='2025-01-01', %s='2025-12-31'",
              startName, endName, startName, endName));
    }
  }

  /**
   * Validates that if a dependent parameter is present, a required parameter must also be present.
   *
   * @param dependent the dependent parameter value
   * @param depName the dependent parameter name
   * @param required the required parameter value
   * @param reqName the required parameter name
   */
  public void requireIfPresent(String dependent, String depName, String required, String reqName) {
    if (StringUtils.hasText(dependent) && !StringUtils.hasText(required)) {
      errors.add(String.format("%s requires %s to be specified", depName, reqName));
    }
  }

  /**
   * Validates that a required parameter is present and not blank.
   *
   * @param value the parameter value
   * @param name the parameter name for error messages
   */
  public void require(String value, String name) {
    if (!StringUtils.hasText(value)) {
      errors.add(String.format("%s is required", name));
    }
  }

  /**
   * Validates that a required parameter is present and is a valid UUID format.
   *
   * @param value the parameter value
   * @param name the parameter name for error messages
   */
  public void requireUuid(String value, String name) {
    if (!StringUtils.hasText(value)) {
      errors.add(String.format("%s is required", name));
    } else if (!isValidUuid(value)) {
      errors.add(
          String.format(
              "%s must be a valid UUID format (e.g., 550e8400-e29b-41d4-a716-446655440000)", name));
    }
  }

  /**
   * Checks if the string is a valid UUID format.
   *
   * @param str the string to check
   * @return true if valid UUID, false otherwise
   */
  private static boolean isValidUuid(String str) {
    try {
      UUID.fromString(str);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  /**
   * Validates that at least one of the specified values is present.
   *
   * @param message error message if all values are missing
   * @param values the values to check (at least one must have text)
   */
  public void requireAtLeastOne(String message, String... values) {
    boolean anyPresent = Arrays.stream(values).anyMatch(StringUtils::hasText);
    if (!anyPresent) {
      errors.add(message);
    }
  }

  /**
   * Adds a warning if the condition is true.
   *
   * @param condition the condition to check
   * @param message the warning message if condition is true
   */
  public void warnIf(boolean condition, String message) {
    if (condition) {
      warnings.add(message);
    }
  }

  /**
   * Adds an error if the condition is true.
   *
   * @param condition the condition to check
   * @param message the error message if condition is true
   */
  public void errorIf(boolean condition, String message) {
    if (condition) {
      errors.add(message);
    }
  }

  /**
   * Adds an error message. Protected for use by Spec classes and subclasses.
   *
   * @param message the error message
   */
  protected void addError(String message) {
    errors.add(message);
  }

  /**
   * Adds a warning message. Protected for use by Spec classes and subclasses.
   *
   * @param message the warning message
   */
  protected void addWarning(String message) {
    warnings.add(message);
  }

  @Override
  public boolean isValid() {
    return errors.isEmpty();
  }

  @Override
  public List<String> errors() {
    return List.copyOf(errors);
  }

  @Override
  public List<String> warnings() {
    return List.copyOf(warnings);
  }
}
