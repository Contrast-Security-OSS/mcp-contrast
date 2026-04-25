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
package com.contrast.labs.ai.mcp.contrast.tool.attack;

import static org.assertj.core.api.Assertions.assertThat;

import com.contrast.labs.ai.mcp.contrast.config.IntegrationTestConfig;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.Rule;
import com.contrast.labs.ai.mcp.contrast.util.AbstractIntegrationTest;
import com.contrast.labs.ai.mcp.contrast.util.TestDataDiscoveryHelper;
import com.contrast.labs.ai.mcp.contrast.util.TestDataDiscoveryHelper.ApplicationWithProtectRules;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Integration tests for GetProtectRulesTool.
 *
 * <p>These tests require Contrast credentials to be set in environment variables and at least one
 * application with Protect rules configured. Run: source .env.integration-test && mvn verify
 */
@Slf4j
@SpringBootTest
@Import(IntegrationTestConfig.class)
@EnabledIfEnvironmentVariable(named = "CONTRAST_HOST_NAME", matches = ".+")
class GetProtectRulesToolIT extends AbstractIntegrationTest<GetProtectRulesToolIT.TestData> {

  @Autowired private GetProtectRulesTool getProtectRulesTool;

  // Contrast Protect rule mode enumeration as returned by the TeamServer API (uppercase).
  // If a real response yields a value outside this set, the mode-assertion test below fails
  // loudly and this constant is updated after confirming the new mode is legitimate.
  private static final Set<String> KNOWN_PROTECT_MODES =
      Set.of(
          "MONITORING",
          "BLOCKING",
          "BLOCK_AT_PERIMETER",
          "OFF",
          "NO_ACTION",
          "PERMIT",
          "MONITOR_BLOCK",
          "DISABLED");

  // Rule.type values discriminating response shape. Standard Protect rules populate uuid, modes,
  // and perimeter capability flags; Virtual Patches populate enabledDev/Qa/Prod instead.
  private static final String TYPE_PROTECT_RULE = "Protect Rule";
  private static final String TYPE_VIRTUAL_PATCH = "Virtual Patch";

  static class TestData {
    String appId;
    String appName;
    int ruleCount;

    @Override
    public String toString() {
      return String.format(
          "TestData{appId='%s', appName='%s', ruleCount=%d}", appId, appName, ruleCount);
    }
  }

  @Override
  protected String testDisplayName() {
    return "GetProtectRulesTool Integration Test";
  }

  @Override
  protected Class<TestData> testDataType() {
    return TestData.class;
  }

  /**
   * Locates an application that has at least one Protect rule configured (standard rule or Virtual
   * Patch). The tests need a non-zero rule count to exercise {@code get_protect_rules}'s rule-shape
   * assertions ({@code allSatisfy} over {@code name}/{@code type}/{@code description}/{@code id}/
   * {@code canBlock}); a zero-rule app would silently pass the assertion. The discovered {@code
   * appId} also drives the happy-path retrieval test.
   */
  @Override
  protected TestData performDiscovery() throws IOException {
    Optional<ApplicationWithProtectRules> protectCandidate =
        TestDataDiscoveryHelper.findApplicationWithProtectRules(orgId, sdkExtension);

    if (protectCandidate.isEmpty()) {
      throw new NoTestDataException(
          "No application with Protect rules found. "
              + "Please configure Protect on at least one application.");
    }

    var candidate = protectCandidate.get();
    var data = new TestData();
    data.appId = candidate.getApplication().getAppId();
    data.appName = candidate.getApplication().getName();
    data.ruleCount = candidate.getRuleCount();
    return data;
  }

  @Override
  protected void logTestDataDetails(TestData data) {
    log.info("{}", data);
  }

  @Test
  void testDiscoveredTestDataExists() {
    assertThat(testData).as("discovery must populate test data").isNotNull();
    assertThat(testData.appId).as("discovery must resolve a non-blank app ID").isNotBlank();
    assertThat(testData.appName).as("discovery must resolve a non-blank app name").isNotBlank();
    assertThat(testData.ruleCount)
        .as("test application must have at least one Protect rule configured")
        .isPositive();
  }

