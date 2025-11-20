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
package com.contrast.labs.ai.mcp.contrast;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.contrast.labs.ai.mcp.contrast.sdkextension.SDKHelper;
import com.contrastsecurity.sdk.ContrastSDK;
import com.contrastsecurity.sdk.scan.Projects;
import com.contrastsecurity.sdk.scan.ScanManager;
import com.contrastsecurity.sdk.scan.Scans;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

class SastServiceTest {
  private SastService sastService;
  private ContrastSDK contrastSDK;
  private ScanManager scanManager;
  private Projects projects;
  private Scans scans;

  @BeforeEach
  void setUp() {
    sastService = new SastService();
    ReflectionTestUtils.setField(sastService, "hostName", "app.contrastsecurity.com");
    ReflectionTestUtils.setField(sastService, "apiKey", "test-api-key");
    ReflectionTestUtils.setField(sastService, "serviceKey", "test-service-key");
    ReflectionTestUtils.setField(sastService, "userName", "test-user");
    ReflectionTestUtils.setField(sastService, "orgID", "test-org-id");
    ReflectionTestUtils.setField(sastService, "httpProxyHost", "");
    ReflectionTestUtils.setField(sastService, "httpProxyPort", "");

    contrastSDK = mock();
    scanManager = mock();
    projects = mock();
    scans = mock();
  }

  @Test
  void getScanProject_should_return_project_when_project_exists() throws IOException {
    // Arrange
    var projectName = "test-project";
    var mockProject =
        AnonymousProjectBuilder.validProject().withName(projectName).withId("project-123").build();

    try (MockedStatic<SDKHelper> sdkHelper = mockStatic(SDKHelper.class)) {
      sdkHelper
          .when(() -> SDKHelper.getSDK(any(), any(), any(), any(), any(), any()))
          .thenReturn(contrastSDK);
      when(contrastSDK.scan(any())).thenReturn(scanManager);
      when(scanManager.projects()).thenReturn(projects);
      when(projects.findByName(projectName)).thenReturn(Optional.of(mockProject));

      // Act
      var result = sastService.getScanProject(projectName);

      // Assert
      assertThat(result).isNotNull();
      assertThat(result.name()).isEqualTo(projectName);
      assertThat(result.id()).isEqualTo("project-123");
    }
  }

  @Test
  void getScanProject_should_throw_IOException_when_project_not_found() throws IOException {
    // Arrange
    var projectName = "non-existent-project";

    try (MockedStatic<SDKHelper> sdkHelper = mockStatic(SDKHelper.class)) {
      sdkHelper
          .when(() -> SDKHelper.getSDK(any(), any(), any(), any(), any(), any()))
          .thenReturn(contrastSDK);
      when(contrastSDK.scan(any())).thenReturn(scanManager);
      when(scanManager.projects()).thenReturn(projects);
      when(projects.findByName(projectName)).thenReturn(Optional.empty());

      // Act & Assert
      assertThatThrownBy(() -> sastService.getScanProject(projectName))
          .isInstanceOf(IOException.class)
          .hasMessage("Project not found");
    }
  }

  @Test
  void getScanProject_should_throw_IOException_when_SDK_throws_exception() throws IOException {
    // Arrange
    var projectName = "test-project";

    try (MockedStatic<SDKHelper> sdkHelper = mockStatic(SDKHelper.class)) {
      sdkHelper
          .when(() -> SDKHelper.getSDK(any(), any(), any(), any(), any(), any()))
          .thenReturn(contrastSDK);
      when(contrastSDK.scan(any())).thenReturn(scanManager);
      when(scanManager.projects()).thenReturn(projects);
      when(projects.findByName(projectName))
          .thenThrow(new RuntimeException("SDK connection error"));

      // Act & Assert
      assertThatThrownBy(() -> sastService.getScanProject(projectName))
          .isInstanceOf(RuntimeException.class)
          .hasMessage("SDK connection error");
    }
  }

  @Test
  void getLatestScanResult_should_return_sarif_json_when_scan_exists() throws IOException {
    // Arrange
    var projectName = "test-project";
    var scanId = "scan-123";
    var sarifJson = "{\"version\":\"2.1.0\",\"runs\":[]}";

    var mockProject =
        AnonymousProjectBuilder.validProject()
            .withName(projectName)
            .withId("project-123")
            .withLastScanId(scanId)
            .build();

    var mockScan = AnonymousScanBuilder.validScan().withSarif(sarifJson).build();

    try (MockedStatic<SDKHelper> sdkHelper = mockStatic(SDKHelper.class)) {
      sdkHelper
          .when(() -> SDKHelper.getSDK(any(), any(), any(), any(), any(), any()))
          .thenReturn(contrastSDK);
      when(contrastSDK.scan(any())).thenReturn(scanManager);
      when(scanManager.projects()).thenReturn(projects);
      when(scanManager.scans(any())).thenReturn(scans);
      when(projects.findByName(projectName)).thenReturn(Optional.of(mockProject));
      when(scans.get(scanId)).thenReturn(mockScan);

      // Act
      var result = sastService.getLatestScanResult(projectName);

      // Assert
      assertThat(result).isNotNull();
      assertThat(result).contains("\"version\":\"2.1.0\"");
      assertThat(result).contains("\"runs\":[]");
    }
  }

