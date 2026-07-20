/*
 * Copyright 2026 Contrast Security
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

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/** Parses the shared public {@code property,DIRECTION} sort contract into wire syntax. */
public final class ToolSortParser {

  private static final Set<String> VALID_DIRECTIONS = Set.of("ASC", "DESC");

  private ToolSortParser() {}

  /**
   * Validates and translates a public sort value.
   *
   * @param context validation result collector
   * @param sort public sort value, or {@code null}
   * @param fields public property names mapped to their wire names
   * @param propertyCaseSensitive whether public property matching is case-sensitive
   * @param defaultSort value returned when sort is absent or invalid
   * @return translated wire sort, prefixed with {@code -} for descending order
   */
  public static @Nullable String parse(
      @NonNull ToolValidationContext context,
      @Nullable String sort,
      @NonNull Map<String, String> fields,
      boolean propertyCaseSensitive,
      @Nullable String defaultSort) {
    if (!StringUtils.hasText(sort)) {
      return defaultSort;
    }

    var trimmedSort = sort.trim();
    var parts = Arrays.stream(trimmedSort.split(",", -1)).map(String::trim).toList();
    if (parts.size() != 2) {
      context.addError(sortError(trimmedSort, fields));
      return defaultSort;
    }

    var property =
        fields.keySet().stream()
            .filter(
                candidate -> propertyMatches(candidate, parts.getFirst(), propertyCaseSensitive))
            .findFirst();
    var direction = parts.get(1).toUpperCase(Locale.ROOT);
    if (property.isEmpty() || !VALID_DIRECTIONS.contains(direction)) {
      context.addError(sortError(trimmedSort, fields));
      return defaultSort;
    }

    var wireProperty = fields.get(property.get());
    return "DESC".equals(direction) ? "-" + wireProperty : wireProperty;
  }

  private static boolean propertyMatches(
      String candidate, String requested, boolean propertyCaseSensitive) {
    return propertyCaseSensitive
        ? candidate.equals(requested)
        : candidate.equalsIgnoreCase(requested);
  }

  private static String sortError(String sort, Map<String, String> fields) {
    return String.format(
        "Invalid sort: '%s'. Expected format: property,DIRECTION. Valid properties: %s. Valid"
            + " directions: %s.",
        sort,
        fields.keySet().stream().sorted().collect(Collectors.joining(", ")),
        VALID_DIRECTIONS.stream().sorted().collect(Collectors.joining(", ")));
  }
}
