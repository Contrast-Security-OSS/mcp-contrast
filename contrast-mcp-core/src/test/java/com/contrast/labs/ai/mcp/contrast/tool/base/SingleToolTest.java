/*
 * Copyright 2026 Contrast Security
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
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SingleToolTest {
  private static final String AUTH_OR_NOT_FOUND_MESSAGE =
      "Authentication failed or resource not found. Verify credentials and that the resource ID"
          + " is correct.";

  private TestGetTool tool;

  @BeforeEach
  void setUp() {
    tool = new TestGetTool();
  }

  @Test
  void executePipeline_should_call_doExecute_with_validated_params() {
    var capturedParams = new AtomicReference<TestParams>();

    tool.setDoExecuteHandler(
        (params, notices) -> {
          capturedParams.set(params);
          return "result";
        });

    var result = tool.executePipeline(() -> TestParams.valid());

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.found()).isTrue();
    assertThat(result.data()).isEqualTo("result");
    assertThat(capturedParams.get()).isNotNull();
  }

  @Test
  void executePipeline_should_return_validation_error_when_params_invalid() {
    tool.setDoExecuteHandler(
        (params, notices) -> {
          throw new RuntimeException("Should not be called");
        });

    var result = tool.executePipeline(() -> TestParams.invalid("Field is required"));

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors()).containsExactly("Field is required");
    assertThat(result.data()).isNull();
  }

  @Test
  void executePipeline_should_return_not_found_when_doExecute_returns_null() {
    tool.setDoExecuteHandler((params, notices) -> null);

    var result = tool.executePipeline(() -> TestParams.valid());

    assertThat(result.isSuccess()).isTrue(); // Not found is not an error
    assertThat(result.found()).isFalse();
    assertThat(result.data()).isNull();
    assertThat(result.notices()).contains("Resource not found");
  }

  @Test
  void executePipeline_should_return_not_found_for_resource_not_found_exception() {
    tool.setDoExecuteHandler(
        (params, notices) -> {
          throw new ResourceNotFoundException(
              "Vuln not found", "GET", "/api/vulns/123", "Not Found");
        });

    var result = tool.executePipeline(() -> TestParams.valid());

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.found()).isFalse();
    assertThat(result.data()).isNull();
    assertThat(result.notices()).anyMatch(w -> w.contains("not found"));
  }

  @Test
  void executePipeline_should_handle_unauthorized_exception() {
    tool.setDoExecuteHandler(
        (params, notices) -> {
          throw new UnauthorizedException(
              "Invalid credentials", "GET", "/api/test", 401, "Unauthorized");
        });

    var result = tool.executePipeline(() -> TestParams.valid());

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors()).containsExactly(AUTH_OR_NOT_FOUND_MESSAGE);
  }

  @Test
  void executePipeline_should_handle_unauthorized_exception_403() {
    tool.setDoExecuteHandler(
        (params, notices) -> {
          throw new UnauthorizedException("Forbidden", "GET", "/api/test", 403, "Forbidden");
        });

    var result = tool.executePipeline(() -> TestParams.valid());

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors())
        .containsExactly(
            "Access denied or resource not found. Verify credentials and that the resource ID is"
                + " correct.");
  }

  @Test
  void executePipeline_should_handle_http_response_exception_429() {
    tool.setDoExecuteHandler(
        (params, notices) -> {
          throw new HttpResponseException(
              "Rate limited", "GET", "/api/test", 429, "Too Many Requests");
        });

    var result = tool.executePipeline(() -> TestParams.valid());

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors()).containsExactly("Rate limit exceeded. Retry after a brief pause.");
  }

  @Test
  void executePipeline_should_handle_http_response_exception_500() {
    tool.setDoExecuteHandler(
        (params, notices) -> {
          throw new HttpResponseException(
              "Server error", "GET", "/api/test", 500, "Internal Server Error");
        });

    var result = tool.executePipeline(() -> TestParams.valid());

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors())
        .containsExactly(
            "The service returned an error. Narrow filters or reduce page size, then retry.");
  }

  @Test
  void executePipeline_should_handle_generic_exception() {
    tool.setDoExecuteHandler(
        (params, notices) -> {
          throw new RuntimeException("Unexpected failure");
        });

    var result = tool.executePipeline(() -> TestParams.valid());

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors())
        .singleElement()
        .satisfies(error -> assertThat(error).startsWith("An internal error occurred (ref: "));
  }

  @Test
  void executePipeline_should_not_expose_exception_message_in_error() {
    tool.setDoExecuteHandler(
        (params, notices) -> {
          throw new RuntimeException("sensitive: /api/ng/org-id/traces");
        });

    var result = tool.executePipeline(() -> TestParams.valid());

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors())
        .singleElement()
        .satisfies(
            error -> {
              assertThat(error).startsWith("An internal error occurred (ref: ");
              assertThat(error).doesNotContain("/api/ng/");
              assertThat(error).doesNotContain("org-id");
              assertThat(error).doesNotContain("traces");
            });
  }

  @Test
  void executePipeline_should_surface_illegalArgumentException_message_as_user_error() {
    var iaeMessage =
        "Session metadata field(s) not found for application 'app-1': nonexistent_field_xyz_12345."
            + " Use get_session_metadata(appId) to discover available field names.";
    tool.setDoExecuteHandler(
        (params, collector) -> {
          throw new IllegalArgumentException(iaeMessage);
        });

    var result = tool.executePipeline(() -> TestParams.valid());

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors())
        .as("IllegalArgumentException message must surface verbatim as the user-facing error")
        .singleElement()
        .isEqualTo(iaeMessage);
    assertThat(result.errors())
        .as("IllegalArgumentException must not be masked as a generic internal error")
        .noneMatch(e -> e.contains("An internal error occurred"));
  }

  @Test
  void executePipeline_should_surface_actionableToolErrorException_message_as_user_error() {
    var actionableMessage = "Fix this condition before retrying.";
    tool.setDoExecuteHandler(
        (params, collector) -> {
          throw new ActionableToolErrorException(actionableMessage);
        });

    var result = tool.executePipeline(() -> TestParams.valid());

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors())
        .as("ActionableToolErrorException message must surface verbatim")
        .containsExactly(actionableMessage);
    assertThat(result.data()).isNull();
  }

  @Test
  void executePipeline_should_include_params_notices() {
    tool.setDoExecuteHandler((params, notices) -> "result");

    var result = tool.executePipeline(() -> TestParams.withNotice("Deprecated parameter"));

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.notices()).contains("Deprecated parameter");
  }

  @Test
  void executePipeline_should_allow_doExecute_to_add_notices() {
    tool.setDoExecuteHandler(
        (params, collector) -> {
          collector.notice("Partial data returned");
          return "result";
        });

    var result = tool.executePipeline(() -> TestParams.valid());

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.notices()).contains("Partial data returned");
  }

  @Test
  void executePipeline_should_preserve_notices_when_unauthorized_exception_occurs() {
    tool.setDoExecuteHandler(
        (params, collector) -> {
          collector.notice("Notice added before exception");
          throw new UnauthorizedException(
              "Invalid credentials", "GET", "/api/test", 401, "Unauthorized");
        });

    var result = tool.executePipeline(() -> TestParams.withNotice("Initial notice"));

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors()).containsExactly(AUTH_OR_NOT_FOUND_MESSAGE);
    assertThat(result.notices())
        .containsExactlyInAnyOrder("Initial notice", "Notice added before exception");
  }

  @Test
  void executePipeline_should_preserve_notices_when_http_response_exception_occurs() {
    tool.setDoExecuteHandler(
        (params, collector) -> {
          collector.notice("Notice added before exception");
          throw new HttpResponseException(
              "Rate limited", "GET", "/api/test", 429, "Too Many Requests");
        });

    var result = tool.executePipeline(() -> TestParams.withNotice("Initial notice"));

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors()).containsExactly("Rate limit exceeded. Retry after a brief pause.");
    assertThat(result.notices())
        .containsExactlyInAnyOrder("Initial notice", "Notice added before exception");
  }

  // Test implementation of SingleTool
  private static class TestGetTool extends SingleTool<TestParams, String> {
    private DoExecuteHandler handler;

    void setDoExecuteHandler(DoExecuteHandler handler) {
      this.handler = handler;
    }

    @Override
    protected String doExecute(TestParams params, NoticeCollector collector) throws Exception {
      if (handler != null) {
        return handler.execute(params, collector);
      }
      return null;
    }

    @FunctionalInterface
    interface DoExecuteHandler {
      String execute(TestParams params, NoticeCollector collector) throws Exception;
    }
  }

  // Test params implementation
  private record TestParams(boolean isValid, List<String> errors, List<String> notices)
      implements ToolParams {

    static TestParams valid() {
      return new TestParams(true, List.of(), List.of());
    }

    static TestParams invalid(String error) {
      return new TestParams(false, List.of(error), List.of());
    }

    static TestParams withNotice(String notice) {
      return new TestParams(true, List.of(), List.of(notice));
    }
  }
}
