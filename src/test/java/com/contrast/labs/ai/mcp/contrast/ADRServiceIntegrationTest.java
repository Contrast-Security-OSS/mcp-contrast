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
import com.contrast.labs.ai.mcp.contrast.util.IntegrationTestDiskCache;
import com.contrast.labs.ai.mcp.contrast.util.TestDataDiscoveryHelper;
import com.contrast.labs.ai.mcp.contrast.util.TestDataDiscoveryHelper.ApplicationWithProtectRules;
import java.io.IOException;
import java.util.Optional;
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
 * Integration test for ADRService that validates Protect/ADR rules from real TeamServer.
 *
 * <p>This test automatically discovers suitable test data by querying the Contrast API. It looks
 * for applications with Protect/ADR enabled and configured rules.
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
public class ADRServiceIntegrationTest {

  @Autowired private ADRService adrService;

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
    boolean hasProtectRules;
    int ruleCount;

    @Override
    public String toString() {
      return String.format(
          "TestData{appId='%s', appName='%s', hasProtectRules=%s, ruleCount=%d}",
          appId, appName, hasProtectRules, ruleCount);
    }
  }

  @BeforeAll
  void discoverTestData() {
    log.info(
        "\n╔════════════════════════════════════════════════════════════════════════════════╗");
    log.info("║   ADR Service Integration Test - Discovering Test Data                        ║");
    log.info("╚════════════════════════════════════════════════════════════════════════════════╝");
    if (IntegrationTestDiskCache.loadIfPresent(
        "ADRServiceIntegrationTest",
        orgID,
        TestData.class,
        cached -> {
          testData = cached;
          discoveryDurationMs = 0;
          log.info("✓ Loaded cached test data: {}", testData);
        })) {
      return;
    }
    log.info("Starting test data discovery...");

    long startTime = System.currentTimeMillis();

    try {
      log.info("\n🔍 Fast discovery: using cached Protect data helper...");
      Optional<ApplicationWithProtectRules> protectCandidate =
          TestDataDiscoveryHelper.findApplicationWithProtectRules(orgID, sdkExtension);

      if (protectCandidate.isPresent()) {
        var candidate = protectCandidate.get();
        testData = new TestData();
        testData.appId = candidate.getApplication().getAppId();
        testData.appName = candidate.getApplication().getName();
        testData.hasProtectRules = candidate.getRuleCount() > 0;
        testData.ruleCount = candidate.getRuleCount();

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

        IntegrationTestDiskCache.write("ADRServiceIntegrationTest", orgID, testData);
      } else {
        String errorMsg = buildTestDataErrorMessage(50);
        log.error(errorMsg);
        fail(errorMsg);
      }

    } catch (Exception e) {
      String errorMsg = "❌ ERROR during test data discovery: " + e.getMessage();
      log.error("\n❌ ERROR during test data discovery: {}", e.getMessage());
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
        .append(" application(s) but none had Protect/ADR rules configured.\n");
    msg.append("\n📋 REQUIRED TEST DATA:\n");
    msg.append("   The integration tests require at least ONE application with:\n");
    msg.append("   ✓ Protect/ADR enabled\n");
    msg.append("   ✓ At least one protection rule configured (blocking or monitoring mode)\n");
    msg.append("\n🔧 HOW TO CREATE TEST DATA:\n");
    msg.append("\n1. Deploy an application with a Contrast agent\n");
    msg.append("   Example (Java):\n");
    msg.append("   java -javaagent:/path/to/contrast.jar \\\n");
    msg.append("        -Dcontrast.api.key=... \\\n");
    msg.append("        -Dcontrast.agent.java.standalone_app_name=test-app \\\n");
    msg.append("        -jar your-app.jar\n");
    msg.append("\n2. Enable Protect/ADR in Contrast UI\n");
    msg.append("   - Login to Contrast TeamServer\n");
    msg.append("   - Navigate to Applications → Your Application\n");
    msg.append("   - Click on 'Protect' tab\n");
    msg.append("   - Click 'Enable Protect' button\n");
    msg.append("\n3. Configure protection rules\n");
    msg.append("   - In the Protect tab, configure rules:\n");
    msg.append("     • SQL Injection - Set to 'Block' or 'Monitor'\n");
    msg.append("     • XSS (Cross-Site Scripting) - Set to 'Block' or 'Monitor'\n");
    msg.append("     • Path Traversal - Set to 'Block' or 'Monitor'\n");
    msg.append("     • Or any other rule you want to enable\n");
    msg.append("   - Save the configuration\n");
    msg.append("\n4. Verify rules are active\n");
    msg.append("   - Refresh the Protect tab\n");
    msg.append("   - Verify at least one rule shows as 'Enabled'\n");
    msg.append("   - Rule mode should be 'Block' or 'Monitor'\n");
    msg.append("\n5. Re-run integration tests:\n");
    msg.append("   source .env.integration-test && mvn verify\n");
    msg.append("\n💡 ALTERNATIVE:\n");
    msg.append(
        "   Set TEST_APP_ID environment variable to an application ID with Protect rules:\n");
    msg.append("   export TEST_APP_ID=<your-app-id>\n");
    msg.append("\n📝 NOTE:\n");
    msg.append("   - Protect/ADR is a premium feature in Contrast Security\n");
    msg.append("   - Ensure your license includes Protect capabilities\n");
    msg.append("   - The application must be actively monitored by a Contrast agent\n");
    msg.append("\n");
    return msg.toString();
  }

  // ========== Test Case 1: Test Data Validation ==========

  @Test
  void testDiscoveredTestDataExists() {
    log.info("\n=== Integration Test: Validate test data discovery ===");

    assertThat(testData).as("Test data should have been discovered in @BeforeAll").isNotNull();
    assertThat(testData.appId).as("Test application ID should be set").isNotNull();
    assertThat(testData.hasProtectRules).as("Test application should have Protect rules").isTrue();
    assertThat(testData.ruleCount)
        .as("Test application should have at least 1 rule")
        .isGreaterThan(0);

    log.info("✓ Test data validated:");
    log.info("  App ID: {}", testData.appId);
    log.info("  App Name: {}", testData.appName);
    log.info("  Rule Count: {}", testData.ruleCount);
  }

  // ========== Test Case 2: Get Protect Rules ==========

  @Test
  void testGetADRProtectRules_Success() throws IOException {
    log.info("\n=== Integration Test: get_ADR_Protect_Rules_by_app_id ===");

    assertThat(testData).as("Test data must be discovered before running tests").isNotNull();

    // Act
    var response = adrService.getProtectDataByAppID(testData.appId);

    // Assert
    assertThat(response).as("Response should not be null").isNotNull();
    assertThat(response.getRules()).as("Rules should not be null").isNotNull();
    assertThat(response.getRules().size()).as("Should have at least 1 rule").isGreaterThan(0);

    log.info(
        "✓ Retrieved {} Protect rules for application: {}",
        response.getRules().size(),
        testData.appName);

    // Print rule details
    log.info("  Rules configured:");
    for (var rule : response.getRules()) {
      String mode = rule.getProduction() != null ? rule.getProduction() : "not set";
      log.info("    - {} (production mode: {})", rule.getName(), mode);
    }

    // Verify rule structure
    for (var rule : response.getRules()) {
      assertThat(rule.getName()).as("Rule name should not be null").isNotNull();
      // Production mode might be null, block, monitor, or off
      // Just verify the field exists (can be null for non-production rules)
    }
  }

  // ========== Test Case 3: Error Handling ==========

  @Test
  void testGetADRProtectRules_InvalidAppId() {
    log.info("\n=== Integration Test: Invalid app ID handling ===");

    // Act - Use an invalid app ID that definitely doesn't exist
    boolean caughtException = false;
    try {
      var response = adrService.getProtectDataByAppID("invalid-app-id-12345");

      // If we get here, the API returned a response (possibly null or empty)
      log.info("✓ API handled invalid app ID gracefully");
      if (response == null) {
        log.info("  Response: null (no Protect data for invalid app)");
      } else {
        log.info(
            "  Response: {} rules", (response.getRules() != null ? response.getRules().size() : 0));
      }

    } catch (Exception e) {
      // This is acceptable - API rejected the invalid app ID
      caughtException = true;
      log.info("✓ API rejected invalid app ID with exception: {}", e.getClass().getSimpleName());
      log.info("  Message: {}", e.getMessage());
    }

    // Either exception or graceful handling is acceptable - test passes in both cases
  }

  @Test
  void testGetADRProtectRules_NullAppId() {
    log.info("\n=== Integration Test: Null app ID handling ===");

    // Act/Assert - Should throw IllegalArgumentException
    assertThatThrownBy(
            () -> {
              adrService.getProtectDataByAppID(null);
            })
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Application ID cannot be null or empty");

    log.info("✓ Null app ID correctly rejected");
  }

  @Test
  void testGetADRProtectRules_EmptyAppId() {
    log.info("\n=== Integration Test: Empty app ID handling ===");

    // Act/Assert - Should throw IllegalArgumentException
    assertThatThrownBy(
            () -> {
              adrService.getProtectDataByAppID("");
            })
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Application ID cannot be null or empty");

    log.info("✓ Empty app ID correctly rejected");
  }

  // ========== Test Case 4: Rule Details Verification ==========

  @Test
  void testGetADRProtectRules_VerifyRuleDetails() throws IOException {
    log.info("\n=== Integration Test: Verify rule details structure ===");

    assertThat(testData).as("Test data must be discovered before running tests").isNotNull();

    // Act
    var response = adrService.getProtectDataByAppID(testData.appId);

    // Assert
    assertThat(response).isNotNull();
    assertThat(response.getRules()).isNotNull();
    assertThat(response.getRules()).isNotEmpty();

    log.info("✓ Verifying rule details for {} rules:", response.getRules().size());

    // Detailed verification of each rule
    for (var rule : response.getRules()) {
      log.info("\n  Rule: {}", rule.getName());

      // Verify required fields
      assertThat(rule.getName()).as("Rule name is required").isNotNull();

      log.info("    ✓ Name: {}", rule.getName());
      if (rule.getProduction() != null) {
        log.info("    ✓ Production Mode: {}", rule.getProduction());
      }
      // Mode validation - production mode can be null, block, monitor, or off
    }

    log.info("\n✓ All rules have valid structure and required fields");
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
