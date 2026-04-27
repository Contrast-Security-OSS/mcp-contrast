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
package com.contrast.labs.ai.mcp.contrast.tool.library;

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

  // CVSS v3 scores range from 0.0 to 10.0 inclusive.
  private static final double MIN_CVSS_SCORE = 0.0;
  private static final double MAX_CVSS_SCORE = 10.0;

  // Percentage fields in ImpactStats are reported on a 0–100 scale.
  private static final double MIN_PERCENTAGE = 0.0;
  private static final double MAX_PERCENTAGE = 100.0;

  // Shared substring for SingleTool's 5xx mapping — validation errors must never look like this.
  private static final String CONTRAST_API_ERROR = "Contrast API error";

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

  /**
   * Locates an application with libraries and, when possible, captures a real CVE ID exposed by one
   * of those libraries. The CVE ID is the only input that exercises the happy path of {@code
   * list_applications_by_cve}; without it the CVE-driven tests fail loudly with a precondition
   * message rather than masking the missing seed data. {@code hasVulnerableLibrary} drives the
   * {@code afterCacheHit}/{@code afterDiscovery} warning so the cause is visible in test logs.
   */
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
      log.warn("   CVE-related tests will fail loudly to surface missing seeded data.");
      log.warn("   To enable full testing, use an application with vulnerable dependencies.");
    }
  }

  @Test
  void testDiscoveredTestDataExists() {
    assertThat(testData).as("discovery must populate test data").isNotNull();
    assertThat(testData.appId).as("discovery must resolve a non-blank app ID").isNotBlank();
    assertThat(testData.appName).as("discovery must resolve a non-blank app name").isNotBlank();
    if (testData.hasVulnerableLibrary) {
      assertThat(testData.vulnerableCveId)
          .as("hasVulnerableLibrary=true must yield a non-blank CVE ID")
          .isNotBlank();
    }
  }

  @Test
  void listApplicationsByCve_should_return_cve_data() {
    assertThat(testData.vulnerableCveId)
        .as(
            "requires seeded app with at least one vulnerable library exposing a CVE — "
                + "see INTEGRATION_TESTS.md")
        .isNotBlank();

    var result = tool.listApplicationsByCve(testData.vulnerableCveId);

    assertThat(result.errors()).as("valid CVE must not produce errors").isEmpty();
    assertThat(result.isSuccess()).as("response must be successful").isTrue();
    assertThat(result.found()).as("known CVE must be found").isTrue();
    assertThat(result.data()).as("data must be populated on success").isNotNull();
    assertThat(result.data().getApps())
        .as("known-vulnerable CVE must return at least one impacted app")
        .isNotEmpty();
    assertThat(result.data().getLibraries())
        .as("known-vulnerable CVE must return at least one vulnerable library")
        .isNotEmpty();
    assertThat(result.data().getCve()).as("response must carry the queried CVE record").isNotNull();
    assertThat(result.data().getCve().getName())
        .as("response CVE name must echo the queried CVE")
        .isEqualTo(testData.vulnerableCveId);
  }

  @Test
  void listApplicationsByCve_should_populate_cve_metadata() {
    assertThat(testData.vulnerableCveId)
        .as("requires seeded app with vulnerable CVE — see INTEGRATION_TESTS.md")
        .isNotBlank();

    var result = tool.listApplicationsByCve(testData.vulnerableCveId);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.data()).isNotNull();

    var cve = result.data().getCve();
    assertThat(cve).as("cve metadata must be populated").isNotNull();
    assertThat(cve.getName()).as("cve.name must echo the queried CVE").isNotBlank();
    assertThat(cve.getDescription())
        .as("cve.description must be populated for a known CVE")
        .isNotBlank();
    assertThat(cve.getScore())
        .as("cve.score must be within CVSS range [%s, %s]", MIN_CVSS_SCORE, MAX_CVSS_SCORE)
        .isBetween(MIN_CVSS_SCORE, MAX_CVSS_SCORE);
  }

  @Test
  void listApplicationsByCve_should_populate_impact_stats() {
    assertThat(testData.vulnerableCveId)
        .as("requires seeded app with vulnerable CVE — see INTEGRATION_TESTS.md")
        .isNotBlank();

    var result = tool.listApplicationsByCve(testData.vulnerableCveId);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.data()).isNotNull();

    var stats = result.data().getImpactStats();
    assertThat(stats).as("impactStats must be populated").isNotNull();
    assertThat(stats.getImpactedAppCount())
        .as("impactedAppCount must be non-negative")
        .isNotNegative();
    assertThat(stats.getTotalAppCount()).as("totalAppCount must be non-negative").isNotNegative();
    assertThat(stats.getImpactedServerCount())
        .as("impactedServerCount must be non-negative")
        .isNotNegative();
    assertThat(stats.getTotalServerCount())
        .as("totalServerCount must be non-negative")
        .isNotNegative();
    assertThat(stats.getImpactedAppCount())
        .as("impactedAppCount cannot exceed totalAppCount")
        .isLessThanOrEqualTo(stats.getTotalAppCount());
    assertThat(stats.getImpactedServerCount())
        .as("impactedServerCount cannot exceed totalServerCount")
        .isLessThanOrEqualTo(stats.getTotalServerCount());
    assertThat(stats.getAppPercentage())
        .as("appPercentage must be in [%s, %s]", MIN_PERCENTAGE, MAX_PERCENTAGE)
        .isBetween(MIN_PERCENTAGE, MAX_PERCENTAGE);
    assertThat(stats.getServerPercentage())
        .as("serverPercentage must be in [%s, %s]", MIN_PERCENTAGE, MAX_PERCENTAGE)
        .isBetween(MIN_PERCENTAGE, MAX_PERCENTAGE);
  }

  @Test
  void listApplicationsByCve_should_expose_server_list() {
    assertThat(testData.vulnerableCveId)
        .as("requires seeded app with vulnerable CVE — see INTEGRATION_TESTS.md")
        .isNotBlank();

    var result = tool.listApplicationsByCve(testData.vulnerableCveId);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.data()).isNotNull();

    // The servers list is a documented response field — it must be present even if some
    // environments legitimately have no impacted servers. A null list would indicate a
    // deserialization or contract regression.
    assertThat(result.data().getServers())
        .as("servers list must be non-null (may be empty in minimal environments)")
        .isNotNull();
  }

  @Test
  void listApplicationsByCve_should_populate_app_fields() {
    assertThat(testData.vulnerableCveId)
        .as("requires seeded app with vulnerable CVE — see INTEGRATION_TESTS.md")
        .isNotBlank();

    var result = tool.listApplicationsByCve(testData.vulnerableCveId);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.data()).isNotNull();
    assertThat(result.data().getApps())
        .as("known-vulnerable CVE must return at least one impacted app")
        .isNotEmpty();

    // Every returned App must populate identity plus non-negative seen-timestamps. Enrichment
    // counters are validated separately in the class-usage test.
    //
    // lastSeen==0 is a documented sentinel for "app has never been observed running" (common
    // for SCA-only apps that are catalogued but have no runtime activity). Only enforce
    // lastSeen >= firstSeen when lastSeen is populated.
    assertThat(result.data().getApps())
        .as("every impacted app must populate identity and timestamp fields")
        .allSatisfy(
            app -> {
              assertThat(app.getAppId()).as("app.appId").isNotBlank();
              assertThat(app.getName()).as("app.name").isNotBlank();
              assertThat(app.getFirstSeen())
                  .as("%s.firstSeen must be non-negative", app.getAppId())
                  .isNotNegative();
              assertThat(app.getLastSeen())
                  .as("%s.lastSeen must be non-negative", app.getAppId())
                  .isNotNegative();
              if (app.getLastSeen() > 0) {
                assertThat(app.getLastSeen())
                    .as("%s.lastSeen must not precede firstSeen when populated", app.getAppId())
                    .isGreaterThanOrEqualTo(app.getFirstSeen());
              }
            });

    // Identity must actually be populated for every app — a real test of the payload, not just
    // shape. At least one app's firstSeen must be populated (non-zero) to prove the timestamp
    // field decodes end-to-end, not just defaults to 0.
    assertThat(result.data().getApps())
        .as("at least one impacted app must carry a populated firstSeen timestamp")
        .anyMatch(app -> app.getFirstSeen() > 0);
  }

  @Test
  void listApplicationsByCve_should_populate_class_usage() {
    assertThat(testData.vulnerableCveId)
        .as("requires seeded app with vulnerable CVE — see INTEGRATION_TESTS.md")
        .isNotBlank();

    var result = tool.listApplicationsByCve(testData.vulnerableCveId);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.data()).isNotNull();
    assertThat(result.data().getApps())
        .as("known-vulnerable CVE must return at least one impacted app")
        .isNotEmpty();

    // The enrichment step only sets classCount/classUsage when the app's library matches the
    // vulnerable library hash AND classesUsed > 0 — see ListApplicationsByCveTool#enrichApps.
    // Invariants that must hold for EVERY app regardless of whether enrichment fired:
    //   0 <= classUsage <= classCount
    assertThat(result.data().getApps())
        .as("every app must report classUsage within [0, classCount]")
        .allSatisfy(
            app -> {
              assertThat(app.getClassCount()).as("%s.classCount", app.getAppId()).isNotNegative();
              assertThat(app.getClassUsage()).as("%s.classUsage", app.getAppId()).isNotNegative();
              assertThat(app.getClassUsage())
                  .as("%s.classUsage must not exceed classCount", app.getAppId())
                  .isLessThanOrEqualTo(app.getClassCount());
            });

    // Enrichment observability: at least one app must have populated class usage, proving the
    // SDKHelper.getLibsForID lookup and hash-matching loop executed end-to-end against real
    // Contrast data. If this fails, the test org is missing apps that actively load any
    // vulnerable library's classes — see INTEGRATION_TESTS.md.
    assertThat(result.data().getApps())
        .as(
            "requires seeded app actively using the vulnerable library's classes so enrichment "
                + "fires — see INTEGRATION_TESTS.md")
        .anyMatch(app -> app.getClassCount() > 0 && app.getClassUsage() > 0);
  }

  @Test
  void listApplicationsByCve_should_handle_nonexistent_cve() {
    // Well-formatted but unknown CVE: the Contrast API returns an empty body, so Gson
    // deserializes to null CveData. ListApplicationsByCveTool#doExecute returns null for a
    // null payload, which SingleTool's pipeline converts to the canonical notFound response
    // (isSuccess=true, found=false, data=null, errors empty). Deterministic single-path
    // contract — no dual-path log-and-pass.
    var result = tool.listApplicationsByCve("CVE-9999-99999");

    assertThat(result.errors())
        .as("unknown CVE must not surface as an error (notFound path, not error path)")
        .isEmpty();
    assertThat(result.isSuccess())
        .as("notFound is a successful response: no errors produced")
        .isTrue();
    assertThat(result.found()).as("unknown CVE must not be found").isFalse();
    assertThat(result.data()).as("notFound response must not carry data").isNull();
    assertThat(result.errors())
        .as("unknown CVE must not surface as a generic 5xx API error")
        .noneMatch(e -> e.contains(CONTRAST_API_ERROR));
  }

  @Test
  void listApplicationsByCve_should_reject_invalid_cve_format() {
    var invalidCve = "not-a-cve";

    var result = tool.listApplicationsByCve(invalidCve);

    assertThat(result.isSuccess()).as("invalid CVE format must fail validation").isFalse();
    assertThat(result.found()).as("validation failure must not report found").isFalse();
    assertThat(result.data()).as("validation failure must not carry data").isNull();
    // Assert the full validation message shape, including the example and format spec. A bare
    // substring match on "cveId" would coincidentally pass because that's the parameter name.
    assertThat(result.errors())
        .as("validation error must describe required CVE format")
        .containsExactly(
            "cveId must be in CVE format (e.g., CVE-2021-44228). "
                + "Format: CVE-YYYY-NNNNN where YYYY is the year and NNNNN is a sequence number.");
    assertThat(result.errors())
        .as("validation error must not surface as a Contrast API error")
        .noneMatch(e -> e.contains(CONTRAST_API_ERROR));
  }

  @Test
  void listApplicationsByCve_should_reject_null_cve_id() {
    var result = tool.listApplicationsByCve(null);

    assertThat(result.isSuccess()).as("null CVE must fail validation").isFalse();
    assertThat(result.found()).as("validation failure must not report found").isFalse();
    assertThat(result.data()).as("validation failure must not carry data").isNull();
    assertThat(result.errors())
        .as("validation error must state cveId is required")
        .containsExactly("cveId is required");
    assertThat(result.errors())
        .as("validation error must not surface as a Contrast API error")
        .noneMatch(e -> e.contains(CONTRAST_API_ERROR));
  }

  @Test
  void listApplicationsByCve_should_reject_empty_cve_id() {
    var result = tool.listApplicationsByCve("");

    assertThat(result.isSuccess()).as("empty CVE must fail validation").isFalse();
    assertThat(result.found()).as("validation failure must not report found").isFalse();
    assertThat(result.data()).as("validation failure must not carry data").isNull();
    assertThat(result.errors())
        .as("validation error must state cveId is required")
        .containsExactly("cveId is required");
    assertThat(result.errors())
        .as("validation error must not surface as a Contrast API error")
        .noneMatch(e -> e.contains(CONTRAST_API_ERROR));
  }
}
