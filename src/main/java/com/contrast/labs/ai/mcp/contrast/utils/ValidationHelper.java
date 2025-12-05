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
package com.contrast.labs.ai.mcp.contrast.utils;

import com.contrast.labs.ai.mcp.contrast.FilterHelper;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import org.springframework.util.StringUtils;

/**
 * Fluent validation builder for MCP tool parameters. Collects errors (hard failures) and warnings
 * (soft failures) during validation, enabling consistent parameter handling across all tools.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * EnumSet<RuleSeverity> severities = null;
 * List<String> statuses = null;
 *
 * var v = ValidationHelper.builder()
 *     .pagination(page, pageSize)
 *     .enumSet(sevParam, RuleSeverity.class, "severities", s -> severities = s)
 *     .stringList(statusParam, VALID_STATUSES, DEFAULT_STATUSES, "statuses", s -> statuses = s)
 *     .requireWith(sessionValue, "sessionMetadataValue", sessionName, "sessionMetadataName");
 *
 * if (!v.isValid()) {
 *   return PaginatedResponse.error(v.page(), v.pageSize(), String.join("; ", v.errors()));
 * }
 * }</pre>
 */
public class ValidationHelper {

  /** Default page size when none specified. */
  public static final int DEFAULT_PAGE_SIZE = 50;

  /** Maximum allowed page size. */
  public static final int MAX_PAGE_SIZE = 100;

  private final List<String> errors = new ArrayList<>();
  private final List<String> warnings = new ArrayList<>();
  private int page = 1;
  private int pageSize = DEFAULT_PAGE_SIZE;

  private ValidationHelper() {}

  /** Create a new validation builder. */
  public static ValidationHelper builder() {
    return new ValidationHelper();
  }

  /**
   * Validate and clamp pagination parameters. Invalid values are corrected with warnings (soft
   * failure).
   *
   * @param page Requested page number (1-based), null defaults to 1
   * @param pageSize Requested page size, null defaults to 50
   * @return this builder for chaining
   */
  public ValidationHelper pagination(Integer page, Integer pageSize) {
    // Validate page (soft failure - clamp to 1)
    if (page == null || page < 1) {
      this.page = 1;
      if (page != null && page < 1) {
        warnings.add(String.format("Invalid page %d, using 1", page));
      }
    } else {
      this.page = page;
    }

    // Validate pageSize (soft failure - clamp to range)
    if (pageSize == null) {
      this.pageSize = DEFAULT_PAGE_SIZE;
    } else if (pageSize < 1) {
      this.pageSize = DEFAULT_PAGE_SIZE;
      warnings.add(String.format("Invalid pageSize %d, using %d", pageSize, DEFAULT_PAGE_SIZE));
    } else if (pageSize > MAX_PAGE_SIZE) {
      this.pageSize = MAX_PAGE_SIZE;
      warnings.add(
          String.format(
              "pageSize %d exceeds max %d, using %d", pageSize, MAX_PAGE_SIZE, MAX_PAGE_SIZE));
    } else {
      this.pageSize = pageSize;
    }

    return this;
  }

  /**
   * Parse comma-separated enum values into an EnumSet. All values must be valid enum constants
   * (hard failure).
   *
   * @param value Comma-separated enum values (e.g., "CRITICAL,HIGH")
   * @param enumClass The enum class to parse into
   * @param paramName Parameter name for error messages
   * @param setter Consumer to receive the parsed EnumSet (or null if empty/invalid)
   * @param <E> The enum type
   * @return this builder for chaining
   */
  public <E extends Enum<E>> ValidationHelper enumSet(
      String value, Class<E> enumClass, String paramName, Consumer<EnumSet<E>> setter) {
    if (!StringUtils.hasText(value)) {
      setter.accept(null);
      return this;
    }

    List<String> parsed = FilterHelper.parseCommaSeparatedUpperCase(value);
    if (parsed == null || parsed.isEmpty()) {
      setter.accept(null);
      return this;
    }

    EnumSet<E> result = EnumSet.noneOf(enumClass);
    List<String> invalid = new ArrayList<>();

    for (String item : parsed) {
      try {
        result.add(Enum.valueOf(enumClass, item));
      } catch (IllegalArgumentException e) {
        invalid.add(item);
      }
    }

    if (!invalid.isEmpty()) {
      E[] constants = enumClass.getEnumConstants();
      List<String> validValues = new ArrayList<>();
      for (E constant : constants) {
        validValues.add(constant.name());
      }
      errors.add(
          String.format("Invalid %s: %s. Valid values: %s", paramName, invalid, validValues));
      setter.accept(null);
    } else {
      setter.accept(result.isEmpty() ? null : result);
    }

    return this;
  }

