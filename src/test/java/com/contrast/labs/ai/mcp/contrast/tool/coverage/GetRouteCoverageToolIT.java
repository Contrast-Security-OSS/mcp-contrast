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
package com.contrast.labs.ai.mcp.contrast.tool.coverage;

import static org.assertj.core.api.Assertions.assertThat;

import com.contrast.labs.ai.mcp.contrast.config.IntegrationTestConfig;
import com.contrast.labs.ai.mcp.contrast.result.RouteLight;
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
 * Integration test for GetRouteCoverageTool that validates route coverage data from real
 * TeamServer.
 *
 * <p>This test automatically discovers suitable test data by querying the Contrast API. It looks
 * for applications with route coverage, sessions, and session metadata.
 *
 * <p>This test only runs if CONTRAST_HOST_NAME environment variable is set.
 *
 * <p>Required environment variables:
 *
 * <ul>
 *   <li>CONTRAST_HOST_NAME (e.g., app.contrastsecurity.com)
 *   <li>CONTRAST_API_KEY
 *   <li>CONTRAST_SERVICE_KEY
 *   <li>CONTRAST_USERNAME
 *   <li>CONTRAST_ORG_ID
 * </ul>
 *
 * <p>Run locally: source .env.integration-test && mvn verify
 */
@Slf4j
@SpringBootTest
@Import(IntegrationTestConfig.class)
@EnabledIfEnvironmentVariable(named = "CONTRAST_HOST_NAME", matches = ".+")
public class GetRouteCoverageToolIT
    extends AbstractIntegrationTest<GetRouteCoverageToolIT.TestData> {

  @Autowired private GetRouteCoverageTool getRouteCoverageTool;

  /** Container for discovered test data. */
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
    return "GetRouteCoverageTool Integration Test";
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
        TestDataDiscoveryHelper.findApplicationWithRouteCoverage(orgId, sdkExtension);

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
    msg.append("\n");
    return msg.toString();
  }

  // ========== Validation tests ==========

  @Test
  void getRouteCoverage_should_return_validation_error_for_missing_appId() {
    var result = getRouteCoverageTool.getRouteCoverage(null, null, null, null);

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors()).anyMatch(e -> e.contains("appId") && e.contains("required"));
  }

  @Test
  void getRouteCoverage_should_return_validation_error_for_unpaired_metadata_name() {
    var result = getRouteCoverageTool.getRouteCoverage(testData.appId, "branch", null, null);

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors())
        .anyMatch(e -> e.contains("sessionMetadataValue") && e.contains("required"));
  }

  @Test
  void getRouteCoverage_should_return_validation_error_for_unpaired_metadata_value() {
    var result = getRouteCoverageTool.getRouteCoverage(testData.appId, null, "main", null);

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors())
        .anyMatch(e -> e.contains("sessionMetadataName") && e.contains("required"));
  }

  // ========== Unfiltered query tests ==========

  @Test
  void getRouteCoverage_should_retrieve_routes_unfiltered() {
    log.info("\n=== Integration Test: get_route_coverage (unfiltered) ===");

    var result = getRouteCoverageTool.getRouteCoverage(testData.appId, null, null, null);

    assertThat(result).as("Response should not be null").isNotNull();
    assertThat(result.isSuccess()).as("Response should indicate success").isTrue();
    assertThat(result.found()).as("Should find routes").isTrue();
    assertThat(result.data()).as("Data should not be null").isNotNull();
    assertThat(result.data().routes()).as("Routes should not be null").isNotNull();
    assertThat(result.data().routes().size() > 0).as("Should have at least 1 route").isTrue();

    log.info(
        "✓ Retrieved {} routes for application: {}",
        result.data().routes().size(),
        testData.appName);

    // Light response includes aggregate statistics
    log.info("  Exercised routes: {}", result.data().exercisedCount());
    log.info("  Discovered routes: {}", result.data().discoveredCount());
    log.info("  Coverage percent: {}%", result.data().coveragePercent());

    // Verify all routes have essential fields in light format
    for (RouteLight route : result.data().routes()) {
      assertThat(route.signature()).as("Route signature should not be null").isNotNull();
      assertThat(route.routeHash()).as("Route hash should not be null").isNotNull();
    }
  }

  // ========== Session metadata filter tests ==========

  @Test
  void getRouteCoverage_should_filter_by_session_metadata() {
    log.info("\n=== Integration Test: get_route_coverage (session metadata filter) ===");

    if (!testData.hasSessionMetadata) {
      log.warn("Skipping test - no session metadata available");
      return;
    }

    var result =
        getRouteCoverageTool.getRouteCoverage(
            testData.appId, testData.sessionMetadataName, testData.sessionMetadataValue, null);

    assertThat(result).as("Response should not be null").isNotNull();
    assertThat(result.isSuccess()).as("Response should indicate success").isTrue();
    assertThat(result.data()).as("Data should not be null").isNotNull();
    assertThat(result.data().routes()).as("Routes should not be null").isNotNull();

    log.info(
        "✓ Retrieved {} routes for application: {}",
        result.data().routes().size(),
        testData.appName);
    log.info(
        "  Filtered by session metadata: {}={}",
        testData.sessionMetadataName,
        testData.sessionMetadataValue);

    // Verify routes have essential fields in light format
    for (RouteLight route : result.data().routes()) {
      assertThat(route.signature())
          .as("Route signature should be present for filtered routes")
          .isNotNull();
    }
  }

  // ========== Latest session filter tests ==========

  @Test
  void getRouteCoverage_should_filter_by_latest_session() {
    log.info("\n=== Integration Test: get_route_coverage (latest session) ===");

    if (!testData.hasSessionMetadata) {
      log.warn("Skipping test - no session metadata available (required for latest session)");
      return;
    }

    var result = getRouteCoverageTool.getRouteCoverage(testData.appId, null, null, true);

    assertThat(result).as("Response should not be null").isNotNull();
    assertThat(result.isSuccess())
        .as("Response should indicate success. Application should have session metadata.")
        .isTrue();
    assertThat(result.data()).as("Data should not be null").isNotNull();
    assertThat(result.data().routes())
        .as("Routes should not be null when success is true")
        .isNotNull();

    log.info("✓ Retrieved {} routes from latest session", result.data().routes().size());
    log.info("  Application: {}", testData.appName);

    // Light response includes aggregate statistics
    log.info("  Exercised: {}", result.data().exercisedCount());
    log.info("  Discovered: {}", result.data().discoveredCount());

    // Verify all routes have essential fields in light format
    for (RouteLight route : result.data().routes()) {
      assertThat(route.signature())
          .as("Route signature should be present for latest session")
          .isNotNull();
    }
  }

  // ========== Empty string handling (MCP-OU8 bug fix) ==========

  @Test
  void getRouteCoverage_should_treat_empty_strings_as_null() {
    log.info("\n=== Integration Test: Empty string parameters (MCP-OU8 bug fix) ===");

    // Empty strings should be treated as null and trigger unfiltered query
    var result = getRouteCoverageTool.getRouteCoverage(testData.appId, "", "", false);

    assertThat(result).as("Response should not be null").isNotNull();
    assertThat(result.isSuccess()).as("Response should be successful").isTrue();

    log.info("✓ Response successful: {}", result.isSuccess());
    log.info("✓ Routes returned: {}", result.data().routes().size());

    // Should return routes (assuming the app has route coverage)
    if (testData.hasRouteCoverage) {
      assertThat(result.data().routes().size() > 0)
          .as(
              "Empty strings should return all routes (unfiltered query) when app has route"
                  + " coverage")
          .isTrue();
      log.info("✓ Routes found via unfiltered query (empty strings treated as null)");
    }
  }

  // ========== Error handling tests ==========

  @Test
  void getRouteCoverage_should_handle_invalid_appId_gracefully() {
    log.info("\n=== Integration Test: Invalid app ID handling ===");

    try {
      var result = getRouteCoverageTool.getRouteCoverage("invalid-app-id-12345", null, null, null);

      // Either not found or error is acceptable
      log.info("✓ API handled invalid app ID gracefully");
      log.info("  Success: {}, Found: {}", result.isSuccess(), result.found());

    } catch (Exception e) {
      log.info("✓ API rejected invalid app ID with error: {}", e.getClass().getSimpleName());
    }
  }

  @Test
  void getRouteCoverage_should_handle_nonexistent_metadata_gracefully() {
    log.info("\n=== Integration Test: Non-existent session metadata ===");

    // Use metadata name/value that definitely doesn't exist
    var result =
        getRouteCoverageTool.getRouteCoverage(
            testData.appId, "nonexistent-metadata-name-12345", "nonexistent-value-67890", null);

    assertThat(result).as("Response should not be null").isNotNull();
    assertThat(result.isSuccess())
        .as("Response should indicate success even with no matching sessions")
        .isTrue();

    log.info("✓ API handled non-existent metadata gracefully");
    log.info(
        "  Routes returned: {} (expected 0 for non-existent metadata)",
        result.data().routes().size());

    // With non-existent metadata, we expect 0 routes
    assertThat(result.data().routes().size())
        .as("Non-existent metadata should return 0 routes")
        .isEqualTo(0);
  }

  // ========== Route structure validation ==========

  @Test
  void getRouteCoverage_should_populate_route_fields_for_all_routes() {
    log.info("\n=== Integration Test: Route light structure validation ===");

    var result = getRouteCoverageTool.getRouteCoverage(testData.appId, null, null, null);

    assertThat(result).as("Response should not be null").isNotNull();
    assertThat(result.isSuccess()).as("Response should indicate success").isTrue();
    assertThat(result.data().routes().size() > 0)
        .as("Should have at least 1 route to validate")
        .isTrue();

    log.info("✓ Validating structure of {} routes", result.data().routes().size());

    // Validate structure of each route in light format
    for (RouteLight route : result.data().routes()) {
      // Route itself should have key fields
      assertThat(route.signature())
          .as("Route signature should be present")
          .isNotNull()
          .isNotEmpty();
      assertThat(route.routeHash()).as("Route hash should be present").isNotNull().isNotEmpty();

      // status field should be set
      assertThat(route.status()).as("Route status should be present").isNotNull();
    }

    // Verify aggregate statistics are computed
    assertThat(result.data().totalRoutes()).as("Total routes should be set").isGreaterThan(0);
    assertThat(result.data().exercisedCount() + result.data().discoveredCount())
        .as("Exercised + discovered should equal total")
        .isEqualTo(result.data().totalRoutes());

    log.info("✓ All {} routes have valid light structure", result.data().routes().size());
  }

  // ========== Comparison test ==========

  @Test
  void getRouteCoverage_should_return_same_or_more_routes_unfiltered_vs_filtered() {
    log.info("\n=== Integration Test: Compare different filter types ===");

    if (!testData.hasSessionMetadata) {
      log.warn("Skipping test - no session metadata available");
      return;
    }

    // Get route coverage using different filters
    var unfilteredResult = getRouteCoverageTool.getRouteCoverage(testData.appId, null, null, null);

    var sessionMetadataResult =
        getRouteCoverageTool.getRouteCoverage(
            testData.appId, testData.sessionMetadataName, testData.sessionMetadataValue, null);

    var latestSessionResult =
        getRouteCoverageTool.getRouteCoverage(testData.appId, null, null, true);

    // Assert all methods returned data
    assertThat(unfilteredResult.isSuccess()).as("Unfiltered query should succeed").isTrue();
    assertThat(sessionMetadataResult.isSuccess())
        .as("Session metadata query should succeed")
        .isTrue();
    assertThat(latestSessionResult.isSuccess()).as("Latest session query should succeed").isTrue();

    log.info("✓ All filter types work correctly:");
    log.info("  Unfiltered routes:        {}", unfilteredResult.data().routes().size());
    log.info("  Session metadata routes:  {}", sessionMetadataResult.data().routes().size());
    log.info("  Latest session routes:    {}", latestSessionResult.data().routes().size());

    // Verify unfiltered should have >= filtered results
    assertThat(
            unfilteredResult.data().routes().size() >= sessionMetadataResult.data().routes().size())
        .as("Unfiltered query should return same or more routes than filtered query")
        .isTrue();
  }
}
