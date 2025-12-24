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
package com.contrast.labs.ai.mcp.contrast.tool.sast.params;

import com.contrast.labs.ai.mcp.contrast.tool.base.BaseToolParams;
import com.contrast.labs.ai.mcp.contrast.tool.validation.ToolValidationContext;

/**
 * Validation parameters for GetSastResultsTool. Validates required projectName.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * var params = GetSastResultsParams.of("my-project");
 * if (!params.isValid()) {
 *   // Handle errors
 * }
 * }</pre>
 */
public class GetSastResultsParams extends BaseToolParams {

  private String projectName;

  /** Private constructor - use static factory method {@link #of}. */
  private GetSastResultsParams() {}

  /**
   * Parse and validate SAST results get parameters.
   *
   * @param projectName Project name (required)
   * @return GetSastResultsParams with validation state
   */
  public static GetSastResultsParams of(String projectName) {
    var params = new GetSastResultsParams();
    var ctx = new ToolValidationContext();

    // Validate required field
    ctx.require(projectName, "projectName");
    params.projectName = projectName;

    params.setValidationResult(ctx);
    return params;
  }

  public String projectName() {
    return projectName;
  }
}
