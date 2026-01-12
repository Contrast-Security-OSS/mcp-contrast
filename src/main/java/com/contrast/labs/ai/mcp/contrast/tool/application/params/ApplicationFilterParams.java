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
package com.contrast.labs.ai.mcp.contrast.tool.application.params;

import com.contrast.labs.ai.mcp.contrast.sdkextension.data.application.Application;
import com.contrast.labs.ai.mcp.contrast.tool.base.BaseToolParams;
import com.contrast.labs.ai.mcp.contrast.tool.validation.ToolValidationContext;
import org.springframework.util.StringUtils;

/**
 * Filter parameters for SearchApplicationsTool. All filters are optional and combined with AND
 * logic.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * var params = ApplicationFilterParams.of("myapp", "Production", "env", "prod");
 * if (!params.isValid()) {
 *   // Handle errors
 * }
 * boolean matches = params.matches(application);
 * }</pre>
 */
public class ApplicationFilterParams extends BaseToolParams {

  private String name;
  private String tag;
  private String metadataName;
  private String metadataValue;

  /** Private constructor - use static factory method {@link #of}. */
  private ApplicationFilterParams() {}

  /**
   * Parse and validate application filter parameters.
   *
   * @param name Application name filter (partial, case-insensitive)
   * @param tag Tag filter (exact, case-sensitive)
   * @param metadataName Metadata field name filter (case-insensitive)
   * @param metadataValue Metadata field value filter (case-insensitive, requires metadataName)
   * @return ApplicationFilterParams with validation state
   */
  public static ApplicationFilterParams of(
      String name, String tag, String metadataName, String metadataValue) {

    var params = new ApplicationFilterParams();
    var ctx = new ToolValidationContext();

    // Store raw values - all filters are optional
    params.name = StringUtils.hasText(name) ? name : null;
    params.tag = StringUtils.hasText(tag) ? tag : null;
    params.metadataName = StringUtils.hasText(metadataName) ? metadataName : null;
    params.metadataValue = StringUtils.hasText(metadataValue) ? metadataValue : null;

    // Cross-field validation: metadataValue requires metadataName
    ctx.requireIfPresent(metadataValue, "metadataValue", metadataName, "metadataName");

    params.setValidationResult(ctx);
    return params;
  }

  /**
   * Tests if an application matches all specified filters. Filters are combined with AND logic.
   *
   * @param app the application to test
   * @return true if application matches all filters, false otherwise
   */
  public boolean matches(Application app) {
    return matchesNameFilter(app) && matchesTagFilter(app) && matchesMetadataFilter(app);
  }

  /**
   * Check if application matches name filter (partial, case-insensitive).
   *
   * @param app the application to check
   * @return true if matches or no filter specified
   */
  private boolean matchesNameFilter(Application app) {
    if (name == null) {
      return true;
    }
    return app.getName() != null && app.getName().toLowerCase().contains(name.toLowerCase());
  }

  /**
   * Check if application matches tag filter (exact, case-sensitive).
   *
   * @param app the application to check
   * @return true if matches or no filter specified
   */
  private boolean matchesTagFilter(Application app) {
    if (tag == null) {
      return true;
    }
    return app.getTags().contains(tag);
  }

  /**
   * Check if application matches metadata filter (case-insensitive for both name and value).
   *
   * @param app the application to check
   * @return true if matches or no filter specified
   */
  private boolean matchesMetadataFilter(Application app) {
    if (metadataName == null) {
      return true;
    }

    for (var metadata : app.getMetadataEntities()) {
      if (metadata == null || metadata.getName() == null) {
        continue;
      }

      boolean nameMatches = metadata.getName().equalsIgnoreCase(metadataName);

      if (metadataValue != null) {
        // Both name and value must match
        if (nameMatches
            && metadata.getValue() != null
            && metadata.getValue().equalsIgnoreCase(metadataValue)) {
          return true;
        }
      } else {
        // Name only - any value is acceptable
        if (nameMatches) {
          return true;
        }
      }
    }

    return false;
  }

  public String getName() {
    return name;
  }

  public String getTag() {
    return tag;
  }

  public String getMetadataName() {
    return metadataName;
  }

  public String getMetadataValue() {
    return metadataValue;
  }
}
