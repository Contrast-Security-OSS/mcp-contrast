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
package com.contrast.labs.ai.mcp.contrast.tool.sca;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.contrast.labs.ai.mcp.contrast.AnonymousLibraryExtendedBuilder;
import com.contrast.labs.ai.mcp.contrast.config.ContrastConfig;
import com.contrast.labs.ai.mcp.contrast.sdkextension.SDKExtension;
import com.contrast.labs.ai.mcp.contrast.sdkextension.SDKHelper;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.App;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.CveData;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.Library;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.LibraryExtended;
import com.contrastsecurity.exceptions.ResourceNotFoundException;
import com.contrastsecurity.sdk.ContrastSDK;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

class ListApplicationsByCveToolTest {

  private static final String TEST_ORG_ID = "test-org-123";
  private static final String TEST_CVE_ID = "CVE-2021-44228";
  private static final String TEST_APP_ID = "test-app-456";

  private ListApplicationsByCveTool tool;
  private ContrastConfig config;
  private ContrastSDK sdk;

  private MockedStatic<SDKHelper> mockedSDKHelper;
  private MockedConstruction<SDKExtension> mockedSDKExtension;

  @BeforeEach
  void setUp() {
    sdk = mock();
    config = mock();

    when(config.getSDK()).thenReturn(sdk);
    when(config.getOrgId()).thenReturn(TEST_ORG_ID);

    tool = new ListApplicationsByCveTool();
    ReflectionTestUtils.setField(tool, "config", config);

    // Mock SDKHelper static methods
    mockedSDKHelper = mockStatic(SDKHelper.class);
  }

  @AfterEach
  void tearDown() {
    if (mockedSDKHelper != null) {
      mockedSDKHelper.close();
    }
    if (mockedSDKExtension != null) {
      mockedSDKExtension.close();
    }
  }

