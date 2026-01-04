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
 * Integration tests for GetSastProjectTool.
 *
 * <p>These tests require Contrast credentials to be set in environment variables and a valid SAST
 * project name configured. Run: source .env.integration-test && mvn verify
 */
@Slf4j
@SpringBootTest
@TestPropertySource(locations = "classpath:application-integration-test.properties")
@Tag("integration")
@EnabledIfEnvironmentVariable(named = "CONTRAST_HOST_NAME", matches = ".+")
class GetSastProjectToolIT {

  @Autowired private GetSastProjectTool getSastProjectTool;

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
  void getScanProject_should_return_project_for_valid_name() {
    log.info("\n=== Integration Test: get_scan_project ===");

    var response = getSastProjectTool.getScanProject(testProjectName);

    assertThat(response).as("Response should not be null").isNotNull();
    assertThat(response.isSuccess()).as("Response should be successful").isTrue();
    assertThat(response.data()).as("Project data should not be null").isNotNull();
    assertThat(response.data().name()).as("Project name should match").isEqualTo(testProjectName);
    assertThat(response.data().id()).as("Project ID should be set").isNotNull();

    log.info("✓ Retrieved project: {} (id: {})", response.data().name(), response.data().id());
    log.info("  Language: {}", response.data().language());
    log.info("  Completed scans: {}", response.data().completedScans());
    log.info(
        "  Vulnerabilities: critical={}, high={}, medium={}, low={}",
        response.data().critical(),
        response.data().high(),
        response.data().medium(),
        response.data().low());
  }

  @Test
  void getScanProject_should_return_error_for_null_projectName() {
    log.info("\n=== Integration Test: Null project name handling ===");

    var response = getSastProjectTool.getScanProject(null);

    assertThat(response).as("Response should not be null").isNotNull();
    assertThat(response.isSuccess()).as("Should not be successful").isFalse();
    assertThat(response.errors()).anyMatch(e -> e.contains("projectName is required"));

    log.info("✓ Null project name correctly rejected");
  }

  @Test
  void getScanProject_should_return_error_for_empty_projectName() {
    log.info("\n=== Integration Test: Empty project name handling ===");

    var response = getSastProjectTool.getScanProject("");

    assertThat(response).as("Response should not be null").isNotNull();
    assertThat(response.isSuccess()).as("Should not be successful").isFalse();
    assertThat(response.errors()).anyMatch(e -> e.contains("projectName is required"));

    log.info("✓ Empty project name correctly rejected");
  }

  @Test
  void getScanProject_should_return_notFound_for_nonexistent_project() {
    log.info("\n=== Integration Test: Non-existent project handling ===");

    var nonExistentProject = "nonexistent-project-" + System.currentTimeMillis();
    var response = getSastProjectTool.getScanProject(nonExistentProject);

    assertThat(response).as("Response should not be null").isNotNull();
    assertThat(response.found()).as("Should not find non-existent project").isFalse();
    assertThat(response.data()).as("Data should be null").isNull();

    log.info("✓ Non-existent project correctly returned not found");
  }

  @Test
  void getScanProject_should_verify_project_has_required_fields() {
    log.info("\n=== Integration Test: Verify project structure ===");

    var response = getSastProjectTool.getScanProject(testProjectName);

    assertThat(response.isSuccess()).isTrue();
    assertThat(response.data()).isNotNull();

    // Verify required fields are present
    assertThat(response.data().id()).as("Project ID is required").isNotNull();
    assertThat(response.data().name()).as("Project name is required").isNotNull();
    assertThat(response.data().language()).as("Language is required").isNotNull();

    log.info("✓ All required fields present:");
    log.info("  - id: {}", response.data().id());
    log.info("  - name: {}", response.data().name());
    log.info("  - language: {}", response.data().language());
  }

  /**
   * Regression test for bug AIML-343: get_scan_project returned {"data":{}} despite found:true.
   *
   * <p>The bug was caused by Jackson inability to serialize the SDK Project interface (which uses
   * method-style accessors like id() instead of getId()). The fix creates a ScanProject record that
   * Jackson can serialize properly.
   */
  @Test
  void getScanProject_regression_all_fields_should_be_populated() {
    log.info("\n=== Regression Test: AIML-343 - Data should not be empty ===");

    var response = getSastProjectTool.getScanProject(testProjectName);

    assertThat(response.found()).as("Project should be found").isTrue();
    assertThat(response.data()).as("Data should not be null").isNotNull();

    // THE BUG: data was serialized as {} (empty object)
    // AFTER FIX: all fields should be populated
    var project = response.data();

    // Core identity fields (never null)
    assertThat(project.id()).as("id field must be populated").isNotBlank();
    assertThat(project.name()).as("name field must be populated").isNotBlank();
    assertThat(project.organizationId()).as("organizationId field must be populated").isNotBlank();
    assertThat(project.language()).as("language field must be populated").isNotBlank();

    // Boolean field
    assertThat(project.archived()).as("archived field accessible").isNotNull();

    // Vulnerability counts (valid integers, may be zero)
    assertThat(project.critical()).as("critical count accessible").isGreaterThanOrEqualTo(0);
    assertThat(project.high()).as("high count accessible").isGreaterThanOrEqualTo(0);
    assertThat(project.medium()).as("medium count accessible").isGreaterThanOrEqualTo(0);
    assertThat(project.low()).as("low count accessible").isGreaterThanOrEqualTo(0);
    assertThat(project.note()).as("note count accessible").isGreaterThanOrEqualTo(0);

    // Scan counts
    assertThat(project.completedScans()).as("completedScans accessible").isGreaterThanOrEqualTo(0);

    // Namespace filters should be lists (possibly empty, never null)
    assertThat(project.includeNamespaceFilters())
        .as("includeNamespaceFilters should be a list")
        .isNotNull();
    assertThat(project.excludeNamespaceFilters())
        .as("excludeNamespaceFilters should be a list")
        .isNotNull();

    log.info("✓ AIML-343 regression test passed - all fields populated:");
    log.info("  - id: {}", project.id());
    log.info("  - name: {}", project.name());
    log.info("  - organizationId: {}", project.organizationId());
    log.info("  - language: {}", project.language());
    log.info("  - archived: {}", project.archived());
    log.info("  - critical: {}", project.critical());
    log.info("  - high: {}", project.high());
    log.info("  - medium: {}", project.medium());
    log.info("  - low: {}", project.low());
    log.info("  - note: {}", project.note());
    log.info("  - lastScanId: {}", project.lastScanId());
    log.info("  - lastScanTime: {}", project.lastScanTime());
    log.info("  - completedScans: {}", project.completedScans());
    log.info("  - includeNamespaceFilters: {}", project.includeNamespaceFilters());
    log.info("  - excludeNamespaceFilters: {}", project.excludeNamespaceFilters());
  }
}
