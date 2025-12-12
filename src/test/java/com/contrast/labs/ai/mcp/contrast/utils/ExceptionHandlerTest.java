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

import static org.assertj.core.api.Assertions.assertThat;

import com.contrastsecurity.exceptions.ResourceNotFoundException;
import com.contrastsecurity.exceptions.UnauthorizedException;
import java.io.IOException;
import org.junit.jupiter.api.Test;

/** Tests for ExceptionHandler categorized error handling. */
class ExceptionHandlerTest {

  // ========== toToolResponse Tests ==========

  @Test
  void toToolResponse_should_return_error_response() {
    var exception = new RuntimeException("Something went wrong");

    var response = ExceptionHandler.toToolResponse(exception);

    assertThat(response.isSuccess()).isFalse();
    assertThat(response.data()).isNull();
    assertThat(response.errors()).hasSize(1);
    assertThat(response.errors().get(0)).contains("Internal error");
    assertThat(response.errors().get(0)).contains("Something went wrong");
  }

  @Test
  void toToolResponse_should_handle_authentication_error() {
    var exception =
        new UnauthorizedException("Unauthorized", "GET", "/api/test", 401, "Unauthorized");

    var response = ExceptionHandler.toToolResponse(exception);

    assertThat(response.isSuccess()).isFalse();
    assertThat(response.errors()).hasSize(1);
    assertThat(response.errors().get(0)).contains("Authentication failed");
  }

  @Test
  void toToolResponse_should_handle_not_found_error() {
    var exception =
        new ResourceNotFoundException("App xyz not found", "GET", "/api/apps/xyz", "Not Found");

    var response = ExceptionHandler.toToolResponse(exception);

    assertThat(response.isSuccess()).isFalse();
    assertThat(response.errors()).hasSize(1);
    assertThat(response.errors().get(0)).contains("Resource not found");
    assertThat(response.errors().get(0)).contains("xyz");
  }

  @Test
  void toToolResponse_should_handle_network_error() {
    var exception = new IOException("Connection timed out");

    var response = ExceptionHandler.toToolResponse(exception);

    assertThat(response.isSuccess()).isFalse();
    assertThat(response.errors()).hasSize(1);
    assertThat(response.errors().get(0)).contains("Network error");
    assertThat(response.errors().get(0)).contains("Connection timed out");
  }

  @Test
  void toErrorString_should_return_formatted_string() {
    var exception = new RuntimeException("Something went wrong");

    @SuppressWarnings("deprecation")
    var response = ExceptionHandler.toErrorString(exception);

    assertThat(response).startsWith("Error: ");
    assertThat(response).contains("Internal error");
    assertThat(response).contains("Something went wrong");
  }

  // ========== toPaginatedResponse Tests ==========

  @Test
  void toPaginatedResponse_should_return_empty_items_with_error() {
    var exception = new RuntimeException("Test error");

    var response = ExceptionHandler.toPaginatedResponse(exception, 2, 25);

    assertThat(response.items()).isEmpty();
    assertThat(response.page()).isEqualTo(2);
    assertThat(response.pageSize()).isEqualTo(25);
    assertThat(response.totalItems()).isEqualTo(0);
    assertThat(response.hasMorePages()).isFalse();
    assertThat(response.errors()).hasSize(1);
    assertThat(response.errors().get(0)).contains("Internal error");
    assertThat(response.warnings()).isEmpty();
  }

  // ========== Authentication Errors ==========

  @Test
  void categorizeException_should_handle_UnauthorizedException() {
    var exception =
        new UnauthorizedException("Unauthorized", "GET", "/api/test", 401, "Unauthorized");

    var message = ExceptionHandler.categorizeException(exception);

    assertThat(message).contains("Authentication failed");
    assertThat(message).contains("CONTRAST_API_KEY");
  }

  // ========== Resource Not Found ==========

  @Test
  void categorizeException_should_handle_ResourceNotFoundException_with_message() {
    var exception =
        new ResourceNotFoundException(
            "Vulnerability abc123 not found", "GET", "/api/vulns/abc123", "Not Found");

    var message = ExceptionHandler.categorizeException(exception);

    assertThat(message).contains("Resource not found");
    assertThat(message).contains("abc123");
  }

  @Test
  void categorizeException_should_handle_ResourceNotFoundException_without_message() {
    // SDK exception includes method/path in getMessage() even with null message param
    var exception = new ResourceNotFoundException(null, "GET", "/api/resource", "Not Found");

    var message = ExceptionHandler.categorizeException(exception);

    assertThat(message).contains("Resource not found");
    // SDK always has some message from method/path/status, so we get "Resource not found: ..."
    assertThat(message).startsWith("Resource not found:");
  }

  // ========== Rate Limiting ==========

  @Test
  void categorizeException_should_detect_rate_limit_in_message() {
    var exception = new RuntimeException("rate limit exceeded");

    var message = ExceptionHandler.categorizeException(exception);

    assertThat(message).contains("Rate limit exceeded");
    assertThat(message).contains("wait");
  }

  @Test
  void categorizeException_should_detect_429_in_message() {
    var exception = new RuntimeException("HTTP 429 Too Many Requests");

    var message = ExceptionHandler.categorizeException(exception);

    assertThat(message).contains("Rate limit exceeded");
  }

  @Test
  void categorizeException_should_detect_too_many_requests() {
    var exception = new RuntimeException("Too many requests, please slow down");

    var message = ExceptionHandler.categorizeException(exception);

    assertThat(message).contains("Rate limit exceeded");
  }

  // ========== Network Errors ==========

  @Test
  void categorizeException_should_handle_IOException() {
    var exception = new IOException("Connection refused");

    var message = ExceptionHandler.categorizeException(exception);

    assertThat(message).contains("Network error");
    assertThat(message).contains("Connection refused");
    assertThat(message).contains("CONTRAST_HOST_NAME");
  }

  // ========== Internal Errors ==========

  @Test
  void categorizeException_should_handle_generic_exception_with_message() {
    var exception = new RuntimeException("Something went wrong");

    var message = ExceptionHandler.categorizeException(exception);

    assertThat(message).contains("Internal error");
    assertThat(message).contains("Something went wrong");
  }

  @Test
  void categorizeException_should_handle_generic_exception_without_message() {
    var exception = new NullPointerException();

    var message = ExceptionHandler.categorizeException(exception);

    assertThat(message).contains("Internal error");
    assertThat(message).contains("NullPointerException");
  }

  // ========== Edge Cases ==========

  @Test
  void categorizeException_should_preserve_page_info_in_response() {
    var exception =
        new UnauthorizedException("Unauthorized", "GET", "/api/test", 401, "Unauthorized");

    var response = ExceptionHandler.toPaginatedResponse(exception, 5, 100);

    assertThat(response.page()).isEqualTo(5);
    assertThat(response.pageSize()).isEqualTo(100);
  }
}