  @Test
  void getProtectRules_should_return_rules_for_valid_appId() {
    var response = getProtectRulesTool.getProtectRules(testData.appId);

    assertThat(response).as("response must not be null").isNotNull();
    assertThat(response.errors()).as("valid appId must not produce errors").isEmpty();
    assertThat(response.isSuccess()).as("response must be successful").isTrue();
    assertThat(response.found()).as("response must indicate data was found").isTrue();
    assertThat(response.data()).as("data must not be null").isNotNull();

    var rules = response.data().getRules();
    assertThat(rules)
        .as("requires seeded Protect rules on app %s — see INTEGRATION_TESTS.md", testData.appName)
        .isNotEmpty();

    // Fields every rule type must populate, regardless of whether it's a standard Protect Rule
    // or a Virtual Patch: human-readable identity plus the canBlock capability flag.
    assertThat(rules)
        .as("every rule must populate core identifying fields across rule types")
        .allSatisfy(
            rule -> {
              assertThat(rule.getName()).as("rule (id=%s).name", rule.getId()).isNotBlank();
              assertThat(rule.getType()).as("rule %s.type", rule.getName()).isNotBlank();
              assertThat(rule.getDescription())
                  .as("rule %s.description", rule.getName())
                  .isNotBlank();
              assertThat(rule.getId()).as("rule %s.id", rule.getName()).isPositive();
              assertThat(rule.getCanBlock()).as("rule %s.canBlock", rule.getName()).isNotNull();
            });
  }

  @Test
  void getProtectRules_should_return_error_for_null_appId() {
    var response = getProtectRulesTool.getProtectRules(null);

    assertThat(response).as("response must not be null").isNotNull();
    assertThat(response.isSuccess()).as("null appId must fail validation").isFalse();
    assertThat(response.data()).as("null appId must not return data").isNull();
    assertThat(response.errors())
        .as("errors must state appId is required with exact shape")
        .containsExactly("appId is required");
    assertThat(response.errors())
        .as("must be a validation error, not a forwarded API error")
        .noneMatch(e -> e.contains("Contrast API error"))
        .noneMatch(e -> e.startsWith("Internal"));
  }

  @Test
  void getProtectRules_should_return_error_for_empty_appId() {
    var response = getProtectRulesTool.getProtectRules("");

    assertThat(response).as("response must not be null").isNotNull();
    assertThat(response.isSuccess()).as("empty appId must fail validation").isFalse();
    assertThat(response.data()).as("empty appId must not return data").isNull();
    assertThat(response.errors())
        .as("errors must state appId is required with exact shape")
        .containsExactly("appId is required");
    assertThat(response.errors())
        .as("must be a validation error, not a forwarded API error")
        .noneMatch(e -> e.contains("Contrast API error"))
        .noneMatch(e -> e.startsWith("Internal"));
  }

  @Test
  void getProtectRules_should_return_error_for_whitespace_appId() {
    // GetProtectRulesParams uses StringUtils.hasText() which treats whitespace-only as blank.
    var response = getProtectRulesTool.getProtectRules("   ");

    assertThat(response).as("response must not be null").isNotNull();
    assertThat(response.isSuccess()).as("whitespace-only appId must fail validation").isFalse();
    assertThat(response.data()).as("whitespace-only appId must not return data").isNull();
    assertThat(response.errors())
        .as("errors must state appId is required with exact shape")
        .containsExactly("appId is required");
    assertThat(response.errors())
        .as("must be a validation error, not a forwarded API error")
        .noneMatch(e -> e.contains("Contrast API error"))
        .noneMatch(e -> e.startsWith("Internal"));
  }

  @Test
  void getProtectRules_should_not_return_populated_data_for_invalid_appId() {
    // Single deterministic expectation: whether the API errors or returns an empty/not-found
    // payload, the tool must never surface populated Protect data for a bogus app ID.
    var response = getProtectRulesTool.getProtectRules("invalid-app-id-12345");

    assertThat(response).as("response must not be null").isNotNull();
    assertThat(response.data()).as("invalid appId must not return populated Protect data").isNull();
    assertThat(response.found()).as("invalid appId must not be marked as found").isFalse();
  }

