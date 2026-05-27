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

import com.contrastsecurity.exceptions.HttpResponseException;
import com.contrastsecurity.exceptions.ResourceNotFoundException;
import com.contrastsecurity.exceptions.UnauthorizedException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CursorPaginatedToolTest {

  private static final String AUTH_OR_NOT_FOUND_MESSAGE =
      "Authentication failed or resource not found. Verify credentials and that the resource ID"
          + " is correct.";
  private static final String OPAQUE_CURSOR = "opaque.cursor/with+symbols==";

  private TestCursorTool tool;

  @BeforeEach
  void setUp() {
    tool = new TestCursorTool();
  }

  @Test
  void executePipeline_should_call_doExecute_with_absent_cursor_for_first_page() {
    var capturedPagination = new AtomicReference<CursorPaginationParams>();

    tool.setDoExecuteHandler(
        (pagination, params, collector) -> {
          capturedPagination.set(pagination);
          return CursorExecutionResult.of(List.of("item1"), "next-token", true);
        });

    var result = tool.executePipeline(null, null, TestParams::valid);

    assertThat(result.isSuccess()).isTrue();
    assertThat(capturedPagination.get().cursor()).isNull();
    assertThat(capturedPagination.get().cursorPresence()).isEqualTo("absent");
    assertThat(result.pageSize()).isEqualTo(50);
    assertThat(result.nextCursor()).isEqualTo("next-token");
    assertThat(result.hasMore()).isTrue();
  }

  @Test
  void executePipeline_should_pass_continuation_cursor_without_parsing() {
    var capturedCursor = new AtomicReference<String>();

    tool.setDoExecuteHandler(
        (pagination, params, collector) -> {
          capturedCursor.set(pagination.cursor());
          return CursorExecutionResult.of(List.of("item2"), null, false);
        });

    var result = tool.executePipeline(OPAQUE_CURSOR, 25, TestParams::valid);

    assertThat(result.isSuccess()).isTrue();
    assertThat(capturedCursor.get()).isEqualTo(OPAQUE_CURSOR);
    assertThat(result.pageSize()).isEqualTo(25);
    assertThat(result.nextCursor()).isNull();
    assertThat(result.hasMore()).isFalse();
  }

  @Test
  void executePipeline_should_not_execute_when_params_are_invalid() {
    var executed = new AtomicBoolean(false);
    tool.setDoExecuteHandler(
        (pagination, params, collector) -> {
          executed.set(true);
          return CursorExecutionResult.of(List.of("item"), null, false);
        });

    var result =
        tool.executePipeline(OPAQUE_CURSOR, 25, () -> TestParams.invalid("issueId is required"));

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors()).containsExactly("issueId is required");
    assertThat(result.errors()).noneMatch(error -> error.contains(OPAQUE_CURSOR));
    assertThat(result.pageSize()).isEqualTo(25);
    assertThat(executed).isFalse();
  }

  @Test
  void executePipeline_should_include_page_size_warnings_without_cursor_value() {
    tool.setDoExecuteHandler(
        (pagination, params, collector) -> CursorExecutionResult.of(List.of("item"), null, false));

    var result = tool.executePipeline(OPAQUE_CURSOR, 0, TestParams::valid);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.pageSize()).isEqualTo(50);
    assertThat(result.warnings())
        .singleElement()
        .satisfies(warning -> assertThat(warning).contains("Invalid pageSize 0"));
    assertThat(result.warnings()).noneMatch(warning -> warning.contains(OPAQUE_CURSOR));
  }

  @Test
  void executePipeline_should_preserve_warnings_when_http_response_exception_occurs() {
    tool.setDoExecuteHandler(
        (pagination, params, collector) -> {
          collector.warn("Warning added before exception");
          throw new HttpResponseException("Expired cursor", "GET", "/api/test", 400, "Bad Request");
        });

    var result =
        tool.executePipeline(OPAQUE_CURSOR, 25, () -> TestParams.withWarning("Initial warning"));

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors()).containsExactly("API error (HTTP 400)");
    assertThat(result.errors()).noneMatch(error -> error.contains(OPAQUE_CURSOR));
    assertThat(result.warnings())
        .containsExactlyInAnyOrder("Initial warning", "Warning added before exception");
    assertThat(result.warnings()).noneMatch(warning -> warning.contains(OPAQUE_CURSOR));
  }

  @Test
  void executePipeline_should_handle_unauthorized_exception_without_cursor_value() {
    tool.setDoExecuteHandler(
        (pagination, params, collector) -> {
          throw new UnauthorizedException(
              "Invalid credentials", "GET", "/api/test", 401, "Unauthorized");
        });

    var result = tool.executePipeline(OPAQUE_CURSOR, 25, TestParams::valid);

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors()).containsExactly(AUTH_OR_NOT_FOUND_MESSAGE);
    assertThat(result.errors()).noneMatch(error -> error.contains(OPAQUE_CURSOR));
  }

  @Test
  void executePipeline_should_handle_unauthorized_exception_403_without_cursor_value() {
    tool.setDoExecuteHandler(
        (pagination, params, collector) -> {
          throw new UnauthorizedException("Forbidden", "GET", "/api/test", 403, "Forbidden");
        });

    var result = tool.executePipeline(OPAQUE_CURSOR, 25, TestParams::valid);

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors())
        .containsExactly(
            "Access denied or resource not found. Verify credentials and that the resource ID is"
                + " correct.");
    assertThat(result.errors()).noneMatch(error -> error.contains(OPAQUE_CURSOR));
  }

  @Test
  void executePipeline_should_handle_resource_not_found_without_cursor_value() {
    tool.setDoExecuteHandler(
        (pagination, params, collector) -> {
          throw new ResourceNotFoundException("Not found", "GET", "/api/test", "Not Found");
        });

    var result = tool.executePipeline(OPAQUE_CURSOR, 25, TestParams::valid);

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors()).containsExactly("Resource not found");
    assertThat(result.errors()).noneMatch(error -> error.contains(OPAQUE_CURSOR));
  }

  @Test
  void executePipeline_should_not_expose_generic_exception_message_or_cursor_value() {
    tool.setDoExecuteHandler(
        (pagination, params, collector) -> {
          throw new RuntimeException("sensitive cursor " + OPAQUE_CURSOR + " at /api/ng/org");
        });

    var result = tool.executePipeline(OPAQUE_CURSOR, 25, TestParams::valid);

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors())
        .singleElement()
        .satisfies(
            error -> {
              assertThat(error).startsWith("An internal error occurred (ref: ");
              assertThat(error).doesNotContain(OPAQUE_CURSOR);
              assertThat(error).doesNotContain("/api/ng/");
              assertThat(error).doesNotContain("org");
            });
  }

  @Test
  void executePipeline_should_track_duration() {
    tool.setDoExecuteHandler(
        (pagination, params, collector) -> CursorExecutionResult.of(List.of("item"), null, false));

    var result = tool.executePipeline(null, 25, TestParams::valid);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.durationMs()).isNotNull();
    assertThat(result.durationMs()).isGreaterThanOrEqualTo(0);
  }

  @Test
  void executePipeline_should_add_empty_results_warning() {
    tool.setDoExecuteHandler(
        (pagination, params, collector) -> CursorExecutionResult.of(List.of(), null, false));

    var result = tool.executePipeline(null, 25, TestParams::valid);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.items()).isEmpty();
    assertThat(result.warnings())
        .containsExactly("No results found matching the specified criteria.");
  }

  @Test
  void executePipeline_should_not_add_generic_empty_warning_when_tool_explains_empty_result() {
    tool.setDoExecuteHandler(
        (pagination, params, collector) -> {
          collector.warnForEmptyResults("No cursor widgets found.");
          return CursorExecutionResult.of(List.of(), null, false);
        });

    var result = tool.executePipeline(null, 25, TestParams::valid);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.items()).isEmpty();
    assertThat(result.warnings()).containsExactly("No cursor widgets found.");
  }

  private static class TestCursorTool extends CursorPaginatedTool<TestParams, String> {
    private DoExecuteHandler handler;

    void setDoExecuteHandler(DoExecuteHandler handler) {
      this.handler = handler;
    }

    @Override
    protected CursorExecutionResult<String> doExecute(
        CursorPaginationParams pagination, TestParams params, WarningCollector collector)
        throws Exception {
      if (handler != null) {
        return handler.execute(pagination, params, collector);
      }
      return CursorExecutionResult.empty();
    }

    @FunctionalInterface
    interface DoExecuteHandler {
      CursorExecutionResult<String> execute(
          CursorPaginationParams pagination, TestParams params, WarningCollector collector)
          throws Exception;
    }
  }

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
