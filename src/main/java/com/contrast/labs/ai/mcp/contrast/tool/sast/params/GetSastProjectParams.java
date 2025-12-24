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

import com.contrast.labs.ai.mcp.contrast.tool.validation.ToolValidationContext;

/**
 * Validation parameters for GetSastProjectTool. Validates required projectName.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * var params = GetSastProjectParams.of("my-project");
 * if (!params.isValid()) {
 *   // Handle errors
 * }
 * }</pre>
 */
public class GetSastProjectParams extends ToolValidationContext {

  private String projectName;

  /** Private constructor - use static factory method {@link #of}. */
  private GetSastProjectParams() {}

  /**
   * Parse and validate SAST project get parameters.
   *
   * @param projectName Project name (required)
   * @return GetSastProjectParams with validation state
   */
  public static GetSastProjectParams of(String projectName) {
    var params = new GetSastProjectParams();

    // Validate required field
    params.require(projectName, "projectName");

    params.projectName = projectName;

    return params;
  }

  public String projectName() {
    return projectName;
  }
}
