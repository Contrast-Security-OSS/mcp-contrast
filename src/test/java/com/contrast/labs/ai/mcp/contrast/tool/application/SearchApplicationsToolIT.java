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
package com.contrast.labs.ai.mcp.contrast.tool.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.contrast.labs.ai.mcp.contrast.config.IntegrationTestConfig;
import com.contrast.labs.ai.mcp.contrast.result.ApplicationData;
import com.contrast.labs.ai.mcp.contrast.util.AbstractIntegrationTest;
import com.contrast.labs.ai.mcp.contrast.util.IntegrationTestDataCache;
import java.io.IOException;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Integration tests for SearchApplicationsTool.
 *
 * <p>Requires CONTRAST_HOST_NAME environment variable to be set.
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
class SearchApplicationsToolIT extends AbstractIntegrationTest<SearchApplicationsToolIT.TestData> {

  @Autowired private SearchApplicationsTool searchApplicationsTool;

  // Shared pagination constants — keep in sync with ValidationConstants.
  private static final int DEFAULT_PAGE_SIZE = 50;
  private static final int MAX_PAGE_SIZE = 100;
  private static final int PAGINATION_PROBE_SIZE = 2;
  private static final int MIN_APPS_FOR_PAGINATION = PAGINATION_PROBE_SIZE + 1;

  // SingleTool/PaginatedTool's 5xx mapping — validation errors must never look like this.
  private static final String CONTRAST_API_ERROR = "Contrast API error";

  // A value that no real application will plausibly carry — used for OR/narrowing assertions.
  private static final String UNLIKELY_METADATA_VALUE = "never-matching-value-xyzqwer987654321";

  /** Discovered test data. Populated once per class by {@link #performDiscovery()}. */
  static class TestData {
    int totalApplications;
    String sampleAppName;
    String sampleAppId;
    // Tag discovery: any app with at least one tag contributes the first tag value.
    String sampleTag;
    String appIdWithSampleTag;
    // Metadata discovery: first app carrying at least one populated metadata entry contributes
    // the (field, value) pair. Field name uses the displayLabel exposed by the metadata-fields
    // endpoint so case-insensitivity tests exercise the real server mapping.
    String sampleMetadataField;
    String sampleMetadataValue;

    @Override
    public String toString() {
      return String.format(
          "TestData{totalApplications=%d, sampleAppName='%s', sampleAppId='%s', "
              + "sampleTag='%s', sampleMetadataField='%s', sampleMetadataValue='%s'}",
          totalApplications,
          sampleAppName,
          sampleAppId,
          sampleTag,
          sampleMetadataField,
          sampleMetadataValue);
    }
  }

  @Override
  protected String testDisplayName() {
    return "SearchApplicationsTool Integration Test";
  }

  @Override
  protected Class<TestData> testDataType() {
    return TestData.class;
  }

  @Override
  protected void logTestDataDetails(TestData data) {
    log.info("Test data: {}", data);
  }

  @Override
  protected TestData performDiscovery() throws IOException {
    var data = new TestData();

    var applications = IntegrationTestDataCache.getApplications(orgId, sdkExtension);
    if (applications == null || applications.isEmpty()) {
      throw new NoTestDataException(
          "SearchApplicationsToolIT requires at least one application in the organization — "
              + "see INTEGRATION_TESTS.md");
    }

    data.totalApplications = applications.size();
    var firstApp = applications.get(0);
    data.sampleAppName = firstApp.getName();
    data.sampleAppId = firstApp.getAppId();

    for (var app : applications) {
      if (data.sampleTag == null && app.getTags() != null && !app.getTags().isEmpty()) {
        var firstTag = app.getTags().get(0);
        if (firstTag != null && !firstTag.isBlank()) {
          data.sampleTag = firstTag;
          data.appIdWithSampleTag = app.getAppId();
        }
      }

      if (data.sampleMetadataField == null
          && app.getMetadataEntities() != null
          && !app.getMetadataEntities().isEmpty()) {
        for (var entry : app.getMetadataEntities()) {
          if (entry != null
              && entry.getName() != null
              && !entry.getName().isBlank()
              && entry.getValue() != null
              && !entry.getValue().isBlank()) {
            data.sampleMetadataField = entry.getName();
            data.sampleMetadataValue = entry.getValue();
            break;
          }
        }
      }

      if (data.sampleTag != null && data.sampleMetadataField != null) {
        break;
      }
    }

    return data;
  }

