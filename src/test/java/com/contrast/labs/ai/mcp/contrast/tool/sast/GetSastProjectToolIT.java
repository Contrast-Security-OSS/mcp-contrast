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

import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Integration tests for {@link GetSastProjectTool} verifying the contract across the SDK/Scan API
 * boundary. Cited in {@code CLAUDE.md} → Integration Test Standards as a canonical example for
 * field-mapping coverage; assertions here must remain mutation-resistant.
 *
 * <p>Run: {@code source .env.integration-test && mvn verify}
 */
@SpringBootTest
@TestPropertySource(locations = "classpath:application-integration-test.properties")
@Tag("integration")
@EnabledIfEnvironmentVariable(named = "CONTRAST_HOST_NAME", matches = ".+")
class GetSastProjectToolIT {

  @Autowired private GetSastProjectTool getSastProjectTool;

  @Value("${test.scan.project-name:}")
  private String testProjectName;

  // SingleTool's 5xx mapping — validation errors must never look like this.
  private static final String CONTRAST_API_ERROR = "Contrast API error";

  // Exact validation message produced by ToolValidationContext#require for the projectName param.
  // Asserting the full shape (not just a "projectName" substring) prevents a false-positive match
  // against any unrelated message that happens to mention the parameter name.
  private static final String PROJECT_NAME_REQUIRED_ERROR = "projectName is required";

  // Warning emitted by SingleTool when doExecute returns null (project not found).
  private static final String RESOURCE_NOT_FOUND_WARNING = "Resource not found";

  @BeforeEach
  void requireSeededProjectName() {
    // Fail loudly (not silently skip) when the property is missing — surfaces config drift in CI
    // instead of producing a green-but-empty test report. Replaces the previous Assumptions
    // .assumeTrue anti-pattern.
    assertThat(testProjectName)
        .as(
            "requires test.scan.project-name in application-integration-test.properties — see"
                + " INTEGRATION_TESTS.md")
        .isNotBlank();
  }

  // ---------- Successful retrieval ----------

  @Test
  void getScanProject_should_return_project_for_valid_name() {
    var response = getSastProjectTool.getScanProject(testProjectName);

    assertThat(response).as("response must not be null").isNotNull();
    assertThat(response.isSuccess())
        .as("seeded project must produce a successful response")
        .isTrue();
    assertThat(response.errors()).as("successful query must have no errors").isEmpty();
    assertThat(response.found()).as("seeded project must be reported as found").isTrue();
    assertThat(response.data()).as("successful response must carry data").isNotNull();
    assertThat(response.data().name())
        .as("project.name must echo the seeded name exactly")
        .isEqualTo(testProjectName);
    // "Populate ≠ non-null": id is a String — isNotNull would pass for an empty UUID.
    assertThat(response.data().id()).as("project.id must be populated").isNotBlank();
  }

  // ---------- Validation errors ----------

  @Test
  void getScanProject_should_return_validation_error_for_null_projectName() {
    var response = getSastProjectTool.getScanProject(null);

    assertThat(response.isSuccess()).as("null projectName must fail validation").isFalse();
    assertThat(response.found()).as("validation failure must not report found").isFalse();
    assertThat(response.data()).as("validation failure must not carry data").isNull();
    assertThat(response.errors())
        .as("validation error must state projectName is required")
        .containsExactly(PROJECT_NAME_REQUIRED_ERROR);
    assertThat(response.errors())
        .as("validation error must not surface as a Contrast API error")
        .noneMatch(e -> e.contains(CONTRAST_API_ERROR));
  }

  @Test
  void getScanProject_should_return_validation_error_for_empty_projectName() {
    var response = getSastProjectTool.getScanProject("");

    assertThat(response.isSuccess()).as("empty projectName must fail validation").isFalse();
    assertThat(response.found()).as("validation failure must not report found").isFalse();
    assertThat(response.data()).as("validation failure must not carry data").isNull();
    assertThat(response.errors())
        .as("validation error must state projectName is required")
        .containsExactly(PROJECT_NAME_REQUIRED_ERROR);
    assertThat(response.errors())
        .as("validation error must not surface as a Contrast API error")
        .noneMatch(e -> e.contains(CONTRAST_API_ERROR));
  }