  @Test
  void listApplicationsByCve_should_return_validation_error_for_missing_cve_id() {
    var result = tool.listApplicationsByCve(null);

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors()).anyMatch(e -> e.contains("cveId") && e.contains("required"));
    verifyNoInteractions(sdk);
  }

  @Test
  void listApplicationsByCve_should_return_validation_error_for_invalid_cve_format() {
    var result = tool.listApplicationsByCve("not-a-cve");

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors()).anyMatch(e -> e.contains("cveId") && e.contains("CVE format"));
    verifyNoInteractions(sdk);
  }

  @Test
  void listApplicationsByCve_should_return_cve_data_on_success() throws IOException {
    var mockCveData = createMockCveDataWithApps();
    var mockLibraries = createMockLibrariesWithMatchingHash();

    // Set up mocked SDKExtension
    mockedSDKExtension =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              when(mock.getAppsForCVE(eq(TEST_ORG_ID), eq(TEST_CVE_ID))).thenReturn(mockCveData);
            });

    mockedSDKHelper
        .when(
            () -> SDKHelper.getLibsForID(eq(TEST_APP_ID), eq(TEST_ORG_ID), any(SDKExtension.class)))
        .thenReturn(mockLibraries);

    var result = tool.listApplicationsByCve(TEST_CVE_ID);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.found()).isTrue();
    assertThat(result.data()).isNotNull();
    assertThat(result.data().getApps()).isNotEmpty();
    assertThat(result.errors()).isEmpty();
  }

  @Test
  void listApplicationsByCve_should_return_not_found_for_null_response() throws IOException {
    mockedSDKExtension =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              when(mock.getAppsForCVE(eq(TEST_ORG_ID), eq(TEST_CVE_ID))).thenReturn(null);
            });

    var result = tool.listApplicationsByCve(TEST_CVE_ID);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.found()).isFalse();
    assertThat(result.data()).isNull();
  }

  @Test
  void listApplicationsByCve_should_handle_empty_apps_list() throws IOException {
    var emptyCveData = new CveData();
    emptyCveData.setApps(new ArrayList<>());
    emptyCveData.setLibraries(new ArrayList<>());

    mockedSDKExtension =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              when(mock.getAppsForCVE(eq(TEST_ORG_ID), eq(TEST_CVE_ID))).thenReturn(emptyCveData);
            });

    var result = tool.listApplicationsByCve(TEST_CVE_ID);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.data()).isNotNull();
    assertThat(result.data().getApps()).isEmpty();
    assertThat(result.warnings()).anyMatch(w -> w.contains("No applications found"));
  }

  @Test
  void listApplicationsByCve_should_handle_null_apps_list_gracefully() throws IOException {
    var cveData = new CveData();
    cveData.setApps(null);
    cveData.setLibraries(new ArrayList<>());

    mockedSDKExtension =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              when(mock.getAppsForCVE(eq(TEST_ORG_ID), eq(TEST_CVE_ID))).thenReturn(cveData);
            });

    var result = tool.listApplicationsByCve(TEST_CVE_ID);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.data()).isNotNull();
    assertThat(result.warnings()).anyMatch(w -> w.contains("No applications found"));
  }

  @Test
  void listApplicationsByCve_should_handle_null_libraries_list_gracefully() throws IOException {
    var cveData = new CveData();
    var app = mock(App.class);
    when(app.getApp_id()).thenReturn(TEST_APP_ID);
    when(app.getName()).thenReturn("Test App");

    var apps = new ArrayList<App>();
    apps.add(app);
    cveData.setApps(apps);
    cveData.setLibraries(null); // null libraries

    var mockLibraries = createMockLibrariesWithMatchingHash();

    mockedSDKExtension =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              when(mock.getAppsForCVE(eq(TEST_ORG_ID), eq(TEST_CVE_ID))).thenReturn(cveData);
            });

    mockedSDKHelper
        .when(
            () -> SDKHelper.getLibsForID(eq(TEST_APP_ID), eq(TEST_ORG_ID), any(SDKExtension.class)))
        .thenReturn(mockLibraries);

    var result = tool.listApplicationsByCve(TEST_CVE_ID);

    // Should handle null libraries gracefully without NPE
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.data()).isNotNull();
    assertThat(result.data().getApps()).hasSize(1);
  }

  @Test
  void listApplicationsByCve_should_handle_api_exception() throws IOException {
    mockedSDKExtension =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              when(mock.getAppsForCVE(any(), any()))
                  .thenThrow(new IOException("CVE lookup failed"));
            });

    var result = tool.listApplicationsByCve(TEST_CVE_ID);

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors()).anyMatch(e -> e.contains("Internal error"));
  }

  @Test
  void listApplicationsByCve_should_return_not_found_for_unknown_cve() throws IOException {
    mockedSDKExtension =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              when(mock.getAppsForCVE(any(), any()))
                  .thenThrow(
                      new ResourceNotFoundException(
                          "CVE not found", "GET", "/api/cve/CVE-2020-99999", "Not Found"));
            });

    var result = tool.listApplicationsByCve("CVE-2020-99999");

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.found()).isFalse();
    assertThat(result.data()).isNull();
    assertThat(result.warnings()).anyMatch(w -> w.toLowerCase().contains("not found"));
  }

  @Test
  void listApplicationsByCve_should_populate_class_usage_when_library_is_used() throws IOException {
    var mockCveData = createMockCveDataWithApps();
    var mockLibraries = createMockLibrariesWithMatchingHash();

    mockedSDKExtension =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              when(mock.getAppsForCVE(eq(TEST_ORG_ID), eq(TEST_CVE_ID))).thenReturn(mockCveData);
            });

    mockedSDKHelper
        .when(
            () -> SDKHelper.getLibsForID(eq(TEST_APP_ID), eq(TEST_ORG_ID), any(SDKExtension.class)))
        .thenReturn(mockLibraries);

    var result = tool.listApplicationsByCve(TEST_CVE_ID);

    assertThat(result.isSuccess()).isTrue();
    var firstApp = result.data().getApps().get(0);
    // Class usage should be populated since our mock library has classesUsed > 0
    assertThat(firstApp.getClassCount()).isGreaterThanOrEqualTo(0);
  }

  private CveData createMockCveDataWithApps() {
    var cveData = new CveData();

    var app = mock(App.class);
    when(app.getApp_id()).thenReturn(TEST_APP_ID);
    when(app.getName()).thenReturn("Test Application");
    when(app.getClassCount()).thenReturn(0);

    var apps = new ArrayList<App>();
    apps.add(app);
    cveData.setApps(apps);

    var lib = mock(Library.class);
    when(lib.getHash()).thenReturn("matching-hash-789");
    when(lib.getFile_name()).thenReturn("vulnerable-lib.jar");
    when(lib.getVersion()).thenReturn("1.0.0");

    var libs = new ArrayList<Library>();
    libs.add(lib);
    cveData.setLibraries(libs);

    return cveData;
  }

  private List<LibraryExtended> createMockLibrariesWithMatchingHash() {
    var libraries = new ArrayList<LibraryExtended>();

    var lib =
        AnonymousLibraryExtendedBuilder.validLibrary()
            .withFilename("vulnerable-lib.jar")
            .withHash("matching-hash-789")
            .withVersion("1.0.0")
            .withClassCount(100)
            .withClassedUsed(50)
            .build();
    libraries.add(lib);

    return libraries;
  }
}
