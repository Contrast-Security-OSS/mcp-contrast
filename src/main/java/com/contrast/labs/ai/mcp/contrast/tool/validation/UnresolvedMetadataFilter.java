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

import java.util.List;

/**
 * Represents a session metadata filter before field name resolution.
 *
 * <p>The field name is what the user provides (e.g., "branch", "developer"). This must be resolved
 * to a field ID by looking up available session metadata for the application before it can be used
 * in API calls.
 *
 * @param fieldName The user-provided field name (case-insensitive during resolution)
 * @param values The filter values (OR logic - matches if any value matches)
 */
public record UnresolvedMetadataFilter(String fieldName, List<String> values) {
  public UnresolvedMetadataFilter {
    if (fieldName == null || fieldName.isBlank()) {
      throw new IllegalArgumentException("fieldName cannot be null or blank");
    }
    values = values == null ? List.of() : List.copyOf(values);
  }
}