  // ---------- Discovery precondition ----------

  @Test
  void testDiscoveredTestDataExists() {
    assertThat(testData).as("discovery must populate test data").isNotNull();
    assertThat(testData.totalApplications)
        .as("organization must carry at least one application")
        .isPositive();
    assertThat(testData.sampleAppId).as("sample appId must be non-blank").isNotBlank();
    assertThat(testData.sampleAppName).as("sample appName must be non-blank").isNotBlank();
  }

  // ---------- Response shape and identity ----------

  @Test
  void searchApplications_should_return_valid_response() {
    // Focus on server-driven behavior: totalItems must be a non-negative count, and
    // hasMorePages must be internally consistent with items + pageSize. Input echoes
    // (page=1, pageSize=10) are tautologies — asserting them would only prove that the
    // tool returns what it was given, not that it behaves correctly against the server.
    var response = searchApplicationsTool.searchApplications(1, 10, null, null, null);

    assertThat(response).as("response must never be null").isNotNull();
    assertThat(response.isSuccess()).as("baseline query must succeed").isTrue();
    assertThat(response.errors()).as("baseline query must have no errors").isEmpty();
    assertThat(response.items()).as("items list must never be null").isNotNull();
    assertThat(response.totalItems())
        .as("totalItems must be a non-negative count, never a sentinel")
        .isNotNull()
        .isNotNegative();
    // hasMorePages consistency: if this page did not fill up, there must be no more pages.
    if (response.items().size() < response.pageSize()) {
      assertThat(response.hasMorePages())
          .as("partial page must report hasMorePages=false")
          .isFalse();
    }
  }

  @Test
  void searchApplications_should_include_application_details() {
    var response = searchApplicationsTool.searchApplications(1, 10, null, null, null);

    assertThat(response.isSuccess()).as("response must be successful").isTrue();
    assertThat(response.items())
        .as("requires seeded applications — see INTEGRATION_TESTS.md")
        .isNotEmpty();

    // Core identity invariants across every returned app. The defensive getters on
    // ApplicationData reflect API defaults (collections never null), so a regression that
    // drops one to null would surface here.
    assertThat(response.items())
        .as("every app must populate core identity and expose non-null collection fields")
        .allSatisfy(
            app -> {
              assertThat(app.name()).as("app.name").isNotBlank();
              assertThat(app.appID()).as("%s.appID", app.name()).isNotBlank();
              assertThat(app.metadata())
                  .as("%s.metadata must be non-null collection", app.name())
                  .isNotNull();
              assertThat(app.tags())
                  .as("%s.tags must be non-null collection", app.name())
                  .isNotNull();
              assertThat(app.technologies())
                  .as("%s.technologies must be non-null collection", app.name())
                  .isNotNull();
            });
  }

  @Test
  void searchApplications_should_populate_status_field_somewhere() {
    // status is nullable on the Contrast Application model (some SCA-only apps decline to
    // report a status), so blanket allSatisfy(isNotBlank) would produce false positives.
    // Instead, prove the field decodes end-to-end by requiring at least one returned app
    // with a populated status — a regression that always left status null would surface here.
    var response = searchApplicationsTool.searchApplications(1, MAX_PAGE_SIZE, null, null, null);

    assertThat(response.isSuccess()).as("response must be successful").isTrue();
    assertThat(response.items())
        .as("requires seeded applications — see INTEGRATION_TESTS.md")
        .isNotEmpty();
    assertThat(response.items())
        .as("at least one app must populate a non-blank status — proves field decodes")
        .anyMatch(app -> app.status() != null && !app.status().isBlank());
  }

