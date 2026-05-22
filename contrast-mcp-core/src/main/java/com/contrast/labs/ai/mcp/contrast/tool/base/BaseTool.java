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

import java.util.Objects;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;

/**
 * Base class for all Contrast MCP tools. Provides common infrastructure shared across paginated and
 * single-item tools.
 */
public abstract class BaseTool {

  private static final AutoCloseable NOOP_AUTHENTICATION_SCOPE = () -> {};

  private AuthenticationStrategy authenticationStrategy;

  @Autowired(required = false)
  public final void setAuthenticationStrategy(AuthenticationStrategy authenticationStrategy) {
    this.authenticationStrategy = authenticationStrategy;
  }

  /**
   * Returns whether a transport-specific authentication strategy is configured.
   *
   * @return true when a strategy has been injected
   */
  public final boolean isAuthenticationStrategyConfigured() {
    return authenticationStrategy != null;
  }

  protected final AutoCloseable authenticate(@Nullable ToolContext toolContext) throws Exception {
    if (authenticationStrategy == null) {
      return NOOP_AUTHENTICATION_SCOPE;
    }
    var scope = authenticationStrategy.authenticate(toolContext);
    return Objects.requireNonNull(
        scope, "AuthenticationStrategy returned null authentication scope");
  }

  /**
   * Maps HTTP error codes to user-friendly error messages.
   *
   * @param code HTTP status code
   * @return user-friendly error message
   */
  protected String mapHttpErrorCode(int code) {
    return switch (code) {
      case 401 -> "Your authentication token has expired. Please re-authenticate and retry.";
      case 403 -> "Access denied. User lacks permission for this resource.";
      case 404 -> "Resource not found.";
      case 429 -> "Rate limit exceeded. Retry after a brief pause.";
      case 500, 502, 503 ->
          "The service returned an error. Narrow filters or reduce page size, then retry.";
      default -> "API error (HTTP " + code + ")";
    };
  }
}
