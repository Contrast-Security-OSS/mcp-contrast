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
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-integration-test.properties")
@Tag("integration")
class SastServiceIntegrationTest {
  @Autowired private SastService sastService;

  @Value("${contrast.host-name:${CONTRAST_HOST_NAME:}}")
  private String hostName;

  @Value("${test.scan.project-name:}")
  private String testProjectName;

  @BeforeEach
  void setUp() {
    // Skip tests if integration test credentials not configured
    assumeTrue(
        hostName != null && !hostName.isEmpty(),
        "Integration tests require CONTRAST_HOST_NAME to be configured");
    assumeTrue(
        testProjectName != null && !testProjectName.isEmpty(),
        "Integration tests require test.scan.project-name to be configured in"
            + " application-integration-test.properties");
  }

  @Test
  void getScanProject_should_return_valid_project_from_teamserver() throws IOException {
    // Act
    var project = sastService.getScanProject(testProjectName);

    // Assert
    assertThat(project).isNotNull();
    assertThat(project.name()).isEqualTo(testProjectName);
    assertThat(project.id()).isNotNull();
    assertThat(project.id()).isNotEmpty();
  }

  @Test
  void getScanProject_should_throw_IOException_for_nonexistent_project() {
    // Arrange
    var nonExistentProject = "nonexistent-project-" + System.currentTimeMillis();

    // Act & Assert
    assertThatThrownBy(() -> sastService.getScanProject(nonExistentProject))
        .isInstanceOf(IOException.class)
        .hasMessage("Project not found");
  }

  @Test
  void getLatestScanResult_should_return_valid_sarif_from_teamserver() throws IOException {
    // Act
    var sarifJson = sastService.getLatestScanResult(testProjectName);

    // Assert
    assertThat(sarifJson).isNotNull();
    assertThat(sarifJson).isNotEmpty();

    // Verify it's valid SARIF JSON with version 2.1.0
    assertThat(sarifJson).contains("\"version\":");
    assertThat(sarifJson).contains("2.1.0");
    assertThat(sarifJson).contains("\"runs\":");
    assertThat(sarifJson).contains("\"$schema\":");
  }

  @Test
  void getLatestScanResult_should_throw_IOException_for_nonexistent_project() {
    // Arrange
    var nonExistentProject = "nonexistent-project-" + System.currentTimeMillis();

    // Act & Assert
    assertThatThrownBy(() -> sastService.getLatestScanResult(nonExistentProject))
        .isInstanceOf(IOException.class)
        .hasMessage("Project not found");
  }

  @Test
  void end_to_end_workflow_should_retrieve_project_and_sarif_results() throws IOException {
    // Act - Get project first
    var project = sastService.getScanProject(testProjectName);

    // Assert project is valid
    assertThat(project).isNotNull();
    assertThat(project.name()).isEqualTo(testProjectName);
    assertThat(project.lastScanId()).isNotNull();

    // Act - Get SARIF results
    var sarifJson = sastService.getLatestScanResult(testProjectName);

    // Assert SARIF is valid
    assertThat(sarifJson).isNotNull();
    assertThat(sarifJson).isNotEmpty();
    assertThat(sarifJson).contains("2.1.0");
    assertThat(sarifJson).contains("\"runs\":");
  }

  @Test
  void getScanProject_should_handle_exact_match_only() throws IOException {
    // This test verifies that project name matching is exact (case-sensitive)
    // It assumes testProjectName has a specific casing

    // Act - Try with exact name
    var projectExact = sastService.getScanProject(testProjectName);

    // Assert exact match works
    assertThat(projectExact).isNotNull();
    assertThat(projectExact.name()).isEqualTo(testProjectName);

    // Act & Assert - Try with different casing (should fail)
    var lowerCaseName = testProjectName.toLowerCase();
    if (!lowerCaseName.equals(testProjectName)) {
      assertThatThrownBy(() -> sastService.getScanProject(lowerCaseName))
          .isInstanceOf(IOException.class)
          .hasMessage("Project not found");
    }
  }
}
