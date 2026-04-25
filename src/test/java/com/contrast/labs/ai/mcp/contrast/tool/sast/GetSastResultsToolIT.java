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
package com.contrast.labs.ai.mcp.contrast.tool.sast;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Integration tests for {@link GetSastResultsTool}.
 *
 * <p>Requires Contrast credentials and a seeded SAST project with at least one completed scan.
 * Configure via {@code application-integration-test.properties} ({@code test.scan.project-name})
 * and run via {@code source .env.integration-test && mvn verify}. See INTEGRATION_TESTS.md.
 *
 * <p>The "no completed scans" and "scan id not found" warning paths are covered by {@link
 * GetSastResultsToolTest} (mocked) — they are not reproducible against a live organisation without
 * dedicated, brittle fixtures.
 */
@Slf4j
@SpringBootTest
@TestPropertySource(locations = "classpath:application-integration-test.properties")
@Tag("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfEnvironmentVariable(named = "CONTRAST_HOST_NAME", matches = ".+")
class GetSastResultsToolIT {

  @Autowired private GetSastResultsTool getSastResultsTool;

  @Value("${test.scan.project-name:}")
  private String testProjectName;

  // Verbatim deprecation warning emitted by GetSastResultsTool#doExecute. Pinning to the full
  // message catches regressions where the wording drifts (and AI consumers' downstream parsing
  // breaks) far better than a "DEPRECATED" substring match.
  private static final String DEPRECATION_WARNING =
      "DEPRECATED: This tool returns raw SARIF which may be very large. "
          + "Consider using future paginated SAST search tools for better AI-friendly access.";

  // Validation message format from ToolValidationContext#require — see SingleTool#executePipeline.
  private static final String PROJECT_NAME_REQUIRED = "projectName is required";

  // 5xx mapping in BaseTool#mapHttpErrorCode. Validation paths and resource-not-found paths must
  // never surface as this — guard against regressions that catch validation failures with the
  // generic 5xx handler.
  private static final String CONTRAST_API_ERROR_FRAGMENT = "Contrast API error";

  // SARIF v2.1.0 — the only spec version Contrast Scan emits. See @Tool description.
  private static final String SARIF_VERSION = "2.1.0";

  // Driver name promised by GetSastResultsTool's @Tool description ("Tool information (Contrast
  // Scan)"). A regression that swapped the SARIF source would surface here.
  private static final String CONTRAST_SCAN_DRIVER_NAME = "Contrast Scan";

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @BeforeAll
  void verifyConfiguration() {
    // Replaces the previous Assumptions.assumeTrue, which silently skipped the entire class when
    // configuration was missing. Failing loudly surfaces config drift in CI rather than allowing
    // a green build with zero coverage.
    assertThat(testProjectName)
        .as(
            "requires test.scan.project-name in application-integration-test.properties — "
                + "see INTEGRATION_TESTS.md")
        .isNotBlank();
  }

  // ---------- Successful retrieval ----------

  @Test
  void getScanResults_should_return_parseable_sarif_2_1_0() throws Exception {
    var response = getSastResultsTool.getScanResults(testProjectName);

    assertThat(response).isNotNull();
    assertThat(response.isSuccess())
        .as("seeded SAST project '%s' must produce a successful response", testProjectName)
        .isTrue();
    assertThat(response.errors()).as("successful query must surface no errors").isEmpty();
    assertThat(response.data())
        .as("SARIF data must be populated for a project with completed scans")
        .isNotBlank();

    // Parse as JSON so a malformed body (e.g., truncated stream, gzipped payload, HTML error
    // page returned with 200) fails here rather than passing a substring check.
    var root = OBJECT_MAPPER.readTree(response.data());
    assertThat(root.isObject()).as("SARIF root must be a JSON object").isTrue();
    assertThat(root.has("$schema"))
        .as("SARIF root must declare $schema (mandatory in 2.1.0)")
        .isTrue();
    assertThat(root.path("$schema").asText()).as("$schema must be a non-blank string").isNotBlank();
    assertThat(root.path("version").asText())
        .as("Contrast Scan emits SARIF %s", SARIF_VERSION)
        .isEqualTo(SARIF_VERSION);
    assertThat(root.path("runs").isArray()).as("SARIF runs must be a JSON array").isTrue();
  }

  @Test
  void getScanResults_should_expose_contrast_scan_tool_info() throws Exception {
    var response = getSastResultsTool.getScanResults(testProjectName);

    assertThat(response.isSuccess()).as("seeded query must succeed").isTrue();

    var root = OBJECT_MAPPER.readTree(response.data());
    var runs = root.path("runs");
    assertThat(runs.isArray()).as("runs must be a JSON array").isTrue();
    // Seeded project has at least one completed scan, which always produces one run. An empty
    // runs array against a project with completedScans>0 indicates a serializer regression.
    assertThat(runs)
        .as("seeded project '%s' must surface at least one SARIF run", testProjectName)
        .isNotEmpty();

    JsonNode firstRun = runs.get(0);
    var driver = firstRun.path("tool").path("driver");
    assertThat(driver.isObject())
        .as("runs[0].tool.driver must be a JSON object per SARIF 2.1.0")
        .isTrue();
    assertThat(driver.path("name").asText())
        .as("runs[0].tool.driver.name must identify Contrast Scan")
        .isEqualTo(CONTRAST_SCAN_DRIVER_NAME);
    assertThat(driver.path("rules").isArray())
        .as("runs[0].tool.driver.rules must be a JSON array (may be empty)")
        .isTrue();
    assertThat(firstRun.path("results").isArray())
        .as("runs[0].results must be a JSON array (may be empty)")
        .isTrue();
  }

  @Test
  void getScanResults_should_emit_full_deprecation_warning() {
    var response = getSastResultsTool.getScanResults(testProjectName);

    assertThat(response.isSuccess())
        .as("query must succeed before warnings are inspected (single deterministic outcome)")
        .isTrue();
    // Assert the verbatim message — substring "DEPRECATED" matches any rewording, including
    // garbage text that happens to contain the token, which would not actually warn AI consumers.
    assertThat(response.warnings())
        .as("response must contain the verbatim deprecation warning")
        .contains(DEPRECATION_WARNING);
  }

  // ---------- Validation errors ----------

  @Test
  void getScanResults_should_return_validation_error_for_null_projectName() {
    var response = getSastResultsTool.getScanResults(null);

    assertThat(response.isSuccess()).as("null projectName must fail validation").isFalse();
    assertThat(response.found()).as("validation failure must not report found").isFalse();
    assertThat(response.data()).as("validation failure must not carry data").isNull();
    // Assert exact message — a bare contains("projectName") match would coincidentally succeed
    // on any error mentioning the parameter name (e.g., a 5xx wrapping the field).
    assertThat(response.errors())
        .as("validation must produce the documented projectName-required message")
        .containsExactly(PROJECT_NAME_REQUIRED);
    assertThat(response.errors())
        .as("validation error must not be surfaced as a 5xx Contrast API error")
        .noneMatch(e -> e.contains(CONTRAST_API_ERROR_FRAGMENT));
  }

  @Test
  void getScanResults_should_return_validation_error_for_empty_projectName() {
    var response = getSastResultsTool.getScanResults("");

    assertThat(response.isSuccess()).as("empty projectName must fail validation").isFalse();
    assertThat(response.errors()).containsExactly(PROJECT_NAME_REQUIRED);
    assertThat(response.errors()).noneMatch(e -> e.contains(CONTRAST_API_ERROR_FRAGMENT));
  }

  @Test
  void getScanResults_should_return_validation_error_for_blank_projectName() {
    // Whitespace-only must be rejected by ToolValidationContext#require (StringUtils.hasText) —
    // not silently passed to the SDK where it would surface as a generic notFound.
    var response = getSastResultsTool.getScanResults("   ");

    assertThat(response.isSuccess())
        .as("whitespace-only projectName must fail validation")
        .isFalse();
    assertThat(response.errors()).containsExactly(PROJECT_NAME_REQUIRED);
    assertThat(response.errors()).noneMatch(e -> e.contains(CONTRAST_API_ERROR_FRAGMENT));
  }

  // ---------- Project lookup edge cases ----------

  @Test
  void getScanResults_should_return_notFound_for_nonexistent_project() {
    var nonExistent = "nonexistent-project-" + System.currentTimeMillis();
    var response = getSastResultsTool.getScanResults(nonExistent);

    // SingleTool maps doExecute() returning null to a notFound response: errors=[], found=false,
    // data=null. isSuccess() is true because errors is empty — explicitly assert this contract
    // so a regression that flipped it to an error path would surface.
    assertThat(response.isSuccess()).as("notFound is not an error").isTrue();
    assertThat(response.found()).as("missing project must report found=false").isFalse();
    assertThat(response.data()).as("notFound must not carry data").isNull();
    assertThat(response.errors()).as("notFound must not surface as an error").isEmpty();
    // Deprecation warning is added before the project lookup, so it must still be emitted on
    // the notFound path. A regression that short-circuited warnings on null doExecute would
    // surface here.
    assertThat(response.warnings())
        .as("deprecation warning must still be emitted for missing projects")
        .contains(DEPRECATION_WARNING);
  }

  @Test
  void getScanResults_should_treat_project_name_as_case_sensitive() {
    // Per the @Tool description: "Project names are case-sensitive and must match exactly."
    // Inverting the case of a known-good name must yield notFound — never silently match a
    // differently-cased project. Guard the precondition so a numeric/symbolic seed name (no-op
    // case inversion) fails loudly rather than passing on coincidence.
    var inverted = invertCase(testProjectName);
    assertThat(inverted)
        .as(
            "test.scan.project-name='%s' must contain at least one alphabetic character to "
                + "exercise case-sensitivity",
            testProjectName)
        .isNotEqualTo(testProjectName);

    var response = getSastResultsTool.getScanResults(inverted);

    assertThat(response.found())
        .as("case-inverted name '%s' must not match seeded project '%s'", inverted, testProjectName)
        .isFalse();
    assertThat(response.data()).isNull();
  }

  private static String invertCase(String input) {
    var chars = input.toCharArray();
    for (int i = 0; i < chars.length; i++) {
      var c = chars[i];
      if (Character.isUpperCase(c)) {
        chars[i] = Character.toLowerCase(c);
      } else if (Character.isLowerCase(c)) {
        chars[i] = Character.toUpperCase(c);
      }
    }
    return new String(chars);
  }
}
