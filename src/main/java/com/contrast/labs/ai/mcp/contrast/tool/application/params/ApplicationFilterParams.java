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

import com.contrast.labs.ai.mcp.contrast.tool.base.BaseToolParams;
import com.contrast.labs.ai.mcp.contrast.tool.validation.ToolValidationContext;
import com.contrast.labs.ai.mcp.contrast.tool.validation.UnresolvedMetadataFilter;
import java.util.List;
import org.springframework.util.StringUtils;

/**
 * Filter parameters for SearchApplicationsTool. All filters are optional and combined with AND
 * logic on the server side.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * var params = ApplicationFilterParams.of("myapp", "Production", "{\"env\":\"prod\"}");
 * if (!params.isValid()) {
 *   // Handle errors
 * }
 * }</pre>
 */
public class ApplicationFilterParams extends BaseToolParams {

  private String name;
  private String tag;
  private List<UnresolvedMetadataFilter> metadataFilters;

  /** Private constructor - use static factory method {@link #of}. */
  private ApplicationFilterParams() {}

  /**
   * Parse and validate application filter parameters.
   *
   * @param name Application name filter (server-side text search)
   * @param tag Tag filter (exact, case-sensitive)
   * @param metadataFiltersJson JSON object for metadata filtering
   * @return ApplicationFilterParams with validation state
   */
  public static ApplicationFilterParams of(String name, String tag, String metadataFiltersJson) {

    var params = new ApplicationFilterParams();
    var ctx = new ToolValidationContext();

    // Store raw values - all filters are optional
    params.name = StringUtils.hasText(name) ? name : null;
    params.tag = StringUtils.hasText(tag) ? tag : null;
    params.metadataFilters =
        ctx.metadataJsonFilterParam(metadataFiltersJson, "metadataFilters").get();

    params.setValidationResult(ctx);
    return params;
  }

  public String getName() {
    return name;
  }

  public String getTag() {
    return tag;
  }

  public List<UnresolvedMetadataFilter> getMetadataFilters() {
    return metadataFilters;
  }
}
