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

import com.contrast.labs.ai.mcp.contrast.config.IntegrationTestConfig;
import com.contrast.labs.ai.mcp.contrast.util.AbstractIntegrationTest;
import com.contrast.labs.ai.mcp.contrast.util.TestDataDiscoveryHelper;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Integration tests for ListApplicationsByCveTool.
 *
 * <p>Requires CONTRAST_HOST_NAME environment variable to be set and an application with vulnerable
 * libraries containing CVEs.
 */
@Slf4j
@SpringBootTest
@Import(IntegrationTestConfig.class)
@EnabledIfEnvironmentVariable(named = "CONTRAST_HOST_NAME", matches = ".+")
class ListApplicationsByCveToolIT
    extends AbstractIntegrationTest<ListApplicationsByCveToolIT.TestData> {

  @Autowired private ListApplicationsByCveTool tool;

  static class TestData {
    String appId;
    String appName;
    String vulnerableCveId;
    boolean hasVulnerableLibrary;

    @Override
    public String toString() {
      return String.format(
          "TestData{appId='%s', appName='%s', vulnerableCveId='%s', hasVulnerableLibrary=%s}",
          appId, appName, vulnerableCveId, hasVulnerableLibrary);
    }
  }

  @Override
  protected String testDisplayName() {
    return "ListApplicationsByCveTool Integration Test";
  }

  @Override
  protected Class<TestData> testDataType() {
    return TestData.class;
  }

  @Override
  protected TestData performDiscovery() throws IOException {
    var appWithLibraries =
        TestDataDiscoveryHelper.findApplicationWithLibraries(orgId, sdkExtension);

    if (appWithLibraries.isEmpty()) {
      throw new NoTestDataException("No application with libraries found in organization");
    }

    var result = appWithLibraries.get();
    var data = new TestData();
    data.appId = result.getApplication().getAppId();
    data.appName = result.getApplication().getName();
    data.hasVulnerableLibrary = result.hasVulnerableLibrary();
    data.vulnerableCveId = result.getVulnerableCveId();
    return data;
  }

  @Override
  protected void logTestDataDetails(TestData data) {
    log.info("Test data: {}", data);
  }

  @Override
  protected void afterCacheHit(TestData data) {
    warnIfNoVulnerableLibraries(data);
  }

  @Override
  protected void afterDiscovery(TestData data) {
    warnIfNoVulnerableLibraries(data);
  }

  private void warnIfNoVulnerableLibraries(TestData data) {
    if (!data.hasVulnerableLibrary) {
      log.warn("\n⚠️  WARNING: Application has libraries but NO VULNERABLE LIBRARIES");
      log.warn("   CVE-related tests will be skipped.");
      log.warn("   To enable full testing, use an application with vulnerable dependencies.");
    }
  }

  @Test
  void testDiscoveredTestDataExists() {
    log.info("\n=== Integration Test: Validate test data discovery ===");

    assertThat(testData).as("Test data should have been discovered").isNotNull();
    assertThat(testData.appId).as("App ID should be set").isNotNull();

    if (testData.hasVulnerableLibrary) {
      assertThat(testData.vulnerableCveId).as("CVE ID should be set").isNotNull();
    }
  }

  @Test
  void listApplicationsByCve_should_return_cve_data() {
    log.info("\n=== Integration Test: list_applications_by_cve ===");

    if (testData.vulnerableCveId == null) {
      log.info("⚠️  SKIPPED: No vulnerable libraries with CVEs found in test data");
      return;
    }

    var result = tool.listApplicationsByCve(testData.vulnerableCveId);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.found()).isTrue();
    assertThat(result.data()).isNotNull();
    assertThat(result.data().getApps()).as("Apps list should not be null").isNotNull();
    assertThat(result.data().getLibraries()).as("Libraries list should not be null").isNotNull();
    assertThat(result.errors()).isEmpty();

    log.info("CVE {} affects:", testData.vulnerableCveId);
    log.info("  Applications: {}", result.data().getApps().size());
    log.info("  Vulnerable libraries: {}", result.data().getLibraries().size());
  }

  @Test
  void listApplicationsByCve_should_populate_class_usage() {
    log.info("\n=== Integration Test: CVE class usage population ===");

    if (testData.vulnerableCveId == null) {
      log.info("⚠️  SKIPPED: No vulnerable libraries with CVEs found in test data");
      return;
    }

    var result = tool.listApplicationsByCve(testData.vulnerableCveId);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.data()).isNotNull();
    assertThat(result.data().getApps()).isNotEmpty();

    // Log class usage for each app
    for (var app : result.data().getApps()) {
      log.info(
          "App: {} (class count: {}, class usage: {})",
          app.getName(),
          app.getClassCount(),
          app.getClassUsage());

      assertThat(app.getClassCount())
          .as("Class count should be non-negative for app: " + app.getName())
          .isGreaterThanOrEqualTo(0);
    }
  }

  @Test
  void listApplicationsByCve_should_handle_nonexistent_cve() {
    log.info("\n=== Integration Test: Non-existent CVE handling ===");

    var result = tool.listApplicationsByCve("CVE-9999-99999");

    // Should either return not found or an error - both are acceptable
    if (result.isSuccess()) {
      // If success, it should indicate not found
      assertThat(result.found()).isFalse();
      log.info("API returned not found for non-existent CVE");
    } else {
      // If error, it should have an appropriate error message
      assertThat(result.errors()).isNotEmpty();
      log.info("API rejected non-existent CVE with error: {}", result.errors());
    }
  }

  @Test
  void listApplicationsByCve_should_reject_invalid_cve_format() {
    log.info("\n=== Integration Test: Invalid CVE format handling ===");

    var result = tool.listApplicationsByCve("not-a-cve");

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors()).anyMatch(e -> e.contains("cveId") && e.contains("CVE format"));
    log.info("Correctly rejected invalid CVE format");
  }
}
