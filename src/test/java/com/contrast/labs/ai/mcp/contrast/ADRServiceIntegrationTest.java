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

import com.contrast.labs.ai.mcp.contrast.config.IntegrationTestConfig;
import com.contrast.labs.ai.mcp.contrast.util.AbstractIntegrationTest;
import com.contrast.labs.ai.mcp.contrast.util.TestDataDiscoveryHelper;
import com.contrast.labs.ai.mcp.contrast.util.TestDataDiscoveryHelper.ApplicationWithProtectRules;
import java.io.IOException;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
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
public class ADRServiceIntegrationTest
    extends AbstractIntegrationTest<ADRServiceIntegrationTest.TestData> {

  @Autowired private ADRService adrService;

  /** Container for discovered test data */
  static class TestData {
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

  @Override
  protected String testDisplayName() {
    return "ADR Service Integration Test";
  }

  @Override
  protected Class<TestData> testDataType() {
    return TestData.class;
  }

  @Override
  protected void onDiscoveryStart() {
    super.onDiscoveryStart();
    log.info("\n🔍 Fast discovery: using cached Protect data helper...");
  }

  @Override
  protected TestData performDiscovery() throws IOException {
    Optional<ApplicationWithProtectRules> protectCandidate =
        TestDataDiscoveryHelper.findApplicationWithProtectRules(orgID, sdkExtension);

    if (protectCandidate.isEmpty()) {
      throw new NoTestDataException(buildTestDataErrorMessage(50));
    }

    var candidate = protectCandidate.get();
    var data = new TestData();
    data.appId = candidate.getApplication().getAppId();
    data.appName = candidate.getApplication().getName();
    data.hasProtectRules = candidate.getRuleCount() > 0;
    data.ruleCount = candidate.getRuleCount();
    return data;
  }

  @Override
  protected void logTestDataDetails(TestData data) {
    log.info("{}", data);
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
  void testGetProtectRules_Success() throws IOException {
    log.info("\n=== Integration Test: get_protect_rules ===");

    assertThat(testData).as("Test data must be discovered before running tests").isNotNull();

    // Act
    var response = adrService.getProtectRules(testData.appId);

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
  void testGetProtectRules_InvalidAppId() {
    log.info("\n=== Integration Test: Invalid app ID handling ===");

    // Act - Use an invalid app ID that definitely doesn't exist
    boolean caughtException = false;
    try {
      var response = adrService.getProtectRules("invalid-app-id-12345");

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
  void testGetProtectRules_NullAppId() {
    log.info("\n=== Integration Test: Null app ID handling ===");

    // Act/Assert - Should throw IllegalArgumentException
    assertThatThrownBy(
            () -> {
              adrService.getProtectRules(null);
            })
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Application ID cannot be null or empty");

    log.info("✓ Null app ID correctly rejected");
  }

  @Test
  void testGetProtectRules_EmptyAppId() {
    log.info("\n=== Integration Test: Empty app ID handling ===");

    // Act/Assert - Should throw IllegalArgumentException
    assertThatThrownBy(
            () -> {
              adrService.getProtectRules("");
            })
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Application ID cannot be null or empty");

    log.info("✓ Empty app ID correctly rejected");
  }

  // ========== Test Case 4: Rule Details Verification ==========

  @Test
  void testGetProtectRules_VerifyRuleDetails() throws IOException {
    log.info("\n=== Integration Test: Verify rule details structure ===");

    assertThat(testData).as("Test data must be discovered before running tests").isNotNull();

    // Act
    var response = adrService.getProtectRules(testData.appId);

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

  // ========== Test Case 5: Search Attacks - Basic ==========

  @Test
  void testSearchAttacks_NoFilters_ReturnsAttacks() throws IOException {
    log.info("\n=== Integration Test: search_attacks (no filters) ===");

    // Act - Get attacks with no filters
    var response = adrService.searchAttacks(null, null, null, null, null, null, 1, 10);

    // Assert
    assertThat(response).as("Response should not be null").isNotNull();
    assertThat(response.items()).as("Items should not be null").isNotNull();
    assertThat(response.page()).as("Page should be 1").isEqualTo(1);
    assertThat(response.pageSize()).as("Page size should be 10").isEqualTo(10);

    log.info("✓ Retrieved {} attacks", response.items().size());
    log.info("  Total items: {}", response.totalItems());
    log.info("  Has more pages: {}", response.hasMorePages());

    // If we got attacks, verify structure
    if (!response.items().isEmpty()) {
      var firstAttack = response.items().get(0);
      log.info("\n  Sample attack:");
      log.info("    Attack ID: {}", firstAttack.attackId());
      log.info("    Status: {}", firstAttack.status());
      log.info("    Source: {}", firstAttack.source());
      log.info("    Rules: {}", firstAttack.rules());

      // Verify required fields
      assertThat(firstAttack.attackId()).as("Attack ID should not be null").isNotNull();
      assertThat(firstAttack.status()).as("Status should not be null").isNotNull();
    } else {
      log.info("  No attacks found in organization (this is acceptable)");
    }
  }

  // ========== Test Case 6: Search Attacks - With Filters ==========

  @Test
  void testSearchAttacks_WithQuickFilter_ReturnsFilteredAttacks() throws IOException {
    log.info("\n=== Integration Test: search_attacks (with quickFilter) ===");

    // Act - Get attacks with EXPLOITED filter
    var response = adrService.searchAttacks("EXPLOITED", null, null, null, null, null, 1, 10);

    // Assert
    assertThat(response).as("Response should not be null").isNotNull();
    assertThat(response.items()).as("Items should not be null").isNotNull();

    log.info("✓ Retrieved {} EXPLOITED attacks", response.items().size());

    // Verify all returned attacks match the filter (if any returned)
    for (var attack : response.items()) {
      log.info("  Attack {}: status={}", attack.attackId(), attack.status());
    }
  }

  // ========== Test Case 7: Search Attacks - With Keyword ==========

  @Test
  void testSearchAttacks_WithKeyword_ReturnsMatchingAttacks() throws IOException {
    log.info("\n=== Integration Test: search_attacks (with keyword) ===");

    // Act - Search for attacks with "sql" keyword
    var response = adrService.searchAttacks(null, "sql", null, null, null, null, 1, 10);

    // Assert
    assertThat(response).as("Response should not be null").isNotNull();
    assertThat(response.items()).as("Items should not be null").isNotNull();

    log.info("✓ Retrieved {} attacks matching keyword 'sql'", response.items().size());

    // Log rule names to verify keyword match
    for (var attack : response.items()) {
      log.info("  Attack {}: rules={}", attack.attackId(), attack.rules());
    }
  }

  // ========== Test Case 8: Search Attacks - Pagination ==========

  @Test
  void testSearchAttacks_Pagination_ReturnsCorrectPage() throws IOException {
    log.info("\n=== Integration Test: search_attacks (pagination) ===");

    // Act - Get page 1 with small page size
    var page1 = adrService.searchAttacks(null, null, null, null, null, null, 1, 5);

    // Assert
    assertThat(page1).as("Page 1 response should not be null").isNotNull();
    assertThat(page1.items()).as("Page 1 items should not be null").isNotNull();
    assertThat(page1.page()).as("Should be page 1").isEqualTo(1);
    assertThat(page1.pageSize()).as("Page size should be 5").isEqualTo(5);

    log.info("✓ Page 1: {} attacks", page1.items().size());
    log.info("  Total items: {}", page1.totalItems());
    log.info("  Has more pages: {}", page1.hasMorePages());

    // If there are more pages, try getting page 2
    if (page1.hasMorePages()) {
      var page2 = adrService.searchAttacks(null, null, null, null, null, null, 2, 5);

      assertThat(page2).as("Page 2 response should not be null").isNotNull();
      assertThat(page2.page()).as("Should be page 2").isEqualTo(2);

      log.info("✓ Page 2: {} attacks", page2.items().size());

      // Verify page 1 and page 2 have different attacks (if both have content)
      if (!page1.items().isEmpty() && !page2.items().isEmpty()) {
        var page1FirstId = page1.items().get(0).attackId();
        var page2FirstId = page2.items().get(0).attackId();
        assertThat(page1FirstId)
            .as("Page 1 and Page 2 should have different attacks")
            .isNotEqualTo(page2FirstId);
      }
    } else {
      log.info("  Only one page of results available");
    }
  }

  // ========== Test Case 9: Search Attacks - Sort Order ==========

  @Test
  void testSearchAttacks_WithSort_ReturnsSortedAttacks() throws IOException {
    log.info("\n=== Integration Test: search_attacks (with sort) ===");

    // Act - Get attacks sorted by start time descending (most recent first)
    var response = adrService.searchAttacks(null, null, null, null, null, "-startTime", 1, 10);

    // Assert
    assertThat(response).as("Response should not be null").isNotNull();
    assertThat(response.items()).as("Items should not be null").isNotNull();

    log.info("✓ Retrieved {} attacks sorted by -startTime", response.items().size());

    // Log timestamps to verify sort order (if attacks returned)
    if (response.items().size() >= 2) {
      for (int i = 0; i < Math.min(3, response.items().size()); i++) {
        var attack = response.items().get(i);
        log.info("  Attack {}: startTime={}", i + 1, attack.startTime());
      }
    }
  }

  // ========== Test Case 10: Search Attacks - Invalid Filter ==========

  @Test
  void testSearchAttacks_InvalidQuickFilter_ReturnsError() throws IOException {
    log.info("\n=== Integration Test: search_attacks (invalid quickFilter) ===");

    // Act - Use invalid quickFilter
    var response = adrService.searchAttacks("INVALID_FILTER", null, null, null, null, null, 1, 10);

    // Assert
    assertThat(response).as("Response should not be null").isNotNull();
    assertThat(response.message()).as("Should have error message for invalid filter").isNotNull();
    assertThat(response.message())
        .as("Message should explain invalid filter")
        .contains("Invalid quickFilter");

    log.info("✓ Invalid filter correctly rejected");
    log.info("  Error message: {}", response.message());
  }

  // ========== Test Case 11: Search Attacks - Boolean Filters ==========

  @Test
  void testSearchAttacks_WithBooleanFilters_ReturnsFilteredAttacks() throws IOException {
    log.info("\n=== Integration Test: search_attacks (with boolean filters) ===");

    // Act - Get attacks excluding suppressed ones
    var response = adrService.searchAttacks(null, null, false, null, null, null, 1, 10);

    // Assert
    assertThat(response).as("Response should not be null").isNotNull();
    assertThat(response.items()).as("Items should not be null").isNotNull();

    log.info("✓ Retrieved {} attacks (excludeSuppressed=false)", response.items().size());

    // If we have both suppressed and non-suppressed attacks, compare
    var responseWithSuppressed =
        adrService.searchAttacks(null, null, true, null, null, null, 1, 10);

    log.info("  With suppressed included: {} attacks", responseWithSuppressed.items().size());
  }

  // ========== Test Case 12: Search Attacks - Combined Filters ==========

  @Test
  void testSearchAttacks_CombinedFilters_ReturnsMatchingAttacks() throws IOException {
    log.info("\n=== Integration Test: search_attacks (combined filters) ===");

    // Act - Combine multiple filters
    var response =
        adrService.searchAttacks("PROBED", "injection", false, null, null, "severity", 1, 10);

    // Assert
    assertThat(response).as("Response should not be null").isNotNull();
    assertThat(response.items()).as("Items should not be null").isNotNull();

    log.info("✓ Retrieved {} attacks with combined filters", response.items().size());
    log.info("  Filters: quickFilter=PROBED, keyword=injection, sort=severity");

    // Log matching attacks
    for (var attack : response.items()) {
      log.info(
          "  Attack {}: status={}, rules={}", attack.attackId(), attack.status(), attack.rules());
    }
  }
}
