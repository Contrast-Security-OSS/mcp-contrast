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
import com.contrast.labs.ai.mcp.contrast.util.AbstractIntegrationTest;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Integration test for GetSessionMetadataTool that validates session metadata retrieval against
 * real TeamServer.
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
public class GetSessionMetadataToolIT
    extends AbstractIntegrationTest<GetSessionMetadataToolIT.TestData> {

  @Autowired private GetSessionMetadataTool getSessionMetadataTool;
  @Autowired private SearchApplicationsTool searchApplicationsTool;

  // SingleTool's 5xx mapping — validation errors must never look like this.
  private static final String CONTRAST_API_ERROR = "Contrast API error";

  // SingleTool's 401/403 mapping — see BaseTool#mapHttpErrorCode and
  // SingleTool#executePipeline (UnauthorizedException branch). Surfaces for both invalid
  // credentials and unknown application IDs (TeamServer returns 403 rather than 404 for an
  // appId the caller cannot see).
  private static final String AUTH_OR_NOT_FOUND_ERROR =
      "Authentication failed or resource not found. Verify credentials and that the resource ID"
          + " is correct.";

  // Tool warning emitted when the SDK returns null (no recorded sessions).
  private static final String NO_METADATA_WARNING_FRAGMENT = "No session metadata found";

  // Probe depth for discovering an app with populated session metadata. Bounded to keep
  // discovery fast on orgs with many applications.
  private static final int METADATA_PROBE_DEPTH = 10;

  // String the API will not match against any real application.
  private static final String NONEXISTENT_APP_ID = "nonexistent-app-id-12345";

  /** Discovered test data — populated once per class by {@link #performDiscovery()}. */
  static class TestData {
    String sampleAppId;

    /**
     * App ID known to expose at least one populated session metadata filter group. Remains blank if
     * no probed app surfaced metadata; the populate test fails loudly in that case.
     */
    String sampleAppIdWithMetadata;

    @Override
    public String toString() {
      return String.format(
          "TestData{sampleAppId='%s', sampleAppIdWithMetadata='%s'}",
          sampleAppId, sampleAppIdWithMetadata);
    }
  }

  @Override
  protected String testDisplayName() {
    return "GetSessionMetadataTool Integration Test";
  }

  @Override
  protected Class<TestData> testDataType() {
    return TestData.class;
  }

  @Override
  protected void logTestDataDetails(TestData data) {
    log.info("Test data: {}", data);
  }

  /**
   * Captures two app IDs from the first page of applications:
   *
   * <ul>
   *   <li>{@code sampleAppId} — any app in the org. Drives baseline retrieval tests and the
   *       error-path test for an unknown-but-syntactically-valid app ID.
   *   <li>{@code sampleAppIdWithMetadata} — the first probed app whose session metadata response
   *       carries a populated filter group. The "populate" test asserts {@code isNotEmpty} on the
   *       returned filters and would otherwise fail or pass vacuously if every probed app had no
   *       accumulated session metadata.
   * </ul>
   *
   * Probing stops at the first metadata-bearing app to keep discovery cost bounded; if none is
   * found the populate test fails loudly with a precondition message.
   */
  @Override
  protected TestData performDiscovery() throws IOException {
    var response =
        searchApplicationsTool.searchApplications(1, METADATA_PROBE_DEPTH, null, null, null);

    if (!response.isSuccess() || response.items().isEmpty()) {
      throw new NoTestDataException(
          "GetSessionMetadataToolIT requires at least one application in the organization — "
              + "see INTEGRATION_TESTS.md");
    }

    var data = new TestData();
    data.sampleAppId = response.items().get(0).appID();

    // Probe the page for an app that has accumulated session metadata. Stops at the first
    // hit so discovery cost stays bounded. If no app surfaces metadata, the populate test
    // fails loudly with a precondition message.
    for (var app : response.items()) {
      var probe = getSessionMetadataTool.getSessionMetadata(app.appID());
      if (probe.isSuccess()
          && probe.found()
          && probe.data() != null
          && probe.data().getFilters() != null
          && !probe.data().getFilters().isEmpty()) {
        data.sampleAppIdWithMetadata = app.appID();
        break;
      }
    }

    return data;
  }

  // ---------- Discovery precondition ----------

  @Test
  void testDiscoveredTestDataExists() {
    assertThat(testData).as("discovery must populate test data").isNotNull();
    assertThat(testData.sampleAppId)
        .as("discovery must resolve at least one application — see INTEGRATION_TESTS.md")
        .isNotBlank();
  }

  // ---------- Validation errors ----------

  @Test
  void getSessionMetadata_should_return_validation_error_for_missing_app_id() {
    var result = getSessionMetadataTool.getSessionMetadata(null);

    assertThat(result.isSuccess()).as("null appId must fail validation").isFalse();
    assertThat(result.found()).as("validation failure must not report found").isFalse();
    assertThat(result.data()).as("validation failure must not carry data").isNull();
    // Assert full message shape — a bare "appId" + "required" substring would coincidentally
    // match an unrelated message containing the parameter name.
    assertThat(result.errors())
        .as("validation error must state appId is required")
        .containsExactly("appId is required");
    assertThat(result.errors())
        .as("validation error must not surface as a Contrast API error")
        .noneMatch(e -> e.contains(CONTRAST_API_ERROR));
  }

  @Test
  void getSessionMetadata_should_return_validation_error_for_empty_app_id() {
    var result = getSessionMetadataTool.getSessionMetadata("");

    assertThat(result.isSuccess()).as("empty appId must fail validation").isFalse();
    assertThat(result.found()).as("validation failure must not report found").isFalse();
    assertThat(result.data()).as("validation failure must not carry data").isNull();
    assertThat(result.errors())
        .as("validation error must state appId is required")
        .containsExactly("appId is required");
    assertThat(result.errors())
        .as("validation error must not surface as a Contrast API error")
        .noneMatch(e -> e.contains(CONTRAST_API_ERROR));
  }

  @Test
  void getSessionMetadata_should_return_validation_error_for_blank_app_id() {
    var result = getSessionMetadataTool.getSessionMetadata("   ");

    assertThat(result.isSuccess()).as("whitespace-only appId must fail validation").isFalse();
    assertThat(result.found()).as("validation failure must not report found").isFalse();
    assertThat(result.errors())
        .as("validation error must state appId is required")
        .containsExactly("appId is required");
    assertThat(result.errors())
        .as("validation error must not surface as a Contrast API error")
        .noneMatch(e -> e.contains(CONTRAST_API_ERROR));
  }

  // ---------- Successful retrieval ----------

  @Test
  void getSessionMetadata_should_succeed_for_valid_app_id() {
    // Single deterministic outcome: a valid appId must produce a successful response with no
    // errors. Whether the app has accumulated session metadata is exercised separately by
    // _should_populate_metadata_fields.
    var result = getSessionMetadataTool.getSessionMetadata(testData.sampleAppId);

    assertThat(result.isSuccess())
        .as("valid appId %s must produce a successful response", testData.sampleAppId)
        .isTrue();
    assertThat(result.errors()).as("successful query must have no errors").isEmpty();
    assertThat(result.errors())
        .as("successful response must not surface as a Contrast API error")
        .noneMatch(e -> e.contains(CONTRAST_API_ERROR));
  }

  @Test
  void getSessionMetadata_should_populate_metadata_fields() {
    // Precondition: requires a seeded app with at least one populated session metadata filter
    // group. Discovery surfaces this via testData.sampleAppIdWithMetadata; if absent across the
    // probe depth, fail loudly with a diagnostic message rather than silently passing.
    assertThat(testData.sampleAppIdWithMetadata)
        .as(
            "requires a seeded application with populated session metadata within the first %d"
                + " applications — see INTEGRATION_TESTS.md",
            METADATA_PROBE_DEPTH)
        .isNotBlank();

    var result = getSessionMetadataTool.getSessionMetadata(testData.sampleAppIdWithMetadata);

    assertThat(result.isSuccess()).as("seeded session metadata query must succeed").isTrue();
    assertThat(result.errors()).as("seeded query must produce no errors").isEmpty();
    assertThat(result.found()).as("seeded session metadata must be reported as found").isTrue();
    assertThat(result.data()).as("seeded query must carry data").isNotNull();
    // "Populate ≠ non-null": the @Tool description promises filter groups exposing branch
    // names, build IDs, and other custom metadata. Assert the collection is non-empty and
    // every group carries a non-blank identifier so a regression that decoded an empty list
    // or null group ids would surface here.
    assertThat(result.data().getFilters())
        .as("response must include at least one filter group")
        .isNotNull()
        .isNotEmpty();
    assertThat(result.data().getFilters())
        .as("every filter group must populate a non-blank id and a non-null values list")
        .allSatisfy(
            group -> {
              assertThat(group.getId()).as("group.id must be non-blank").isNotBlank();
              assertThat(group.getValues())
                  .as("group %s values must be a non-null list", group.getId())
                  .isNotNull();
            });
  }

  // ---------- API-rejection path ----------

  @Test
  void getSessionMetadata_should_surface_actionable_error_for_unknown_app_id() {
    // TeamServer rejects unknown applications with 403 Forbidden ("Authorization failure"),
    // which the SDK surfaces as UnauthorizedException and SingleTool maps to the
    // auth-or-not-found user message. Single deterministic outcome — no isSuccess?A:B
    // dual-path. A regression that surfaced this as a generic 5xx error or a stack trace
    // would surface here.
    var result = getSessionMetadataTool.getSessionMetadata(NONEXISTENT_APP_ID);

    assertThat(result.isSuccess())
        .as("unknown appId must surface as a non-success response")
        .isFalse();
    assertThat(result.found()).as("unknown appId must not report found").isFalse();
    assertThat(result.data()).as("error response must not carry data").isNull();
    assertThat(result.errors())
        .as("unknown appId must produce the documented auth-or-not-found error")
        .containsExactly(AUTH_OR_NOT_FOUND_ERROR);
    assertThat(result.errors())
        .as("unknown appId must not be surfaced as a generic 5xx Contrast API error")
        .noneMatch(e -> e.contains(CONTRAST_API_ERROR));
    assertThat(result.warnings())
        .as("error path must not also emit the no-metadata warning")
        .noneMatch(w -> w.contains(NO_METADATA_WARNING_FRAGMENT));
  }
}
