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
package com.contrast.labs.ai.mcp.contrast.tool.library;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.contrast.labs.ai.mcp.contrast.client.ContrastApiClient;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.LibrariesExtended;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.LibraryExtended;
import com.contrastsecurity.exceptions.HttpResponseException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;

class ListApplicationLibrariesToolTest {

  private static final String APP_ID = "app-123";
  private static final String SECRET_BODY = "token=raw-token-value&apiKey=secret";

  private ContrastApiClient contrastApiClient;
  private ListApplicationLibrariesTool tool;

  @BeforeEach
  void setUp() {
    contrastApiClient = mock();
    tool = new ListApplicationLibrariesTool(contrastApiClient);
  }

  @Test
  void listApplicationLibraries_should_route_pagination_through_contrast_api_client()
      throws Exception {
    var library = library("log4j-core-2.17.1.jar", "hash-123");
    var response = new LibrariesExtended();
    response.setLibraries(List.of(library));
    response.setCount(4L);

    when(contrastApiClient.getLibraryPage(eq(APP_ID), eq(2), eq(2))).thenReturn(response);

    var result = tool.listApplicationLibraries(2, 2, APP_ID, null);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.items()).containsExactly(library);
    assertThat(result.totalItems()).isEqualTo(4);
    assertThat(result.hasMorePages()).isTrue();
  }

  @Test
  void listApplicationLibraries_should_forward_tool_context_and_preserve_tool_name()
      throws Exception {
    var response = new LibrariesExtended();
    response.setLibraries(List.of(library("spring-core.jar", "hash-456")));
    response.setCount(1L);
    var toolContext = new ToolContext(Map.of("requestId", "req-123"));
    var capturedContext = new AtomicReference<ToolContext>();
    tool.setAuthenticationStrategy(
        context -> {
          capturedContext.set(context);
          return () -> {};
        });

    when(contrastApiClient.getLibraryPage(eq(APP_ID), eq(10), eq(0))).thenReturn(response);

    var result = tool.listApplicationLibraries(1, 10, APP_ID, toolContext);
    Method method =
        ListApplicationLibrariesTool.class.getDeclaredMethod(
            "listApplicationLibraries",
            Integer.class,
            Integer.class,
            String.class,
            ToolContext.class);

    assertThat(result.isSuccess()).isTrue();
    assertThat(capturedContext.get()).isSameAs(toolContext);
    assertThat(method.getAnnotation(Tool.class).name()).isEqualTo("list_application_libraries");
  }

  @Test
  void listApplicationLibraries_should_return_validation_error_for_missing_app_id() {
    var result = tool.listApplicationLibraries(null, null, null, null);

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors()).anyMatch(e -> e.contains("appId") && e.contains("required"));
    verifyNoInteractions(contrastApiClient);
  }

  @Test
  void listApplicationLibraries_should_warn_for_empty_first_page() throws Exception {
    var response = new LibrariesExtended();
    response.setLibraries(List.of());
    response.setCount(0L);

    when(contrastApiClient.getLibraryPage(eq(APP_ID), eq(50), eq(0))).thenReturn(response);

    var result = tool.listApplicationLibraries(1, null, APP_ID, null);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.items()).isEmpty();
    assertThat(result.totalItems()).isEqualTo(0);
    assertThat(result.warnings())
        .containsExactly(
            "No libraries found for this application. "
                + "The application may not have any third-party dependencies, "
                + "or library data may not have been collected yet.");
  }

  @Test
  void listApplicationLibraries_should_not_warn_for_page_beyond_results() throws Exception {
    var response = new LibrariesExtended();
    response.setLibraries(List.of());
    response.setCount(5L);

    when(contrastApiClient.getLibraryPage(eq(APP_ID), eq(50), eq(450))).thenReturn(response);

    var result = tool.listApplicationLibraries(10, 50, APP_ID, null);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.items()).isEmpty();
    assertThat(result.totalItems()).isEqualTo(5);
    assertThat(result.warnings()).isEmpty();
  }

  @Test
  void listApplicationLibraries_should_cap_page_size_at_50() throws Exception {
    var response = new LibrariesExtended();
    response.setLibraries(List.of(library("library.jar", "hash-789")));
    response.setCount(200L);

    when(contrastApiClient.getLibraryPage(eq(APP_ID), eq(50), eq(0))).thenReturn(response);

    var result = tool.listApplicationLibraries(1, 100, APP_ID, null);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.pageSize()).isEqualTo(50);
    assertThat(result.warnings())
        .contains("Requested pageSize 100 exceeds maximum 50, capped to 50");
    verify(contrastApiClient, never()).getLibraryPage(eq(APP_ID), eq(100), anyInt());
  }

  @Test
  void listApplicationLibraries_should_map_downstream_403_without_exposing_response_body()
      throws Exception {
    when(contrastApiClient.getLibraryPage(eq(APP_ID), eq(10), eq(0)))
        .thenThrow(
            new HttpResponseException(
                "Forbidden",
                "GET",
                "/ng/org/apps/app-123/libraries",
                403,
                "Forbidden",
                SECRET_BODY));

    var result = tool.listApplicationLibraries(1, 10, APP_ID, null);

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors())
        .containsExactly(
            "Access denied or resource not found. Verify credentials and that the resource ID is"
                + " correct.");
    assertThat(result.toString()).doesNotContain(SECRET_BODY, "/ng/org/apps/app-123/libraries");
  }

  @Test
  void listApplicationLibraries_should_map_downstream_429_without_exposing_response_body()
      throws Exception {
    when(contrastApiClient.getLibraryPage(eq(APP_ID), eq(10), eq(0)))
        .thenThrow(
            new HttpResponseException(
                "Rate limited",
                "GET",
                "/ng/org/apps/app-123/libraries",
                429,
                "Too Many Requests",
                SECRET_BODY));

    var result = tool.listApplicationLibraries(1, 10, APP_ID, null);

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors()).containsExactly("Rate limit exceeded. Retry after a brief pause.");
    assertThat(result.toString()).doesNotContain(SECRET_BODY, "/ng/org/apps/app-123/libraries");
  }

  private static LibraryExtended library(String filename, String hash) {
    var library = new LibraryExtended();
    library.setFilename(filename);
    library.setHash(hash);
    return library;
  }
}
