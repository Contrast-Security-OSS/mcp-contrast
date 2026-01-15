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
package com.contrast.labs.ai.mcp.contrast.tool.base;

import com.contrast.labs.ai.mcp.contrast.config.ContrastSDKFactory;
import com.contrastsecurity.sdk.ContrastSDK;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Base class for all Contrast MCP tools. Provides common infrastructure shared across paginated and
 * single-item tools.
 *
 * <p>Subclasses get access to:
 *
 * <ul>
 *   <li>ContrastSDK instance via {@link #getContrastSDK()}
 *   <li>Organization ID via {@link #getOrgId()}
 *   <li>HTTP error code mapping via {@link #mapHttpErrorCode(int)}
 * </ul>
 */
public abstract class BaseTool {

  @Autowired protected ContrastSDKFactory sdkFactory;

  /**
   * Returns the cached ContrastSDK instance from the factory.
   *
   * @return cached ContrastSDK
   */
  protected ContrastSDK getContrastSDK() {
    return sdkFactory.getSDK();
  }

  /**
   * Returns the organization ID from the factory.
   *
   * @return organization ID
   */
  protected String getOrgId() {
    return sdkFactory.getOrgId();
  }

  /**
   * Maps HTTP error codes to user-friendly error messages.
   *
   * @param code HTTP status code
   * @return user-friendly error message
   */
  protected String mapHttpErrorCode(int code) {
    return switch (code) {
      case 401 ->
          "Authentication failed or resource not found. Verify credentials and that the resource ID"
              + " is correct.";
      case 403 -> "Access denied. User lacks permission for this resource.";
      case 404 -> "Resource not found.";
      case 429 -> "Rate limit exceeded. Retry later.";
      case 500, 502, 503 -> "Contrast API error. Try again later.";
      default -> "API error (HTTP " + code + ")";
    };
  }
}
