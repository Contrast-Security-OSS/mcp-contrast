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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.contrast.labs.ai.mcp.contrast.config.ContrastSDKFactory;
import com.contrast.labs.ai.mcp.contrast.sdkextension.SDKExtension;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.application.AppMetadataField;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.application.Application;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.application.ApplicationsResponse;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.application.Metadata;
import com.contrastsecurity.sdk.ContrastSDK;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.springframework.test.util.ReflectionTestUtils;

class SearchApplicationsToolTest {

  private SearchApplicationsTool tool;
  private ContrastSDKFactory sdkFactory;
  private ContrastSDK sdk;

  @BeforeEach
  void setUp() {
    sdk = mock();
    sdkFactory = mock();

    when(sdkFactory.getSDK()).thenReturn(sdk);
    when(sdkFactory.getOrgId()).thenReturn("test-org-id");

    tool = new SearchApplicationsTool();
    ReflectionTestUtils.setField(tool, "sdkFactory", sdkFactory);
  }

  @Test
  void searchApplications_should_return_validation_error_for_invalid_json_metadata() {
    var result = tool.searchApplications(1, 10, null, null, "{invalid json}");

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors()).anyMatch(e -> e.contains("Invalid JSON"));
    verifyNoInteractions(sdk);
  }

  @Test
  void searchApplications_should_return_all_applications_when_no_filters() throws Exception {
    var app1 = createApp("App1", "Active");
    var app2 = createApp("App2", "Inactive");
    var response = createResponse(List.of(app1, app2), 2);

    try (MockedConstruction<SDKExtension> mocked =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              when(mock.getApplicationsFiltered(
                      anyString(), isNull(), isNull(), isNull(), anyInt(), anyInt()))
                  .thenReturn(response);
            })) {

      var result = tool.searchApplications(1, 10, null, null, null);

      assertThat(result.isSuccess()).isTrue();
      assertThat(result.items()).hasSize(2);
      assertThat(result.totalItems()).isEqualTo(2);
    }
  }

  @Test
  void searchApplications_should_pass_name_filter_to_server() throws Exception {
    var app1 = createApp("MyProductionApp", "Active");
    var response = createResponse(List.of(app1), 1);

    try (MockedConstruction<SDKExtension> mocked =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              when(mock.getApplicationsFiltered(
                      anyString(), eq("prod"), isNull(), isNull(), anyInt(), anyInt()))
                  .thenReturn(response);
            })) {

      var result = tool.searchApplications(1, 10, "prod", null, null);

      assertThat(result.isSuccess()).isTrue();
      assertThat(result.items()).hasSize(1);

      var mockExtension = mocked.constructed().get(0);
      verify(mockExtension)
          .getApplicationsFiltered(anyString(), eq("prod"), isNull(), isNull(), anyInt(), anyInt());
    }
  }

  @Test
  void searchApplications_should_pass_tag_filter_to_server() throws Exception {
    var app1 = createAppWithTags("App1", List.of("Production", "Critical"));
    var response = createResponse(List.of(app1), 1);

    try (MockedConstruction<SDKExtension> mocked =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              when(mock.getApplicationsFiltered(
                      anyString(), isNull(), any(String[].class), isNull(), anyInt(), anyInt()))
                  .thenReturn(response);
            })) {

      var result = tool.searchApplications(1, 10, null, "Production", null);

      assertThat(result.isSuccess()).isTrue();
      assertThat(result.items()).hasSize(1);
      assertThat(result.items().get(0).name()).isEqualTo("App1");
    }
  }

  @Test
  void searchApplications_should_resolve_metadata_filters_and_pass_to_server() throws Exception {
    var app1 = createAppWithMetadata("App1", "environment", "production");
    var response = createResponse(List.of(app1), 1);
    var metadataField = new AppMetadataField();
    metadataField.setFieldId(123L);
    metadataField.setDisplayLabel("environment");

    try (MockedConstruction<SDKExtension> mocked =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              when(mock.getApplicationMetadataFields(anyString()))
                  .thenReturn(List.of(metadataField));
              when(mock.getApplicationsFiltered(
                      anyString(), isNull(), isNull(), anyList(), anyInt(), anyInt()))
                  .thenReturn(response);
            })) {

      var result = tool.searchApplications(1, 10, null, null, "{\"environment\":\"production\"}");

      assertThat(result.isSuccess()).isTrue();
      assertThat(result.items()).hasSize(1);
      assertThat(result.items().get(0).name()).isEqualTo("App1");

      var mockExtension = mocked.constructed().get(0);
      verify(mockExtension).getApplicationMetadataFields(anyString());
      verify(mockExtension)
          .getApplicationsFiltered(anyString(), isNull(), isNull(), anyList(), anyInt(), anyInt());
    }
  }

  @Test
  void searchApplications_should_return_error_for_unknown_metadata_field() throws Exception {
    var metadataField = new AppMetadataField();
    metadataField.setFieldId(123L);
    metadataField.setDisplayLabel("environment");

    try (MockedConstruction<SDKExtension> mocked =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              when(mock.getApplicationMetadataFields(anyString()))
                  .thenReturn(List.of(metadataField));
            })) {

      var result = tool.searchApplications(1, 10, null, null, "{\"unknownField\":\"value\"}");

      assertThat(result.isSuccess()).isFalse();
      assertThat(result.errors())
          .anyMatch(e -> e.contains("unknownField") && e.contains("not found"));

      var mockExtension = mocked.constructed().get(0);
      verify(mockExtension).getApplicationMetadataFields(anyString());
      verify(mockExtension, never())
          .getApplicationsFiltered(anyString(), any(), any(), anyList(), anyInt(), anyInt());
    }
  }

  @Test
  void searchApplications_should_return_empty_when_no_match() throws Exception {
    var response = createResponse(List.of(), 0);

    try (MockedConstruction<SDKExtension> mocked =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              when(mock.getApplicationsFiltered(
                      anyString(), eq("nonexistent"), isNull(), isNull(), anyInt(), anyInt()))
                  .thenReturn(response);
            })) {

      var result = tool.searchApplications(1, 10, "nonexistent", null, null);

      assertThat(result.isSuccess()).isTrue();
      assertThat(result.items()).isEmpty();
      assertThat(result.totalItems()).isEqualTo(0);
      assertThat(result.warnings()).anyMatch(w -> w.contains("No results found"));
    }
  }

  @Test
  void searchApplications_should_use_server_pagination() throws Exception {
    var app1 = createApp("App3", "Active");
    var app2 = createApp("App4", "Active");
    var response = createResponse(List.of(app1, app2), 5);

    try (MockedConstruction<SDKExtension> mocked =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              when(mock.getApplicationsFiltered(
                      anyString(), isNull(), isNull(), isNull(), eq(2), eq(2)))
                  .thenReturn(response);
            })) {

      // Request page 2 with page size 2 (offset should be 2)
      var result = tool.searchApplications(2, 2, null, null, null);

      assertThat(result.isSuccess()).isTrue();
      assertThat(result.items()).hasSize(2);
      assertThat(result.totalItems()).isEqualTo(5);
      assertThat(result.hasMorePages()).isTrue();

      var mockExtension = mocked.constructed().get(0);
      verify(mockExtension)
          .getApplicationsFiltered(anyString(), isNull(), isNull(), isNull(), eq(2), eq(2));
    }
  }

  @Test
  void searchApplications_should_include_metadata_in_response() throws Exception {
    var app = createAppWithMetadata("App1", "environment", "production");
    var response = createResponse(List.of(app), 1);

    try (MockedConstruction<SDKExtension> mocked =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              when(mock.getApplicationsFiltered(
                      anyString(), isNull(), isNull(), isNull(), anyInt(), anyInt()))
                  .thenReturn(response);
            })) {

      var result = tool.searchApplications(1, 10, null, null, null);

      assertThat(result.isSuccess()).isTrue();
      assertThat(result.items()).hasSize(1);
      assertThat(result.items().get(0).metadata()).isNotEmpty();
      assertThat(result.items().get(0).metadata().get(0).name()).isEqualTo("environment");
      assertThat(result.items().get(0).metadata().get(0).value()).isEqualTo("production");
    }
  }

  @Test
  void searchApplications_should_handle_null_response() throws Exception {
    try (MockedConstruction<SDKExtension> mocked =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              when(mock.getApplicationsFiltered(
                      anyString(), isNull(), isNull(), isNull(), anyInt(), anyInt()))
                  .thenReturn(null);
            })) {

      var result = tool.searchApplications(1, 10, null, null, null);

      assertThat(result.isSuccess()).isTrue();
      assertThat(result.items()).isEmpty();
      assertThat(result.warnings()).anyMatch(w -> w.contains("API returned no application data"));
    }
  }

  // -- Helper methods --

  private Application createApp(String name, String status) {
    var app = new Application();
    app.setName(name);
    app.setStatus(status);
    app.setAppId("app-" + name.toLowerCase());
    app.setLanguage("Java");
    app.setLastSeen(System.currentTimeMillis());
    return app;
  }

  private Application createAppWithTags(String name, List<String> tags) {
    var app = createApp(name, "Active");
    app.setTags(tags);
    return app;
  }

  private Application createAppWithMetadata(String name, String metaName, String metaValue) {
    var app = createApp(name, "Active");
    var metadata = new Metadata();
    metadata.setName(metaName);
    metadata.setValue(metaValue);
    app.setMetadataEntities(List.of(metadata));
    return app;
  }

  private ApplicationsResponse createResponse(List<Application> apps, int count) {
    var response = new ApplicationsResponse();
    response.setApplications(apps);
    response.setCount(count);
    return response;
  }
}
