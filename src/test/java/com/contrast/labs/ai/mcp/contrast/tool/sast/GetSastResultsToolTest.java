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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.contrast.labs.ai.mcp.contrast.AnonymousProjectBuilder;
import com.contrast.labs.ai.mcp.contrast.AnonymousScanBuilder;
import com.contrast.labs.ai.mcp.contrast.config.ContrastConfig;
import com.contrastsecurity.sdk.ContrastSDK;
import com.contrastsecurity.sdk.scan.Projects;
import com.contrastsecurity.sdk.scan.ScanManager;
import com.contrastsecurity.sdk.scan.Scans;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

/** Unit tests for GetSastResultsTool. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GetSastResultsToolTest {

  private GetSastResultsTool tool;

  @Mock private ContrastConfig config;

  @Mock private ContrastSDK sdk;

  @Mock private ScanManager scanManager;

  @Mock private Projects projects;

  @Mock private Scans scans;

  private static final String TEST_ORG_ID = "test-org-123";
  private static final String TEST_PROJECT_NAME = "test-project";
  private static final String TEST_SCAN_ID = "scan-123";

  @BeforeEach
  void setUp() {
    tool = new GetSastResultsTool();
    ReflectionTestUtils.setField(tool, "config", config);
    when(config.getSDK()).thenReturn(sdk);
    when(config.getOrgId()).thenReturn(TEST_ORG_ID);
    when(sdk.scan(any())).thenReturn(scanManager);
    when(scanManager.projects()).thenReturn(projects);
  }

  @Test
  void getScanResults_should_return_sarif_when_scan_exists() throws IOException {
    var sarifJson = "{\"version\":\"2.1.0\",\"runs\":[]}";

    var mockProject =
        AnonymousProjectBuilder.validProject()
            .withName(TEST_PROJECT_NAME)
            .withId("project-123")
            .withLastScanId(TEST_SCAN_ID)
            .build();

    var mockScan = AnonymousScanBuilder.validScan().withSarif(sarifJson).build();

    when(projects.findByName(TEST_PROJECT_NAME)).thenReturn(Optional.of(mockProject));
    when(scanManager.scans(any())).thenReturn(scans);
    when(scans.get(TEST_SCAN_ID)).thenReturn(mockScan);

    var result = tool.getScanResults(TEST_PROJECT_NAME);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.found()).isTrue();
    assertThat(result.data()).isNotNull();
    assertThat(result.data()).contains("\"version\":\"2.1.0\"");
    assertThat(result.data()).contains("\"runs\":[]");
  }

  @Test
  void getScanResults_should_include_deprecation_warning() throws IOException {
    var sarifJson = "{\"version\":\"2.1.0\",\"runs\":[]}";

    var mockProject =
        AnonymousProjectBuilder.validProject()
            .withName(TEST_PROJECT_NAME)
            .withId("project-123")
            .withLastScanId(TEST_SCAN_ID)
            .build();

    var mockScan = AnonymousScanBuilder.validScan().withSarif(sarifJson).build();

    when(projects.findByName(TEST_PROJECT_NAME)).thenReturn(Optional.of(mockProject));
    when(scanManager.scans(any())).thenReturn(scans);
    when(scans.get(TEST_SCAN_ID)).thenReturn(mockScan);

    var result = tool.getScanResults(TEST_PROJECT_NAME);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.warnings()).anyMatch(w -> w.contains("DEPRECATED"));
  }

  @Test
  void getScanResults_should_return_validation_error_for_null_projectName() {
    var result = tool.getScanResults(null);

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors()).anyMatch(e -> e.contains("projectName is required"));
  }

  @Test
  void getScanResults_should_return_validation_error_for_empty_projectName() {
    var result = tool.getScanResults("");

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors()).anyMatch(e -> e.contains("projectName is required"));
  }

  @Test
  void getScanResults_should_return_notFound_when_project_does_not_exist() throws IOException {
    when(projects.findByName(TEST_PROJECT_NAME)).thenReturn(Optional.empty());

    var result = tool.getScanResults(TEST_PROJECT_NAME);

    assertThat(result.found()).isFalse();
    assertThat(result.data()).isNull();
    assertThat(result.warnings()).anyMatch(w -> w.contains("DEPRECATED"));
  }

  @Test
  void getScanResults_should_warn_when_lastScanId_is_null() throws IOException {
    var mockProject =
        AnonymousProjectBuilder.validProject()
            .withName(TEST_PROJECT_NAME)
            .withId("project-123")
            .withLastScanId(null)
            .build();

    when(projects.findByName(TEST_PROJECT_NAME)).thenReturn(Optional.of(mockProject));

    var result = tool.getScanResults(TEST_PROJECT_NAME);

    assertThat(result.found()).isFalse();
    assertThat(result.data()).isNull();
    assertThat(result.warnings()).anyMatch(w -> w.contains("No scan results available"));
    assertThat(result.warnings()).anyMatch(w -> w.contains("DEPRECATED"));
  }

  @Test
  void getScanResults_should_warn_when_scan_is_null() throws IOException {
    var mockProject =
        AnonymousProjectBuilder.validProject()
            .withName(TEST_PROJECT_NAME)
            .withId("project-123")
            .withLastScanId(TEST_SCAN_ID)
            .build();

    when(projects.findByName(TEST_PROJECT_NAME)).thenReturn(Optional.of(mockProject));
    when(scanManager.scans(any())).thenReturn(scans);
    when(scans.get(TEST_SCAN_ID)).thenReturn(null);

    var result = tool.getScanResults(TEST_PROJECT_NAME);

    assertThat(result.found()).isFalse();
    assertThat(result.data()).isNull();
    assertThat(result.warnings())
        .anyMatch(w -> w.contains("Scan ID " + TEST_SCAN_ID + " not found"));
    assertThat(result.warnings()).anyMatch(w -> w.contains("DEPRECATED"));
  }

  @Test
  void getScanResults_should_handle_sdk_exception() throws IOException {
    when(projects.findByName(TEST_PROJECT_NAME)).thenThrow(new RuntimeException("SDK error"));

    var result = tool.getScanResults(TEST_PROJECT_NAME);

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors()).anyMatch(e -> e.contains("Internal error"));
  }
}
