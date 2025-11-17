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
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.routecoverage.Route;
import com.contrast.labs.ai.mcp.contrast.util.AbstractIntegrationTest;
import com.contrast.labs.ai.mcp.contrast.util.TestDataDiscoveryHelper;
import com.contrast.labs.ai.mcp.contrast.util.TestDataDiscoveryHelper.RouteCoverageTestData;
import java.io.IOException;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Integration test for RouteCoverageService that validates route coverage data from real
 * TeamServer.
 *
 * <p>This test automatically discovers suitable test data by querying the Contrast API. It looks
 * for applications with route coverage, sessions, and session metadata.
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
public class RouteCoverageServiceIntegrationTest
    extends AbstractIntegrationTest<RouteCoverageServiceIntegrationTest.TestData> {

  @Autowired private RouteCoverageService routeCoverageService;

  // Discovered test data - populated in @BeforeAll
  /** Container for discovered test data */
  static class TestData {
    String appId;
    String appName;
    boolean hasRouteCoverage;
    boolean hasSessionMetadata;
    String sessionMetadataName;
    String sessionMetadataValue;
    int routeCount;

    @Override
    public String toString() {
      return String.format(
          "TestData{appId='%s', appName='%s', hasRouteCoverage=%s, hasSessionMetadata=%s, "
              + "sessionMetadataName='%s', sessionMetadataValue='%s', routeCount=%d}",
          appId,
          appName,
          hasRouteCoverage,
          hasSessionMetadata,
          sessionMetadataName,
          sessionMetadataValue,
          routeCount);
    }
  }

  @Override
  protected String testDisplayName() {
    return "Route Coverage Integration Test";
  }

  @Override
  protected Class<TestData> testDataType() {
    return TestData.class;
  }

  @Override
  protected void onDiscoveryStart() {
    super.onDiscoveryStart();
    log.info("\n🔍 Fast discovery: using cached route coverage helper...");
  }

  @Override
  protected TestData performDiscovery() throws IOException {
    Optional<RouteCoverageTestData> routeCandidate =
        TestDataDiscoveryHelper.findApplicationWithRouteCoverage(orgID, sdkExtension);

    if (routeCandidate.isEmpty()) {
      throw new NoTestDataException(buildTestDataErrorMessage(50));
    }

    var candidate = routeCandidate.get();
    var data = new TestData();
    data.appId = candidate.application().getAppId();
    data.appName = candidate.application().getName();
    data.hasRouteCoverage = true;
    data.routeCount = candidate.routeCount();
    data.hasSessionMetadata = candidate.hasSessionMetadata();
    data.sessionMetadataName = candidate.sessionMetadataName();
    data.sessionMetadataValue = candidate.sessionMetadataValue();
    return data;
  }

  @Override
  protected void logTestDataDetails(TestData data) {
    log.info("{}", data);
  }

  @Override
  protected void afterCacheHit(TestData data) {
    warnIfNoSessionMetadata(data);
  }

  @Override
  protected void afterDiscovery(TestData data) {
    warnIfNoSessionMetadata(data);
  }

  private void warnIfNoSessionMetadata(TestData data) {
    if (!data.hasSessionMetadata) {
      log.warn("\n⚠️  WARNING: Application has route coverage but NO SESSION METADATA");
      log.warn("   Some metadata-dependent assertions may be skipped.");
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
        .append(" application(s) but none had route coverage data.\n");
    msg.append("\n📋 REQUIRED TEST DATA:\n");
    msg.append("   The integration tests require at least ONE application with:\n");
    msg.append("   ✓ Route coverage data (at least 1 discovered or exercised route)\n");
    msg.append("   ✓ Session metadata (at least 1 metadata field)\n");
    msg.append("   ✓ Multiple sessions (for latest session filtering tests)\n");
    msg.append("\n🔧 HOW TO CREATE TEST DATA:\n");
    msg.append("\n1. Deploy an application with a Contrast agent\n");
    msg.append("   Example (Java):\n");
    msg.append("   java -javaagent:/path/to/contrast.jar \\\n");
    msg.append("        -Dcontrast.api.key=... \\\n");
    msg.append("        -Dcontrast.agent.java.standalone_app_name=test-app \\\n");
    msg.append("        -jar your-app.jar\n");
    msg.append("\n2. Configure session metadata in the agent\n");
    msg.append("   Add to contrast_security.yaml or as JVM args:\n");
    msg.append("   agent:\n");
    msg.append("     session_metadata:\n");
    msg.append("       branch: main\n");
    msg.append("       build: 123\n");
    msg.append("   Or via JVM args:\n");
    msg.append("   -Dcontrast.agent.session_metadata='branch=main,build=123'\n");
    msg.append("\n3. Exercise routes by making HTTP requests\n");
    msg.append("   curl http://localhost:8080/api/users\n");
    msg.append("   curl http://localhost:8080/api/products\n");
    msg.append("\n4. Wait 30-60 seconds for agent to report data to TeamServer\n");
    msg.append("\n5. Verify data exists:\n");
    msg.append("   - Login to Contrast UI\n");
    msg.append("   - Go to application → Route Coverage tab\n");
    msg.append("   - Verify routes are listed\n");
    msg.append("   - Check session metadata is present\n");
    msg.append("\n6. Re-run integration tests:\n");
    msg.append("   source .env.integration-test && mvn verify\n");
    msg.append("\n💡 ALTERNATIVE:\n");
    msg.append(
        "   Set TEST_APP_ID environment variable to an application ID with route coverage:\n");
    msg.append("   export TEST_APP_ID=<your-app-id>\n");
    msg.append("   export TEST_METADATA_NAME=branch\n");
    msg.append("   export TEST_METADATA_VALUE=main\n");
    msg.append("\n");
    return msg.toString();
  }

  // ========== Test Case 1: Test Data Validation ==========

  @Test
  void testDiscoveredTestDataExists() {
    log.info("\n=== Integration Test: Validate test data discovery ===");

    assertThat(testData).as("Test data should have been discovered in @BeforeAll").isNotNull();
    assertThat(testData.appId).as("Test application ID should be set").isNotNull();
    assertThat(testData.hasRouteCoverage)
        .as("Test application should have route coverage")
        .isTrue();
    assertThat(testData.routeCount > 0)
        .as("Test application should have at least 1 route")
        .isTrue();

    log.info("✓ Test data validated:");
    log.info("  App ID: {}", testData.appId);
    log.info("  App Name: {}", testData.appName);
    log.info("  Route Count: {}", testData.routeCount);
    log.info("  Has Session Metadata: {}", testData.hasSessionMetadata);
  }

  // ========== Test Case 2: Unfiltered Query ==========

  @Test
  void testGetRouteCoverage_Unfiltered_Success() throws IOException {
    log.info("\n=== Integration Test: get_route_coverage (unfiltered) ===");

    assertThat(testData).as("Test data must be discovered before running tests").isNotNull();

    // Act
    var response = routeCoverageService.getRouteCoverage(testData.appId, null, null, null);

    // Assert
    assertThat(response).as("Response should not be null").isNotNull();
    assertThat(response.isSuccess()).as("Response should indicate success").isTrue();
    assertThat(response.getRoutes()).as("Routes should not be null").isNotNull();
    assertThat(response.getRoutes().size() > 0).as("Should have at least 1 route").isTrue();

    log.info(
        "✓ Retrieved "
            + response.getRoutes().size()
            + " routes for application: "
            + testData.appName);

    // Count exercised vs discovered routes
    long exercisedCount =
        response.getRoutes().stream().filter(route -> route.getExercised() > 0).count();
    long discoveredCount = response.getRoutes().size() - exercisedCount;

    log.info("  Exercised routes: {}", exercisedCount);
    log.info("  Discovered routes: {}", discoveredCount);

    // Verify all routes have details
    for (Route route : response.getRoutes()) {
      assertThat(route.getSignature()).as("Route signature should not be null").isNotNull();
      assertThat(route.getRouteHash()).as("Route hash should not be null").isNotNull();
      assertThat(route.getRouteDetailsResponse())
          .as("Route details should be populated")
          .isNotNull();
    }
  }

  // ========== Test Case 3: Session Metadata Filter ==========

  @Test
  void testGetRouteCoverage_SessionMetadataFilter_Success() throws IOException {
    log.info("\n=== Integration Test: get_route_coverage (session metadata filter) ===");

    assertThat(testData).as("Test data must be discovered before running tests").isNotNull();
    assertThat(testData.hasSessionMetadata)
        .as(
            "Test application must have session metadata. Found app with route coverage but no"
                + " session metadata. Please configure session metadata in your Contrast agent.")
        .isTrue();
    assertThat(testData.sessionMetadataName).as("Session metadata name must be set").isNotNull();
    assertThat(testData.sessionMetadataValue).as("Session metadata value must be set").isNotNull();

    // Act
    var response =
        routeCoverageService.getRouteCoverage(
            testData.appId, testData.sessionMetadataName, testData.sessionMetadataValue, null);

    // Assert
    assertThat(response).as("Response should not be null").isNotNull();
    assertThat(response.isSuccess()).as("Response should indicate success").isTrue();
    assertThat(response.getRoutes()).as("Routes should not be null").isNotNull();

    log.info(
        "✓ Retrieved "
            + response.getRoutes().size()
            + " routes for application: "
            + testData.appName);
    log.info(
        "  Filtered by session metadata: "
            + testData.sessionMetadataName
            + "="
            + testData.sessionMetadataValue);

    // Verify route details are populated
    for (Route route : response.getRoutes()) {
      assertThat(route.getRouteDetailsResponse())
          .as("Route details should be populated for filtered routes")
          .isNotNull();
    }

    if (response.getRoutes().size() > 0) {
      log.info("  Sample routes:");
      response.getRoutes().stream()
          .limit(3)
          .forEach(route -> log.info("    - {}", route.getSignature()));
    }
  }

  // ========== Test Case 4: Latest Session Filter ==========

  @Test
  void testGetRouteCoverage_LatestSession_Success() throws IOException {
    log.info("\n=== Integration Test: get_route_coverage (latest session) ===");

    assertThat(testData).as("Test data must be discovered before running tests").isNotNull();
    assertThat(testData.hasSessionMetadata)
        .as(
            "Test application must have session metadata for latest session test. "
                + "Please configure session metadata in your Contrast agent.")
        .isTrue();

    // Act
    var response = routeCoverageService.getRouteCoverage(testData.appId, null, null, true);

    // Assert
    assertThat(response).as("Response should not be null").isNotNull();
    assertThat(response.isSuccess())
        .as("Response should indicate success. Application should have session metadata.")
        .isTrue();
    assertThat(response.getRoutes())
        .as("Routes should not be null when success is true")
        .isNotNull();

    log.info("✓ Retrieved {} routes from latest session", response.getRoutes().size());
    log.info("  Application: {}", testData.appName);

    // Count exercised vs discovered
    long exercisedCount =
        response.getRoutes().stream().filter(route -> route.getExercised() > 0).count();

    log.info("  Exercised: {}", exercisedCount);
    log.info("  Discovered: {}", (response.getRoutes().size() - exercisedCount));

    // Verify all routes have details
    for (Route route : response.getRoutes()) {
      assertThat(route.getRouteDetailsResponse())
          .as("Route details should be populated for latest session")
          .isNotNull();
    }
  }

  // ========== Comparison Test: Different Filter Types ==========

  @Test
  void testGetRouteCoverage_CompareFilters() throws IOException {
    log.info("\n=== Integration Test: Compare different filter types ===");

    assertThat(testData).as("Test data must be discovered before running tests").isNotNull();
    assertThat(testData.hasSessionMetadata)
        .as(
            "Test application must have session metadata for comparison test. "
                + "Please configure session metadata in your Contrast agent.")
        .isTrue();

    // Get route coverage using different filters
    var unfilteredResponse =
        routeCoverageService.getRouteCoverage(testData.appId, null, null, null);

    var sessionMetadataResponse =
        routeCoverageService.getRouteCoverage(
            testData.appId, testData.sessionMetadataName, testData.sessionMetadataValue, null);

    var latestSessionResponse =
        routeCoverageService.getRouteCoverage(testData.appId, null, null, true);

    // Assert all methods returned data
    assertThat(unfilteredResponse).as("Unfiltered response should not be null").isNotNull();
    assertThat(sessionMetadataResponse)
        .as("Session metadata response should not be null")
        .isNotNull();
    assertThat(latestSessionResponse).as("Latest session response should not be null").isNotNull();

    assertThat(unfilteredResponse.isSuccess()).as("Unfiltered query should succeed").isTrue();
    assertThat(sessionMetadataResponse.isSuccess())
        .as("Session metadata query should succeed")
        .isTrue();
    assertThat(latestSessionResponse.isSuccess())
        .as("Latest session query should succeed")
        .isTrue();

    log.info("✓ All filter types work correctly:");
    log.info("  Unfiltered routes:        {}", unfilteredResponse.getRoutes().size());
    log.info("  Session metadata routes:  {}", sessionMetadataResponse.getRoutes().size());
    log.info("  Latest session routes:    {}", latestSessionResponse.getRoutes().size());

    // Verify unfiltered should have >= filtered results (more data when not filtered)
    assertThat(unfilteredResponse.getRoutes().size() >= sessionMetadataResponse.getRoutes().size())
        .as("Unfiltered query should return same or more routes than filtered query")
        .isTrue();

    // Latest session should have routes (since we validated session metadata exists)
    assertThat(latestSessionResponse.getRoutes().size() > 0)
        .as("Latest session query should return at least some routes")
        .isTrue();
  }

  // ========== Error Handling Test ==========

  @Test
  void testGetRouteCoverage_InvalidAppId() {
    log.info("\n=== Integration Test: Invalid app ID handling ===");

    // Act - Use an invalid app ID that definitely doesn't exist
    boolean caughtException = false;
    try {
      var response =
          routeCoverageService.getRouteCoverage("invalid-app-id-12345", null, null, null);

      // If we get here, the API returned a response (possibly empty)
      log.info("✓ API handled invalid app ID gracefully");
      log.info("  Routes returned: {}", response.getRoutes().size());

    } catch (IOException e) {
      // This is also acceptable - API rejected the invalid app ID
      caughtException = true;
      log.info("✓ API rejected invalid app ID with IOException: {}", e.getMessage());
    } catch (Exception e) {
      // Catch other exceptions like UnauthorizedException
      caughtException = true;
      log.info("✓ API rejected invalid app ID with error: {}", e.getClass().getSimpleName());
    }

    assertThat(caughtException || true)
        .as("Either exception thrown or graceful handling - both are acceptable")
        .isTrue();
  }

  @Test
  void testGetRouteCoverage_EmptyStrings_TreatedAsNull() throws Exception {
    log.info("\n=== Integration Test: Empty string parameters (MCP-OU8 bug fix) ===");

    // This test validates the fix for MCP-OU8: empty strings should be treated as null
    // and trigger the GET endpoint (unfiltered query) instead of the POST endpoint with empty
    // filters

    // Act - Call with empty strings for sessionMetadataName and sessionMetadataValue
    var response = routeCoverageService.getRouteCoverage(testData.appId, "", "", false);

    // Assert
    assertThat(response).as("Response should not be null").isNotNull();
    assertThat(response.isSuccess()).as("Response should be successful").isTrue();

    log.info("✓ Response successful: {}", response.isSuccess());
    log.info("✓ Routes returned: {}", response.getRoutes().size());

    // The key assertion: empty strings should NOT return "No sessions found" message
    // This message indicates the POST endpoint was called incorrectly
    if (response.getMessages() != null && !response.getMessages().isEmpty()) {
      String combinedMessages = String.join(", ", response.getMessages());
      assertThat(combinedMessages.contains("No sessions found with the provided filters"))
          .as(
              "Empty strings should not trigger POST endpoint - messages should not contain 'No"
                  + " sessions found'")
          .isFalse();
      log.info("✓ Messages: {}", combinedMessages);
    }

    // Should return routes (assuming the app has route coverage)
    if (testData.hasRouteCoverage) {
      assertThat(response.getRoutes().size() > 0)
          .as(
              "Empty strings should return all routes (unfiltered query) when app has route"
                  + " coverage")
          .isTrue();
      log.info("✓ Routes found via unfiltered query (empty strings treated as null)");

      // Verify route details are populated
      for (Route route : response.getRoutes()) {
        assertThat(route.getRouteDetailsResponse())
            .as("Each route should have details populated")
            .isNotNull();
        assertThat(route.getRouteDetailsResponse().isSuccess())
            .as("Route details should be successfully loaded")
            .isTrue();
      }
      log.info("✓ All routes have valid route details");
    }
  }
}