  @Test
  void searchApplications_should_populate_language_field_somewhere() {
    // Same rationale as status: nullable on the model, so anyMatch is the correct
    // end-to-end existence proof rather than allSatisfy.
    var response = searchApplicationsTool.searchApplications(1, MAX_PAGE_SIZE, null, null, null);

    assertThat(response.isSuccess()).as("response must be successful").isTrue();
    assertThat(response.items())
        .as("requires seeded applications — see INTEGRATION_TESTS.md")
        .isNotEmpty();
    assertThat(response.items())
        .as("at least one app must populate a non-blank language — proves field decodes")
        .anyMatch(app -> app.language() != null && !app.language().isBlank());
  }

  // ---------- Name filter ----------

  @Test
  void searchApplications_should_filter_by_name() {
    // The server-side text filter searches displayName, contextPath, tags, and metadata
    // values (OR across fields). ApplicationData exposes three of the four (contextPath
    // is not projected), so every returned row must match the partial on at least one of
    // name/tags/metadata. A legitimate contextPath-only match would surface as a row
    // failing this disjunction, which is tolerable for the assertion as a tightening
    // signal; a regression that broadened the filter to unrelated rows would surface the
    // same way. Keeping pageSize at MAX_PAGE_SIZE reduces flakiness when many rows match.
    var partialName =
        testData.sampleAppName.substring(0, Math.min(3, testData.sampleAppName.length()));

    var response =
        searchApplicationsTool.searchApplications(1, MAX_PAGE_SIZE, partialName, null, null);

    assertThat(response.isSuccess()).as("name-filtered query must succeed").isTrue();
    assertThat(response.items())
        .as(
            "name filter '%s' (derived from seed app %s) must return at least one result",
            partialName, testData.sampleAppName)
        .isNotEmpty();

    var lowerPartial = partialName.toLowerCase(Locale.ROOT);
    assertThat(response.items())
        .as(
            "every result must carry '%s' in name, tags, or metadata value"
                + " (contextPath is not projected on ApplicationData)",
            partialName)
        .allSatisfy(
            app -> {
              boolean inName =
                  app.name() != null && app.name().toLowerCase(Locale.ROOT).contains(lowerPartial);
              boolean inTags =
                  app.tags().stream()
                      .anyMatch(
                          t -> t != null && t.toLowerCase(Locale.ROOT).contains(lowerPartial));
              boolean inMetadata =
                  app.metadata().stream()
                      .anyMatch(
                          m ->
                              m != null
                                  && m.value() != null
                                  && m.value().toLowerCase(Locale.ROOT).contains(lowerPartial));
              assertThat(inName || inTags || inMetadata)
                  .as(
                      "%s must match '%s' in name/tags/metadata (name=%s, tags=%s,"
                          + " metadataValues=%s)",
                      app.appID(),
                      partialName,
                      app.name(),
                      app.tags(),
                      app.metadata().stream().map(m -> m == null ? null : m.value()).toList())
                  .isTrue();
            });
  }

  // ---------- Pagination ----------

