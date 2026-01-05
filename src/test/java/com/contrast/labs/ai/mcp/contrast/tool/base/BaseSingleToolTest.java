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
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BaseSingleToolTest {

  private TestGetTool tool;

  @BeforeEach
  void setUp() {
    tool = new TestGetTool();
  }

  @Test
  void executePipeline_should_call_doExecute_with_validated_params() {
    var capturedParams = new AtomicReference<TestParams>();

    tool.setDoExecuteHandler(
        (params, warnings) -> {
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
        (params, warnings) -> {
          throw new RuntimeException("Should not be called");
        });

    var result = tool.executePipeline(() -> TestParams.invalid("Field is required"));

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors()).containsExactly("Field is required");
    assertThat(result.data()).isNull();
  }

  @Test
  void executePipeline_should_return_not_found_when_doExecute_returns_null() {
    tool.setDoExecuteHandler((params, warnings) -> null);

    var result = tool.executePipeline(() -> TestParams.valid());

    assertThat(result.isSuccess()).isTrue(); // Not found is not an error
    assertThat(result.found()).isFalse();
    assertThat(result.data()).isNull();
    assertThat(result.warnings()).contains("Resource not found");
  }

  @Test
  void executePipeline_should_return_not_found_for_resource_not_found_exception() {
    tool.setDoExecuteHandler(
        (params, warnings) -> {
          throw new ResourceNotFoundException(
              "Vuln not found", "GET", "/api/vulns/123", "Not Found");
        });

    var result = tool.executePipeline(() -> TestParams.valid());

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.found()).isFalse();
    assertThat(result.data()).isNull();
    assertThat(result.warnings()).anyMatch(w -> w.contains("not found"));
  }

  @Test
  void executePipeline_should_handle_unauthorized_exception() {
    tool.setDoExecuteHandler(
        (params, warnings) -> {
          throw new UnauthorizedException(
              "Invalid credentials", "GET", "/api/test", 401, "Unauthorized");
        });

    var result = tool.executePipeline(() -> TestParams.valid());

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors()).containsExactly("Authentication failed. Check API credentials.");
  }

  @Test
  void executePipeline_should_handle_http_response_exception_403() {
    tool.setDoExecuteHandler(
        (params, warnings) -> {
          throw new UnauthorizedException("Forbidden", "GET", "/api/test", 403, "Forbidden");
        });

    var result = tool.executePipeline(() -> TestParams.valid());

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors()).containsExactly("Authentication failed. Check API credentials.");
  }

  @Test
  void executePipeline_should_handle_http_response_exception_429() {
    tool.setDoExecuteHandler(
        (params, warnings) -> {
          throw new HttpResponseException(
              "Rate limited", "GET", "/api/test", 429, "Too Many Requests");
        });

    var result = tool.executePipeline(() -> TestParams.valid());

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors()).containsExactly("Rate limit exceeded. Retry later.");
  }

  @Test
  void executePipeline_should_handle_http_response_exception_500() {
    tool.setDoExecuteHandler(
        (params, warnings) -> {
          throw new HttpResponseException(
              "Server error", "GET", "/api/test", 500, "Internal Server Error");
        });

    var result = tool.executePipeline(() -> TestParams.valid());

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors()).containsExactly("Contrast API error. Try again later.");
  }

  @Test
  void executePipeline_should_handle_generic_exception() {
    tool.setDoExecuteHandler(
        (params, warnings) -> {
          throw new RuntimeException("Unexpected failure");
        });

    var result = tool.executePipeline(() -> TestParams.valid());

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors()).containsExactly("Internal error: Unexpected failure");
  }

  @Test
  void executePipeline_should_include_params_warnings() {
    tool.setDoExecuteHandler((params, warnings) -> "result");

    var result = tool.executePipeline(() -> TestParams.withWarning("Deprecated parameter"));

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.warnings()).contains("Deprecated parameter");
  }

  @Test
  void executePipeline_should_allow_doExecute_to_add_warnings() {
    tool.setDoExecuteHandler(
        (params, warnings) -> {
          warnings.add("Partial data returned");
          return "result";
        });

    var result = tool.executePipeline(() -> TestParams.valid());

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.warnings()).contains("Partial data returned");
  }

  @Test
  void executePipeline_should_preserve_warnings_when_unauthorized_exception_occurs() {
    tool.setDoExecuteHandler(
        (params, warnings) -> {
          warnings.add("Warning added before exception");
          throw new UnauthorizedException(
              "Invalid credentials", "GET", "/api/test", 401, "Unauthorized");
        });

    var result = tool.executePipeline(() -> TestParams.withWarning("Initial warning"));

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors()).containsExactly("Authentication failed. Check API credentials.");
    assertThat(result.warnings())
        .containsExactlyInAnyOrder(
            "Initial warning",
            "Warning added before exception",
            "Authentication failed. Check API credentials.");
  }

  // Test implementation of BaseSingleTool
  private static class TestGetTool extends BaseSingleTool<TestParams, String> {
    private DoExecuteHandler handler;

    void setDoExecuteHandler(DoExecuteHandler handler) {
      this.handler = handler;
    }

    @Override
    protected String doExecute(TestParams params, List<String> warnings) throws Exception {
      if (handler != null) {
        return handler.execute(params, warnings);
      }
      return null;
    }

    @FunctionalInterface
    interface DoExecuteHandler {
      String execute(TestParams params, List<String> warnings) throws Exception;
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