  /**
   * Validate a comma-separated string list against allowed values. Supports smart defaults with
   * warnings.
   *
   * @param value Comma-separated string values
   * @param allowedValues Set of valid values (case-sensitive)
   * @param defaultValues Default values to use when value is null/empty (null for no defaults)
   * @param paramName Parameter name for error/warning messages
   * @param setter Consumer to receive the parsed list
   * @return this builder for chaining
   */
  public ValidationHelper stringList(
      String value,
      Set<String> allowedValues,
      List<String> defaultValues,
      String paramName,
      Consumer<List<String>> setter) {

    if (!StringUtils.hasText(value)) {
      // Use defaults if provided
      if (defaultValues != null && !defaultValues.isEmpty()) {
        setter.accept(new ArrayList<>(defaultValues));
        warnings.add(
            String.format(
                "Using default %s: %s. To see all values, specify %s explicitly.",
                paramName, defaultValues, paramName));
      } else {
        setter.accept(null);
      }
      return this;
    }

    List<String> parsed = FilterHelper.parseCommaSeparated(value);
    if (parsed == null || parsed.isEmpty()) {
      setter.accept(null);
      return this;
    }

    // Validate each value against allowed set
    List<String> invalid = new ArrayList<>();
    for (String item : parsed) {
      if (!allowedValues.contains(item)) {
        invalid.add(item);
      }
    }

    if (!invalid.isEmpty()) {
      errors.add(
          String.format("Invalid %s: %s. Valid values: %s", paramName, invalid, allowedValues));
      setter.accept(null);
    } else {
      setter.accept(parsed);
    }

    return this;
  }

  /**
   * Validate that when one parameter has a value, another required parameter must also have a
   * value. Hard failure if requirement not met.
   *
   * @param value The parameter value being checked
   * @param valueName Name of the parameter being checked
   * @param required The required parameter value
   * @param requiredName Name of the required parameter
   * @return this builder for chaining
   */
  public ValidationHelper requireWith(
      String value, String valueName, String required, String requiredName) {
    if (StringUtils.hasText(value) && !StringUtils.hasText(required)) {
      errors.add(String.format("%s requires %s", valueName, requiredName));
    }
    return this;
  }

  /**
   * Validate that two parameters are not both specified. Hard failure if both have values.
   *
   * @param param1Name Name of first parameter
   * @param value1 Value of first parameter
   * @param param2Name Name of second parameter
   * @param value2 Value of second parameter
   * @return this builder for chaining
   */
  public ValidationHelper mutuallyExclusive(
      String param1Name, Object value1, String param2Name, Object value2) {
    boolean has1 =
        value1 != null && (!(value1 instanceof String) || StringUtils.hasText((String) value1));
    boolean has2 =
        value2 != null && (!(value2 instanceof String) || StringUtils.hasText((String) value2));

    if (has1 && has2) {
      errors.add(String.format("%s and %s are mutually exclusive", param1Name, param2Name));
    }
    return this;
  }

  /**
   * Add a custom error message. Use for validation logic not covered by built-in methods.
   *
   * @param errorMessage The error message to add
   * @return this builder for chaining
   */
  public ValidationHelper addError(String errorMessage) {
    if (StringUtils.hasText(errorMessage)) {
      errors.add(errorMessage);
    }
    return this;
  }

  /**
   * Add a custom warning message. Use for informational messages not covered by built-in methods.
   *
   * @param warningMessage The warning message to add
   * @return this builder for chaining
   */
  public ValidationHelper addWarning(String warningMessage) {
    if (StringUtils.hasText(warningMessage)) {
      warnings.add(warningMessage);
    }
    return this;
  }

  /**
   * Check if validation passed (no errors).
   *
   * @return true if no errors, false if any errors exist
   */
  public boolean isValid() {
    return errors.isEmpty();
  }

  /**
   * Get the validated page number (always >= 1).
   *
   * @return validated page number
   */
  public int page() {
    return page;
  }

  /**
   * Get the validated page size (always 1-100).
   *
   * @return validated page size
   */
  public int pageSize() {
    return pageSize;
  }

  /**
   * Calculate 0-based offset for SDK pagination.
   *
   * @return (page - 1) * pageSize
   */
  public int offset() {
    return (page - 1) * pageSize;
  }

  /**
   * Get all validation errors (hard failures).
   *
   * @return immutable list of error messages
   */
  public List<String> errors() {
    return List.copyOf(errors);
  }

  /**
   * Get all validation warnings (soft failures).
   *
   * @return immutable list of warning messages
   */
  public List<String> warnings() {
    return List.copyOf(warnings);
  }
}
