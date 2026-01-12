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
import com.contrast.labs.ai.mcp.contrast.util.AbstractIntegrationTest;
import com.contrast.labs.ai.mcp.contrast.util.TestDataDiscoveryHelper;
import com.contrast.labs.ai.mcp.contrast.util.TestDataDiscoveryHelper.ApplicationWithProtectRules;
import java.io.IOException;
import java.util.Optional;
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
    log.info("\n=== Integration Test: Validate test data discovery ===");

    assertThat(testData).as("Test data should have been discovered").isNotNull();
    assertThat(testData.appId).as("Test application ID should be set").isNotNull();
    assertThat(testData.ruleCount)
        .as("Test application should have at least 1 rule")
        .isGreaterThan(0);

    log.info("✓ Test data validated:");
    log.info("  App ID: {}", testData.appId);
    log.info("  App Name: {}", testData.appName);
    log.info("  Rule Count: {}", testData.ruleCount);
  }

  @Test
  void getProtectRules_should_return_rules_for_valid_appId() {
    log.info("\n=== Integration Test: get_protect_rules ===");

    assertThat(testData).as("Test data must be discovered before running tests").isNotNull();

    var response = getProtectRulesTool.getProtectRules(testData.appId);

    assertThat(response).as("Response should not be null").isNotNull();
    assertThat(response.isSuccess()).as("Response should be successful").isTrue();
    assertThat(response.data()).as("Data should not be null").isNotNull();
    assertThat(response.data().getRules()).as("Rules should not be null").isNotNull();
    assertThat(response.data().getRules().size())
        .as("Should have at least 1 rule")
        .isGreaterThan(0);

    log.info(
        "✓ Retrieved {} Protect rules for application: {}",
        response.data().getRules().size(),
        testData.appName);

    log.info("  Rules configured:");
    for (var rule : response.data().getRules()) {
      String mode = Optional.ofNullable(rule.getProduction()).orElse("not set");
      log.info("    - {} (production mode: {})", rule.getName(), mode);
    }

    for (var rule : response.data().getRules()) {
      assertThat(rule.getName()).as("Rule name should not be null").isNotNull();
    }
  }

  @Test
  void getProtectRules_should_return_error_for_null_appId() {
    log.info("\n=== Integration Test: Null app ID handling ===");

    var response = getProtectRulesTool.getProtectRules(null);

    assertThat(response).as("Response should not be null").isNotNull();
    assertThat(response.isSuccess()).as("Should not be successful").isFalse();
    assertThat(response.errors()).anyMatch(e -> e.contains("appId is required"));

    log.info("✓ Null app ID correctly rejected");
  }

  @Test
  void getProtectRules_should_return_error_for_empty_appId() {
    log.info("\n=== Integration Test: Empty app ID handling ===");

    var response = getProtectRulesTool.getProtectRules("");

    assertThat(response).as("Response should not be null").isNotNull();
    assertThat(response.isSuccess()).as("Should not be successful").isFalse();
    assertThat(response.errors()).anyMatch(e -> e.contains("appId is required"));

    log.info("✓ Empty app ID correctly rejected");
  }

  @Test
  void getProtectRules_should_handle_invalid_appId_gracefully() {
    log.info("\n=== Integration Test: Invalid app ID handling ===");

    var response = getProtectRulesTool.getProtectRules("invalid-app-id-12345");

    assertThat(response).as("Response should not be null").isNotNull();

    // API may return null/empty or throw exception - both are acceptable
    if (response.isSuccess()) {
      log.info("✓ API handled invalid app ID gracefully");
      if (response.data() == null) {
        log.info("  Response: null (no Protect data for invalid app)");
      }
    } else {
      log.info("✓ API rejected invalid app ID");
      log.info("  Errors: {}", response.errors());
    }
  }

  @Test
  void getProtectRules_should_return_rule_details() {
    log.info("\n=== Integration Test: Verify rule details ===");

    assertThat(testData).as("Test data must be discovered before running tests").isNotNull();

    var response = getProtectRulesTool.getProtectRules(testData.appId);

    assertThat(response.isSuccess()).isTrue();
    assertThat(response.data().getRules()).isNotEmpty();

    log.info("✓ Verifying rule details for {} rules:", response.data().getRules().size());

    for (var rule : response.data().getRules()) {
      log.info("\n  Rule: {}", rule.getName());
      assertThat(rule.getName()).as("Rule name is required").isNotNull();
      log.info("    ✓ Name: {}", rule.getName());
      if (rule.getProduction() != null) {
        log.info("    ✓ Production Mode: {}", rule.getProduction());
      }
    }

    log.info("\n✓ All rules have valid structure and required fields");
  }
}
