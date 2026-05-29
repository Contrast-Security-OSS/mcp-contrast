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
package com.contrast.labs.ai.mcp.contrast.tool.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.contrast.labs.ai.mcp.contrast.client.ContrastApiClient;
import com.contrastsecurity.exceptions.HttpResponseException;
import com.contrastsecurity.models.MetadataFilterResponse;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;

class GetSessionMetadataToolTest {

  private static final String TEST_APP_ID = "app-123";
  private static final String SECRET_BODY = "bearer raw-token-value apiKey=secret";

  private ContrastApiClient contrastApiClient;
  private GetSessionMetadataTool tool;

  @BeforeEach
  void setUp() {
    contrastApiClient = mock();
    tool = new GetSessionMetadataTool(contrastApiClient);
  }

  @Test
  void getSessionMetadata_should_return_validation_error_for_missing_app_id() {
    var result = tool.getSessionMetadata(null, null);

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors()).containsExactly("appId is required");
    verifyNoInteractions(contrastApiClient);
  }

  @Test
  void getSessionMetadata_should_return_data_and_forward_tool_context() throws Exception {
    MetadataFilterResponse response = new MetadataFilterResponse();
    var toolContext = new ToolContext(Map.of("requestId", "req-123"));
    var capturedContext = new AtomicReference<ToolContext>();
    tool.setAuthenticationStrategy(
        context -> {
          capturedContext.set(context);
          return () -> {};
        });
    when(contrastApiClient.getSessionMetadata(TEST_APP_ID)).thenReturn(response);

    var result = tool.getSessionMetadata(TEST_APP_ID, toolContext);
    Method method =
        GetSessionMetadataTool.class.getDeclaredMethod(
            "getSessionMetadata", String.class, ToolContext.class);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.found()).isTrue();
    assertThat(result.data()).isSameAs(response);
    assertThat(result.errors()).isEmpty();
    assertThat(capturedContext.get()).isSameAs(toolContext);
    assertThat(method.getAnnotation(Tool.class).name()).isEqualTo("get_session_metadata");
    assertThat(method.getParameterTypes()).containsExactly(String.class, ToolContext.class);
    verify(contrastApiClient).getSessionMetadata(TEST_APP_ID);
  }

  @Test
  void getSessionMetadata_should_warn_when_client_returns_null() throws Exception {
    when(contrastApiClient.getSessionMetadata(TEST_APP_ID)).thenReturn(null);

    var result = tool.getSessionMetadata(TEST_APP_ID, null);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.found()).isFalse();
    assertThat(result.data()).isNull();
    assertThat(result.warnings()).contains("No session metadata found for this application.");
  }

  @Test
  void getSessionMetadata_should_map_downstream_403_without_exposing_response_body()
      throws Exception {
    when(contrastApiClient.getSessionMetadata(TEST_APP_ID))
        .thenThrow(
            new HttpResponseException(
                "Forbidden", "GET", "/ng/org/apps/app-123", 403, "Forbidden", SECRET_BODY));

    var result = tool.getSessionMetadata(TEST_APP_ID, null);

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors())
        .containsExactly(
            "Access denied or resource not found. Verify credentials and that the resource ID is"
                + " correct.");
    assertThat(result.toString()).doesNotContain(SECRET_BODY, "/ng/org/apps");
  }

  @Test
  void getSessionMetadata_should_map_downstream_429_without_exposing_response_body()
      throws Exception {
    when(contrastApiClient.getSessionMetadata(TEST_APP_ID))
        .thenThrow(
            new HttpResponseException(
                "Rate limited",
                "GET",
                "/ng/org/apps/app-123",
                429,
                "Too Many Requests",
                SECRET_BODY));

    var result = tool.getSessionMetadata(TEST_APP_ID, null);

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors()).containsExactly("Rate limit exceeded. Retry after a brief pause.");
    assertThat(result.toString()).doesNotContain(SECRET_BODY, "/ng/org/apps");
  }
}