  @Test
  void searchApplications_should_handle_pagination() {
    // Fail fast if the org is too small to exercise multi-page behavior.
    assertThat(testData.totalApplications)
        .as(
            "pagination test requires org to have > %d applications — see INTEGRATION_TESTS.md",
            PAGINATION_PROBE_SIZE)
        .isGreaterThanOrEqualTo(MIN_APPS_FOR_PAGINATION);

    var page1 =
        searchApplicationsTool.searchApplications(1, PAGINATION_PROBE_SIZE, null, null, null);
    assertThat(page1.isSuccess()).as("page 1 must succeed").isTrue();
    assertThat(page1.items())
        .as("page 1 must contain exactly %d items", PAGINATION_PROBE_SIZE)
        .hasSize(PAGINATION_PROBE_SIZE);
    assertThat(page1.hasMorePages()).as("page 1 must report hasMorePages=true").isTrue();

    var page2 =
        searchApplicationsTool.searchApplications(2, PAGINATION_PROBE_SIZE, null, null, null);
    assertThat(page2.isSuccess()).as("page 2 must succeed").isTrue();
    assertThat(page2.page()).as("page 2 page number").isEqualTo(2);

    int totalItems = page1.totalItems();
    int expectedPage2Size = Math.min(PAGINATION_PROBE_SIZE, totalItems - PAGINATION_PROBE_SIZE);
    assertThat(page2.items())
        .as("page 2 must contain min(pageSize, remaining) = %d items", expectedPage2Size)
        .hasSize(expectedPage2Size);

    // Disjointness by appID proves the server honored the offset rather than returning the
    // same page twice. appID is a stable per-app identifier.
    var page1Ids = page1.items().stream().map(ApplicationData::appID).toList();
    var page2Ids = page2.items().stream().map(ApplicationData::appID).toList();
    assertThat(page2Ids)
        .as("page 2 IDs must be disjoint from page 1 IDs")
        .doesNotContainAnyElementsOf(page1Ids);
  }

  @Test
  void searchApplications_should_return_total_count_consistent_with_items() {
    // When a single page captures the entire result set, totalItems must equal items.size().
    // A server regression that returned a sentinel total would surface here.
    var response = searchApplicationsTool.searchApplications(1, MAX_PAGE_SIZE, null, null, null);

    assertThat(response.isSuccess()).as("response must be successful").isTrue();
    assertThat(response.totalItems())
        .as("totalItems must be populated")
        .isNotNull()
        .isNotNegative();

    if (!response.hasMorePages()) {
      assertThat(response.totalItems())
          .as("single-page result: totalItems must equal items.size()")
          .isEqualTo(response.items().size());
    }
  }

  // ---------- Pagination defaults and clamping ----------

  @Test
  void searchApplications_should_default_page_and_pageSize_when_null() {
    // Null page/pageSize must route through PaginationParams defaults — page=1, pageSize=50
    // — with no clamp warnings. A change to the default contract would surface here.
    var response = searchApplicationsTool.searchApplications(null, null, null, null, null);

    assertThat(response.isSuccess()).as("defaults must not fail validation").isTrue();
    assertThat(response.page()).as("null page must default to 1").isEqualTo(1);
    assertThat(response.pageSize())
        .as("null pageSize must default to %d", DEFAULT_PAGE_SIZE)
        .isEqualTo(DEFAULT_PAGE_SIZE);
    assertThat(response.warnings())
        .as("defaults must not produce pagination-clamp warnings")
        .noneMatch(w -> w.contains("Invalid page"))
        .noneMatch(w -> w.contains("Invalid pageSize"))
        .noneMatch(w -> w.contains("exceeds maximum"));
  }

  @Test
  void searchApplications_should_clamp_page_below_one() {
    // PaginationParams treats page < 1 as a soft failure: clamp to 1 and emit a warning.
    var response = searchApplicationsTool.searchApplications(0, 10, null, null, null);

    assertThat(response.isSuccess()).as("page=0 must soft-fail and continue").isTrue();
    assertThat(response.page()).as("page=0 must be clamped to 1").isEqualTo(1);
    assertThat(response.warnings())
        .as("clamping must emit an 'Invalid page number' warning")
        .anyMatch(w -> w.contains("Invalid page number 0"));
  }

  @Test
  void searchApplications_should_clamp_non_positive_pageSize() {
    // pageSize < 1 must be replaced with DEFAULT_PAGE_SIZE and a warning emitted.
    var response = searchApplicationsTool.searchApplications(1, 0, null, null, null);

    assertThat(response.isSuccess()).as("pageSize=0 must soft-fail and continue").isTrue();
    assertThat(response.pageSize())
        .as("pageSize=0 must fall back to default %d", DEFAULT_PAGE_SIZE)
        .isEqualTo(DEFAULT_PAGE_SIZE);
    assertThat(response.warnings())
        .as("clamping must emit an 'Invalid pageSize' warning")
        .anyMatch(w -> w.contains("Invalid pageSize 0"));
  }