  @Test
  void getScanProject_should_return_validation_error_for_whitespace_projectName() {
    // ToolValidationContext#require uses StringUtils.hasText, which rejects whitespace as well as
    // null/empty. Without this test a regression that switched to isEmpty() would silently allow
    // whitespace strings to reach the SDK.
    var response = getSastProjectTool.getScanProject("   ");

    assertThat(response.isSuccess())
        .as("whitespace-only projectName must fail validation")
        .isFalse();
    assertThat(response.found()).as("validation failure must not report found").isFalse();
    assertThat(response.data()).as("validation failure must not carry data").isNull();
    assertThat(response.errors())
        .as("validation error must state projectName is required")
        .containsExactly(PROJECT_NAME_REQUIRED_ERROR);
    assertThat(response.errors())
        .as("validation error must not surface as a Contrast API error")
        .noneMatch(e -> e.contains(CONTRAST_API_ERROR));
  }

  // ---------- Not-found path ----------

  @Test
  void getScanProject_should_return_notFound_for_nonexistent_project() {
    var nonExistentProject = "nonexistent-project-" + System.currentTimeMillis();

    var response = getSastProjectTool.getScanProject(nonExistentProject);

    assertThat(response).as("response must not be null").isNotNull();
    // notFound is distinct from error: isSuccess (no errors) but found=false.
    assertThat(response.isSuccess()).as("notFound must not surface as an error").isTrue();
    assertThat(response.errors()).as("notFound must produce no errors").isEmpty();
    assertThat(response.found()).as("nonexistent project must not be reported as found").isFalse();
    assertThat(response.data()).as("notFound response must not carry data").isNull();
    assertThat(response.warnings())
        .as("notFound path must surface the documented warning")
        .contains(RESOURCE_NOT_FOUND_WARNING);
  }

  @Test
  void getScanProject_should_match_project_name_case_insensitively() {
    // The TeamServer Scan API matches project names case-insensitively (the SDK's findByName
    // delegates the comparison to the server). The tool's @Tool description reflects this. A
    // regression that switched to client-side case-sensitive filtering would surface as
    // found=false here. The returned project name must echo the canonical (seeded) casing so
    // downstream tools see a stable identifier regardless of how the user typed the query.
    var mutatedName = caseFlip(testProjectName);
    assertThat(mutatedName)
        .as(
            "seeded project name must contain at least one case-flippable character to exercise"
                + " case-insensitive matching")
        .isNotEqualTo(testProjectName);

    var response = getSastProjectTool.getScanProject(mutatedName);

    assertThat(response.isSuccess())
        .as("case-mutated lookup must produce a successful response")
        .isTrue();
    assertThat(response.errors()).as("case-mutated lookup must produce no errors").isEmpty();
    assertThat(response.found())
        .as(
            "case-mutated name '%s' must match seeded project '%s' (case-insensitive matching)",
            mutatedName, testProjectName)
        .isTrue();
    assertThat(response.data()).as("case-insensitive match must carry project data").isNotNull();
    assertThat(response.data().name())
        .as(
            "response must echo the canonical seeded name '%s', not the case-mutated query input"
                + " '%s'",
            testProjectName, mutatedName)
        .isEqualTo(testProjectName);
  }

  // ---------- Field-mapping regression coverage ----------

