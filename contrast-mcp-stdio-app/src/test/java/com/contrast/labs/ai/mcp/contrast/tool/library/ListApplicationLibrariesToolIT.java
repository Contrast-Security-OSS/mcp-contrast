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
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.LibraryExtended;
import com.contrast.labs.ai.mcp.contrast.util.AbstractIntegrationTest;
import com.contrast.labs.ai.mcp.contrast.util.TestDataDiscoveryHelper;
import com.contrastsecurity.http.RuleSeverity;
import java.io.IOException;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Integration tests for ListApplicationLibrariesTool.
 *
 * <p>Requires CONTRAST_HOST_NAME environment variable to be set.
 */
@Slf4j
@SpringBootTest
@Import(IntegrationTestConfig.class)
@EnabledIfEnvironmentVariable(named = "CONTRAST_HOST_NAME", matches = ".+")
class ListApplicationLibrariesToolIT
    extends AbstractIntegrationTest<ListApplicationLibrariesToolIT.TestData> {

  @Autowired private ListApplicationLibrariesTool tool;

  // Contrast library security grades are single letters A, B, C, D, F (no E).
  private static final Set<String> VALID_GRADES = Set.of("A", "B", "C", "D", "F");

  // Contrast tool-specific and shared pagination constants — keep in sync with
  // ValidationConstants.DEFAULT_PAGE_SIZE and API_MAX_PAGE_SIZE.
  private static final int DEFAULT_PAGE_SIZE = 50;
  private static final int API_MAX_PAGE_SIZE = 50;
  private static final int PAGINATION_PROBE_SIZE = 5;
  private static final int MIN_LIBS_FOR_PAGINATION = PAGINATION_PROBE_SIZE + 1;

  static class TestData {
    String appId;
    String appName;
    int expectedLibraryCount;

    @Override
    public String toString() {
      return String.format(
          "TestData{appId='%s', appName='%s', expectedLibraryCount=%d}",
          appId, appName, expectedLibraryCount);
    }
  }

  @Override
  protected String testDisplayName() {
    return "ListApplicationLibrariesTool Integration Test";
  }

  @Override
  protected Class<TestData> testDataType() {
    return TestData.class;
  }

  /**
   * Locates an application that has at least one library attached. The {@code appId} drives the
   * happy-path call to {@code list_application_libraries}; a library count of zero would let the
   * core {@code allSatisfy} library-shape assertions vacuously pass, so the test depends on a
   * positive count. {@code expectedLibraryCount} is captured for the discovery precondition test.
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
    data.expectedLibraryCount = result.getLibraries().size();
    return data;
  }

  @Override
  protected void logTestDataDetails(TestData data) {
    log.info("Test data: {}", data);
  }

  @Test
  void testDiscoveredTestDataExists() {
    assertThat(testData).as("discovery must populate test data").isNotNull();
    assertThat(testData.appId).as("discovery must resolve a non-blank app ID").isNotBlank();
    assertThat(testData.appName).as("discovery must resolve a non-blank app name").isNotBlank();
    assertThat(testData.expectedLibraryCount)
        .as("test application must have at least one library")
        .isPositive();
  }

  @Test
  void listApplicationLibraries_should_return_libraries() {
    var result = tool.listApplicationLibraries(null, null, testData.appId);

    assertThat(result.errors()).as("valid appId must not produce errors").isEmpty();
    assertThat(result.isSuccess()).as("response must be successful").isTrue();
    assertThat(result.items())
        .as("requires seeded libraries on app %s — see INTEGRATION_TESTS.md", testData.appName)
        .isNotEmpty();

    // Every library must populate the core identity fields, and each per-severity bucket must
    // lie within [0, totalVulnerabilities] — a negative count would indicate a sentinel leaking
    // through, and a count exceeding total would indicate a deserialization mix-up. Full
    // arithmetic consistency (sum == total) is asserted in the dedicated severity-counts test
    // against the vulnerabilities array, which avoids mixing API-provided and array-derived
    // sources. classCount/classesUsed arithmetic is covered in the class-usage test.
    assertThat(result.items())
        .as("every library must populate core identity and in-range vulnerability-count fields")
        .allSatisfy(
            lib -> {
              assertThat(lib.getFilename()).as("%s.filename", lib.getHash()).isNotBlank();
              assertThat(lib.getHash()).as("%s.hash", lib.getFilename()).isNotBlank();
              assertThat(lib.getVersion()).as("%s.version", lib.getFilename()).isNotBlank();
              int total = lib.getTotalVulnerabilities();
              assertThat(lib.getCriticalVulnerabilities())
                  .as(
                      "%s.criticalVulnerabilities must be in [0, total=%d]",
                      lib.getFilename(), total)
                  .isBetween(0, total);
              assertThat(lib.getHighVulnerabilities())
                  .as("%s.highVulnerabilities must be in [0, total=%d]", lib.getFilename(), total)
                  .isBetween(0, total);
              assertThat(lib.getMediumVulnerabilities())
                  .as("%s.mediumVulnerabilities must be in [0, total=%d]", lib.getFilename(), total)
                  .isBetween(0, total);
              assertThat(lib.getLowVulnerabilities())
                  .as("%s.lowVulnerabilities must be in [0, total=%d]", lib.getFilename(), total)
                  .isBetween(0, total);
              assertThat(lib.getNoteVulnerabilities())
                  .as("%s.noteVulnerabilities must be in [0, total=%d]", lib.getFilename(), total)
                  .isBetween(0, total);
            });
  }

  @Test
  void listApplicationLibraries_should_default_page_and_pageSize_when_null() {
    // Null page/pageSize must route through PaginationParams defaults — page=1, pageSize=50 —
    // with no pagination warnings. A change to the default contract would surface here.
    var result = tool.listApplicationLibraries(null, null, testData.appId);

    assertThat(result.isSuccess()).as("defaults must not fail validation").isTrue();
    assertThat(result.page()).as("null page must default to 1").isEqualTo(1);
    assertThat(result.pageSize())
        .as("null pageSize must default to %d", DEFAULT_PAGE_SIZE)
        .isEqualTo(DEFAULT_PAGE_SIZE);
    assertThat(result.warnings())
        .as("defaults must not produce pagination-clamp warnings")
        .noneMatch(w -> w.contains("Invalid page"))
        .noneMatch(w -> w.contains("Invalid pageSize"))
        .noneMatch(w -> w.contains("exceeds maximum"));
  }

  @Test
  void listApplicationLibraries_should_surface_error_response_for_invalid_app_id() {
    // A bogus app ID must surface as a deterministic error response, not a silent empty-success
    // payload. The specific HTTP status (403 vs 404) and mapped message are environment-dependent
    // (TeamServer deliberately conflates "unknown" with "forbidden" for enumeration defence), so
    // we assert the tool-contract shape: errors present, no items, no total, no more pages, and
    // no warnings mixed in with the error.
    var result = tool.listApplicationLibraries(null, null, "invalid-app-id-12345");

    assertThat(result.isSuccess())
        .as("invalid appId must surface as an error, not a silent empty success")
        .isFalse();
    assertThat(result.errors())
        .as("error response must carry at least one non-blank error message")
        .isNotEmpty()
        .allSatisfy(msg -> assertThat(msg).isNotBlank());
    assertThat(result.items()).as("error response must carry no items").isEmpty();
    assertThat(result.totalItems()).as("error response must report zero total").isZero();
    assertThat(result.hasMorePages()).as("error response must not claim more pages").isFalse();
    assertThat(result.warnings()).as("error response must not mix in warnings").isEmpty();
  }

  @Test
  void listApplicationLibraries_should_include_class_usage_data() {
    var result = tool.listApplicationLibraries(null, null, testData.appId);

    assertThat(result.isSuccess()).as("response must be successful").isTrue();
    assertThat(result.items())
        .as("requires seeded libraries on app %s — see INTEGRATION_TESTS.md", testData.appName)
        .isNotEmpty();

    // classesUsed ∈ [0, classCount] for every library. A value above classCount would indicate
    // either a deserialization mix-up between the two fields or an API contract regression.
    assertThat(result.items())
        .as("every library must report classesUsed within [0, classCount]")
        .allSatisfy(
            lib -> {
              assertThat(lib.getClassCount())
                  .as("%s.classCount", lib.getFilename())
                  .isNotNegative();
              assertThat(lib.getClassesUsed())
                  .as("%s.classesUsed", lib.getFilename())
                  .isNotNegative();
              assertThat(lib.getClassesUsed())
                  .as("%s.classesUsed must not exceed classCount", lib.getFilename())
                  .isLessThanOrEqualTo(lib.getClassCount());
            });
  }

  @Test
  void listApplicationLibraries_should_paginate_with_small_page_size() {
    // Fail fast if seeded data is too small to exercise multi-page behavior.
    var fullResult = tool.listApplicationLibraries(null, null, testData.appId);
    assertThat(fullResult.isSuccess()).as("baseline fetch must succeed").isTrue();
    int totalLibraries = fullResult.totalItems();
    assertThat(totalLibraries)
        .as(
            "pagination test requires app %s to have > %d libraries — see INTEGRATION_TESTS.md",
            testData.appName, PAGINATION_PROBE_SIZE)
        .isGreaterThanOrEqualTo(MIN_LIBS_FOR_PAGINATION);

    var page1 = tool.listApplicationLibraries(1, PAGINATION_PROBE_SIZE, testData.appId);
    assertThat(page1.isSuccess()).as("page 1 must succeed").isTrue();
    assertThat(page1.items())
        .as("page 1 must contain exactly %d items", PAGINATION_PROBE_SIZE)
        .hasSize(PAGINATION_PROBE_SIZE);
    assertThat(page1.page()).as("page 1 page number").isEqualTo(1);
    assertThat(page1.pageSize()).as("page 1 size echo").isEqualTo(PAGINATION_PROBE_SIZE);
    assertThat(page1.totalItems()).as("page 1 totalItems echo").isEqualTo(totalLibraries);
    assertThat(page1.hasMorePages()).as("page 1 hasMorePages").isTrue();

    var page2 = tool.listApplicationLibraries(2, PAGINATION_PROBE_SIZE, testData.appId);
    assertThat(page2.isSuccess()).as("page 2 must succeed").isTrue();
    assertThat(page2.page()).as("page 2 page number").isEqualTo(2);

    int expectedPage2Size = Math.min(PAGINATION_PROBE_SIZE, totalLibraries - PAGINATION_PROBE_SIZE);
    assertThat(page2.items())
        .as("page 2 must contain min(pageSize, remaining) = %d items", expectedPage2Size)
        .hasSize(expectedPage2Size);

    // Library hash is a stable per-library identifier; disjointness by hash proves the offset
    // advanced rather than being ignored.
    var page1Hashes = page1.items().stream().map(LibraryExtended::getHash).toList();
    var page2Hashes = page2.items().stream().map(LibraryExtended::getHash).toList();
    assertThat(page2Hashes)
        .as("page 2 hashes must be disjoint from page 1 hashes")
        .doesNotContainAnyElementsOf(page1Hashes);
  }

  @Test
  void listApplicationLibraries_should_have_consistent_severity_counts() {
    var result = tool.listApplicationLibraries(null, null, testData.appId);
    assertThat(result.isSuccess()).as("response must be successful").isTrue();

    // The arithmetic-consistency contract only applies to libraries whose vulnerabilities array
    // is populated — that's the source of truth for medium/low/note counts in LibraryExtended.
    // Fail fast if no such library is seeded so data rot becomes visible, rather than silently
    // passing.
    var vulnerableLibs =
        result.items().stream()
            .filter(lib -> lib.getVulnerabilities() != null && !lib.getVulnerabilities().isEmpty())
            .toList();
    assertThat(vulnerableLibs)
        .as(
            "requires at least one seeded library with a populated vulnerabilities array on app"
                + " %s — see INTEGRATION_TESTS.md",
            testData.appName)
        .isNotEmpty();

    // For every vulnerable library, the sum of array-computed severity counts (across all five
    // buckets) must equal the vulnerabilities array size. Mixing array-computed medium/low/note
    // with API-provided critical/high would produce spurious failures under TS-41988, so we
    // compute critical/high from the array as well.
    assertThat(vulnerableLibs)
        .as(
            "every vulnerable library's severity breakdown must sum to its vulnerabilities array"
                + " size")
        .allSatisfy(
            lib -> {
              int arraySize = lib.getVulnerabilities().size();
              int criticalFromArray = countBySeverity(lib, RuleSeverity.CRITICAL);
              int highFromArray = countBySeverity(lib, RuleSeverity.HIGH);
              int sum =
                  criticalFromArray
                      + highFromArray
                      + lib.getMediumVulnerabilities()
                      + lib.getLowVulnerabilities()
                      + lib.getNoteVulnerabilities();
              assertThat(sum)
                  .as(
                      "%s severity sum (critical=%d, high=%d, medium=%d, low=%d, note=%d) must"
                          + " equal vulns array size (%d) — unknown severityToUse value would"
                          + " cause mismatch",
                      lib.getFilename(),
                      criticalFromArray,
                      highFromArray,
                      lib.getMediumVulnerabilities(),
                      lib.getLowVulnerabilities(),
                      lib.getNoteVulnerabilities(),
                      arraySize)
                  .isEqualTo(arraySize);
            });
  }

  @Test
  void listApplicationLibraries_should_clamp_page_below_one() {
    // PaginationParams treats page < 1 as a soft failure: clamp to 1 and emit a warning.
    var result = tool.listApplicationLibraries(0, PAGINATION_PROBE_SIZE, testData.appId);

    assertThat(result.isSuccess()).as("page=0 must soft-fail and continue").isTrue();
    assertThat(result.page()).as("page=0 must be clamped to 1").isEqualTo(1);
    assertThat(result.warnings())
        .as("clamping must emit an 'Invalid page number' warning")
        .anyMatch(w -> w.contains("Invalid page number 0"));
  }

  @Test
  void listApplicationLibraries_should_clamp_oversized_pageSize() {
    // Tool-specific max is API_MAX_PAGE_SIZE (50). Requests above must be capped with a warning.
    int oversized = API_MAX_PAGE_SIZE * 4;
    var result = tool.listApplicationLibraries(1, oversized, testData.appId);

    assertThat(result.isSuccess()).as("oversized pageSize must soft-fail and continue").isTrue();
    assertThat(result.pageSize())
        .as("pageSize must be capped to API_MAX_PAGE_SIZE=%d", API_MAX_PAGE_SIZE)
        .isEqualTo(API_MAX_PAGE_SIZE);
    assertThat(result.warnings())
        .as("clamping must emit an 'exceeds maximum' warning citing the cap")
        .anyMatch(
            w -> w.contains("exceeds maximum") && w.contains(String.valueOf(API_MAX_PAGE_SIZE)));
  }

  @Test
  void listApplicationLibraries_should_clamp_non_positive_pageSize() {
    // pageSize < 1 must be replaced with DEFAULT_PAGE_SIZE and a warning emitted.
    var result = tool.listApplicationLibraries(1, 0, testData.appId);

    assertThat(result.isSuccess()).as("pageSize=0 must soft-fail and continue").isTrue();
    assertThat(result.pageSize())
        .as("pageSize=0 must fall back to default %d", DEFAULT_PAGE_SIZE)
        .isEqualTo(DEFAULT_PAGE_SIZE);
    assertThat(result.warnings())
        .as("clamping must emit an 'Invalid pageSize' warning")
        .anyMatch(w -> w.contains("Invalid pageSize 0"));
  }

  @Test
  void listApplicationLibraries_should_return_empty_for_page_beyond_range() {
    // Total is known from page 1. Requesting a page far beyond that must succeed with an empty
    // items list (no error, no spurious data) and correctly report hasMorePages=false.
    var baseline = tool.listApplicationLibraries(1, PAGINATION_PROBE_SIZE, testData.appId);
    assertThat(baseline.isSuccess()).as("baseline fetch must succeed").isTrue();
    int total = baseline.totalItems();
    assertThat(total).as("baseline must report totalItems").isPositive();

    int beyond = (total / PAGINATION_PROBE_SIZE) + 10;
    var result = tool.listApplicationLibraries(beyond, PAGINATION_PROBE_SIZE, testData.appId);

    assertThat(result.isSuccess()).as("out-of-range page must not error").isTrue();
    assertThat(result.items()).as("out-of-range page must yield no items").isEmpty();
    assertThat(result.hasMorePages())
        .as("out-of-range page must report hasMorePages=false")
        .isFalse();
    assertThat(result.totalItems())
        .as("out-of-range page must still echo total count")
        .isEqualTo(total);
  }

  @Test
  void listApplicationLibraries_should_populate_grade_with_valid_letter() {
    var result = tool.listApplicationLibraries(null, null, testData.appId);
    assertThat(result.isSuccess()).as("response must be successful").isTrue();

    // Contrast only scores open-source dependencies; custom libs (e.g., the app's own jar) return
    // sentinel "?" for grade. Filter them out so the assertion targets the documented A-F domain.
    var scorableLibs = result.items().stream().filter(lib -> !lib.isCustom()).toList();
    assertThat(scorableLibs)
        .as("requires at least one non-custom library on app %s", testData.appName)
        .isNotEmpty();

    assertThat(scorableLibs)
        .as("every non-custom library's grade must be one of %s", VALID_GRADES)
        .allSatisfy(
            lib -> assertThat(lib.getGrade()).as("%s.grade", lib.getFilename()).isIn(VALID_GRADES));
  }

  @Test
  void listApplicationLibraries_should_populate_vulnerability_details_when_present() {
    var result = tool.listApplicationLibraries(null, null, testData.appId);
    assertThat(result.isSuccess()).as("response must be successful").isTrue();

    var vulnerableLibs =
        result.items().stream()
            .filter(lib -> lib.getVulnerabilities() != null && !lib.getVulnerabilities().isEmpty())
            .toList();
    assertThat(vulnerableLibs)
        .as(
            "requires at least one seeded library with a populated vulnerabilities array on app"
                + " %s — see INTEGRATION_TESTS.md",
            testData.appName)
        .isNotEmpty();

    // Every vulnerability on every vulnerable library must surface its identifying name and a
    // severity code drawn from the Contrast RuleSeverity enum — a regression in either field
    // would surface a deserialization or contract issue.
    var validSeverityCodes =
        Set.of(
            RuleSeverity.CRITICAL.name(),
            RuleSeverity.HIGH.name(),
            RuleSeverity.MEDIUM.name(),
            RuleSeverity.LOW.name(),
            RuleSeverity.NOTE.name());

    assertThat(vulnerableLibs)
        .as("every vulnerability must populate name and a known severity code")
        .allSatisfy(
            lib ->
                assertThat(lib.getVulnerabilities())
                    .as("%s.vulnerabilities", lib.getFilename())
                    .doesNotContainNull()
                    .allSatisfy(
                        vuln -> {
                          assertThat(vuln.getName())
                              .as("%s vulnerability name", lib.getFilename())
                              .isNotBlank();
                          assertThat(vuln.getSeverityCode())
                              .as(
                                  "%s vulnerability %s severityCode",
                                  lib.getFilename(), vuln.getName())
                              .isNotBlank();
                          assertThat(vuln.getSeverityCode().toUpperCase())
                              .as(
                                  "%s vulnerability %s severityCode must be a known RuleSeverity",
                                  lib.getFilename(), vuln.getName())
                              .isIn(validSeverityCodes);
                        }));
  }

  @Test
  void listApplicationLibraries_should_populate_version_and_staleness_fields() {
    var result = tool.listApplicationLibraries(null, null, testData.appId);
    assertThat(result.isSuccess()).as("response must be successful").isTrue();

    // Custom libs return sentinel "?" for version and -1 for monthsOutdated. Staleness metrics
    // only apply to open-source dependencies, so filter custom out to keep the assertion targeted.
    var scorableLibs = result.items().stream().filter(lib -> !lib.isCustom()).toList();
    assertThat(scorableLibs)
        .as("requires at least one non-custom library on app %s", testData.appName)
        .isNotEmpty();

    assertThat(scorableLibs)
        .as("every non-custom library must populate version and report non-negative staleness")
        .allSatisfy(
            lib -> {
              assertThat(lib.getVersion()).as("%s.version", lib.getFilename()).isNotBlank();
              assertThat(lib.getLibScore()).as("%s.libScore", lib.getFilename()).isNotNegative();
              assertThat(lib.getMonthsOutdated())
                  .as("%s.monthsOutdated", lib.getFilename())
                  .isNotNegative();
              assertThat(lib.getReleaseDate())
                  .as("%s.releaseDate (epoch ms)", lib.getFilename())
                  .isNotNegative();
            });
  }

  @Test
  void listApplicationLibraries_should_expose_filtered_app_id_on_library_rows() {
    var result = tool.listApplicationLibraries(null, null, testData.appId);
    assertThat(result.isSuccess()).as("response must be successful").isTrue();
    assertThat(result.items())
        .as("requires seeded libraries on app %s", testData.appName)
        .isNotEmpty();

    // Because the tool filters by appId, every returned library's app_id (if populated) must
    // match the filter. An empty appId on the row is acceptable — some API shapes omit the
    // reverse reference — but a mismatch indicates cross-app leakage.
    assertThat(result.items())
        .as("library rows that carry an appId must match the filter %s", testData.appId)
        .allSatisfy(
            lib -> {
              if (lib.getAppId() != null && !lib.getAppId().isBlank()) {
                assertThat(lib.getAppId())
                    .as("%s.appId must match filter", lib.getFilename())
                    .isEqualTo(testData.appId);
              }
            });
  }

  private static int countBySeverity(LibraryExtended lib, RuleSeverity severity) {
    return (int)
        lib.getVulnerabilities().stream()
            .filter(v -> severity.name().equalsIgnoreCase(v.getSeverityCode()))
            .count();
  }
}