  @Test
  void searchApplications_should_clamp_oversized_pageSize() {
    // Tool-specific max is MAX_PAGE_SIZE (100). Requests above must be capped with a
    // warning that names the cap.
    int oversized = MAX_PAGE_SIZE * 4;
    var response = searchApplicationsTool.searchApplications(1, oversized, null, null, null);

    assertThat(response.isSuccess()).as("oversized pageSize must soft-fail and continue").isTrue();
    assertThat(response.pageSize())
        .as("pageSize must be capped to MAX_PAGE_SIZE=%d", MAX_PAGE_SIZE)
        .isEqualTo(MAX_PAGE_SIZE);
    assertThat(response.warnings())
        .as("clamping must emit an 'exceeds maximum' warning citing the cap")
        .anyMatch(w -> w.contains("exceeds maximum") && w.contains(String.valueOf(MAX_PAGE_SIZE)));
  }

  // ---------- Tag filter ----------

  @Test
  void searchApplications_should_filter_by_tag() {
    assertThat(testData.sampleTag)
        .as(
            "tag filter test requires at least one app with a non-blank tag — "
                + "see INTEGRATION_TESTS.md")
        .isNotBlank();

    var response =
        searchApplicationsTool.searchApplications(1, MAX_PAGE_SIZE, null, testData.sampleTag, null);

    assertThat(response.isSuccess())
        .as("tag-filtered query for '%s' must succeed", testData.sampleTag)
        .isTrue();
    assertThat(response.items())
        .as(
            "tag filter must return at least the seed app that carries tag '%s'",
            testData.sampleTag)
        .isNotEmpty()
        .anyMatch(app -> testData.appIdWithSampleTag.equals(app.appID()));

    // Tag match is exact and case-insensitive per the @Tool docs. Every returned app must
    // carry the tag (case-insensitively) in its tags list — a silent broadening of the
    // filter would surface as an app without the tag.
    assertThat(response.items())
        .as("every returned app must carry tag '%s' (case-insensitive)", testData.sampleTag)
        .allSatisfy(
            app ->
                assertThat(app.tags())
                    .as("%s.tags vs filter", app.appID())
                    .isNotNull()
                    .anyMatch(t -> t != null && t.equalsIgnoreCase(testData.sampleTag)));
  }

  // ---------- Metadata filters ----------

  @Test
  void searchApplications_should_filter_by_metadata() {
    assertThat(testData.sampleMetadataField)
        .as(
            "metadata filter test requires at least one app with populated metadata — "
                + "see INTEGRATION_TESTS.md")
        .isNotBlank();
    assertThat(testData.sampleMetadataValue).isNotBlank();

    var filter =
        String.format(
            "{\"%s\":\"%s\"}", testData.sampleMetadataField, testData.sampleMetadataValue);

    var response = searchApplicationsTool.searchApplications(1, MAX_PAGE_SIZE, null, null, filter);

    assertThat(response.errors()).as("valid metadata filter must not produce errors").isEmpty();
    assertThat(response.isSuccess()).as("metadata-filtered query must succeed").isTrue();
    assertThat(response.items())
        .as(
            "metadata filter {%s=%s} must return at least one app carrying that metadata",
            testData.sampleMetadataField, testData.sampleMetadataValue)
        .isNotEmpty();
  }