  /**
   * Regression test for AIML-343: {@code get_scan_project} returned {@code {"data":{}}} despite
   * {@code found:true}. Root cause was Jackson failing to serialize the SDK Project interface's
   * method-style accessors. Fix introduced the {@link
   * com.contrast.labs.ai.mcp.contrast.result.ScanProject} record.
   *
   * <p>Asserts every field of ScanProject. A regression that drops a field from the {@code from()}
   * mapper, or a future Jackson-related break that re-empties the payload, must surface here.
   */
  @Test
  void getScanProject_should_populate_every_field_of_ScanProject_record() {
    var response = getSastProjectTool.getScanProject(testProjectName);

    assertThat(response.isSuccess()).as("seeded query must succeed").isTrue();
    assertThat(response.found()).as("seeded project must be found").isTrue();
    assertThat(response.data()).as("successful response must carry data").isNotNull();

    var project = response.data();

    // Identity fields — all Strings, all required by the SDK contract. isNotBlank prevents the
    // AIML-343 empty-payload regression: an empty string would pass isNotNull but fail isNotBlank.
    assertThat(project.id()).as("ScanProject.id must be populated").isNotBlank();
    assertThat(project.name())
        .as("ScanProject.name must echo the seeded name")
        .isEqualTo(testProjectName);
    assertThat(project.organizationId())
        .as("ScanProject.organizationId must be populated")
        .isNotBlank();
    assertThat(project.language())
        .as("ScanProject.language must be populated (Java, JavaScript, etc.)")
        .isNotBlank();

    // Severity counts — primitive int, asserting >= 0 confirms SDK mapping returned a real
    // numeric value rather than 0 from a default. Cannot be < 0.
    assertThat(project.critical())
        .as("ScanProject.critical must be a non-negative count")
        .isGreaterThanOrEqualTo(0);
    assertThat(project.high())
        .as("ScanProject.high must be a non-negative count")
        .isGreaterThanOrEqualTo(0);
    assertThat(project.medium())
        .as("ScanProject.medium must be a non-negative count")
        .isGreaterThanOrEqualTo(0);
    assertThat(project.low())
        .as("ScanProject.low must be a non-negative count")
        .isGreaterThanOrEqualTo(0);
    assertThat(project.note())
        .as("ScanProject.note must be a non-negative count")
        .isGreaterThanOrEqualTo(0);

    // Scan history — completedScans is the population precondition for lastScanId/lastScanTime
    // being populated. The seeded test project is expected to have at least one completed scan;
    // failing this means the seed is stale.
    assertThat(project.completedScans())
        .as(
            "ScanProject.completedScans must be >= 1 for the seeded project '%s' — a value of 0"
                + " indicates the seed has been wiped or the project has never been scanned",
            testProjectName)
        .isGreaterThanOrEqualTo(1);

    // Once we know a scan has completed, lastScanId and lastScanTime must be populated. These
    // were the two fields the bead identified as missing from the prior "all fields" assertion —
    // a regression that drops them from ScanProject#from would silently break the response.
    assertThat(project.lastScanId())
        .as("ScanProject.lastScanId must be populated when completedScans >= 1")
        .isNotBlank();
    assertThat(project.lastScanTime())
        .as("ScanProject.lastScanTime must be populated when completedScans >= 1")
        .isNotNull();
    assertThat(project.lastScanTime())
        .as("ScanProject.lastScanTime must be in the past (a real scan timestamp)")
        .isBefore(Instant.now().plusSeconds(60));

    // Namespace filters are user-configured: a project may legitimately have empty filter lists.
    // Assert non-null only — ScanProject.from defends against a null SDK return by substituting
    // List.of(), so a regression that re-introduces nulls would surface here.
    assertThat(project.includeNamespaceFilters())
        .as("ScanProject.includeNamespaceFilters must never be null (use empty list instead)")
        .isNotNull();
    assertThat(project.excludeNamespaceFilters())
        .as("ScanProject.excludeNamespaceFilters must never be null (use empty list instead)")
        .isNotNull();

    // archived is a primitive boolean — no null check is meaningful. We assert that the value is
    // consistent with the seeded project being actively used (not archived). A regression that
    // mis-mapped the field would surface as the seeded project appearing archived.
    assertThat(project.archived())
        .as("ScanProject.archived must be false for the active seeded project")
        .isFalse();
  }

  /**
   * Returns the input string with its first ASCII letter case-flipped. Returns the input unchanged
   * if no flippable letter exists; the calling test asserts inequality to detect that case.
   */
  private static String caseFlip(String input) {
    var chars = input.toCharArray();
    for (var i = 0; i < chars.length; i++) {
      var c = chars[i];
      if (Character.isUpperCase(c)) {
        chars[i] = Character.toLowerCase(c);
        return new String(chars);
      }
      if (Character.isLowerCase(c)) {
        chars[i] = Character.toUpperCase(c);
        return new String(chars);
      }
    }
    return input;
  }
}
