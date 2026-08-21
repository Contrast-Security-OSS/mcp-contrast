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
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;

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
    return executePipeline(paramsSupplier, null);
  }

  protected final SingleToolResponse<R> executePipeline(
      Supplier<P> paramsSupplier, @Nullable ToolContext toolContext) {
    var requestId = UUID.randomUUID().toString().substring(0, REQUEST_ID_PREFIX_LENGTH);
    long startTime = System.currentTimeMillis();

    // 1. Parse tool-specific params (collects all errors)
    var params = paramsSupplier.get();

    // 2. Collector accumulates notices from all stages
    var collector = NoticeCollector.forContext(Map.of(LoggingKeys.REQUEST_ID, requestId));
    params.notices().forEach(collector::notice);

    // 3. Single validation checkpoint - ALL errors collected
    if (!params.isValid()) {
      logValidationError(requestId, params.errors());
      return SingleToolResponse.error(params.errors());
    }

    // 4. Execute - doExecute returns item or null if not found
    try (var ignored = authenticate(toolContext)) {
      var result = doExecute(params, collector);
      var duration = System.currentTimeMillis() - startTime;

      if (result == null) {
        logNotFound(requestId, duration);
        return SingleToolResponse.notFound(RESOURCE_NOT_FOUND_MESSAGE, collector.snapshot());
      }

      logSuccess(requestId, duration);
      return SingleToolResponse.success(result, collector.snapshot());

    } catch (ResourceNotFoundException e) {
      var duration = System.currentTimeMillis() - startTime;
      logNotFound(requestId, duration);
      return SingleToolResponse.notFound(RESOURCE_NOT_FOUND_MESSAGE, collector.snapshot());
    } catch (UnauthorizedException e) {
      return handleException(e, requestId, mapHttpErrorCode(e.getCode()), collector);
    } catch (HttpResponseException e) {
      return handleHttpResponseException(e, requestId, collector);
    } catch (ActionableToolErrorException | IllegalArgumentException e) {
      // User-input rejection raised mid-execution. The exception message is the actionable user
      // message.
      return handleException(e, requestId, e.getMessage(), collector);
    } catch (Exception e) {
      log.atError()
          .addKeyValue(LoggingKeys.REQUEST_ID, requestId)
          .addKeyValue(LoggingKeys.EXCEPTION_TYPE, e.getClass().getSimpleName())
          .setMessage("Request failed unexpectedly")
          .log();
      return SingleToolResponse.error("An internal error occurred (ref: " + requestId + ")");
    }
  }

  /**
   * Subclasses implement single-item retrieval logic.
   *
   * @param params validated tool-specific params
   * @param collector notice accumulator - call {@link NoticeCollector#notice}, {@link
   *     NoticeCollector#tryFetch}, or {@link NoticeCollector#tryRun} to record notices
   * @return the item, or null if not found
   * @throws Exception any exception from SDK or processing
   */
  protected abstract R doExecute(P params, NoticeCollector collector) throws Exception;

  private SingleToolResponse<R> handleException(
      Exception e, String requestId, String userMessage, NoticeCollector collector) {
    log.atWarn()
        .addKeyValue(LoggingKeys.REQUEST_ID, requestId)
        .addKeyValue(LoggingKeys.EXCEPTION_TYPE, e.getClass().getSimpleName())
        .setMessage("Request failed")
        .log();
    return new SingleToolResponse<>(null, List.of(userMessage), collector.snapshot(), false);
  }

  private SingleToolResponse<R> handleHttpResponseException(
      HttpResponseException e, String requestId, NoticeCollector collector) {

    String errorMessage = mapHttpErrorCode(e.getCode());

    log.atWarn()
        .addKeyValue(LoggingKeys.REQUEST_ID, requestId)
        .addKeyValue(LoggingKeys.HTTP_STATUS, e.getCode())
        .setMessage("API error")
        .log();

    return new SingleToolResponse<>(null, List.of(errorMessage), collector.snapshot(), false);
  }

  private void logValidationError(String requestId, List<String> errors) {
    log.atDebug()
        .addKeyValue(LoggingKeys.REQUEST_ID, requestId)
        .addKeyValue(LoggingKeys.ERROR_COUNT, errors.size())
        .setMessage("Validation failed: {}")
        .addArgument(String.join(", ", errors))
        .log();
  }

  private void logNotFound(String requestId, long duration) {
    log.atDebug()
        .addKeyValue(LoggingKeys.REQUEST_ID, requestId)
        .addKeyValue(LoggingKeys.DURATION_MS, duration)
        .setMessage(RESOURCE_NOT_FOUND_MESSAGE)
        .log();
  }

  private void logSuccess(String requestId, long duration) {
    log.atDebug()
        .addKeyValue(LoggingKeys.REQUEST_ID, requestId)
        .addKeyValue(LoggingKeys.DURATION_MS, duration)
        .setMessage("Request completed successfully")
        .log();
  }
}