  @Test
  void searchApplications_should_match_metadata_field_name_case_insensitively() {
    // Field names are resolved case-insensitively on the client side (see
    // SearchApplicationsTool#buildAppMetadataFieldMapping). Querying with upper-cased and
    // lower-cased field names must produce identical result counts; otherwise the mapping
    // regressed to case-sensitive.
    assertThat(testData.sampleMetadataField)
        .as("metadata test requires seeded metadata — see INTEGRATION_TESTS.md")
        .isNotBlank();

    var original =
        String.format(
            "{\"%s\":\"%s\"}", testData.sampleMetadataField, testData.sampleMetadataValue);
    var upperField =
        String.format(
            "{\"%s\":\"%s\"}",
            testData.sampleMetadataField.toUpperCase(Locale.ROOT), testData.sampleMetadataValue);

    var originalResp =
        searchApplicationsTool.searchApplications(1, MAX_PAGE_SIZE, null, null, original);
    var upperResp =
        searchApplicationsTool.searchApplications(1, MAX_PAGE_SIZE, null, null, upperField);

    assertThat(originalResp.isSuccess()).as("original-case query must succeed").isTrue();
    assertThat(upperResp.isSuccess())
        .as("upper-case field-name query must succeed (field name is case-insensitive)")
        .isTrue();
    assertThat(upperResp.totalItems())
        .as("upper-case field-name query must match original-case result count")
        .isEqualTo(originalResp.totalItems());
  }

  @Test
  void searchApplications_should_match_metadata_value_case_insensitively() {
    // Per the @Tool docs, metadata values match case-insensitively. Querying with
    // upper-cased value must yield the same result count as the original.
    assertThat(testData.sampleMetadataValue)
        .as("metadata test requires seeded metadata — see INTEGRATION_TESTS.md")
        .isNotBlank();

    var original =
        String.format(
            "{\"%s\":\"%s\"}", testData.sampleMetadataField, testData.sampleMetadataValue);
    var upperValue =
        String.format(
            "{\"%s\":\"%s\"}",
            testData.sampleMetadataField, testData.sampleMetadataValue.toUpperCase(Locale.ROOT));

    var originalResp =
        searchApplicationsTool.searchApplications(1, MAX_PAGE_SIZE, null, null, original);
    var upperResp =
        searchApplicationsTool.searchApplications(1, MAX_PAGE_SIZE, null, null, upperValue);

    assertThat(originalResp.isSuccess()).as("original-case value query must succeed").isTrue();
    assertThat(upperResp.isSuccess())
        .as("upper-case value query must succeed (value is case-insensitive)")
        .isTrue();
    assertThat(upperResp.totalItems())
        .as("upper-case value query must match original-case result count")
        .isEqualTo(originalResp.totalItems());
  }

  @Test
  void searchApplications_should_apply_or_logic_within_multiple_metadata_values() {
    // Multiple values for a single field are OR-joined per the @Tool docs. Querying with
    // [realValue, unlikelyValue] must return the same rows as querying with [realValue]
    // alone — the unlikely value adds no new matches and OR must not drop matches.
    assertThat(testData.sampleMetadataField)
        .as("metadata test requires seeded metadata — see INTEGRATION_TESTS.md")
        .isNotBlank();

    var singleValue =
        String.format(
            "{\"%s\":\"%s\"}", testData.sampleMetadataField, testData.sampleMetadataValue);
    var orValues =
        String.format(
            "{\"%s\":[\"%s\",\"%s\"]}",
            testData.sampleMetadataField, testData.sampleMetadataValue, UNLIKELY_METADATA_VALUE);

    var singleResp =
        searchApplicationsTool.searchApplications(1, MAX_PAGE_SIZE, null, null, singleValue);
    var orResp = searchApplicationsTool.searchApplications(1, MAX_PAGE_SIZE, null, null, orValues);

    assertThat(singleResp.isSuccess()).as("single-value query must succeed").isTrue();
    assertThat(orResp.isSuccess()).as("OR-values query must succeed").isTrue();
    assertThat(orResp.totalItems())
        .as("OR with a no-match value must not drop matches nor introduce spurious rows")
        .isEqualTo(singleResp.totalItems());
  }

