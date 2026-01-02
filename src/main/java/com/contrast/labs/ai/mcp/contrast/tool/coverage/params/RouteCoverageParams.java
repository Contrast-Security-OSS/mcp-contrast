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
package com.contrast.labs.ai.mcp.contrast.tool.coverage.params;

import com.contrast.labs.ai.mcp.contrast.tool.base.BaseToolParams;
import com.contrast.labs.ai.mcp.contrast.tool.validation.ToolValidationContext;
import org.springframework.util.StringUtils;

/**
 * Validation parameters for GetRouteCoverageTool. Validates required appId and optional session
 * filtering parameters.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * var params = RouteCoverageParams.of("app-123", "branch", "main", false);
 * if (!params.isValid()) {
 *   // Handle errors
 * }
 * }</pre>
 */
public class RouteCoverageParams extends BaseToolParams {

  private String appId;
  private String sessionMetadataName;
  private String sessionMetadataValue;
  private Boolean useLatestSession;

  /** Private constructor - use static factory method {@link #of}. */
  private RouteCoverageParams() {}

  /**
   * Parse and validate route coverage parameters.
   *
   * @param appId Application ID (required)
   * @param sessionMetadataName Session metadata field name (optional, must be paired with value)
   * @param sessionMetadataValue Session metadata field value (optional, must be paired with name)
   * @param useLatestSession If true, filter by latest session (optional)
   * @return RouteCoverageParams with validation state
   */
  public static RouteCoverageParams of(
      String appId,
      String sessionMetadataName,
      String sessionMetadataValue,
      Boolean useLatestSession) {
    var params = new RouteCoverageParams();
    var ctx = new ToolValidationContext();

    // Validate required field
    ctx.require(appId, "appId");

    // Validate paired metadata params - both or neither must be provided
    // Treat empty strings as null (no filter)
    boolean hasName = StringUtils.hasText(sessionMetadataName);
    boolean hasValue = StringUtils.hasText(sessionMetadataValue);

    ctx.errorIf(
        hasName && !hasValue,
        "sessionMetadataValue is required when sessionMetadataName is provided");
    ctx.errorIf(
        hasValue && !hasName,
        "sessionMetadataName is required when sessionMetadataValue is provided");

    // Warn if both useLatestSession and metadata params provided (mutually exclusive)
    ctx.warnIf(
        Boolean.TRUE.equals(useLatestSession) && hasName,
        "Both useLatestSession and sessionMetadataName provided - "
            + "useLatestSession takes precedence and sessionMetadata filter will be ignored");

    // Store values (empty strings normalized to null)
    params.appId = appId;
    params.sessionMetadataName = hasName ? sessionMetadataName : null;
    params.sessionMetadataValue = hasValue ? sessionMetadataValue : null;
    params.useLatestSession = useLatestSession;

    params.setValidationResult(ctx);
    return params;
  }

  public String appId() {
    return appId;
  }

  public String sessionMetadataName() {
    return sessionMetadataName;
  }

  public String sessionMetadataValue() {
    return sessionMetadataValue;
  }

  public Boolean useLatestSession() {
    return useLatestSession;
  }

  /** Returns true if useLatestSession is explicitly set to true. */
  public boolean isUseLatestSession() {
    return Boolean.TRUE.equals(useLatestSession);
  }

  /** Returns true if session metadata filtering is configured (both name and value present). */
  public boolean hasSessionMetadataFilter() {
    return sessionMetadataName != null && sessionMetadataValue != null;
  }
}
