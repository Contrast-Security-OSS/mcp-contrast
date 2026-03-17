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

import com.contrastsecurity.exceptions.HttpResponseException;
import com.contrastsecurity.exceptions.ResourceNotFoundException;
import com.contrastsecurity.exceptions.UnauthorizedException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;

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
public abstract class SingleTool<P extends ToolParams, R> extends BaseTool {

  private static final int REQUEST_ID_PREFIX_LENGTH = 8;

  /**
   * Template method - defines the mandatory processing pipeline for single-item retrieval.
   * Subclasses implement doExecute() for tool-specific logic. This method is FINAL to enforce
   * consistent processing.
   *
   * @param paramsSupplier lazy supplier of tool-specific parameters
   * @return tool response with item or errors
   */
  protected final SingleToolResponse<R> executePipeline(Supplier<P> paramsSupplier) {
    var requestId = UUID.randomUUID().toString().substring(0, REQUEST_ID_PREFIX_LENGTH);
    long startTime = System.currentTimeMillis();

    // 1. Parse tool-specific params (collects all errors)
    var params = paramsSupplier.get();

    // 2. Collector accumulates warnings from all stages
    var collector = WarningCollector.forContext(log, Map.of("requestId", requestId));
    params.warnings().forEach(collector::warn);

    // 3. Single validation checkpoint - ALL errors collected
    if (!params.isValid()) {
      logValidationError(requestId, params.errors());
      return SingleToolResponse.error(params.errors());
    }

    // 4. Execute - doExecute returns item or null if not found
    try {
      var result = doExecute(params, collector);
      var duration = System.currentTimeMillis() - startTime;

      if (result == null) {
        logNotFound(requestId, duration);
        return SingleToolResponse.notFound("Resource not found", collector.snapshot());
      }

      logSuccess(requestId, duration);
      return SingleToolResponse.success(result, collector.snapshot());

    } catch (ResourceNotFoundException e) {
      var duration = System.currentTimeMillis() - startTime;
      logNotFound(requestId, duration);
      return SingleToolResponse.notFound("Resource not found", collector.snapshot());
    } catch (UnauthorizedException e) {
      return handleException(
          e,
          requestId,
          "Authentication failed or resource not found. Verify credentials and that the resource ID"
              + " is correct.",
          collector);
    } catch (HttpResponseException e) {
      return handleHttpResponseException(e, requestId, collector);
    } catch (Exception e) {
      log.atError()
          .addKeyValue("requestId", requestId)
          .setCause(e)
          .setMessage("Request failed unexpectedly")
          .log();
      return SingleToolResponse.error("An internal error occurred (ref: " + requestId + ")");
    }
  }

  /**
   * Subclasses implement single-item retrieval logic.
   *
   * @param params validated tool-specific params
   * @param collector warning accumulator - call {@link WarningCollector#warn}, {@link
   *     WarningCollector#tryFetchRequired}, or {@link WarningCollector#tryFetch} to record warnings
   * @return the item, or null if not found
   * @throws Exception any exception from SDK or processing
   */
  protected abstract R doExecute(P params, WarningCollector collector) throws Exception;

  private SingleToolResponse<R> handleException(
      Exception e, String requestId, String userMessage, WarningCollector collector) {
    log.atWarn()
        .addKeyValue("requestId", requestId)
        .addKeyValue("exceptionType", e.getClass().getSimpleName())
        .setMessage("Request failed: {}")
        .addArgument(e.getMessage())
        .log();
    collector.warn(userMessage);
    return new SingleToolResponse<>(null, List.of(userMessage), collector.snapshot(), false);
  }

  private SingleToolResponse<R> handleHttpResponseException(
      HttpResponseException e, String requestId, WarningCollector collector) {

    String errorMessage = mapHttpErrorCode(e.getCode());

    log.atWarn()
        .addKeyValue("requestId", requestId)
        .addKeyValue("httpStatus", e.getCode())
        .setMessage("API error: {}")
        .addArgument(e.getMessage())
        .log();

    return new SingleToolResponse<>(null, List.of(errorMessage), collector.snapshot(), false);
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