  @Test
  void searchApplications_should_narrow_results_with_unmatched_metadata_value() {
    // Filters must actually narrow: a known field paired with an unlikely value must
    // return zero rows (not the full unfiltered set). This is the minimum proof that the
    // filter was forwarded rather than silently dropped.
    assertThat(testData.sampleMetadataField)
        .as("metadata test requires seeded metadata — see INTEGRATION_TESTS.md")
        .isNotBlank();

    var filter =
        String.format("{\"%s\":\"%s\"}", testData.sampleMetadataField, UNLIKELY_METADATA_VALUE);

    var response = searchApplicationsTool.searchApplications(1, MAX_PAGE_SIZE, null, null, filter);

    assertThat(response.isSuccess()).as("query with no-match value must succeed").isTrue();
    assertThat(response.items())
        .as("metadata value '%s' must match no applications", UNLIKELY_METADATA_VALUE)
        .isEmpty();
    assertThat(response.totalItems()).as("no-match query must report totalItems=0").isEqualTo(0);
  }

  // ---------- Metadata validation errors ----------

  @Test
  void searchApplications_should_reject_unknown_metadata_field() {
    // A metadata field that does not exist in the org must surface as an actionable error
    // naming the offending field. Assert the full message shape — a bare substring match
    // on the field name would coincidentally pass because we passed that name ourselves.
    var bogusField = "never_a_real_field_zzq_123";
    var filter = String.format("{\"%s\":\"anything\"}", bogusField);

    var response = searchApplicationsTool.searchApplications(1, 10, null, null, filter);

    assertThat(response.isSuccess()).as("query with unknown metadata field must fail").isFalse();
    assertThat(response.items()).as("error response must carry no items").isEmpty();
    assertThat(response.errors())
        .as("error must name the offending field and describe resolution")
        .isNotEmpty()
        .anyMatch(
            e ->
                e.contains("Metadata field(s) not found")
                    && e.contains(bogusField)
                    && e.contains("Available fields"));
    assertThat(response.errors())
        .as("validation error must not be surfaced as a Contrast API error")
        .noneMatch(e -> e.contains(CONTRAST_API_ERROR));
  }

  @Test
  void searchApplications_should_reject_empty_metadata_value() {
    // Empty/whitespace-only values must be rejected up front by the tool with a message
    // naming the field and describing the empty-value rule.
    var filter = "{\"freeform\":\"\"}";

    var response = searchApplicationsTool.searchApplications(1, 10, null, null, filter);

    assertThat(response.isSuccess()).as("empty metadata value must fail validation").isFalse();
    assertThat(response.items()).as("validation failure must carry no items").isEmpty();
    assertThat(response.errors())
        .as("error must name the field and cite the non-empty rule")
        .isNotEmpty()
        .anyMatch(
            e -> e.contains("metadataFilters") && e.contains("freeform") && e.contains("empty"));
    assertThat(response.errors())
        .as("validation error must not be surfaced as a Contrast API error")
        .noneMatch(e -> e.contains(CONTRAST_API_ERROR));
  }

  @Test
  void searchApplications_should_reject_invalid_metadata_json() {
    // Malformed JSON must be rejected up front with a message that names the offending
    // parameter and shows the expected format — not a bare "Invalid JSON" substring.
    var response = searchApplicationsTool.searchApplications(1, 10, null, null, "{invalid json}");

    assertThat(response.isSuccess()).as("malformed JSON must fail validation").isFalse();
    assertThat(response.items()).as("validation failure must carry no items").isEmpty();
    assertThat(response.errors())
        .as("error must name the offending parameter and show the expected format")
        .isNotEmpty()
        .anyMatch(
            e -> e.contains("Invalid JSON for metadataFilters") && e.contains("Expected format"));
    assertThat(response.errors())
        .as("validation error must not be surfaced as a Contrast API error")
        .noneMatch(e -> e.contains(CONTRAST_API_ERROR));
  }
}
