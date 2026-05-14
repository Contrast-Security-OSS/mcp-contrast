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

import com.contrast.labs.ai.mcp.contrast.tool.base.FilterHelper;
import java.util.List;
import java.util.Set;

/**
 * Fluent validation spec for comma-separated string list parameters. Parses comma-separated values
 * into a List with optional allowed value constraints.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * var statuses = ctx.stringListParam(statusParam, "statuses")
 *     .allowedValues(Set.of("Reported", "Confirmed", "Fixed"))
 *     .defaultTo(List.of("Reported", "Confirmed"), "Excluding Fixed by default")
 *     .get();
 * }</pre>
 */
public class StringListSpec {

  private final ToolValidationContext ctx;
  private final String value;
  private final String name;
  private List<String> defaultValue;
  private String defaultReason;
  private Set<String> allowedValues;
  private boolean convertToUpperCase;

  StringListSpec(ToolValidationContext ctx, String value, String name) {
    this.ctx = ctx;
    this.value = value;
    this.name = name;
  }

  /**
   * Sets a default value to use when the parameter is null or blank.
   *
   * @param val the default list value
   * @param reason explanation for AI feedback (added as warning when default is used)
   * @return this for fluent chaining
   */
  public StringListSpec defaultTo(List<String> val, String reason) {
    this.defaultValue = val;
    this.defaultReason = reason;
    return this;
  }

  /**
   * Sets allowed values with case-insensitive matching. Input values are normalized to the
   * canonical form (e.g., "reported" becomes "Reported"). Invalid items are added as errors.
   *
   * @param values set of valid values in canonical form
   * @return this for fluent chaining
   */
  public StringListSpec allowedValues(Set<String> values) {
    this.allowedValues = values;
    return this;
  }

  /**
   * Converts all values to uppercase. Useful for case-insensitive enum matching.
   *
   * @return this for fluent chaining
   */
  public StringListSpec toUpperCase() {
    this.convertToUpperCase = true;
    return this;
  }

  /**
   * Validates and returns the parsed list.
   *
   * @return validated list, or null if no value and no default
   */
  public List<String> get() {
    List<String> parsed = FilterHelper.parseCommaSeparated(value);

    if (parsed == null) {
      if (defaultValue != null) {
        ctx.addWarning(defaultReason);
        return List.copyOf(defaultValue);
      }
      return null;
    }

    // Apply uppercase conversion if requested
    if (convertToUpperCase) {
      parsed = parsed.stream().map(String::toUpperCase).toList();
    }

    if (allowedValues != null) {
      // Build lowercase -> canonical mapping for case-insensitive matching
      var canonicalMap = new java.util.HashMap<String, String>();
      for (String allowed : allowedValues) {
        canonicalMap.put(allowed.toLowerCase(), allowed);
      }

      // Validate and normalize each item
      var normalized = new java.util.ArrayList<String>();
      for (String item : parsed) {
        String canonical = canonicalMap.get(item.toLowerCase());
        if (canonical != null) {
          normalized.add(canonical);
        } else {
          ctx.addError(
              String.format(
                  "Invalid %s: '%s'. Valid values: %s",
                  name, item, String.join(", ", allowedValues)));
        }
      }
      parsed = normalized;
    }

    return ctx.isValid() ? List.copyOf(parsed) : null;
  }
}
