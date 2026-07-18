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
package com.contrast.labs.ai.mcp.contrast.tool.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.contrast.labs.ai.mcp.contrast.client.ContrastApiClient;
import com.contrast.labs.ai.mcp.contrast.result.ServerSummary;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.server.ServerDetail;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.server.ServerFilterBody;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.server.ServersResponse;
import com.contrast.labs.ai.mcp.contrast.tool.base.PaginatedToolResponse;
import com.contrastsecurity.exceptions.HttpResponseException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;

class SearchServersToolTest {

  private static final String SECRET_BODY = "token=raw-token-value&apiKey=secret";

  private ContrastApiClient contrastApiClient;
  private SearchServersTool tool;

  @BeforeEach
  void setUp() {
    contrastApiClient = mock();
    tool = new SearchServersTool(contrastApiClient);
  }

  @Test
  void searchServers_should_route_filters_pagination_sort_and_expand_through_client()
      throws Exception {
    var server = server(42L, "prod-1");
    server.setApplicationCount(1L);
    var application = new ServerDetail.ServerApplicationDetail();
    application.setAppId("app-a");
    application.setName("checkout");
    application.setLanguage("JAVA");
    server.setApplications(List.of(application));
    when(contrastApiClient.searchServers(any(), eq(25), eq(50), eq("-version"), eq(true)))
        .thenReturn(response(3L, server));

    var result =
        tool.searchServers(
            3,
            25,
            "prod",
            "production",
            "online",
            "info",
            "blue",
            "app-a",
            false,
            "5.0.0",
            true,
            "agentVersion,DESC");

    var filterCaptor = ArgumentCaptor.forClass(ServerFilterBody.class);
    verify(contrastApiClient)
        .searchServers(filterCaptor.capture(), eq(25), eq(50), eq("-version"), eq(true));
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.page()).isEqualTo(3);
    assertThat(result.pageSize()).isEqualTo(25);
    assertThat(result.totalItems()).isEqualTo(3);
    assertThat(result.items())
        .singleElement()
        .satisfies(
            item -> {
              assertThat(item.serverId()).isEqualTo(42L);
              assertThat(item.name()).isEqualTo("prod-1");
              assertThat(item.applications())
                  .extracting(applicationSummary -> applicationSummary.appId())
                  .containsExactly("app-a");
            });
    assertThat(filterCaptor.getValue().getQ()).isEqualTo("prod");
    assertThat(filterCaptor.getValue().getServerEnvironments()).containsExactly("PRODUCTION");
    assertThat(filterCaptor.getValue().getQuickFilter()).isEqualTo("ONLINE");
    assertThat(filterCaptor.getValue().getLogLevels()).containsExactly("INFO");
    assertThat(filterCaptor.getValue().getTags()).containsExactly("blue");
    assertThat(filterCaptor.getValue().getApplicationsIds()).containsExactly("app-a");
    assertThat(filterCaptor.getValue().getAgentVersions()).containsExactly("5.0.0");
  }

  @Test
  void searchServers_should_forward_tool_context_and_preserve_tool_name() throws Exception {
    var toolContext = new ToolContext(Map.of("requestId", "req-123"));
    var capturedContext = new AtomicReference<ToolContext>();
    tool.setAuthenticationStrategy(
        context -> {
          capturedContext.set(context);
          return () -> {};
        });
    when(contrastApiClient.searchServers(any(), eq(10), eq(0), eq("-lastActivity"), eq(false)))
        .thenReturn(response(1L, server(1L, "server-1")));

    var result =
        tool.searchServers(
            1, 10, null, null, null, null, null, null, null, null, null, null, toolContext);
    Method method =
        SearchServersTool.class.getDeclaredMethod(
            "searchServers",
            Integer.class,
            Integer.class,
            String.class,
            String.class,
            String.class,
            String.class,
            String.class,
            String.class,
            Boolean.class,
            String.class,
            Boolean.class,
            String.class,
            ToolContext.class);

    assertThat(result.isSuccess()).isTrue();
    assertThat(capturedContext.get()).isSameAs(toolContext);
    assertThat(method.getAnnotation(Tool.class).name()).isEqualTo("search_servers");
  }

  @Test
  void searchServers_should_reject_mutually_exclusive_filters_without_calling_client() {
    var result =
        tool.searchServers(1, 10, null, null, null, null, null, "app-a", true, null, null, null);

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors())
        .containsExactly(
            "applicationIds and withoutApplications are mutually exclusive: choose application"
                + " IDs or servers without applications, not both");
    verifyNoInteractions(contrastApiClient);
  }

  @Test
  void searchServers_should_cap_page_size_and_warn() throws Exception {
    when(contrastApiClient.searchServers(any(), eq(100), eq(0), anyString(), eq(false)))
        .thenReturn(response(1L, server(1L, "server-1")));

    var result = allServers(1, 250);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.pageSize()).isEqualTo(100);
    assertThat(result.warnings())
        .contains("Requested pageSize 250 exceeds maximum 100, capped to 100");
    verify(contrastApiClient, never())
        .searchServers(any(), eq(250), anyInt(), anyString(), anyBoolean());
  }

  @Test
  void searchServers_should_return_standard_warning_for_valid_empty_tag_filter_result()
      throws Exception {
    var response = response(0L);
    when(contrastApiClient.searchServers(any(), eq(50), eq(0), anyString(), eq(false)))
        .thenReturn(response);

    var result =
        tool.searchServers(
            1, 50, null, null, null, null, "missing-tag", null, null, null, null, null);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.items()).isEmpty();
    assertThat(result.totalItems()).isZero();
    assertThat(result.errors()).isEmpty();
    assertThat(result.warnings())
        .containsExactly("No results found matching the specified criteria.");
  }

  @Test
  void searchServers_should_clamp_oversized_total_to_integer_maximum() throws Exception {
    when(contrastApiClient.searchServers(any(), eq(50), eq(0), anyString(), eq(false)))
        .thenReturn(response(Long.MAX_VALUE, server(1L, "server-1")));

    var result = allServers(1, null);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.totalItems()).isEqualTo(Integer.MAX_VALUE);
  }

  @Test
  void searchServers_should_return_standard_warning_for_valid_empty_result() throws Exception {
    when(contrastApiClient.searchServers(any(), eq(50), eq(0), anyString(), eq(false)))
        .thenReturn(response(0L));

    var result = allServers(1, null);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.items()).isEmpty();
    assertThat(result.totalItems()).isZero();
    assertThat(result.warnings())
        .containsExactly("No results found matching the specified criteria.");
  }

  @ParameterizedTest
  @MethodSource("httpErrors")
  void searchServers_should_sanitize_http_errors_without_exposing_response_body(
      int status, String expectedMessage) throws Exception {
    when(contrastApiClient.searchServers(any(), eq(10), eq(0), anyString(), eq(false)))
        .thenThrow(apiFailure(status));

    var result = allServers(1, 10);

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors()).containsExactly(expectedMessage);
    assertThat(result.toString()).doesNotContain(SECRET_BODY, "/ng/org/servers/filter");
  }

  private static Stream<Arguments> httpErrors() {
    return Stream.of(
        Arguments.of(400, "API error (HTTP 400)"),
        Arguments.of(
            401,
            "Authentication failed or resource not found. Verify credentials and that the"
                + " resource ID is correct."),
        Arguments.of(
            403,
            "Access denied or resource not found. Verify credentials and that the resource ID is"
                + " correct."),
        Arguments.of(404, "Resource not found."),
        Arguments.of(429, "Rate limit exceeded. Retry after a brief pause."),
        Arguments.of(
            500, "The service returned an error. Narrow filters or reduce page size, then retry."),
        Arguments.of(
            503, "The service returned an error. Narrow filters or reduce page size, then retry."));
  }

  private PaginatedToolResponse<ServerSummary> allServers(Integer page, Integer pageSize) {
    return tool.searchServers(
        page, pageSize, null, null, null, null, null, null, null, null, null, null);
  }

  private static ServersResponse response(long count, ServerDetail... servers) {
    var response = new ServersResponse();
    response.setSuccess(true);
    response.setCount(count);
    response.setServers(List.of(servers));
    return response;
  }

  private static ServerDetail server(long serverId, String name) {
    var server = new ServerDetail();
    server.setServerId(serverId);
    server.setName(name);
    server.setLatestAgentVersion("5.1.0");
    server.setTags(List.of());
    server.setApplicationCount(0L);
    return server;
  }

  private static HttpResponseException apiFailure(int status) {
    return new HttpResponseException(
        "Downstream failure",
        "POST",
        "/ng/org/servers/filter",
        status,
        "Downstream failure",
        SECRET_BODY);
  }
}
