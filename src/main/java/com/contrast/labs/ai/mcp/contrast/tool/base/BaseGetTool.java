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

import com.contrast.labs.ai.mcp.contrast.config.ContrastConfig;
import com.contrastsecurity.exceptions.HttpResponseException;
import com.contrastsecurity.exceptions.ResourceNotFoundException;
import com.contrastsecurity.exceptions.UnauthorizedException;
import com.contrastsecurity.sdk.ContrastSDK;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Abstract base class for non-paginated MCP get tools. Enforces a consistent processing pipeline
 * via Template Method pattern.
 *
 * <p>Subclasses implement {@link #doExecute} for tool-specific logic. The base class handles:
 *
 * <ul>
 *   <li>Tool parameter validation
 *   <li>Exception handling with user-friendly messages
 *   <li>Request ID generation for log correlation
 *   <li>Duration tracking for performance monitoring
 *   <li>Consistent response building
 * </ul>
 *
 * @param <P> the tool parameters type (must implement {@link ToolParams})
 * @param <R> the result item type
 */
@Slf4j
public abstract class BaseGetTool<P extends ToolParams, R> {

  @Autowired protected ContrastConfig config;

  /**
   * Template method - defines the mandatory processing pipeline for single-item retrieval.
   * Subclasses implement doExecute() for tool-specific logic. This method is FINAL to enforce
   * consistent processing.
   *
   * @param paramsSupplier lazy supplier of tool-specific parameters
   * @return tool response with item or errors
   */
  protected final ToolResponse<R> executePipeline(Supplier<P> paramsSupplier) {
    var requestId = UUID.randomUUID().toString().substring(0, 8);
    long startTime = System.currentTimeMillis();

    // 1. Parse tool-specific params (collects all errors)
    var params = paramsSupplier.get();

    // 2. MUTABLE warnings list - doExecute can ADD to this
    var warnings = new ArrayList<>(params.warnings());

    // 3. Single validation checkpoint - ALL errors collected
    if (!params.isValid()) {
      logValidationError(requestId, params.errors());
      return ToolResponse.error(params.errors());
    }

    // 4. Execute - doExecute returns item or null if not found
    try {
      var result = doExecute(params, warnings);
      var duration = System.currentTimeMillis() - startTime;

      if (result == null) {
        logNotFound(requestId, duration);
        return ToolResponse.notFound("Resource not found", warnings);
      }

      logSuccess(requestId, duration);
      return ToolResponse.success(result, warnings);

    } catch (ResourceNotFoundException e) {
      return handleException(e, requestId, "Resource not found: " + e.getMessage(), warnings);
    } catch (UnauthorizedException e) {
      return handleException(
          e, requestId, "Authentication failed. Check API credentials.", warnings);
    } catch (HttpResponseException e) {
      return handleHttpResponseException(e, requestId, warnings);
    } catch (Exception e) {
      log.atError()
          .addKeyValue("requestId", requestId)
          .setCause(e)
          .setMessage("Request failed unexpectedly")
          .log();
      return ToolResponse.error("Internal error: " + e.getMessage());
    }
  }

  /**
   * Subclasses implement single-item retrieval logic.
   *
   * @param params validated tool-specific params
   * @param warnings MUTABLE list - add execution-time warnings here
   * @return the item, or null if not found
   * @throws Exception any exception from SDK or processing
   */
  protected abstract R doExecute(P params, List<String> warnings) throws Exception;

  /**
   * Returns the cached ContrastSDK instance from configuration.
   *
   * @return cached ContrastSDK
   */
  protected ContrastSDK getContrastSDK() {
    return config.getSDK();
  }

  /**
   * Returns the organization ID from configuration.
   *
   * @return organization ID
   */
  protected String getOrgId() {
    return config.getOrgId();
  }

  private ToolResponse<R> handleException(
      Exception e, String requestId, String userMessage, List<String> warnings) {
    log.atWarn()
        .addKeyValue("requestId", requestId)
        .addKeyValue("exceptionType", e.getClass().getSimpleName())
        .setMessage("Request failed: {}")
        .addArgument(e.getMessage())
        .log();
    // Include warnings in error response for context
    var allWarnings = new ArrayList<>(warnings);
    allWarnings.add(userMessage);
    return new ToolResponse<>(null, List.of(userMessage), warnings, false);
  }

  private ToolResponse<R> handleHttpResponseException(
      HttpResponseException e, String requestId, List<String> warnings) {

    String errorMessage =
        switch (e.getCode()) {
          case 401 -> "Authentication failed. Verify API credentials.";
          case 403 -> "Access denied. User lacks permission for this resource.";
          case 404 -> "Resource not found.";
          case 429 -> "Rate limit exceeded. Retry later.";
          case 500, 502, 503 -> "Contrast API error. Try again later.";
          default -> "API error: " + e.getMessage();
        };

    log.atWarn()
        .addKeyValue("requestId", requestId)
        .addKeyValue("httpStatus", e.getCode())
        .setMessage("API error: {}")
        .addArgument(e.getMessage())
        .log();

    return ToolResponse.error(errorMessage);
  }

  private void logValidationError(String requestId, List<String> errors) {
    log.atDebug()
        .addKeyValue("requestId", requestId)
        .addKeyValue("errorCount", errors.size())
        .setMessage("Validation failed: {}")
        .addArgument(String.join(", ", errors))
        .log();
  }

  private void logNotFound(String requestId, long duration) {
    log.atDebug()
        .addKeyValue("requestId", requestId)
        .addKeyValue("durationMs", duration)
        .setMessage("Resource not found")
        .log();
  }

  private void logSuccess(String requestId, long duration) {
    log.atDebug()
        .addKeyValue("requestId", requestId)
        .addKeyValue("durationMs", duration)
        .setMessage("Request completed successfully")
        .log();
  }
}
