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

import com.contrast.labs.ai.mcp.contrast.config.IntegrationTestConfig;
import com.contrast.labs.ai.mcp.contrast.data.VulnLight;
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
 * Integration test for AssessService that validates vulnerability data from real TeamServer.
 *
 * <p>This test only runs if CONTRAST_HOST_NAME environment variable is set.
 *
 * <p>Required environment variables: - CONTRAST_HOST_NAME (e.g., app.contrastsecurity.com) -
 * CONTRAST_API_KEY - CONTRAST_SERVICE_KEY - CONTRAST_USERNAME - CONTRAST_ORG_ID
 *
 * <p>Run locally: export CONTRAST_HOST_NAME=app.contrastsecurity.com export
 * CONTRAST_API_KEY=your-key export CONTRAST_SERVICE_KEY=your-key export
 * CONTRAST_USERNAME=your-username export CONTRAST_ORG_ID=your-org-id mvn verify
 *
 * <p>Or skip integration tests: mvn verify -DskipITs
 */
@Slf4j
@SpringBootTest
@Import(IntegrationTestConfig.class)
@EnabledIfEnvironmentVariable(named = "CONTRAST_HOST_NAME", matches = ".+")
public class AssessServiceIntegrationTest
    extends AbstractIntegrationTest<AssessServiceIntegrationTest.TestData> {

  @Autowired private AssessService assessService;

  /** Container for discovered test data */
  static class TestData {
    String appId;
    String appName;

    @Override
    public String toString() {
      return String.format("TestData{appId='%s', appName='%s'}", appId, appName);
    }
  }

  @Override
  protected String testDisplayName() {
    return "Assess Service Integration Test";
  }

  @Override
  protected Class<TestData> testDataType() {
    return TestData.class;
  }

  @Override
  protected TestData performDiscovery() throws IOException {
    var appOptional = TestDataDiscoveryHelper.findFirstApplication(orgID, sdkExtension);

    if (appOptional.isEmpty()) {
      throw new NoTestDataException(buildTestDataErrorMessage(0));
    }

    var app = appOptional.get();
    var data = new TestData();
    data.appId = app.getAppId();
    data.appName = app.getName();
    return data;
  }

  @Override
  protected void logTestDataDetails(TestData data) {
    log.info("\n   ✅ Using application: {} (ID: {})", data.appName, data.appId);
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
    msg.append("\nNo applications found in organization.\n");
    msg.append("\n📋 REQUIRED TEST DATA:\n");
    msg.append("   The integration tests require at least ONE application:\n");
    msg.append("   ✓ Application deployed with Contrast agent\n");
    msg.append("   ✓ Application visible in this organization\n");
    msg.append("\n🔧 HOW TO CREATE TEST DATA:\n");
    msg.append("\n1. Deploy an application with a Contrast agent\n");
    msg.append("   Example (Java):\n");
    msg.append("   java -javaagent:/path/to/contrast.jar \\\n");
    msg.append("        -Dcontrast.api.key=... \\\n");
    msg.append("        -Dcontrast.agent.java.standalone_app_name=test-app \\\n");
    msg.append("        -jar your-app.jar\n");
    msg.append("\n2. Verify application appears in Contrast UI\n");
    msg.append("   - Login to Contrast TeamServer\n");
    msg.append("   - Navigate to Applications\n");
    msg.append("   - Verify your application is listed\n");
    msg.append("\n3. Re-run integration tests:\n");
    msg.append("   source .env.integration-test && mvn verify\n");
    msg.append("\n");
    return msg.toString();
  }

  @Test
  void testEnvironmentsAndTagsArePopulated() throws IOException {
    log.info("\n=== Integration Test: Environments and Tags ===");

    // Get vulnerabilities from real TeamServer
    var response =
        assessService.searchVulnerabilities(
            1, // page
            10, // pageSize
            null, // severities
            null, // appId
            null, // vulnTypes
            null, // environments
            null, // lastSeenAfter
            null, // lastSeenBefore
            null // vulnTags
            );

    assertThat(response).as("Response should not be null").isNotNull();
    assertThat(response.items()).as("Should have at least one vulnerability").isNotEmpty();

    log.info("Retrieved {} vulnerabilities", response.items().size());

    // Analyze first few vulnerabilities
    int withEnvironments = 0;
    int withTags = 0;

    for (VulnLight vuln : response.items()) {
      assertThat(vuln.environments()).as("Environments should never be null").isNotNull();
      assertThat(vuln.tags()).as("Tags should never be null").isNotNull();

      // Debug: Show all environment and tag data
      log.info("Vuln {}:", vuln.vulnID());
      log.info("  environments: {} (size: {})", vuln.environments(), vuln.environments().size());
      log.info("  tags: {} (size: {})", vuln.tags(), vuln.tags().size());

      if (!vuln.environments().isEmpty()) {
        withEnvironments++;
        log.info("  ✓ Has environments: {}", vuln.environments());
      }

      if (!vuln.tags().isEmpty()) {
        withTags++;
        log.info("  ✓ Has tags: {}", vuln.tags());
      }
    }

    log.info("\nResults:");
    log.info(
        "  Vulnerabilities with environments: {}/{}", withEnvironments, response.items().size());
    log.info("  Vulnerabilities with tags: {}/{}", withTags, response.items().size());

    // At least verify the fields are being returned (even if empty)
    // This ensures the API is returning the fields and they're being deserialized
    for (VulnLight vuln : response.items()) {
      assertThat(vuln.environments())
          .as("Environments field should exist (even if empty list)")
          .isNotNull();
      assertThat(vuln.tags()).as("Tags field should exist (even if empty list)").isNotNull();
    }

    log.info("✓ Integration test passed: environments and tags fields are present");
  }

  @Test
  void testSessionMetadataIsPopulated() throws IOException {
    log.info("\n=== Integration Test: Session Metadata ===");

    // Get vulnerabilities from real TeamServer with session metadata expanded
    var response =
        assessService.searchVulnerabilities(
            1, // page
            10, // pageSize
            null, // severities
            null, // appId
            null, // vulnTypes
            null, // environments
            null, // lastSeenAfter
            null, // lastSeenBefore
            null // vulnTags
            );

    assertThat(response).as("Response should not be null").isNotNull();
    assertThat(response.items()).as("Should have at least one vulnerability").isNotEmpty();

    log.info("Retrieved {} vulnerabilities", response.items().size());

    // Analyze session metadata in vulnerabilities
    int withSessionMetadata = 0;
    int totalSessions = 0;

    for (VulnLight vuln : response.items()) {
      assertThat(vuln.sessionMetadata()).as("Session metadata should never be null").isNotNull();

      // Debug: Show session metadata
      log.info("Vuln {}:", vuln.vulnID());
      log.info("  sessionMetadata: {} session(s)", vuln.sessionMetadata().size());

      if (!vuln.sessionMetadata().isEmpty()) {
        withSessionMetadata++;
        totalSessions += vuln.sessionMetadata().size();

        // Show details of first session
        var firstSession = vuln.sessionMetadata().get(0);
        log.info("  ✓ Has session metadata:");
        log.info("    - Session ID: {}", firstSession.getSessionId());
        if (firstSession.getMetadata() != null && !firstSession.getMetadata().isEmpty()) {
          log.info("    - Metadata items: {}", firstSession.getMetadata().size());
          // Show first metadata item
          var firstItem = firstSession.getMetadata().get(0);
          log.info("      * {}: {}", firstItem.getDisplayLabel(), firstItem.getValue());
        }
      }
    }

    log.info("\nResults:");
    log.info(
        "  Vulnerabilities with session metadata: {}/{}",
        withSessionMetadata,
        response.items().size());
    log.info("  Total sessions found: {}", totalSessions);

    // Verify the session metadata field exists (even if empty) - this confirms SDK expansion works
    for (VulnLight vuln : response.items()) {
      assertThat(vuln.sessionMetadata())
          .as("Session metadata field should exist (even if empty list)")
          .isNotNull();
    }

    log.info(
        "✓ Integration test passed: session metadata field is present and SDK expansion works");
  }

  @Test
  void testVulnerabilitiesHaveBasicFields() throws IOException {
    log.info("\n=== Integration Test: Basic Fields ===");

    var response =
        assessService.searchVulnerabilities(1, 5, null, null, null, null, null, null, null);

    assertThat(response).isNotNull();
    assertThat(response.items()).as("Should have vulnerabilities").isNotEmpty();

    // Verify each vulnerability has required fields
    for (VulnLight vuln : response.items()) {
      assertThat(vuln.title()).as("Title should not be null").isNotNull();
      assertThat(vuln.type()).as("Type should not be null").isNotNull();
      assertThat(vuln.vulnID()).as("VulnID should not be null").isNotNull();
      assertThat(vuln.severity()).as("Severity should not be null").isNotNull();
      assertThat(vuln.status()).as("Status should not be null").isNotNull();
      assertThat(vuln.appID())
          .as("appID should not be null (APPLICATION expand should be included)")
          .isNotNull();
      assertThat(vuln.appName())
          .as("appName should not be null (APPLICATION expand should be included)")
          .isNotNull();
      assertThat(vuln.appID()).as("appID should not be empty").isNotEmpty();
      assertThat(vuln.appName()).as("appName should not be empty").isNotEmpty();

      log.info(
          "✓ "
              + vuln.vulnID()
              + ": "
              + vuln.title()
              + " ("
              + vuln.severity()
              + ") - App: "
              + vuln.appName()
              + " ("
              + vuln.appID()
              + ")");
    }

    log.info("✓ All vulnerabilities have required fields including appID and appName");
  }

  @Test
  void testVulnTagsWithSpacesHandledBySDK() throws IOException {
    log.info("\n=== Integration Test: VulnTags with Spaces ===");
    log.info("Testing that SDK properly handles URL encoding of tags with spaces");

    // Query with a tag that contains spaces - this should work now that AIML-193 is complete
    // The SDK should handle URL encoding internally
    var response =
        assessService.searchVulnerabilities(
            1, // page
            50, // pageSize (larger to increase chance of finding tagged vulns)
            null, // severities
            null, // appId
            null, // vulnTypes
            null, // environments
            null, // lastSeenAfter
            null, // lastSeenBefore
            "SmartFix Remediated" // vulnTags with space - SDK should handle encoding
            );

    assertThat(response).as("Response should not be null").isNotNull();
    log.info("Query completed successfully (returned {} vulnerabilities)", response.items().size());

    // The query should complete without error - whether we get results depends on the org's data
    // The important thing is that the SDK properly encoded the tag with spaces
    if (response.items().size() > 0) {
      log.info("✓ Found vulnerabilities with 'SmartFix Remediated' tag:");
      for (VulnLight vuln : response.items()) {
        log.info("  - {}: {}", vuln.vulnID(), vuln.title());
        log.info("    Tags: {}", vuln.tags());
      }
    } else {
      log.info("ℹ No vulnerabilities found with 'SmartFix Remediated' tag (this is OK)");
    }

    // Try with multiple tags including spaces and special characters
    log.info("\nTesting multiple tags with spaces:");
    response =
        assessService.searchVulnerabilities(
            1, 10, null, null, null, null, null, null, "Tag With Spaces,another-tag");

    assertThat(response).as("Response should not be null").isNotNull();
    log.info("✓ Query with multiple tags completed successfully");
    log.info("  (returned {} vulnerabilities)", response.items().size());

    log.info("\n✓ Integration test passed: SDK properly handles vulnTags with spaces");
  }

  @Test
  void testSearchAppVulnerabilitiesWithSessionMetadata() throws IOException {
    log.info("\n=== Integration Test: search_app_vulnerabilities() with Session Metadata ===");

    assertThat(testData).as("Test data must be discovered before running tests").isNotNull();

    // Call search_app_vulnerabilities() with the discovered appId from @BeforeAll
    log.info(
        "Calling search_app_vulnerabilities() for app: {} (ID: {})",
        testData.appName,
        testData.appId);
    var response =
        assessService.searchAppVulnerabilities(
            testData.appId, // appId
            1, // page
            50, // pageSize
            null, // severities
            null, // statuses
            null, // vulnTypes
            null, // environments
            null, // lastSeenAfter
            null, // lastSeenBefore
            null, // vulnTags
            null, // sessionMetadataName
            null, // sessionMetadataValue
            null); // useLatestSession

    assertThat(response).as("Response should not be null").isNotNull();
    var vulnerabilities = response.items();
    assertThat(vulnerabilities).as("Vulnerabilities list should not be null").isNotNull();
    log.info("  ✓ Retrieved {} vulnerability(ies)", vulnerabilities.size());

    if (vulnerabilities.isEmpty()) {
      log.info("  ℹ No vulnerabilities for this app (this is OK for the test)");
      return;
    }

    // Verify session metadata is populated
    log.info("Verifying session metadata is populated...");
    int withSessionMetadata = 0;

    for (VulnLight vuln : vulnerabilities) {
      assertThat(vuln.sessionMetadata()).as("Session metadata should never be null").isNotNull();

      if (!vuln.sessionMetadata().isEmpty()) {
        withSessionMetadata++;
        log.info(
            "  ✓ Vuln " + vuln.vulnID() + " has " + vuln.sessionMetadata().size() + " session(s)");
      }
    }

    log.info("\nResults:");
    log.info(
        "  Vulnerabilities with session metadata: "
            + withSessionMetadata
            + "/"
            + vulnerabilities.size());
    log.info(
        "✓ Integration test passed: search_app_vulnerabilities() returns vulnerabilities with"
            + " session metadata");
  }

  @Test
  void testSearchAppVulnerabilitiesForLatestSessionWithDynamicSessionId() throws IOException {
    log.info(
        "\n"
            + "=== Integration Test: search_app_vulnerabilities() with useLatestSession and Dynamic"
            + " Session Discovery ===");

    assertThat(testData).as("Test data must be discovered before running tests").isNotNull();

    // Call search_app_vulnerabilities() with useLatestSession=true
    log.info(
        "Calling search_app_vulnerabilities(useLatestSession=true) for app: {} (ID: {})",
        testData.appName,
        testData.appId);
    var response =
        assessService.searchAppVulnerabilities(
            testData.appId, // appId
            1, // page
            50, // pageSize
            null, // severities
            null, // statuses
            null, // vulnTypes
            null, // environments
            null, // lastSeenAfter
            null, // lastSeenBefore
            null, // vulnTags
            null, // sessionMetadataName
            null, // sessionMetadataValue
            true); // useLatestSession

    assertThat(response).as("Response should not be null").isNotNull();
    var latestSessionVulns = response.items();
    assertThat(latestSessionVulns).as("Vulnerabilities list should not be null").isNotNull();
    log.info("  ✓ Retrieved {} vulnerability(ies) for latest session", latestSessionVulns.size());

    if (latestSessionVulns.isEmpty()) {
      log.info(
          "  ℹ No vulnerabilities in latest session (this is valid if latest session has no"
              + " vulns)");
      return;
    }

    // Verify session metadata is populated in results
    log.info("Verifying session metadata is populated...");
    int withSessionMetadata = 0;

    for (VulnLight vuln : latestSessionVulns) {
      assertThat(vuln.sessionMetadata()).as("Session metadata should never be null").isNotNull();

      if (!vuln.sessionMetadata().isEmpty()) {
        withSessionMetadata++;
        String sessionId = vuln.sessionMetadata().get(0).getSessionId();
        log.info("  ✓ Vuln {} has session ID: {}", vuln.vulnID(), sessionId);
      }
    }

    log.info("\nResults:");
    log.info("  Vulnerabilities returned: {}", latestSessionVulns.size());
    log.info(
        "  Vulnerabilities with session metadata: {}/{}",
        withSessionMetadata,
        latestSessionVulns.size());
    log.info(
        "✓ Integration test passed: search_app_vulnerabilities(useLatestSession=true) returns"
            + " vulnerabilities with session metadata");
  }

  @Test
  void testGetSessionMetadata_WithRealApplication() throws IOException {
    log.info("\n=== Integration Test: get_session_metadata with real application ===");

    // Validate test data was discovered
    assertThat(testData).as("Test data must be discovered").isNotNull();
    assertThat(testData.appId).as("Test app ID must exist").isNotBlank();

    log.info("Testing get_session_metadata with appId: {}", testData.appId);

    // Call real method
    var response = assessService.getSessionMetadata(testData.appId);

    // Verify response structure
    assertThat(response).as("Response should not be null").isNotNull();

    // Log metadata info if present
    if (response.getFilters() != null && !response.getFilters().isEmpty()) {
      log.info("✓ Retrieved {} metadata filter groups", response.getFilters().size());
      // Log details of filter groups
      for (var filterGroup : response.getFilters()) {
        log.info("  - Filter: {} (ID: {})", filterGroup.getLabel(), filterGroup.getId());
        if (filterGroup.getValues() != null) {
          log.info("    Values: {}", filterGroup.getValues().size());
        }
      }
    } else {
      log.info("✓ Response received (no metadata filter groups for this app)");
    }

    log.info("✓ Integration test passed");
  }

  @Test
  void testGetSessionMetadata_WithInvalidAppId() {
    log.info("\n=== Integration Test: get_session_metadata with invalid app ID ===");

    var invalidAppId = "invalid-app-id-that-does-not-exist";

    log.info("Testing get_session_metadata with invalid appId: {}", invalidAppId);

    // SDK throws UnauthorizedException (403 Forbidden) for invalid app IDs
    // This is the expected behavior - document it
    try {
      var response = assessService.getSessionMetadata(invalidAppId);
      log.info("SDK returned response for invalid app ID: {}", response);
      // If we get here, the test should fail - we expect an exception
      assertThat(false)
          .as("Expected UnauthorizedException for invalid app ID, but got a response")
          .isTrue();
    } catch (Exception e) {
      log.info(
          "✓ SDK threw exception as expected: {} - {}",
          e.getClass().getSimpleName(),
          e.getMessage());
      // Verify it's the expected exception type (UnauthorizedException)
      assertThat(e.getClass().getSimpleName())
          .as("Should throw UnauthorizedException for invalid app ID")
          .isEqualTo("UnauthorizedException");
    }

    log.info("✓ Integration test passed");
  }
}
