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

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;

/**
 * Fluent validation spec for metadata filter JSON parameters. Parses JSON to a list of {@link
 * UnresolvedMetadataFilter} records where each filter has a field name and list of values. Used for
 * session metadata filters and can be extended for application metadata.
 */
public class MetadataJsonFilterSpec {

  private static final Gson GSON = new Gson();
  private final ToolValidationContext ctx;
  private final String value;
  private final String name;

  MetadataJsonFilterSpec(ToolValidationContext ctx, String value, String name) {
    this.ctx = ctx;
    this.value = value;
    this.name = name;
  }

  /**
   * Parses the JSON value and validates its structure.
   *
   * @return List of UnresolvedMetadataFilter records, or null if empty/invalid
   */
  public List<UnresolvedMetadataFilter> get() {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    try {
      Map<String, Object> rawMap =
          GSON.fromJson(value, new TypeToken<Map<String, Object>>() {}.getType());
      if (rawMap == null || rawMap.isEmpty()) {
        return null;
      }

      var result = new ArrayList<UnresolvedMetadataFilter>();
      var invalidEntries = new ArrayList<String>();

      for (var entry : rawMap.entrySet()) {
        var fieldName = entry.getKey();
        var values = parseValues(entry.getValue(), fieldName, invalidEntries);
        if (values != null) {
          result.add(new UnresolvedMetadataFilter(fieldName, values));
        }
      }

      if (!invalidEntries.isEmpty()) {
        ctx.addError(
            String.format(
                "Invalid values in %s for fields: %s. "
                    + "Values must be strings or arrays of strings.",
                name, String.join(", ", invalidEntries)));
        return null;
      }

      return result.isEmpty() ? null : List.copyOf(result);
    } catch (JsonSyntaxException e) {
      ctx.addError(
          String.format(
              "Invalid JSON for %s: %s. Expected format: {\"field\":\"value\"} or "
                  + "{\"field\":[\"value1\",\"value2\"]}",
              name, e.getMessage()));
      return null;
    }
  }

  private List<String> parseValues(Object val, String fieldName, List<String> invalidEntries) {
    if (val == null) {
      invalidEntries.add(
          String.format("'%s' (empty value - must provide a non-empty value)", fieldName));
      return null;
    }
    if (val instanceof String s) {
      if (s.isBlank()) {
        invalidEntries.add(
            String.format("'%s' (empty value - must provide a non-empty value)", fieldName));
        return null;
      }
      return List.of(s);
    } else if (val instanceof Number n) {
      return List.of(formatNumber(n));
    } else if (val instanceof List<?> list) {
      if (list.isEmpty()) {
        invalidEntries.add(
            String.format("'%s' (empty array - must have at least one value)", fieldName));
        return null;
      }
      var strings = new ArrayList<String>();
      for (Object item : list) {
        if (item == null) {
          invalidEntries.add(
              String.format(
                  "'%s' (contains empty value - all values must be non-empty)", fieldName));
          return null;
        }
        if (item instanceof String s) {
          if (s.isBlank()) {
            invalidEntries.add(
                String.format(
                    "'%s' (contains empty value - all values must be non-empty)", fieldName));
            return null;
          }
          strings.add(s);
        } else if (item instanceof Number n) {
          strings.add(formatNumber(n));
        } else {
          invalidEntries.add(String.format("'%s' (array contains non-string values)", fieldName));
          return null;
        }
      }
      return List.copyOf(strings);
    } else {
      invalidEntries.add(String.format("'%s' (expected string or array of strings)", fieldName));
    }
    return null;
  }

  private List<String> parseListValues(
      List<?> list, String fieldName, List<String> invalidEntries) {
    if (list.isEmpty()) {
      invalidEntries.add(
          String.format("'%s' (empty array - must have at least one value)", fieldName));
      return null;
    }
    var strings = new ArrayList<String>();
    for (Object item : list) {
      if (item == null) {
        invalidEntries.add(
            String.format("'%s' (contains empty value - all values must be non-empty)", fieldName));
        return null;
      }
      if (item instanceof String s) {
        if (s.isBlank()) {
          invalidEntries.add(
              String.format(
                  "'%s' (contains empty value - all values must be non-empty)", fieldName));
          return null;
        }
        strings.add(s);
      } else if (item instanceof Number n) {
        strings.add(formatNumber(n));
      } else {
        invalidEntries.add(String.format("'%s' (array contains non-string values)", fieldName));
        return null;
      }
    }
    return List.copyOf(strings);
  }

  /**
   * Formats a number as a string, using integer format when possible. Gson parses all JSON numbers
   * as doubles, so 42 becomes 42.0. This method converts back to integer format when the value is a
   * whole number.
   */
  private String formatNumber(Number num) {
    double d = num.doubleValue();
    if (d == Math.floor(d) && !Double.isInfinite(d)) {
      return String.valueOf((long) d);
    }
    return num.toString();
  }
}
