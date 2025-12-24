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
package com.contrast.labs.ai.mcp.contrast.tool.applications.params;

import com.contrast.labs.ai.mcp.contrast.tool.base.BaseToolParams;
import com.contrast.labs.ai.mcp.contrast.tool.validation.ToolValidationContext;

/**
 * Validation parameters for GetSessionMetadataTool. Validates required appId.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * var params = GetSessionMetadataParams.of("app-123");
 * if (!params.isValid()) {
 *   // Handle errors
 * }
 * }</pre>
 */
public class GetSessionMetadataParams extends BaseToolParams {

  private String appId;

  /** Private constructor - use static factory method {@link #of}. */
  private GetSessionMetadataParams() {}

  /**
   * Parse and validate session metadata parameters.
   *
   * @param appId Application ID (required)
   * @return GetSessionMetadataParams with validation state
   */
  public static GetSessionMetadataParams of(String appId) {
    var params = new GetSessionMetadataParams();
    var ctx = new ToolValidationContext();

    // Validate required field
    ctx.require(appId, "appId");
    params.appId = appId;

    params.setValidationResult(ctx);
    return params;
  }

  public String appId() {
    return appId;
  }
}