  @Test
  void getProtectRules_should_populate_protect_rule_details() {
    var response = getProtectRulesTool.getProtectRules(testData.appId);
    assertThat(response.isSuccess()).as("response must be successful").isTrue();

    var protectRules = rulesOfType(response.data().getRules(), TYPE_PROTECT_RULE);
    assertThat(protectRules)
        .as(
            "requires seeded standard Protect Rules (type='%s') on app %s — see"
                + " INTEGRATION_TESTS.md",
            TYPE_PROTECT_RULE, testData.appName)
        .isNotEmpty();

    // Standard Protect Rules (e.g., sql-injection, path-traversal) populate the complete shape:
    // slug uuid, per-environment mode strings, perimeter capability booleans, and parent-rule
    // linkage (top-level rules self-reference via parentRuleUuid==uuid).
    assertThat(protectRules)
        .as("every standard Protect Rule must populate its full field shape")
        .allSatisfy(
            rule -> {
              assertThat(rule.getUuid()).as("%s.uuid", rule.getName()).isNotBlank();
              assertThat(rule.getProduction()).as("%s.production", rule.getName()).isNotBlank();
              assertThat(rule.getDevelopment()).as("%s.development", rule.getName()).isNotBlank();
              assertThat(rule.getQa()).as("%s.qa", rule.getName()).isNotBlank();
              assertThat(rule.getCanBlockAtPerimeter())
                  .as("%s.canBlockAtPerimeter", rule.getName())
                  .isNotNull();
              assertThat(rule.getIsMonitorAtPerimeter())
                  .as("%s.isMonitorAtPerimeter", rule.getName())
                  .isNotNull();
              assertThat(rule.getParentRuleUuid())
                  .as("%s.parentRuleUuid", rule.getName())
                  .isNotBlank();
              assertThat(rule.getParentRuleName())
                  .as("%s.parentRuleName", rule.getName())
                  .isNotBlank();
            });
  }

  @Test
  void getProtectRules_should_return_only_known_protect_modes() {
    var response = getProtectRulesTool.getProtectRules(testData.appId);
    var protectRules = rulesOfType(response.data().getRules(), TYPE_PROTECT_RULE);
    assertThat(protectRules)
        .as("requires seeded standard Protect Rules on app %s", testData.appName)
        .isNotEmpty();

    // Every environment mode must match the known Contrast Protect enumeration. An unknown value
    // indicates either a newly introduced mode (update KNOWN_PROTECT_MODES) or a deserialization
    // regression.
    assertThat(protectRules)
        .as(
            "every production/development/qa mode must be in known Protect mode set %s",
            KNOWN_PROTECT_MODES)
        .allSatisfy(
            rule -> {
              assertThat(rule.getProduction())
                  .as("%s.production", rule.getName())
                  .isIn(KNOWN_PROTECT_MODES);
              assertThat(rule.getDevelopment())
                  .as("%s.development", rule.getName())
                  .isIn(KNOWN_PROTECT_MODES);
              assertThat(rule.getQa()).as("%s.qa", rule.getName()).isIn(KNOWN_PROTECT_MODES);
            });
  }

  @Test
  void getProtectRules_should_populate_virtual_patch_enablement_flags() {
    var response = getProtectRulesTool.getProtectRules(testData.appId);
    var virtualPatches = rulesOfType(response.data().getRules(), TYPE_VIRTUAL_PATCH);

    assertThat(virtualPatches)
        .as(
            "requires seeded Virtual Patch (type='%s') on app %s to verify enabledDev/Qa/Prod —"
                + " see INTEGRATION_TESTS.md",
            TYPE_VIRTUAL_PATCH, testData.appName)
        .isNotEmpty();

    // Virtual Patches use per-environment Boolean enablement flags instead of mode strings.
    // Assert all three are populated; a null indicates a deserialization or API-shape regression.
    assertThat(virtualPatches)
        .as("every Virtual Patch must populate enabledDev/enabledQa/enabledProd")
        .allSatisfy(
            rule -> {
              assertThat(rule.getEnabledDev()).as("%s.enabledDev", rule.getName()).isNotNull();
              assertThat(rule.getEnabledQa()).as("%s.enabledQa", rule.getName()).isNotNull();
              assertThat(rule.getEnabledProd()).as("%s.enabledProd", rule.getName()).isNotNull();
            });
  }

  @Test
  void getProtectRules_should_populate_cves_consistently() {
    var response = getProtectRulesTool.getProtectRules(testData.appId);
    var rules = response.data().getRules();
    assertThat(rules).as("response must contain rules to verify").isNotEmpty();

    // cves is nullable per rule; when populated it must contain non-null Cve entries. This catches
    // both an accidental empty list (API shape change) and any null pollution in the list.
    assertThat(rules)
        .as("cves, when populated, must be a non-empty list with no null entries")
        .allSatisfy(
            rule -> {
              if (rule.getCves() != null) {
                assertThat(rule.getCves())
                    .as("%s.cves", rule.getName())
                    .isNotEmpty()
                    .doesNotContainNull();
              }
            });
  }

  private static List<Rule> rulesOfType(List<Rule> rules, String type) {
    return rules.stream().filter(r -> type.equals(r.getType())).toList();
  }
}
