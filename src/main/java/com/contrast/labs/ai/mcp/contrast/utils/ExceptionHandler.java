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
package com.contrast.labs.ai.mcp.contrast.utils;

import com.contrast.labs.ai.mcp.contrast.data.PaginatedResponse;
import com.contrastsecurity.exceptions.ResourceNotFoundException;
import com.contrastsecurity.exceptions.UnauthorizedException;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;

/**
 * Centralized exception handler for MCP tool execution. Converts exceptions into user-friendly
 * error responses with categorized messages suitable for AI consumption.
 *
 * <p>Exception categories:
 *
 * <ul>
 *   <li><b>Authentication</b>: Invalid API credentials
 *   <li><b>Not Found</b>: Resource doesn't exist
 *   <li><b>Rate Limit</b>: Too many requests
 *   <li><b>Network</b>: Connection issues
 *   <li><b>Internal</b>: Unexpected errors (logged for debugging)
 * </ul>
 */
@Slf4j
public class ExceptionHandler {

  private ExceptionHandler() {
    // Utility class - prevent instantiation
  }

  /**
   * Convert an exception to a PaginatedResponse with categorized error message. Use this in tool
   * catch blocks to provide consistent error handling.
   *
   * <p>Usage:
   *
   * <pre>{@code
   * try {
   *   // ... tool logic
   * } catch (Exception e) {
   *   return ExceptionHandler.toPaginatedResponse(e, page, pageSize);
   * }
   * }</pre>
   *
   * @param e The exception to convert
   * @param page Page number to include in response
   * @param pageSize Page size to include in response
   * @param <T> Response item type
   * @return PaginatedResponse with appropriate error message
   */
  public static <T> PaginatedResponse<T> toPaginatedResponse(Exception e, int page, int pageSize) {
    String message = categorizeException(e);
    return PaginatedResponse.error(page, pageSize, message);
  }

  /**
   * Convert an exception to a simple error string for non-paginated tools. Use this in tool catch
   * blocks when the tool doesn't return a PaginatedResponse.
   *
   * <p>Usage:
   *
   * <pre>{@code
   * try {
   *   // ... tool logic
   * } catch (Exception e) {
   *   return ExceptionHandler.toToolResponse(e);
   * }
   * }</pre>
   *
   * @param e The exception to convert
   * @return Formatted error string suitable for tool response
   */
  public static String toToolResponse(Exception e) {
    return "Error: " + categorizeException(e);
  }

  /**
   * Categorize an exception and return a user-friendly error message. Internal errors are logged
   * for debugging.
   *
   * @param e The exception to categorize
   * @return User-friendly error message
   */
  public static String categorizeException(Exception e) {
    // Authentication errors
    if (e instanceof UnauthorizedException) {
      return "Authentication failed. Check API credentials (CONTRAST_API_KEY, "
          + "CONTRAST_SERVICE_KEY, CONTRAST_USERNAME).";
    }

    // Resource not found
    if (e instanceof ResourceNotFoundException) {
      String detail = e.getMessage();
      return detail != null
          ? "Resource not found: " + detail
          : "Resource not found. Check the ID and ensure the resource exists.";
    }

    // Rate limiting (check message content for common patterns)
    String message = e.getMessage();
    if (message != null
        && (message.toLowerCase().contains("rate limit")
            || message.toLowerCase().contains("too many requests")
            || message.contains("429"))) {
      return "Rate limit exceeded. Please wait a few seconds and try again.";
    }

    // Network/IO errors - log as error since this blocks all functionality
    if (e instanceof IOException) {
      log.atError()
          .setCause(e)
          .setMessage("Network error connecting to Contrast server")
          .addKeyValue("exceptionType", e.getClass().getSimpleName())
          .log();
      return "Network error connecting to Contrast server: "
          + e.getMessage()
          + ". Check CONTRAST_HOST_NAME and network connectivity.";
    }

    // Internal/unexpected errors - log for debugging
    log.atError()
        .setCause(e)
        .setMessage("Unexpected error in MCP tool execution")
        .addKeyValue("exceptionType", e.getClass().getSimpleName())
        .log();

    return "Internal error: " + (message != null ? message : e.getClass().getSimpleName());
  }
}
