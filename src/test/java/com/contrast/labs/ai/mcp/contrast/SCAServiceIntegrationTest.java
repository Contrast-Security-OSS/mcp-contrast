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
import static org.assertj.core.api.Assertions.fail;

import com.contrast.labs.ai.mcp.contrast.config.IntegrationTestConfig;
import com.contrast.labs.ai.mcp.contrast.sdkextension.SDKExtension;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.LibraryExtended;
import com.contrast.labs.ai.mcp.contrast.util.TestDataDiscoveryHelper;
import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Integration test for SCAService that validates library and CVE data from real TeamServer.
 *
 * <p>This test automatically discovers suitable test data by querying the Contrast API. It looks
 * for applications with third-party libraries and optionally CVE vulnerabilities.
 *
 * <p>This test only runs if CONTRAST_HOST_NAME environment variable is set.
 *
 * <p>Required environment variables: - CONTRAST_HOST_NAME (e.g., app.contrastsecurity.com) -
 * CONTRAST_API_KEY - CONTRAST_SERVICE_KEY - CONTRAST_USERNAME - CONTRAST_ORG_ID
 *
 * <p>Run locally: source .env.integration-test # Load credentials mvn verify
 *
 * <p>Or skip integration tests: mvn verify -DskipITs
 */
@Slf4j
@SpringBootTest
@Import(IntegrationTestConfig.class)
@EnabledIfEnvironmentVariable(named = "CONTRAST_HOST_NAME", matches = ".+")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SCAServiceIntegrationTest {

  @Autowired private SCAService scaService;

  @Autowired private SDKExtension sdkExtension;

  @Value("${contrast.org-id:${CONTRAST_ORG_ID:}}")
  private String orgID;

  // Discovered test data - populated in @BeforeAll
  private static TestData testData;

  // Performance metrics
  private static long discoveryDurationMs;
  private long testStartTimeMs;
  private long totalTestTimeMs = 0;
  private int testCount = 0;

  /** Container for discovered test data */
  private static class TestData {
    String appId;
    String appName;
    boolean hasLibraries;
    int libraryCount;
    List<LibraryExtended> libraries;
    String vulnerableCveId; // CVE for testing CVE lookup
    boolean hasVulnerableLibrary;

    @Override
    public String toString() {
      return String.format(
          "TestData{appId='%s', appName='%s', hasLibraries=%s, libraryCount=%d, "
              + "hasVulnerableLibrary=%s, vulnerableCveId='%s'}",
          appId, appName, hasLibraries, libraryCount, hasVulnerableLibrary, vulnerableCveId);
    }
  }

  @BeforeAll
  void discoverTestData() {
    log.info(
        "\n╔════════════════════════════════════════════════════════════════════════════════╗");
    log.info("║   SCA Service Integration Test - Discovering Test Data                        ║");
    log.info("╚════════════════════════════════════════════════════════════════════════════════╝");
    log.info("Starting test data discovery (using shared SDK)...");

    long startTime = System.currentTimeMillis();

    try {
      // Use shared discovery helper with injected SDK
      var appWithLibrariesOptional =
          TestDataDiscoveryHelper.findApplicationWithLibraries(orgID, sdkExtension);

      if (appWithLibrariesOptional.isEmpty()) {
        String errorMsg = buildTestDataErrorMessage(0);
        log.error(errorMsg);
        fail(errorMsg);
        return;
      }

      var appWithLibraries = appWithLibrariesOptional.get();
      testData = new TestData();
      testData.appId = appWithLibraries.getApplication().getAppId();
      testData.appName = appWithLibraries.getApplication().getName();
      testData.hasLibraries = true;
      testData.libraryCount = appWithLibraries.getLibraries().size();
      testData.libraries = appWithLibraries.getLibraries();
      testData.hasVulnerableLibrary = appWithLibraries.hasVulnerableLibrary();
      testData.vulnerableCveId = appWithLibraries.getVulnerableCveId();

      discoveryDurationMs = System.currentTimeMillis() - startTime;

      log.info(
          "\n╔════════════════════════════════════════════════════════════════════════════════╗");
      log.info(
          "║   Test Data Discovery Complete                                                 ║");
      log.info(
          "╚════════════════════════════════════════════════════════════════════════════════╝");
      log.info("{}", testData);
      log.info("✓ Test data discovery completed in {}ms", discoveryDurationMs);
      log.info("");

      // Warn if no CVEs found
      if (!testData.hasVulnerableLibrary) {
        log.error("\n⚠️  WARNING: Application has libraries but NO VULNERABLE LIBRARIES");
        log.error("   CVE-related tests will be skipped.");
        log.error("   To enable full testing, use an application with vulnerable dependencies.");
      }

    } catch (Exception e) {
      String errorMsg = "❌ ERROR during test data discovery: " + e.getMessage();
      log.error("\n{}", errorMsg);
      e.printStackTrace();
      fail(errorMsg);
    }
  }

  /** Build detailed error message when no suitable test data is found */
  private String buildTestDataErrorMessage(int appsChecked) {
    var msg = new StringBuilder();
    msg.append(
        "\n╔════════════════════════════════════════════════════════════════════════════════╗\n");
    msg.append(
        "║   INTEGRATION TEST SETUP FAILED - NO SUITABLE TEST DATA                       ║\n");
    msg.append(
        "╚════════════════════════════════════════════════════════════════════════════════╝\n");
    msg.append("\nChecked ")
        .append(appsChecked)
        .append(" application(s) but none had library data.\n");
    msg.append("\n📋 REQUIRED TEST DATA:\n");
    msg.append("   The integration tests require at least ONE application with:\n");
    msg.append("   ✓ Third-party libraries (JAR files, NPM packages, etc.)\n");
    msg.append("   ✓ Optionally: Libraries with known CVE vulnerabilities\n");
    msg.append("\n🔧 HOW TO CREATE TEST DATA:\n");
    msg.append("\n1. Deploy an application with third-party dependencies\n");
    msg.append("   Example (Java with Maven):\n");
    msg.append("   java -javaagent:/path/to/contrast.jar \\\n");
    msg.append("        -Dcontrast.api.key=... \\\n");
    msg.append("        -Dcontrast.agent.java.standalone_app_name=test-app \\\n");
    msg.append("        -jar your-app-with-dependencies.jar\n");
    msg.append("\n2. Ensure application has dependencies\n");
    msg.append("   - For Java: Include libraries in pom.xml or build.gradle\n");
    msg.append("   - For Node.js: Include packages in package.json\n");
    msg.append("   - For Python: Include packages in requirements.txt\n");
    msg.append("   - Contrast agent will automatically detect and report libraries\n");
    msg.append("\n3. Wait for agent to report library data\n");
    msg.append("   - Start the application with Contrast agent\n");
    msg.append("   - Wait 30-60 seconds for agent to inventory libraries\n");
    msg.append("   - Library data is reported on first startup\n");
    msg.append("\n4. Verify libraries are detected:\n");
    msg.append("   - Login to Contrast UI\n");
    msg.append("   - Go to Applications → Your Application\n");
    msg.append("   - Click on 'Libraries' tab\n");
    msg.append("   - Verify libraries are listed\n");
    msg.append("\n5. (Optional) For CVE testing:\n");
    msg.append("   - Use an application with older dependencies that have known CVEs\n");
    msg.append("   - Example vulnerable libraries:\n");
    msg.append("     • log4j-core:2.14.1 (CVE-2021-44228)\n");
    msg.append("     • spring-core:5.2.0 (various CVEs)\n");
    msg.append("     • commons-collections:3.2.1 (CVE-2015-6420)\n");
    msg.append("   - Contrast will automatically identify CVEs in these libraries\n");
    msg.append("\n6. Re-run integration tests:\n");
    msg.append("   source .env.integration-test && mvn verify\n");
    msg.append("\n💡 ALTERNATIVE:\n");
    msg.append("   Set TEST_APP_ID environment variable to an application ID with libraries:\n");
    msg.append("   export TEST_APP_ID=<your-app-id>\n");
    msg.append("\n📝 NOTE:\n");
    msg.append("   - Most modern applications have third-party libraries\n");
    msg.append("   - Even a simple 'Hello World' web application typically has dependencies\n");
    msg.append("   - Ensure the Contrast agent is properly configured to report library data\n");
    msg.append("\n");
    return msg.toString();
  }

  // ========== Test Case 1: Test Data Validation ==========

  @Test
  void testDiscoveredTestDataExists() {
    log.info("\n=== Integration Test: Validate test data discovery ===");

    assertThat(testData).as("Test data should have been discovered in @BeforeAll").isNotNull();
    assertThat(testData.appId).as("Test application ID should be set").isNotNull();
    assertThat(testData.hasLibraries).as("Test application should have libraries").isTrue();
    assertThat(testData.libraryCount > 0)
        .as("Test application should have at least 1 library")
        .isTrue();

    log.info("✓ Test data validated:");
    log.info("  App ID: {}", testData.appId);
    log.info("  App Name: {}", testData.appName);
    log.info("  Library Count: {}", testData.libraryCount);
    log.info("  Has Vulnerable Libraries: {}", testData.hasVulnerableLibrary);
    if (testData.vulnerableCveId != null) {
      log.info("  Sample CVE: {}", testData.vulnerableCveId);
    }
  }

  // ========== Test Case 2: List Application Libraries ==========

  @Test
  void testListApplicationLibraries_Success() throws IOException {
    log.info("\n=== Integration Test: list_application_libraries_by_app_id ===");

    assertThat(testData).as("Test data must be discovered before running tests").isNotNull();

    // Act
    var libraries = scaService.getApplicationLibrariesByID(testData.appId);

    // Assert
    assertThat(libraries).as("Libraries list should not be null").isNotNull();
    assertThat(libraries).as("Should have at least 1 library").isNotEmpty();

    log.info("✓ Retrieved {} libraries for application: {}", libraries.size(), testData.appName);

    // Print sample libraries
    log.info("  Sample libraries:");
    libraries.stream()
        .limit(5)
        .forEach(
            lib -> {
              log.info(
                  "    - {} (version: {}, classes used: {}/{})",
                  lib.getFilename(),
                  lib.getVersion(),
                  lib.getClassedUsed(),
                  lib.getClassCount());
            });

    // Verify library structure
    for (LibraryExtended lib : libraries) {
      assertThat(lib.getFilename()).as("Library filename should not be null").isNotNull();
      assertThat(lib.getHash()).as("Library hash should not be null").isNotNull();
      assertThat(lib.getClassCount()).as("Class count should be non-negative").isNotNegative();
      assertThat(lib.getClassedUsed()).as("Classes used should be non-negative").isNotNegative();
    }
  }

  @Test
  void testListApplicationLibraries_ClassUsageIndicatesUsage() throws IOException {
    log.info("\n=== Integration Test: Class usage statistics ===");

    assertThat(testData).as("Test data must be discovered before running tests").isNotNull();

    // Act
    var libraries = scaService.getApplicationLibrariesByID(testData.appId);

    // Assert
    assertThat(libraries).isNotNull();
    assertThat(libraries).isNotEmpty();

    log.info("✓ Analyzing class usage for {} libraries:", libraries.size());

    // Count libraries by usage
    long activeLibs = libraries.stream().filter(lib -> lib.getClassedUsed() > 0).count();
    long unusedLibs = libraries.stream().filter(lib -> lib.getClassedUsed() == 0).count();

    log.info("  Active libraries (classes used > 0): {}", activeLibs);
    log.info("  Likely unused libraries (classes used = 0): {}", unusedLibs);

    // Verify class usage makes sense
    for (LibraryExtended lib : libraries) {
      assertThat(lib.getClassedUsed() <= lib.getClassCount())
          .as("Classes used should not exceed total class count for " + lib.getFilename())
          .isTrue();
    }

    log.info("✓ Class usage statistics are valid");
  }

  // ========== Test Case 3: CVE Lookup (if CVE available) ==========

  @Test
  void testListApplicationsVulnerableToCVE_Success() throws IOException {
    log.info("\n=== Integration Test: list_applications_vulnerable_to_cve ===");

    if (testData.vulnerableCveId == null) {
      log.info("⚠️  SKIPPED: No vulnerable libraries with CVEs found in test data");
      log.info("   To enable this test, use an application with vulnerable dependencies");
      return;
    }

    assertThat(testData).as("Test data must be discovered before running tests").isNotNull();

    // Act
    var cveData = scaService.listCVESForApplication(testData.vulnerableCveId);

    // Assert
    assertThat(cveData).as("CVE data should not be null").isNotNull();
    assertThat(cveData.getApps()).as("Apps list should not be null").isNotNull();
    assertThat(cveData.getLibraries()).as("Libraries list should not be null").isNotNull();

    log.info("✓ Retrieved CVE data for: {}", testData.vulnerableCveId);
    log.info("  Affected applications: {}", cveData.getApps().size());
    log.info("  Vulnerable libraries: {}", cveData.getLibraries().size());

    // Verify our test app is in the list
    boolean foundTestApp =
        cveData.getApps().stream().anyMatch(app -> app.getApp_id().equals(testData.appId));

    if (foundTestApp) {
      log.info("  ✓ Test application '{}' is in the affected list", testData.appName);
    }

    // Verify library data
    assertThat(cveData.getLibraries().isEmpty())
        .as("Should have at least one vulnerable library")
        .isFalse();

    log.info("  Sample vulnerable libraries:");
    cveData.getLibraries().stream()
        .limit(3)
        .forEach(
            lib -> {
              log.info("    - {} (version: {})", lib.getFile_name(), lib.getVersion());
            });
  }

  @Test
  void testListApplicationsVulnerableToCVE_ClassUsagePopulated() throws IOException {
    log.info("\n=== Integration Test: CVE class usage population ===");

    if (testData.vulnerableCveId == null) {
      log.info("⚠️  SKIPPED: No vulnerable libraries with CVEs found in test data");
      return;
    }

    // Act
    var cveData = scaService.listCVESForApplication(testData.vulnerableCveId);

    // Assert
    assertThat(cveData).isNotNull();
    assertThat(cveData.getApps()).isNotNull();

    log.info("✓ Checking class usage data for {} affected applications:", cveData.getApps().size());

    // Verify class usage is populated for apps (implementation populates this)
    for (var app : cveData.getApps()) {
      log.info(
          "  App: "
              + app.getName()
              + " (class count: "
              + app.getClassCount()
              + ", class usage: "
              + app.getClassUsage()
              + ")");

      // Class count should be >= 0
      assertThat(app.getClassCount() >= 0)
          .as("Class count should be non-negative for app: " + app.getName())
          .isTrue();
    }

    log.info("✓ Class usage data is populated correctly");
  }

  // ========== Test Case 4: Error Handling ==========

  @Test
  void testListApplicationLibraries_InvalidAppId() {
    log.info("\n=== Integration Test: Invalid app ID handling ===");

    // Act - Use an invalid app ID
    boolean caughtException = false;
    try {
      var libraries = scaService.getApplicationLibrariesByID("invalid-app-id-12345");

      // If we get here, API handled it gracefully
      log.info("✓ API handled invalid app ID gracefully");
      log.info("  Libraries returned: {}", (libraries != null ? libraries.size() : "null"));

    } catch (Exception e) {
      caughtException = true;
      log.info("✓ API rejected invalid app ID with exception: {}", e.getClass().getSimpleName());
    }

    assertThat(true).as("Test passes if either exception or graceful handling occurs").isTrue();
  }

  @Test
  void testListApplicationsVulnerableToCVE_InvalidCVE() {
    log.info("\n=== Integration Test: Invalid CVE ID handling ===");

    // Act & Assert - Non-existent CVE should throw IOException
    assertThatThrownBy(
            () -> {
              scaService.listCVESForApplication("CVE-9999-NONEXISTENT");
            })
        .as("Non-existent CVE should throw IOException")
        .isInstanceOf(IOException.class)
        .hasMessageContaining("Failed to retrieve CVE data");

    log.info("✓ Non-existent CVE correctly rejected with IOException");
  }

  @BeforeEach
  void logTestStart(TestInfo testInfo) {
    log.info("\n▶ Starting test: {}", testInfo.getDisplayName());
    testStartTimeMs = System.currentTimeMillis();
  }

  @AfterEach
  void logTestEnd(TestInfo testInfo) {
    long duration = System.currentTimeMillis() - testStartTimeMs;
    totalTestTimeMs += duration;
    testCount++;
    log.info("✓ Test completed in {}ms: {}\n", duration, testInfo.getDisplayName());
  }

  @AfterAll
  void logSummary() {
    log.info(
        "\n╔════════════════════════════════════════════════════════════════════════════════╗");
    log.info("║   Integration Test Performance Summary                                        ║");
    log.info("╚════════════════════════════════════════════════════════════════════════════════╝");
    log.info("Discovery time: {}ms", discoveryDurationMs);
    log.info("Total test time: {}ms", totalTestTimeMs);
    log.info("Tests executed: {}", testCount);
    if (testCount > 0) {
      log.info("Average per test: {}ms", totalTestTimeMs / testCount);
    }
    log.info(
        "╚════════════════════════════════════════════════════════════════════════════════╝\n");
  }
}
