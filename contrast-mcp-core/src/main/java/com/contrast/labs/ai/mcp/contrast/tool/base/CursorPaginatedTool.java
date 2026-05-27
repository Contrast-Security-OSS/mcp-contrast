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

import static com.contrast.labs.ai.mcp.contrast.tool.validation.ValidationConstants.MAX_PAGE_SIZE;

import com.contrastsecurity.exceptions.HttpResponseException;
import com.contrastsecurity.exceptions.ResourceNotFoundException;
import com.contrastsecurity.exceptions.UnauthorizedException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.lang.Nullable;

/**
 * Abstract base class for cursor/keyset-backed MCP list tools. Subclasses must treat cursor values
 * as opaque pass-through continuation tokens.
 *
 * @param <P> tool parameter type
 * @param <R> result item type
 */
@Slf4j
public abstract class CursorPaginatedTool<P extends ToolParams, R> extends BaseTool {

  private static final int REQUEST_ID_PREFIX_LENGTH = 8;

  /**
   * Returns the maximum page size for this tool. Override for narrower endpoint limits.
   *
   * @return maximum page size
   */
  protected int getMaxPageSize() {
    return MAX_PAGE_SIZE;
  }

  protected final CursorToolResponse<R> executePipeline(
      @Nullable String cursor, @Nullable Integer pageSize, Supplier<P> paramsSupplier) {
    return executePipeline(cursor, pageSize, paramsSupplier, null);
  }

  protected final CursorToolResponse<R> executePipeline(
      @Nullable String cursor,
      @Nullable Integer pageSize,
      Supplier<P> paramsSupplier,
      @Nullable ToolContext toolContext) {

    var requestId = UUID.randomUUID().toString().substring(0, REQUEST_ID_PREFIX_LENGTH);
    long startTime = System.currentTimeMillis();

    var pagination = CursorPaginationParams.of(cursor, pageSize, getMaxPageSize());
    var params = paramsSupplier.get();
    var collector = WarningCollector.forContext(Map.of(LoggingKeys.REQUEST_ID, requestId));
    pagination.warnings().forEach(collector::warn);
    params.warnings().forEach(collector::warn);

    if (!params.isValid()) {
      logValidationError(requestId, params.errors(), pagination.cursorPresence());
      return CursorToolResponse.validationError(pagination.pageSize(), params.errors());
    }

    try (var ignored = authenticate(toolContext)) {
      var result = doExecute(pagination, params, collector);
      var duration = System.currentTimeMillis() - startTime;

      return buildSuccessResponse(result, pagination, collector, duration, requestId);

    } catch (UnauthorizedException e) {
      return handleException(e, pagination, requestId, mapHttpErrorCode(e.getCode()));
    } catch (ResourceNotFoundException e) {
      return handleException(e, pagination, requestId, "Resource not found");
    } catch (HttpResponseException e) {
      return handleHttpResponseException(e, pagination, requestId, collector);
    } catch (IllegalArgumentException e) {
      return handleException(e, pagination, requestId, e.getMessage());
    } catch (Exception e) {
      log.atError()
          .addKeyValue(LoggingKeys.REQUEST_ID, requestId)
          .addKeyValue(LoggingKeys.CURSOR, pagination.cursorPresence())
          .addKeyValue(LoggingKeys.EXCEPTION_TYPE, e.getClass().getSimpleName())
          .setMessage("Request failed unexpectedly")
          .log();
      return CursorToolResponse.error(
          pagination.pageSize(), "An internal error occurred (ref: " + requestId + ")");
    }
  }

  /**
   * Subclasses implement cursor-backed execution.
   *
   * @param pagination validated cursor pagination params
   * @param params validated tool-specific params
   * @param collector warning accumulator
   * @return cursor execution result
   * @throws Exception from downstream clients or processing
   */
  protected abstract CursorExecutionResult<R> doExecute(
      CursorPaginationParams pagination, P params, WarningCollector collector) throws Exception;

  private CursorToolResponse<R> handleException(
      Exception e, CursorPaginationParams pagination, String requestId, String userMessage) {
    log.atWarn()
        .addKeyValue(LoggingKeys.REQUEST_ID, requestId)
        .addKeyValue(LoggingKeys.CURSOR, pagination.cursorPresence())
        .addKeyValue(LoggingKeys.EXCEPTION_TYPE, e.getClass().getSimpleName())
        .setMessage("Request failed")
        .log();
    return CursorToolResponse.error(pagination.pageSize(), userMessage);
  }

  private CursorToolResponse<R> handleHttpResponseException(
      HttpResponseException e,
      CursorPaginationParams pagination,
      String requestId,
      WarningCollector collector) {

    String errorMessage = mapHttpErrorCode(e.getCode());

    log.atWarn()
        .addKeyValue(LoggingKeys.REQUEST_ID, requestId)
        .addKeyValue(LoggingKeys.CURSOR, pagination.cursorPresence())
        .addKeyValue(LoggingKeys.HTTP_STATUS, e.getCode())
        .setMessage("API error")
        .log();

    return new CursorToolResponse<>(
        List.of(),
        pagination.pageSize(),
        null,
        false,
        List.of(errorMessage),
        collector.snapshot(),
        null);
  }

  private CursorToolResponse<R> buildSuccessResponse(
      CursorExecutionResult<R> result,
      CursorPaginationParams pagination,
      WarningCollector collector,
      long duration,
      String requestId) {

    if (result.items().isEmpty() && !result.hasMore() && !collector.hasEmptyResultsWarning()) {
      collector.warn("No results found matching the specified criteria.");
    }

    logSuccess(requestId, duration, result.items().size(), pagination.cursorPresence());

    return CursorToolResponse.success(
        result.items(),
        pagination.pageSize(),
        result.nextCursor(),
        result.hasMore(),
        collector.snapshot(),
        duration);
  }

  private void logValidationError(String requestId, List<String> errors, String cursorPresence) {
    log.atDebug()
        .addKeyValue(LoggingKeys.REQUEST_ID, requestId)
        .addKeyValue(LoggingKeys.CURSOR, cursorPresence)
        .addKeyValue(LoggingKeys.ERROR_COUNT, errors.size())
        .setMessage("Validation failed: {}")
        .addArgument(String.join(", ", errors))
        .log();
  }

  private void logSuccess(String requestId, long duration, int itemCount, String cursorPresence) {
    log.atDebug()
        .addKeyValue(LoggingKeys.REQUEST_ID, requestId)
        .addKeyValue(LoggingKeys.CURSOR, cursorPresence)
        .addKeyValue(LoggingKeys.DURATION_MS, duration)
        .addKeyValue(LoggingKeys.ITEM_COUNT, itemCount)
        .setMessage("Request completed successfully")
        .log();
  }
}
