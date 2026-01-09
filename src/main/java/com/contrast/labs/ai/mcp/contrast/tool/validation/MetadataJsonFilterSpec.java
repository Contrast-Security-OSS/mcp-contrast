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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;

/**
 * Fluent validation spec for metadata filter JSON parameters. Parses JSON to Map<String, Object>
 * where values are String or List<String>. Used for session metadata filters and can be extended
 * for application metadata.
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
   * @return Map of field names to values (String or List<String>), or null if empty/invalid
   */
  public Map<String, Object> get() {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    try {
      Map<String, Object> rawMap =
          GSON.fromJson(value, new TypeToken<Map<String, Object>>() {}.getType());
      if (rawMap == null || rawMap.isEmpty()) {
        return null;
      }

      Map<String, Object> result = new LinkedHashMap<>();
      List<String> invalidEntries = new ArrayList<>();

      for (var entry : rawMap.entrySet()) {
        var key = entry.getKey();
        var val = entry.getValue();

        if (val instanceof String) {
          result.put(key, val);
        } else if (val instanceof Number) {
          result.put(key, formatNumber((Number) val));
        } else if (val instanceof List) {
          // Validate array contains only strings/numbers
          List<String> stringList = new ArrayList<>();
          boolean valid = true;
          for (Object item : (List<?>) val) {
            if (item instanceof String) {
              stringList.add((String) item);
            } else if (item instanceof Number) {
              stringList.add(formatNumber((Number) item));
            } else if (item != null) {
              valid = false;
              break;
            }
          }
          if (valid) {
            result.put(key, stringList);
          } else {
            invalidEntries.add(String.format("'%s' (array contains non-string values)", key));
          }
        } else if (val != null) {
          // Reject complex objects
          invalidEntries.add(String.format("'%s' (expected string or array of strings)", key));
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

      return result;
    } catch (JsonSyntaxException e) {
      ctx.addError(
          String.format(
              "Invalid JSON for %s: %s. Expected format: {\"field\":\"value\"} or "
                  + "{\"field\":[\"value1\",\"value2\"]}",
              name, e.getMessage()));
      return null;
    }
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
