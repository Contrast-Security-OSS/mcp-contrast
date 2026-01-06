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
package com.contrast.labs.ai.mcp.contrast.tool.applications;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.contrast.labs.ai.mcp.contrast.config.ContrastConfig;
import com.contrast.labs.ai.mcp.contrast.sdkextension.SDKHelper;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.application.Application;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.application.Metadata;
import com.contrastsecurity.sdk.ContrastSDK;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

class SearchApplicationsToolTest {

  private SearchApplicationsTool tool;
  private ContrastConfig config;
  private ContrastSDK sdk;

  @BeforeEach
  void setUp() {
    sdk = mock();
    config = mock();

    when(config.createSDK()).thenReturn(sdk);
    when(config.getOrgId()).thenReturn("test-org-id");

    tool = new SearchApplicationsTool();
    ReflectionTestUtils.setField(tool, "config", config);
  }

  @Test
  void searchApplications_should_return_validation_error_for_metadata_value_without_name() {
    var result = tool.searchApplications(1, 10, null, null, null, "someValue");

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors())
        .anyMatch(e -> e.contains("metadataValue") && e.contains("metadataName"));
    verifyNoInteractions(sdk);
  }

  @Test
  void searchApplications_should_return_all_applications_when_no_filters() throws Exception {
    var app1 = createApp("App1", "Active");
    var app2 = createApp("App2", "Inactive");

    try (MockedStatic<SDKHelper> sdkHelper = mockStatic(SDKHelper.class)) {
      sdkHelper
          .when(() -> SDKHelper.getApplicationsWithCache(anyString(), any()))
          .thenReturn(List.of(app1, app2));

      var result = tool.searchApplications(1, 10, null, null, null, null);

      assertThat(result.isSuccess()).isTrue();
      assertThat(result.items()).hasSize(2);
      assertThat(result.totalItems()).isEqualTo(2);
    }
  }

  @Test
  void searchApplications_should_filter_by_name_partial_case_insensitive() throws Exception {
    var app1 = createApp("MyProductionApp", "Active");
    var app2 = createApp("TestApp", "Active");

    try (MockedStatic<SDKHelper> sdkHelper = mockStatic(SDKHelper.class)) {
      sdkHelper
          .when(() -> SDKHelper.getApplicationsWithCache(anyString(), any()))
          .thenReturn(List.of(app1, app2));

      var result = tool.searchApplications(1, 10, "prod", null, null, null);

      assertThat(result.isSuccess()).isTrue();
      assertThat(result.items()).hasSize(1);
      assertThat(result.items().get(0).name()).isEqualTo("MyProductionApp");
    }
  }

  @Test
  void searchApplications_should_filter_by_tag_case_sensitive() throws Exception {
    var app1 = createAppWithTags("App1", List.of("Production", "Critical"));
    var app2 = createAppWithTags("App2", List.of("production", "Normal"));

    try (MockedStatic<SDKHelper> sdkHelper = mockStatic(SDKHelper.class)) {
      sdkHelper
          .when(() -> SDKHelper.getApplicationsWithCache(anyString(), any()))
          .thenReturn(List.of(app1, app2));

      var result = tool.searchApplications(1, 10, null, "Production", null, null);

      assertThat(result.isSuccess()).isTrue();
      assertThat(result.items()).hasSize(1);
      assertThat(result.items().get(0).name()).isEqualTo("App1");
    }
  }

  @Test
  void searchApplications_should_filter_by_metadata_name_only() throws Exception {
    var app1 = createAppWithMetadata("App1", "environment", "prod");
    var app2 = createAppWithMetadata("App2", "team", "backend");

    try (MockedStatic<SDKHelper> sdkHelper = mockStatic(SDKHelper.class)) {
      sdkHelper
          .when(() -> SDKHelper.getApplicationsWithCache(anyString(), any()))
          .thenReturn(List.of(app1, app2));

      var result = tool.searchApplications(1, 10, null, null, "environment", null);

      assertThat(result.isSuccess()).isTrue();
      assertThat(result.items()).hasSize(1);
      assertThat(result.items().get(0).name()).isEqualTo("App1");
    }
  }

  @Test
  void searchApplications_should_filter_by_metadata_name_and_value() throws Exception {
    var app1 = createAppWithMetadata("App1", "environment", "production");
    var app2 = createAppWithMetadata("App2", "environment", "development");

    try (MockedStatic<SDKHelper> sdkHelper = mockStatic(SDKHelper.class)) {
      sdkHelper
          .when(() -> SDKHelper.getApplicationsWithCache(anyString(), any()))
          .thenReturn(List.of(app1, app2));

      var result = tool.searchApplications(1, 10, null, null, "environment", "production");

      assertThat(result.isSuccess()).isTrue();
      assertThat(result.items()).hasSize(1);
      assertThat(result.items().get(0).name()).isEqualTo("App1");
    }
  }

  @Test
  void searchApplications_should_return_empty_when_no_match() throws Exception {
    var app1 = createApp("TestApp", "Active");

    try (MockedStatic<SDKHelper> sdkHelper = mockStatic(SDKHelper.class)) {
      sdkHelper
          .when(() -> SDKHelper.getApplicationsWithCache(anyString(), any()))
          .thenReturn(List.of(app1));

      var result = tool.searchApplications(1, 10, "nonexistent", null, null, null);

      assertThat(result.isSuccess()).isTrue();
      assertThat(result.items()).isEmpty();
      assertThat(result.totalItems()).isEqualTo(0);
      assertThat(result.warnings()).anyMatch(w -> w.contains("No results found"));
    }
  }

  @Test
  void searchApplications_should_paginate_results() throws Exception {
    var apps =
        List.of(
            createApp("App1", "Active"),
            createApp("App2", "Active"),
            createApp("App3", "Active"),
            createApp("App4", "Active"),
            createApp("App5", "Active"));

    try (MockedStatic<SDKHelper> sdkHelper = mockStatic(SDKHelper.class)) {
      sdkHelper.when(() -> SDKHelper.getApplicationsWithCache(anyString(), any())).thenReturn(apps);

      // Request page 2 with page size 2
      var result = tool.searchApplications(2, 2, null, null, null, null);

      assertThat(result.isSuccess()).isTrue();
      assertThat(result.items()).hasSize(2);
      assertThat(result.items().get(0).name()).isEqualTo("App3");
      assertThat(result.items().get(1).name()).isEqualTo("App4");
      assertThat(result.totalItems()).isEqualTo(5);
      assertThat(result.hasMorePages()).isTrue();
    }
  }

  @Test
  void searchApplications_should_include_metadata_in_response() throws Exception {
    var app = createAppWithMetadata("App1", "environment", "production");

    try (MockedStatic<SDKHelper> sdkHelper = mockStatic(SDKHelper.class)) {
      sdkHelper
          .when(() -> SDKHelper.getApplicationsWithCache(anyString(), any()))
          .thenReturn(List.of(app));

      var result = tool.searchApplications(1, 10, null, null, null, null);

      assertThat(result.isSuccess()).isTrue();
      assertThat(result.items()).hasSize(1);
      assertThat(result.items().get(0).metadata()).isNotEmpty();
      assertThat(result.items().get(0).metadata().get(0).name()).isEqualTo("environment");
      assertThat(result.items().get(0).metadata().get(0).value()).isEqualTo("production");
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
}
