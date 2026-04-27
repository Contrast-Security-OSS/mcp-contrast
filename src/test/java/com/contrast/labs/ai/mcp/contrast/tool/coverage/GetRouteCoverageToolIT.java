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
import com.contrast.labs.ai.mcp.contrast.util.AbstractIntegrationTest;
import com.contrast.labs.ai.mcp.contrast.util.TestDataDiscoveryHelper;
import com.contrast.labs.ai.mcp.contrast.util.TestDataDiscoveryHelper.RouteCoverageTestData;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Integration tests for {@link GetRouteCoverageTool} verifying the contract across the SDK/Route
 * Coverage API boundary. Cited in {@code CLAUDE.md} → Integration Test Standards as a canonical
 * example for pagination + filter + edge cases; assertions here must remain mutation-resistant.
 *
 * <p>This test automatically discovers suitable test data by querying the Contrast API. It looks
 * for applications with route coverage, sessions, and session metadata.
 *
 * <p>Run locally: {@code source .env.integration-test && mvn verify}
 */
@Slf4j
@SpringBootTest
@Import(IntegrationTestConfig.class)
@EnabledIfEnvironmentVariable(named = "CONTRAST_HOST_NAME", matches = ".+")
public class GetRouteCoverageToolIT
    extends AbstractIntegrationTest<GetRouteCoverageToolIT.TestData> {

  @Autowired private GetRouteCoverageTool getRouteCoverageTool;

  // SingleTool's 5xx mapping — validation errors must never look like this.
  private static final String CONTRAST_API_ERROR = "Contrast API error";

  // Exact validation messages produced by RouteCoverageParams. Asserting full message shape (not a
  // parameter-name substring) prevents a false-positive match against any unrelated error.
  private static final String APP_ID_REQUIRED_ERROR = "appId is required";
  private static final String METADATA_VALUE_REQUIRED_ERROR =
      "sessionMetadataValue is required when sessionMetadataName is provided";
  private static final String METADATA_NAME_REQUIRED_ERROR =
      "sessionMetadataName is required when sessionMetadataValue is provided";

  // Mutually-exclusive-filter warning emitted by RouteCoverageParams when both useLatestSession and
  // session metadata filters are supplied together. The tool's contract documents that
  // useLatestSession takes precedence — this test verifies the user is told.
  private static final String MUTUALLY_EXCLUSIVE_FILTERS_WARNING =
      "Both useLatestSession and sessionMetadataName provided - "
          + "useLatestSession takes precedence and sessionMetadata filter will be ignored";

  // Route status enumeration — see RouteLight javadoc and RouteMapper#toResponseLight, which
  // partitions routes into exercised/discovered counts using these labels (case-insensitive).
  private static final Set<String> KNOWN_ROUTE_STATUSES = Set.of("DISCOVERED", "EXERCISED");

  // Environment enumeration — see RouteLight javadoc: "Distinct environments where route has been
  // observed (DEVELOPMENT, QA, PRODUCTION)". An unknown value indicates either a new environment
  // (update this set) or an upstream regression.
  private static final Set<String> KNOWN_ENVIRONMENTS = Set.of("DEVELOPMENT", "QA", "PRODUCTION");

  // App ID values used by error-path tests. The "invalid" value should pass parameter validation
  // (non-blank) but fail to resolve to any real application; the "malformed metadata" values
  // exercise robustness against unusual but well-formed strings.
  private static final String INVALID_APP_ID = "invalid-app-id-12345";
  private static final String NONEXISTENT_METADATA_NAME = "nonexistent-metadata-name-zzz999";
  private static final String NONEXISTENT_METADATA_VALUE = "nonexistent-value-zzz999";
  private static final String MALFORMED_METADATA_NAME = "name with spaces and !@#$%^&*";
  private static final String MALFORMED_METADATA_VALUE = "value\nwith\tcontrol\rchars";

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

  /**
   * Locates an application that has discovered/exercised route coverage and, when possible, also
   * carries session metadata. {@code appId} + {@code routeCount} drive the happy-path retrieval and
   * pagination tests; {@code sessionMetadataName} / {@code sessionMetadataValue} drive the metadata
   * filter test, which would pass vacuously if the test app had no metadata at all.
   *
   * <p>Session metadata is best-effort: when missing, {@code afterCacheHit}/{@code afterDiscovery}
   * surface a clear warning and the metadata-dependent test fails loudly with a precondition
   * message rather than silently skipping.
   */
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
      log.warn("   Metadata-dependent tests will fail loudly with a clear precondition message.");
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
  void getRouteCoverage_should_return_validation_error_for_null_appId() {
    var response = getRouteCoverageTool.getRouteCoverage(null, null, null, null);

    assertThat(response).as("response must not be null").isNotNull();
    assertThat(response.isSuccess()).as("null appId must fail validation").isFalse();
    assertThat(response.data()).as("validation failure must not carry data").isNull();
    assertThat(response.found()).as("validation failure must not report found").isFalse();
    assertThat(response.errors())
        .as("errors must state appId is required with exact shape")
        .containsExactly(APP_ID_REQUIRED_ERROR);
    assertThat(response.errors())
        .as("must be a validation error, not a forwarded API error")
        .noneMatch(e -> e.contains(CONTRAST_API_ERROR))
        .noneMatch(e -> e.startsWith("Internal"));
  }

  @Test
  void getRouteCoverage_should_return_validation_error_for_empty_appId() {
    var response = getRouteCoverageTool.getRouteCoverage("", null, null, null);

    assertThat(response).as("response must not be null").isNotNull();
    assertThat(response.isSuccess()).as("empty appId must fail validation").isFalse();
    assertThat(response.data()).as("validation failure must not carry data").isNull();
    assertThat(response.errors())
        .as("errors must state appId is required with exact shape")
        .containsExactly(APP_ID_REQUIRED_ERROR);
    assertThat(response.errors())
        .as("must be a validation error, not a forwarded API error")
        .noneMatch(e -> e.contains(CONTRAST_API_ERROR));
  }

  @Test
  void getRouteCoverage_should_return_validation_error_for_whitespace_appId() {
    // ToolValidationContext#require uses StringUtils.hasText, which rejects whitespace as well as
    // null/empty. Without this test a regression that switched to isEmpty() would silently allow
    // whitespace strings to reach the SDK and surface as opaque API errors.
    var response = getRouteCoverageTool.getRouteCoverage("   ", null, null, null);

    assertThat(response).as("response must not be null").isNotNull();
    assertThat(response.isSuccess()).as("whitespace-only appId must fail validation").isFalse();
    assertThat(response.data()).as("validation failure must not carry data").isNull();
    assertThat(response.errors())
        .as("errors must state appId is required with exact shape")
        .containsExactly(APP_ID_REQUIRED_ERROR);
    assertThat(response.errors())
        .as("must be a validation error, not a forwarded API error")
        .noneMatch(e -> e.contains(CONTRAST_API_ERROR));
  }

  @Test
  void getRouteCoverage_should_return_validation_error_for_unpaired_metadata_name() {
    var response = getRouteCoverageTool.getRouteCoverage(testData.appId, "branch", null, null);

    assertThat(response.isSuccess()).as("unpaired metadata name must fail validation").isFalse();
    assertThat(response.data()).as("validation failure must not carry data").isNull();
    assertThat(response.errors())
        .as("errors must state sessionMetadataValue is required with exact shape")
        .containsExactly(METADATA_VALUE_REQUIRED_ERROR);
    assertThat(response.errors())
        .as("must be a validation error, not a forwarded API error")
        .noneMatch(e -> e.contains(CONTRAST_API_ERROR));
  }

  @Test
  void getRouteCoverage_should_return_validation_error_for_unpaired_metadata_value() {
    var response = getRouteCoverageTool.getRouteCoverage(testData.appId, null, "main", null);

    assertThat(response.isSuccess()).as("unpaired metadata value must fail validation").isFalse();
    assertThat(response.data()).as("validation failure must not carry data").isNull();
    assertThat(response.errors())
        .as("errors must state sessionMetadataName is required with exact shape")
        .containsExactly(METADATA_NAME_REQUIRED_ERROR);
    assertThat(response.errors())
        .as("must be a validation error, not a forwarded API error")
        .noneMatch(e -> e.contains(CONTRAST_API_ERROR));
  }

  @Test
  void getRouteCoverage_should_emit_warning_when_both_filters_provided() {
    // Tool contract: useLatestSession takes precedence over session metadata. The tool must
    // surface this precedence to the caller via a warning so silent filter loss is impossible.
    assertThat(testData.hasSessionMetadata)
        .as("requires seeded session metadata on app %s — see INTEGRATION_TESTS.md", testData.appId)
        .isTrue();

    var response =
        getRouteCoverageTool.getRouteCoverage(
            testData.appId, testData.sessionMetadataName, testData.sessionMetadataValue, true);

    assertThat(response.isSuccess())
        .as("query should succeed — useLatestSession path takes precedence")
        .isTrue();
    assertThat(response.warnings())
        .as("must warn that the metadata filter is silently superseded by useLatestSession")
        .contains(MUTUALLY_EXCLUSIVE_FILTERS_WARNING);
  }

  // ========== Successful query tests ==========

  @Test
  void getRouteCoverage_should_retrieve_routes_unfiltered() {
    assertThat(testData.routeCount)
        .as("requires seeded route coverage on app %s — see INTEGRATION_TESTS.md", testData.appId)
        .isPositive();

    var response = getRouteCoverageTool.getRouteCoverage(testData.appId, null, null, null);

    assertThat(response).as("response must not be null").isNotNull();
    assertThat(response.isSuccess()).as("unfiltered query must succeed").isTrue();
    assertThat(response.found()).as("seeded app must be reported as found").isTrue();
    assertThat(response.data()).as("successful response must carry data").isNotNull();
    assertThat(response.data().routes()).as("routes must be populated").isNotEmpty();

    // Aggregate statistics are computed by RouteMapper from per-route status — assert the invariant
    // that every route is partitioned into exactly one of (exercised, discovered).
    assertThat(response.data().totalRoutes())
        .as("totalRoutes must equal exercisedCount + discoveredCount")
        .isEqualTo(response.data().exercisedCount() + response.data().discoveredCount());
    assertThat(response.data().totalRoutes())
        .as("totalRoutes must equal routes.size()")
        .isEqualTo(response.data().routes().size());
  }

  @Test
  void getRouteCoverage_should_filter_by_session_metadata() {
    // Replaces the previous silent-skip anti-pattern (`if (!hasSessionMetadata) return`). Failing
    // loudly here surfaces config drift / data rot in CI instead of producing a green-but-empty
    // test report.
    assertThat(testData.hasSessionMetadata)
        .as(
            "requires seeded app with session metadata on app %s — see INTEGRATION_TESTS.md",
            testData.appId)
        .isTrue();

    var response =
        getRouteCoverageTool.getRouteCoverage(
            testData.appId, testData.sessionMetadataName, testData.sessionMetadataValue, null);

    assertThat(response).as("response must not be null").isNotNull();
    assertThat(response.isSuccess()).as("metadata-filtered query must succeed").isTrue();
    assertThat(response.data()).as("successful response must carry data").isNotNull();
    assertThat(response.data().routes()).as("filtered routes must not be null").isNotNull();

    // Filter assertion: every returned route must carry the essential identifiers populated by
    // RouteMapper. An empty signature here would indicate a downstream serialization regression
    // even if the count is non-zero.
    assertThat(response.data().routes())
        .as("every metadata-filtered route must populate signature and routeHash")
        .allSatisfy(
            route -> {
              assertThat(route.signature()).as("route.signature").isNotBlank();
              assertThat(route.routeHash()).as("route.routeHash").isNotBlank();
            });
  }

  @Test
  void getRouteCoverage_should_filter_by_latest_session() {
    // Replaces the previous silent-skip anti-pattern. Latest-session filter requires the app to
    // have at least one recorded agent session — without that there is nothing to assert against.
    assertThat(testData.hasSessionMetadata)
        .as(
            "requires seeded app with session metadata on app %s — see INTEGRATION_TESTS.md",
            testData.appId)
        .isTrue();

    var response = getRouteCoverageTool.getRouteCoverage(testData.appId, null, null, true);

    assertThat(response).as("response must not be null").isNotNull();
    assertThat(response.isSuccess())
        .as("latest-session query must succeed — app has session metadata")
        .isTrue();
    assertThat(response.found()).as("seeded app with sessions must be reported as found").isTrue();
    assertThat(response.data()).as("successful response must carry data").isNotNull();
    assertThat(response.data().routes()).as("latest-session routes must not be null").isNotNull();
    assertThat(response.data().routes())
        .as("every latest-session route must populate signature")
        .allSatisfy(route -> assertThat(route.signature()).isNotBlank());
  }

  // ========== Empty string handling (MCP-OU8 bug fix) ==========

  @Test
  void getRouteCoverage_should_treat_empty_strings_as_unfiltered() {
    // Replaces the previous dual-path test (`if (hasRouteCoverage)` conditional). RouteCoverage
    // params normalize empty strings to null, so passing "" for both metadata params must produce
    // exactly the same response shape as a true unfiltered query.
    assertThat(testData.routeCount)
        .as("requires seeded route coverage on app %s — see INTEGRATION_TESTS.md", testData.appId)
        .isPositive();

    var emptyStringResponse = getRouteCoverageTool.getRouteCoverage(testData.appId, "", "", false);
    var unfilteredResponse =
        getRouteCoverageTool.getRouteCoverage(testData.appId, null, null, null);

    assertThat(emptyStringResponse.isSuccess()).as("empty-string query must succeed").isTrue();
    assertThat(emptyStringResponse.errors())
        .as("empty strings must not produce paired-metadata validation errors")
        .isEmpty();
    assertThat(emptyStringResponse.data().routes())
        .as("empty-string query must return at least the same routes as unfiltered")
        .isNotEmpty();
    assertThat(emptyStringResponse.data().totalRoutes())
        .as("empty-string query must return identical totalRoutes to a true unfiltered query")
        .isEqualTo(unfilteredResponse.data().totalRoutes());
  }

  // ========== Error handling tests ==========

  @Test
  void getRouteCoverage_should_not_return_populated_data_for_invalid_appId() {
    // Single deterministic expectation: whether the API errors, returns an empty body, or maps to
    // a not-found response, the tool must never surface populated route data for a bogus app ID.
    // Replaces the previous try/catch anti-pattern that swallowed all exceptions silently.
    var response = getRouteCoverageTool.getRouteCoverage(INVALID_APP_ID, null, null, null);

    assertThat(response).as("response must not be null").isNotNull();
    assertThat(response.found()).as("invalid appId must not be marked as found").isFalse();
    assertThat(response.data()).as("invalid appId must not return populated route data").isNull();
  }

  @Test
  void getRouteCoverage_should_return_zero_routes_for_nonexistent_metadata() {
    // Replaces the previous test that passed if the tool returned empty routes regardless of
    // whether the metadata filter actually applied. Comparing nonexistent-filter vs unfiltered
    // proves the filter ran AND that the seeded app actually has routes (so a false-negative
    // empty-routes pass is impossible).
    assertThat(testData.routeCount)
        .as("requires seeded route coverage on app %s — see INTEGRATION_TESTS.md", testData.appId)
        .isPositive();

    var unfiltered = getRouteCoverageTool.getRouteCoverage(testData.appId, null, null, null);
    var nonexistent =
        getRouteCoverageTool.getRouteCoverage(
            testData.appId, NONEXISTENT_METADATA_NAME, NONEXISTENT_METADATA_VALUE, null);

    assertThat(unfiltered.isSuccess()).as("unfiltered baseline must succeed").isTrue();
    assertThat(unfiltered.data().routes())
        .as("unfiltered baseline must return at least one route to make the comparison meaningful")
        .isNotEmpty();
    assertThat(nonexistent.isSuccess())
        .as("nonexistent-metadata query must succeed (empty result, not error)")
        .isTrue();
    assertThat(nonexistent.data().routes())
        .as("nonexistent metadata must filter every route out")
        .isEmpty();
    assertThat(nonexistent.data().totalRoutes())
        .as("nonexistent-metadata totalRoutes must be 0 to match the empty routes list")
        .isZero();
  }

  @Test
  void getRouteCoverage_should_return_zero_routes_for_malformed_metadata() {
    // Distinct from the nonexistent-metadata test: this exercises robustness against unusual but
    // well-formed strings (whitespace, control chars, special characters) that the API receives,
    // serializes through JSON, and matches against. Tool must never throw or return populated data
    // for such input.
    assertThat(testData.routeCount)
        .as("requires seeded route coverage on app %s — see INTEGRATION_TESTS.md", testData.appId)
        .isPositive();

    var response =
        getRouteCoverageTool.getRouteCoverage(
            testData.appId, MALFORMED_METADATA_NAME, MALFORMED_METADATA_VALUE, null);

    assertThat(response).as("response must not be null").isNotNull();
    assertThat(response.isSuccess())
        .as("malformed metadata must not propagate as a Contrast API error")
        .isTrue();
    assertThat(response.data().routes())
        .as("malformed metadata must filter every route out")
        .isEmpty();
  }

  // ========== Route structure validation ==========

  @Test
  void getRouteCoverage_should_populate_all_route_fields_for_all_routes() {
    // RouteLight has 11 fields. Asserting every field guards against (a) a regression in
    // RouteMapper that drops a field, (b) an SDK shape change that returns null where a primitive
    // would silently default to 0, and (c) a Gson deserialization regression that drops nested
    // observation lists.
    assertThat(testData.routeCount)
        .as("requires seeded route coverage on app %s — see INTEGRATION_TESTS.md", testData.appId)
        .isPositive();

    var response = getRouteCoverageTool.getRouteCoverage(testData.appId, null, null, null);

    assertThat(response.isSuccess()).as("unfiltered query must succeed").isTrue();
    assertThat(response.data().routes()).as("must have at least 1 route to validate").isNotEmpty();

    assertThat(response.data().routes())
        .as("every route must populate the full RouteLight shape")
        .allSatisfy(
            route -> {
              // Strings — "Populate ≠ non-null": isNotBlank rejects an empty signature.
              assertThat(route.signature()).as("%s.signature", route.routeHash()).isNotBlank();
              assertThat(route.routeHash()).as("route.routeHash").isNotBlank();
              assertThat(route.status())
                  .as("%s.status must be a known route status", route.routeHash())
                  .isIn(KNOWN_ROUTE_STATUSES);

              // Collections — Compact constructor guarantees observations is non-null; assert that
              // explicit contract here so a future change cannot silently regress to null.
              assertThat(route.environments())
                  .as("%s.environments must not be null (List<String>)", route.routeHash())
                  .isNotNull();
              assertThat(route.observations())
                  .as("%s.observations must not be null (compact constructor)", route.routeHash())
                  .isNotNull();

              // Numeric counts — every route must report non-negative counts. criticalVulns is a
              // subset of vulns so the relationship must hold for every route.
              assertThat(route.vulnerabilities())
                  .as("%s.vulnerabilities", route.routeHash())
                  .isNotNegative();
              assertThat(route.criticalVulnerabilities())
                  .as("%s.criticalVulnerabilities", route.routeHash())
                  .isNotNegative()
                  .isLessThanOrEqualTo(route.vulnerabilities());
              assertThat(route.serversTotal())
                  .as("%s.serversTotal", route.routeHash())
                  .isNotNegative();

              // Timestamps — discovered is "Timestamp when route was first discovered (immutable)";
              // exercised is "0 if never". Both must be non-negative epoch ms.
              assertThat(route.discovered())
                  .as("%s.discovered (epoch ms)", route.routeHash())
                  .isNotNegative();
              assertThat(route.exercised())
                  .as("%s.exercised (epoch ms, 0 if never)", route.routeHash())
                  .isNotNegative();

              // totalObservations is Long (boxed) — assert non-null + non-negative. A null here
              // means the API shape changed or the mapper failed to copy the field.
              assertThat(route.totalObservations())
                  .as("%s.totalObservations", route.routeHash())
                  .isNotNull()
                  .isNotNegative();
            });
  }

  @Test
  void getRouteCoverage_should_only_use_known_environment_values() {
    // RouteLight.environments is documented as a closed set: {DEVELOPMENT, QA, PRODUCTION}. An
    // unknown value indicates either a newly introduced environment (update KNOWN_ENVIRONMENTS) or
    // a deserialization regression. Skipping this assertion would let a typo'd or unmapped enum
    // value silently propagate to AI agents.
    assertThat(testData.routeCount)
        .as("requires seeded route coverage on app %s — see INTEGRATION_TESTS.md", testData.appId)
        .isPositive();

    var response = getRouteCoverageTool.getRouteCoverage(testData.appId, null, null, null);
    assertThat(response.isSuccess()).as("unfiltered query must succeed").isTrue();

    assertThat(response.data().routes())
        .as("every route's environments must contain only known values %s", KNOWN_ENVIRONMENTS)
        .allSatisfy(
            route ->
                assertThat(route.environments())
                    .as("%s.environments", route.routeHash())
                    .allSatisfy(env -> assertThat(env).isIn(KNOWN_ENVIRONMENTS)));
  }

  @Test
  void getRouteCoverage_should_have_consistent_observation_counts() {
    // Per RouteLight contract: observations is the inline list (deduplicated by verb+url) and
    // totalObservations is the raw count. The list size cannot exceed the total count. A violation
    // here would indicate either truncation of totalObservations or duplication in observations.
    assertThat(testData.routeCount)
        .as("requires seeded route coverage on app %s — see INTEGRATION_TESTS.md", testData.appId)
        .isPositive();

    var response = getRouteCoverageTool.getRouteCoverage(testData.appId, null, null, null);
    assertThat(response.isSuccess()).as("unfiltered query must succeed").isTrue();

    assertThat(response.data().routes())
        .as("observations.size() must never exceed totalObservations for any route")
        .allSatisfy(
            route ->
                assertThat((long) route.observations().size())
                    .as(
                        "%s observations.size()=%d vs totalObservations=%d",
                        route.routeHash(), route.observations().size(), route.totalObservations())
                    .isLessThanOrEqualTo(route.totalObservations()));
  }

  // ========== Comparison test ==========

  @Test
  void getRouteCoverage_unfiltered_should_return_at_least_as_many_routes_as_either_filter() {
    // The previous version asserted unfiltered ≥ metadata-filtered but forgot the latest-session
    // case. Filters can only narrow the result, never broaden it — this invariant must hold for
    // both filter modes.
    assertThat(testData.hasSessionMetadata)
        .as(
            "requires seeded app with session metadata on app %s — see INTEGRATION_TESTS.md",
            testData.appId)
        .isTrue();
    assertThat(testData.routeCount)
        .as("requires seeded route coverage on app %s — see INTEGRATION_TESTS.md", testData.appId)
        .isPositive();

    var unfiltered = getRouteCoverageTool.getRouteCoverage(testData.appId, null, null, null);
    var sessionMetadata =
        getRouteCoverageTool.getRouteCoverage(
            testData.appId, testData.sessionMetadataName, testData.sessionMetadataValue, null);
    var latestSession = getRouteCoverageTool.getRouteCoverage(testData.appId, null, null, true);

    assertThat(unfiltered.isSuccess()).as("unfiltered query must succeed").isTrue();
    assertThat(sessionMetadata.isSuccess()).as("session-metadata query must succeed").isTrue();
    assertThat(latestSession.isSuccess()).as("latest-session query must succeed").isTrue();

    int unfilteredCount = unfiltered.data().routes().size();
    int sessionMetadataCount = sessionMetadata.data().routes().size();
    int latestSessionCount = latestSession.data().routes().size();

    assertThat(unfilteredCount)
        .as(
            "unfiltered (%d) must return at least as many routes as metadata filter (%d)",
            unfilteredCount, sessionMetadataCount)
        .isGreaterThanOrEqualTo(sessionMetadataCount);
    assertThat(unfilteredCount)
        .as(
            "unfiltered (%d) must return at least as many routes as latest-session filter (%d)",
            unfilteredCount, latestSessionCount)
        .isGreaterThanOrEqualTo(latestSessionCount);
  }
}
