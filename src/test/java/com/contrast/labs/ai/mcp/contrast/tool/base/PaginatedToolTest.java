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

import static org.assertj.core.api.Assertions.assertThat;

import com.contrast.labs.ai.mcp.contrast.PaginationParams;
import com.contrastsecurity.exceptions.HttpResponseException;
import com.contrastsecurity.exceptions.ResourceNotFoundException;
import com.contrastsecurity.exceptions.UnauthorizedException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PaginatedToolTest {

  private TestSearchTool tool;

  @BeforeEach
  void setUp() {
    tool = new TestSearchTool();
  }

  @Test
  void executePipeline_should_call_doExecute_with_validated_params() {
    var capturedPagination = new AtomicReference<PaginationParams>();
    var capturedParams = new AtomicReference<TestParams>();

    tool.setDoExecuteHandler(
        (pagination, params, warnings) -> {
          capturedPagination.set(pagination);
          capturedParams.set(params);
          return ExecutionResult.of(List.of("item1", "item2"), 2);
        });

    var result = tool.executePipeline(2, 25, () -> TestParams.valid());

    assertThat(result.isSuccess()).isTrue();
    assertThat(capturedPagination.get().page()).isEqualTo(2);
    assertThat(capturedPagination.get().pageSize()).isEqualTo(25);
    assertThat(capturedParams.get()).isNotNull();
  }

  @Test
  void executePipeline_should_return_validation_error_when_params_invalid() {
    tool.setDoExecuteHandler(
        (pagination, params, warnings) -> {
          throw new RuntimeException("Should not be called");
        });

    var result = tool.executePipeline(1, 10, () -> TestParams.invalid("Field is required"));

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors()).containsExactly("Field is required");
    assertThat(result.items()).isEmpty();
  }

  @Test
  void executePipeline_should_handle_unauthorized_exception() {
    tool.setDoExecuteHandler(
        (pagination, params, warnings) -> {
          throw new UnauthorizedException(
              "Invalid credentials", "GET", "/api/test", 401, "Unauthorized");
        });

    var result = tool.executePipeline(1, 10, () -> TestParams.valid());

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors()).containsExactly("Authentication failed. Check API credentials.");
  }

  @Test
  void executePipeline_should_handle_resource_not_found_exception() {
    tool.setDoExecuteHandler(
        (pagination, params, warnings) -> {
          throw new ResourceNotFoundException("App not found", "GET", "/api/apps/123", "Not Found");
        });

    var result = tool.executePipeline(1, 10, () -> TestParams.valid());

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors().get(0)).startsWith("Resource not found:");
  }

  @Test
  void executePipeline_should_handle_http_response_exception_403() {
    tool.setDoExecuteHandler(
        (pagination, params, warnings) -> {
          throw new UnauthorizedException("Forbidden", "GET", "/api/test", 403, "Forbidden");
        });

    var result = tool.executePipeline(1, 10, () -> TestParams.valid());

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors()).containsExactly("Authentication failed. Check API credentials.");
  }

  @Test
  void executePipeline_should_handle_http_response_exception_429() {
    tool.setDoExecuteHandler(
        (pagination, params, warnings) -> {
          throw new HttpResponseException(
              "Rate limited", "GET", "/api/test", 429, "Too Many Requests");
        });

    var result = tool.executePipeline(1, 10, () -> TestParams.valid());

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors()).containsExactly("Rate limit exceeded. Retry later.");
  }

  @Test
  void executePipeline_should_handle_http_response_exception_500() {
    tool.setDoExecuteHandler(
        (pagination, params, warnings) -> {
          throw new HttpResponseException(
              "Server error", "GET", "/api/test", 500, "Internal Server Error");
        });

    var result = tool.executePipeline(1, 10, () -> TestParams.valid());

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors()).containsExactly("Contrast API error. Try again later.");
  }

  @Test
  void executePipeline_should_handle_generic_exception() {
    tool.setDoExecuteHandler(
        (pagination, params, warnings) -> {
          throw new RuntimeException("Unexpected failure");
        });

    var result = tool.executePipeline(1, 10, () -> TestParams.valid());

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors()).containsExactly("Internal error: Unexpected failure");
  }

  @Test
  void executePipeline_should_calculate_hasMorePages_with_known_total() {
    tool.setDoExecuteHandler(
        (pagination, params, warnings) ->
            ExecutionResult.of(List.of("item1", "item2"), 100)); // 100 total items

    var result = tool.executePipeline(1, 10, () -> TestParams.valid());

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.hasMorePages()).isTrue();
    assertThat(result.totalItems()).isEqualTo(100);
  }

  @Test
  void executePipeline_should_calculate_hasMorePages_false_on_last_page() {
    tool.setDoExecuteHandler(
        (pagination, params, warnings) ->
            ExecutionResult.of(List.of("item1", "item2"), 2)); // 2 items total, 2 returned

    var result = tool.executePipeline(1, 10, () -> TestParams.valid());

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.hasMorePages()).isFalse();
  }

  @Test
  void executePipeline_should_calculate_hasMorePages_with_unknown_total() {
    // When total is unknown, assume more pages if we got a full page
    tool.setDoExecuteHandler(
        (pagination, params, warnings) ->
            ExecutionResult.of(List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10")));

    var result = tool.executePipeline(1, 10, () -> TestParams.valid());

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.totalItems()).isNull();
    assertThat(result.hasMorePages()).isTrue(); // Full page returned
  }

  @Test
  void executePipeline_should_calculate_hasMorePages_false_with_unknown_total_partial_page() {
    tool.setDoExecuteHandler(
        (pagination, params, warnings) -> ExecutionResult.of(List.of("1", "2", "3")));

    var result = tool.executePipeline(1, 10, () -> TestParams.valid());

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.totalItems()).isNull();
    assertThat(result.hasMorePages()).isFalse(); // Partial page = no more
  }

  @Test
  void executePipeline_should_add_empty_results_warning() {
    tool.setDoExecuteHandler((pagination, params, warnings) -> ExecutionResult.of(List.of(), 0));

    var result = tool.executePipeline(1, 10, () -> TestParams.valid());

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.items()).isEmpty();
    assertThat(result.warnings()).contains("No results found matching the specified criteria.");
  }

  @Test
  void executePipeline_should_include_pagination_warnings() {
    tool.setDoExecuteHandler(
        (pagination, params, warnings) -> ExecutionResult.of(List.of("item"), 1));

    var result = tool.executePipeline(-1, 10, () -> TestParams.valid()); // Invalid page

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.warnings()).anyMatch(w -> w.contains("Invalid page number"));
  }

  @Test
  void executePipeline_should_include_params_warnings() {
    tool.setDoExecuteHandler(
        (pagination, params, warnings) -> ExecutionResult.of(List.of("item"), 1));

    var result = tool.executePipeline(1, 10, () -> TestParams.withWarning("Deprecated parameter"));

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.warnings()).contains("Deprecated parameter");
  }

  @Test
  void executePipeline_should_allow_doExecute_to_add_warnings() {
    tool.setDoExecuteHandler(
        (pagination, params, warnings) -> {
          warnings.add("Session filtering applied");
          return ExecutionResult.of(List.of("item"), 1);
        });

    var result = tool.executePipeline(1, 10, () -> TestParams.valid());

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.warnings()).contains("Session filtering applied");
  }

  @Test
  void executePipeline_should_track_duration() {
    tool.setDoExecuteHandler(
        (pagination, params, warnings) -> ExecutionResult.of(List.of("item"), 1));

    var result = tool.executePipeline(1, 10, () -> TestParams.valid());

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.durationMs()).isNotNull();
    assertThat(result.durationMs()).isGreaterThanOrEqualTo(0);
  }

  // Test implementation of PaginatedTool
  private static class TestSearchTool extends PaginatedTool<TestParams, String> {
    private DoExecuteHandler handler;

    void setDoExecuteHandler(DoExecuteHandler handler) {
      this.handler = handler;
    }

    @Override
    protected ExecutionResult<String> doExecute(
        PaginationParams pagination, TestParams params, List<String> warnings) throws Exception {
      if (handler != null) {
        return handler.execute(pagination, params, warnings);
      }
      return ExecutionResult.empty();
    }

    @FunctionalInterface
    interface DoExecuteHandler {
      ExecutionResult<String> execute(
          PaginationParams pagination, TestParams params, List<String> warnings) throws Exception;
    }
  }

  // Test params implementation
  private record TestParams(boolean isValid, List<String> errors, List<String> warnings)
      implements ToolParams {

    static TestParams valid() {
      return new TestParams(true, List.of(), List.of());
    }

    static TestParams invalid(String error) {
      return new TestParams(false, List.of(error), List.of());
    }

    static TestParams withWarning(String warning) {
      return new TestParams(true, List.of(), List.of(warning));
    }
  }
}
