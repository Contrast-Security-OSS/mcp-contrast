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
package com.contrast.labs.ai.mcp.contrast.tool.sast;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.contrast.labs.ai.mcp.contrast.client.ContrastApiClient;
import com.contrast.labs.ai.mcp.contrast.result.ScanProject;
import com.contrastsecurity.exceptions.HttpResponseException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;

/** Unit tests for GetSastProjectTool. */
class GetSastProjectToolTest {

  private static final String TEST_PROJECT_NAME = "test-project";
  private static final String SECRET_BODY = "token=raw-token-value&apiKey=secret";

  private ContrastApiClient contrastApiClient;
  private GetSastProjectTool tool;

  @BeforeEach
  void setUp() {
    contrastApiClient = mock();
    tool = new GetSastProjectTool(contrastApiClient);
  }

  @Test
  void getScanProject_should_route_project_name_through_contrast_api_client() throws Exception {
    when(contrastApiClient.getScanProject(TEST_PROJECT_NAME)).thenReturn(project());

    var result = tool.getScanProject(TEST_PROJECT_NAME, null);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.found()).isTrue();
    assertThat(result.data().name()).isEqualTo(TEST_PROJECT_NAME);
    assertThat(result.data().id()).isEqualTo("project-123");
    assertThat(result.data().language()).isEqualTo("Java");
    verify(contrastApiClient).getScanProject(TEST_PROJECT_NAME);
  }

  @Test
  void getScanProject_should_return_project_details() throws Exception {
    when(contrastApiClient.getScanProject(TEST_PROJECT_NAME)).thenReturn(project());

    var result = tool.getScanProject(TEST_PROJECT_NAME, null);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.data().critical()).isEqualTo(5);
    assertThat(result.data().high()).isEqualTo(10);
    assertThat(result.data().medium()).isEqualTo(20);
    assertThat(result.data().low()).isEqualTo(30);
    assertThat(result.data().note()).isEqualTo(2);
    assertThat(result.data().completedScans()).isEqualTo(3);
  }

  @Test
  void getScanProject_should_forward_tool_context_and_preserve_tool_name() throws Exception {
    var toolContext = new ToolContext(Map.of("requestId", "req-123"));
    var capturedContext = new AtomicReference<ToolContext>();
    tool.setAuthenticationStrategy(
        context -> {
          capturedContext.set(context);
          return () -> {};
        });
    when(contrastApiClient.getScanProject(TEST_PROJECT_NAME)).thenReturn(project());

    var result = tool.getScanProject(TEST_PROJECT_NAME, toolContext);
    Method method =
        GetSastProjectTool.class.getDeclaredMethod(
            "getScanProject", String.class, ToolContext.class);

    assertThat(result.isSuccess()).isTrue();
    assertThat(capturedContext.get()).isSameAs(toolContext);
    assertThat(method.getAnnotation(Tool.class).name()).isEqualTo("get_scan_project");
    assertThat(method.getAnnotation(Tool.class).description()).doesNotContain("get_scan_results");
  }

  @Test
  void getScanProject_should_return_validation_error_for_null_projectName() {
    var result = tool.getScanProject(null, null);

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors()).containsExactly("projectName is required");
    verifyNoInteractions(contrastApiClient);
  }

  @Test
  void getScanProject_should_return_validation_error_for_empty_projectName() {
    var result = tool.getScanProject("", null);

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors()).containsExactly("projectName is required");
    verifyNoInteractions(contrastApiClient);
  }

  @Test
  void getScanProject_should_return_notFound_when_project_does_not_exist() throws Exception {
    when(contrastApiClient.getScanProject(TEST_PROJECT_NAME)).thenReturn(null);

    var result = tool.getScanProject(TEST_PROJECT_NAME, null);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.found()).isFalse();
    assertThat(result.data()).isNull();
    verify(contrastApiClient).getScanProject(TEST_PROJECT_NAME);
  }

  @Test
  void getScanProject_should_map_downstream_401_without_exposing_response_body() throws Exception {
    when(contrastApiClient.getScanProject(TEST_PROJECT_NAME)).thenThrow(apiFailure(401));

    var result = tool.getScanProject(TEST_PROJECT_NAME, null);

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors())
        .containsExactly(
            "Your authentication token has expired. Please re-authenticate and retry.");
    assertThat(result.toString()).doesNotContain(SECRET_BODY, "/ng/org/scan");
  }

  @Test
  void getScanProject_should_map_downstream_403_without_exposing_response_body() throws Exception {
    when(contrastApiClient.getScanProject(TEST_PROJECT_NAME)).thenThrow(apiFailure(403));

    var result = tool.getScanProject(TEST_PROJECT_NAME, null);

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors())
        .containsExactly("Access denied. User lacks permission for this resource.");
    assertThat(result.toString()).doesNotContain(SECRET_BODY, "/ng/org/scan");
  }

  @Test
  void getScanProject_should_map_downstream_404_without_exposing_response_body() throws Exception {
    when(contrastApiClient.getScanProject(TEST_PROJECT_NAME)).thenThrow(apiFailure(404));

    var result = tool.getScanProject(TEST_PROJECT_NAME, null);

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors()).containsExactly("Resource not found.");
    assertThat(result.toString()).doesNotContain(SECRET_BODY, "/ng/org/scan");
  }

  @Test
  void getScanProject_should_map_downstream_429_without_exposing_response_body() throws Exception {
    when(contrastApiClient.getScanProject(TEST_PROJECT_NAME)).thenThrow(apiFailure(429));

    var result = tool.getScanProject(TEST_PROJECT_NAME, null);

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors()).containsExactly("Rate limit exceeded. Retry after a brief pause.");
    assertThat(result.toString()).doesNotContain(SECRET_BODY, "/ng/org/scan");
  }

  @Test
  void getScanProject_should_map_downstream_5xx_without_exposing_response_body() throws Exception {
    when(contrastApiClient.getScanProject(TEST_PROJECT_NAME)).thenThrow(apiFailure(503));

    var result = tool.getScanProject(TEST_PROJECT_NAME, null);

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors())
        .containsExactly(
            "The service returned an error. Narrow filters or reduce page size, then retry.");
    assertThat(result.toString()).doesNotContain(SECRET_BODY, "/ng/org/scan");
  }

  @Test
  void getScanProject_should_handle_parsing_failure_without_exposing_stack_trace()
      throws Exception {
    when(contrastApiClient.getScanProject(TEST_PROJECT_NAME))
        .thenThrow(new IllegalStateException("Malformed Scan project response"));

    var result = tool.getScanProject(TEST_PROJECT_NAME, null);

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors()).singleElement().asString().startsWith("An internal error occurred");
    assertThat(result.toString()).doesNotContain("IllegalStateException", "Malformed");
  }

  private static ScanProject project() {
    return new ScanProject(
        "project-123",
        TEST_PROJECT_NAME,
        "org-456",
        false,
        "Java",
        5,
        10,
        20,
        30,
        2,
        "scan-789",
        Instant.parse("2025-01-01T12:00:00Z"),
        3,
        List.of("com.example"),
        List.of("com.example.test"));
  }

  private static HttpResponseException apiFailure(int status) {
    return new HttpResponseException(
        "Downstream failure", "GET", "/ng/org/scan", status, "Downstream failure", SECRET_BODY);
  }
}