  @Test
  void getLatestScanResult_should_throw_IOException_when_project_not_found() throws IOException {
    // Arrange
    var projectName = "non-existent-project";

    try (MockedStatic<SDKHelper> sdkHelper = mockStatic(SDKHelper.class)) {
      sdkHelper
          .when(() -> SDKHelper.getSDK(any(), any(), any(), any(), any(), any()))
          .thenReturn(contrastSDK);
      when(contrastSDK.scan(any())).thenReturn(scanManager);
      when(scanManager.projects()).thenReturn(projects);
      when(projects.findByName(projectName)).thenReturn(Optional.empty());

      // Act & Assert
      assertThatThrownBy(() -> sastService.getLatestScanResult(projectName))
          .isInstanceOf(IOException.class)
          .hasMessage("Project not found");
    }
  }

  @Test
  void getLatestScanResult_should_throw_IOException_when_lastScanId_is_null() throws IOException {
    // Arrange
    var projectName = "project-without-scans";
    var mockProject =
        AnonymousProjectBuilder.validProject()
            .withName(projectName)
            .withId("project-123")
            .withLastScanId(null)
            .build();

    try (MockedStatic<SDKHelper> sdkHelper = mockStatic(SDKHelper.class)) {
      sdkHelper
          .when(() -> SDKHelper.getSDK(any(), any(), any(), any(), any(), any()))
          .thenReturn(contrastSDK);
      when(contrastSDK.scan(any())).thenReturn(scanManager);
      when(scanManager.projects()).thenReturn(projects);
      when(scanManager.scans(any())).thenReturn(scans);
      when(projects.findByName(projectName)).thenReturn(Optional.of(mockProject));

      // Act & Assert
      assertThatThrownBy(() -> sastService.getLatestScanResult(projectName))
          .isInstanceOf(IOException.class)
          .hasMessageContaining("No scan results available")
          .hasMessageContaining("has no completed scans");
    }
  }

  @Test
  void getLatestScanResult_should_throw_IOException_when_scan_is_null() throws IOException {
    // Arrange
    var projectName = "test-project";
    var scanId = "scan-123";
    var mockProject =
        AnonymousProjectBuilder.validProject()
            .withName(projectName)
            .withId("project-123")
            .withLastScanId(scanId)
            .build();

    try (MockedStatic<SDKHelper> sdkHelper = mockStatic(SDKHelper.class)) {
      sdkHelper
          .when(() -> SDKHelper.getSDK(any(), any(), any(), any(), any(), any()))
          .thenReturn(contrastSDK);
      when(contrastSDK.scan(any())).thenReturn(scanManager);
      when(scanManager.projects()).thenReturn(projects);
      when(scanManager.scans(any())).thenReturn(scans);
      when(projects.findByName(projectName)).thenReturn(Optional.of(mockProject));
      when(scans.get(scanId)).thenReturn(null);

      // Act & Assert
      assertThatThrownBy(() -> sastService.getLatestScanResult(projectName))
          .isInstanceOf(IOException.class)
          .hasMessageContaining("No scan results available")
          .hasMessageContaining("Scan ID " + scanId + " not found");
    }
  }

  @Test
  void getLatestScanResult_should_throw_IOException_when_scan_retrieval_fails() throws IOException {
    // Arrange
    var projectName = "test-project";
    var scanId = "scan-123";
    var mockProject =
        AnonymousProjectBuilder.validProject()
            .withName(projectName)
            .withId("project-123")
            .withLastScanId(scanId)
            .build();

    try (MockedStatic<SDKHelper> sdkHelper = mockStatic(SDKHelper.class)) {
      sdkHelper
          .when(() -> SDKHelper.getSDK(any(), any(), any(), any(), any(), any()))
          .thenReturn(contrastSDK);
      when(contrastSDK.scan(any())).thenReturn(scanManager);
      when(scanManager.projects()).thenReturn(projects);
      when(scanManager.scans(any())).thenReturn(scans);
      when(projects.findByName(projectName)).thenReturn(Optional.of(mockProject));
      when(scans.get(scanId)).thenThrow(new RuntimeException("Scan retrieval failed"));

      // Act & Assert
      assertThatThrownBy(() -> sastService.getLatestScanResult(projectName))
          .isInstanceOf(RuntimeException.class)
          .hasMessage("Scan retrieval failed");
    }
  }
}
