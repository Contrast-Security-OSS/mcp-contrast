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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;

/**
 * Abstract base class for paginated MCP search tools. Enforces a consistent processing pipeline via
 * Template Method pattern.
 *
 * <p>Subclasses implement {@link #doExecute} for tool-specific logic. The base class handles:
 *
 * <ul>
 *   <li>Pagination parameter parsing and validation
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
public abstract class PaginatedTool<P extends ToolParams, R> extends BaseTool {

  /**
   * Returns the maximum page size for this tool. Default is 100. Override in subclasses to set a
   * lower limit (e.g., when API has stricter limits).
   *
   * @return maximum page size allowed for this tool
   */
  protected int getMaxPageSize() {
    return MAX_PAGE_SIZE; // 100
  }

  /**
   * Template method - defines the mandatory processing pipeline. Subclasses implement doExecute()
   * for tool-specific logic. This method is FINAL to enforce consistent processing.
   *
   * @param page requested page number (1-based), null defaults to 1
   * @param pageSize requested page size, null defaults to 50
   * @param paramsSupplier lazy supplier of tool-specific parameters
   * @return paginated response with items or errors
   */
  protected final PaginatedToolResponse<R> executePipeline(
      Integer page, Integer pageSize, Supplier<P> paramsSupplier) {

    var requestId = UUID.randomUUID().toString().substring(0, 8);
    long startTime = System.currentTimeMillis();

    // 1. Parse pagination FIRST with tool-specific max (always succeeds with warnings)
    var pagination = PaginationParams.of(page, pageSize, getMaxPageSize());

    // 2. Parse tool-specific params (collects all errors)
    var params = paramsSupplier.get();

    // 3. MUTABLE warnings list - doExecute can ADD to this
    var warnings = new ArrayList<String>();
    warnings.addAll(pagination.warnings());
    warnings.addAll(params.warnings());

    // 4. Single validation checkpoint - ALL errors collected
    if (!params.isValid()) {
      logValidationError(requestId, params.errors());
      return PaginatedToolResponse.validationError(
          pagination.page(), pagination.pageSize(), params.errors());
    }

    // 5. Execute - doExecute returns intermediate result, can add warnings
    try {
      var result = doExecute(pagination, params, warnings);
      var duration = System.currentTimeMillis() - startTime;

      // 6. BASE CLASS builds final response - ensures consistency
      return buildSuccessResponse(result, pagination, warnings, duration, requestId);

    } catch (UnauthorizedException e) {
      return handleException(
          e, pagination, requestId, "Authentication failed. Check API credentials.");
    } catch (ResourceNotFoundException e) {
      return handleException(e, pagination, requestId, "Resource not found: " + e.getMessage());
    } catch (HttpResponseException e) {
      return handleHttpResponseException(e, pagination, requestId);
    } catch (Exception e) {
      log.atError()
          .addKeyValue("requestId", requestId)
          .setCause(e)
          .setMessage("Request failed unexpectedly")
          .log();
      return PaginatedToolResponse.error(
          pagination.page(), pagination.pageSize(), "Internal error: " + e.getMessage());
    }
  }

  /**
   * Subclasses implement tool-specific execution logic.
   *
   * @param pagination validated pagination params
   * @param params validated tool-specific params
   * @param warnings MUTABLE list - add execution-time warnings here
   * @return ExecutionResult with items and optional total count
   * @throws Exception any exception from SDK or processing
   */
  protected abstract ExecutionResult<R> doExecute(
      PaginationParams pagination, P params, List<String> warnings) throws Exception;

  private PaginatedToolResponse<R> handleException(
      Exception e, PaginationParams pagination, String requestId, String userMessage) {
    log.atWarn()
        .addKeyValue("requestId", requestId)
        .addKeyValue("exceptionType", e.getClass().getSimpleName())
        .setMessage("Request failed: {}")
        .addArgument(e.getMessage())
        .log();
    return PaginatedToolResponse.error(pagination.page(), pagination.pageSize(), userMessage);
  }

  private PaginatedToolResponse<R> handleHttpResponseException(
      HttpResponseException e, PaginationParams pagination, String requestId) {

    String errorMessage = mapHttpErrorCode(e.getCode());

    log.atWarn()
        .addKeyValue("requestId", requestId)
        .addKeyValue("httpStatus", e.getCode())
        .setMessage("API error: {}")
        .addArgument(e.getMessage())
        .log();

    return PaginatedToolResponse.error(pagination.page(), pagination.pageSize(), errorMessage);
  }

  private PaginatedToolResponse<R> buildSuccessResponse(
      ExecutionResult<R> result,
      PaginationParams pagination,
      List<String> warnings,
      long duration,
      String requestId) {

    if (result.items().isEmpty() && result.totalItems() != null && result.totalItems() == 0) {
      warnings.add("No results found matching the specified criteria.");
    }

    boolean hasMore = calculateHasMorePages(result, pagination);

    logSuccess(requestId, duration, result.items().size(), result.totalItems());

    return PaginatedToolResponse.success(
        result.items(),
        pagination.page(),
        pagination.pageSize(),
        result.totalItems(),
        hasMore,
        warnings,
        duration);
  }

  private boolean calculateHasMorePages(ExecutionResult<R> result, PaginationParams pagination) {
    if (result.totalItems() != null) {
      return pagination.offset() + result.items().size() < result.totalItems();
    }
    // Unknown total - assume more pages if we got a full page
    return result.items().size() >= pagination.limit();
  }

  private void logValidationError(String requestId, List<String> errors) {
    log.atDebug()
        .addKeyValue("requestId", requestId)
        .addKeyValue("errorCount", errors.size())
        .setMessage("Validation failed: {}")
        .addArgument(String.join(", ", errors))
        .log();
  }

  private void logSuccess(String requestId, long duration, int itemCount, Integer totalItems) {
    log.atDebug()
        .addKeyValue("requestId", requestId)
        .addKeyValue("durationMs", duration)
        .addKeyValue("itemCount", itemCount)
        .addKeyValue("totalItems", totalItems)
        .setMessage("Request completed successfully")
        .log();
  }
}
