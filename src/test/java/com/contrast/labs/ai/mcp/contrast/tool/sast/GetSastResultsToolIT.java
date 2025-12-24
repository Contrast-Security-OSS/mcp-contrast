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
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Integration tests for GetSastResultsTool.
 *
 * <p>These tests require Contrast credentials to be set in environment variables and a valid SAST
 * project with completed scans configured. Run: source .env.integration-test && mvn verify
 */
@Slf4j
@SpringBootTest
@TestPropertySource(locations = "classpath:application-integration-test.properties")
@Tag("integration")
@EnabledIfEnvironmentVariable(named = "CONTRAST_HOST_NAME", matches = ".+")
class GetSastResultsToolIT {

  @Autowired private GetSastResultsTool getSastResultsTool;

  @Value("${contrast.host-name:${CONTRAST_HOST_NAME:}}")
  private String hostName;

  @Value("${test.scan.project-name:}")
  private String testProjectName;

  @BeforeEach
  void setUp() {
    assumeTrue(
        hostName != null && !hostName.isEmpty(),
        "Integration tests require CONTRAST_HOST_NAME to be configured");
    assumeTrue(
        testProjectName != null && !testProjectName.isEmpty(),
        "Integration tests require test.scan.project-name to be configured in"
            + " application-integration-test.properties");
  }

  @Test
  void getScanResults_should_return_sarif_for_valid_project() {
    log.info("\n=== Integration Test: get_scan_results ===");

    var response = getSastResultsTool.getScanResults(testProjectName);

    assertThat(response).as("Response should not be null").isNotNull();
    assertThat(response.isSuccess()).as("Response should be successful").isTrue();
    assertThat(response.data()).as("SARIF data should not be null").isNotNull();
    assertThat(response.data()).as("Should be valid SARIF").isNotEmpty();

    // Verify SARIF structure
    assertThat(response.data()).contains("\"version\":");
    assertThat(response.data()).contains("2.1.0");
    assertThat(response.data()).contains("\"runs\":");

    log.info(
        "✓ Retrieved SARIF data for project: {} ({} characters)",
        testProjectName,
        response.data().length());
  }

  @Test
  void getScanResults_should_include_deprecation_warning() {
    log.info("\n=== Integration Test: Deprecation warning ===");

    var response = getSastResultsTool.getScanResults(testProjectName);

    assertThat(response).as("Response should not be null").isNotNull();
    assertThat(response.isSuccess()).as("Response should be successful").isTrue();
    assertThat(response.warnings()).anyMatch(w -> w.contains("DEPRECATED"));

    log.info("✓ Deprecation warning present in response");
    log.info("  Warnings: {}", response.warnings());
  }

  @Test
  void getScanResults_should_return_error_for_null_projectName() {
    log.info("\n=== Integration Test: Null project name handling ===");

    var response = getSastResultsTool.getScanResults(null);

    assertThat(response).as("Response should not be null").isNotNull();
    assertThat(response.isSuccess()).as("Should not be successful").isFalse();
    assertThat(response.errors()).anyMatch(e -> e.contains("projectName is required"));

    log.info("✓ Null project name correctly rejected");
  }

  @Test
  void getScanResults_should_return_error_for_empty_projectName() {
    log.info("\n=== Integration Test: Empty project name handling ===");

    var response = getSastResultsTool.getScanResults("");

    assertThat(response).as("Response should not be null").isNotNull();
    assertThat(response.isSuccess()).as("Should not be successful").isFalse();
    assertThat(response.errors()).anyMatch(e -> e.contains("projectName is required"));

    log.info("✓ Empty project name correctly rejected");
  }

  @Test
  void getScanResults_should_return_notFound_for_nonexistent_project() {
    log.info("\n=== Integration Test: Non-existent project handling ===");

    var nonExistentProject = "nonexistent-project-" + System.currentTimeMillis();
    var response = getSastResultsTool.getScanResults(nonExistentProject);

    assertThat(response).as("Response should not be null").isNotNull();
    assertThat(response.found()).as("Should not find non-existent project").isFalse();
    assertThat(response.data()).as("Data should be null").isNull();

    log.info("✓ Non-existent project correctly returned not found");
  }

  @Test
  void getScanResults_should_return_valid_sarif_structure() {
    log.info("\n=== Integration Test: Verify SARIF structure ===");

    var response = getSastResultsTool.getScanResults(testProjectName);

    assertThat(response.isSuccess()).isTrue();
    assertThat(response.data()).isNotNull();

    // Verify SARIF 2.1.0 structure
    assertThat(response.data()).contains("\"$schema\":");
    assertThat(response.data()).contains("\"version\":\"2.1.0\"");
    assertThat(response.data()).contains("\"runs\":");

    log.info("✓ SARIF structure validated:");
    log.info("  - Contains $schema");
    log.info("  - Contains version 2.1.0");
    log.info("  - Contains runs array");
  }

  @Test
  void end_to_end_workflow_should_demonstrate_deprecation() {
    log.info("\n=== Integration Test: End-to-end workflow with deprecation notice ===");

    var response = getSastResultsTool.getScanResults(testProjectName);

    if (response.isSuccess()) {
      log.info("✓ SARIF retrieved successfully ({} chars)", response.data().length());

      if (!response.warnings().isEmpty()) {
        log.warn("⚠️ Tool returned warnings:");
        for (var warning : response.warnings()) {
          log.warn("  - {}", warning);
        }
      }

      // Verify the deprecation warning is present
      assertThat(response.warnings())
          .as("Should include deprecation warning")
          .anyMatch(w -> w.contains("DEPRECATED"));

      log.info("\n💡 Note: This tool is deprecated. Consider using future paginated SAST tools.");
    } else {
      log.info("✗ SARIF retrieval failed: {}", response.errors());
    }
  }
}
