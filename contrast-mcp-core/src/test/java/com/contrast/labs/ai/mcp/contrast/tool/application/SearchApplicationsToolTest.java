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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.contrast.labs.ai.mcp.contrast.client.ContrastApiClient;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.application.AppMetadataField;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.application.AppMetadataFilter;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.application.Application;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.application.ApplicationsResponse;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.application.Metadata;
import com.contrastsecurity.exceptions.HttpResponseException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;

class SearchApplicationsToolTest {

  private static final String SECRET_BODY = "token=raw-token-value&apiKey=secret";

  private ContrastApiClient contrastApiClient;
  private SearchApplicationsTool tool;

  @BeforeEach
  void setUp() {
    contrastApiClient = mock();
    tool = new SearchApplicationsTool(contrastApiClient);
  }

  @Test
  void searchApplications_should_return_validation_error_for_invalid_json_metadata() {
    var result = tool.searchApplications(1, 10, null, null, "{invalid json}", null);

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors()).anyMatch(e -> e.contains("Invalid JSON"));
    verifyNoInteractions(contrastApiClient);
  }

  @Test
  void searchApplications_should_route_filters_pagination_and_tool_context_through_client()
      throws Exception {
    var metadataField = metadataField(123L, "environment");
    var response =
        createResponse(List.of(createAppWithMetadata("Orders", "environment", "prod")), 21);
    var toolContext = new ToolContext(Map.of("requestId", "req-123"));
    var capturedContext = new AtomicReference<ToolContext>();
    tool.setAuthenticationStrategy(
        context -> {
          capturedContext.set(context);
          return () -> {};
        });

    when(contrastApiClient.getApplicationMetadataFields()).thenReturn(List.of(metadataField));
    when(contrastApiClient.searchApplications(
            eq("orders"),
            argThat(tags -> tags.length == 1 && "critical".equals(tags[0])),
            argThat(SearchApplicationsToolTest::containsEnvironmentMetadataFilter),
            eq(10),
            eq(10)))
        .thenReturn(response);

    var result =
        tool.searchApplications(
            2, 10, "orders", "critical", "{\"environment\":\"prod\"}", toolContext);
    Method method =
        SearchApplicationsTool.class.getDeclaredMethod(
            "searchApplications",
            Integer.class,
            Integer.class,
            String.class,
            String.class,
            String.class,
            ToolContext.class);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.items()).hasSize(1);
    assertThat(result.items().get(0).name()).isEqualTo("Orders");
    assertThat(result.items().get(0).metadata())
        .singleElement()
        .satisfies(
            metadata -> {
              assertThat(metadata.name()).isEqualTo("environment");
              assertThat(metadata.value()).isEqualTo("prod");
            });
    assertThat(result.totalItems()).isEqualTo(21);
    assertThat(result.hasMorePages()).isTrue();
    assertThat(capturedContext.get()).isSameAs(toolContext);
    assertThat(method.getAnnotation(Tool.class).name()).isEqualTo("search_applications");
    assertThat(method.getParameterTypes())
        .containsExactly(
            Integer.class,
            Integer.class,
            String.class,
            String.class,
            String.class,
            ToolContext.class);
  }

  @Test
  void searchApplications_should_return_error_for_unknown_metadata_field() throws Exception {
    when(contrastApiClient.getApplicationMetadataFields())
        .thenReturn(List.of(metadataField(123L, "environment")));

    var result = tool.searchApplications(1, 10, null, null, "{\"unknownField\":\"value\"}", null);

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors())
        .singleElement()
        .satisfies(
            error -> {
              assertThat(error).contains("Metadata field(s) not found");
              assertThat(error).contains("unknownField");
              assertThat(error).doesNotContain("An internal error occurred", SECRET_BODY);
            });
    verify(contrastApiClient, never())
        .searchApplications(any(), any(), anyList(), anyInt(), anyInt());
  }

  @Test
  void searchApplications_should_warn_for_empty_results_and_cap_page_size() throws Exception {
    when(contrastApiClient.searchApplications(isNull(), isNull(), isNull(), eq(100), eq(0)))
        .thenReturn(createResponse(List.of(), 0));

    var result = tool.searchApplications(1, 1000, null, null, null, null);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.items()).isEmpty();
    assertThat(result.pageSize()).isEqualTo(100);
    assertThat(result.warnings())
        .contains(
            "Requested pageSize 1000 exceeds maximum 100, capped to 100",
            "No results found matching the specified criteria.");
  }

  @Test
  void searchApplications_should_map_downstream_403_without_exposing_response_body()
      throws Exception {
    when(contrastApiClient.searchApplications(isNull(), isNull(), isNull(), eq(10), eq(0)))
        .thenThrow(
            new HttpResponseException(
                "Forbidden", "GET", "/ng/org/apps", 403, "Forbidden", SECRET_BODY));

    var result = tool.searchApplications(1, 10, null, null, null, null);

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors())
        .containsExactly("Access denied. User lacks permission for this resource.");
    assertThat(result.toString()).doesNotContain(SECRET_BODY, "/ng/org/apps");
  }

  @Test
  void searchApplications_should_map_downstream_429_without_exposing_response_body()
      throws Exception {
    when(contrastApiClient.searchApplications(isNull(), isNull(), isNull(), eq(10), eq(0)))
        .thenThrow(
            new HttpResponseException(
                "Rate limited", "GET", "/ng/org/apps", 429, "Too Many Requests", SECRET_BODY));

    var result = tool.searchApplications(1, 10, null, null, null, null);

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors()).containsExactly("Rate limit exceeded. Retry after a brief pause.");
    assertThat(result.toString()).doesNotContain(SECRET_BODY, "/ng/org/apps");
  }

  private static boolean containsEnvironmentMetadataFilter(List<AppMetadataFilter> filters) {
    return filters.size() == 1
        && filters.get(0).getFieldID() == 123L
        && List.of(filters.get(0).getValues()).equals(List.of("prod"));
  }

  private static AppMetadataField metadataField(Long fieldId, String label) {
    var field = new AppMetadataField();
    field.setFieldId(fieldId);
    field.setDisplayLabel(label);
    return field;
  }

  private static Application createAppWithMetadata(String name, String metaName, String metaValue) {
    var app = new Application();
    app.setName(name);
    app.setStatus("Active");
    app.setAppId("app-" + name.toLowerCase());
    app.setLanguage("Java");
    app.setLastSeen(1L);
    var metadata = new Metadata();
    metadata.setName(metaName);
    metadata.setValue(metaValue);
    app.setMetadataEntities(List.of(metadata));
    return app;
  }

  private static ApplicationsResponse createResponse(List<Application> apps, int count) {
    var response = new ApplicationsResponse();
    response.setApplications(apps);
    response.setCount(count);
    return response;
  }
}
