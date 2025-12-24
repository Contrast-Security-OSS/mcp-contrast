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
package com.contrast.labs.ai.mcp.contrast.tool.assess;

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
 * Integration test for SearchVulnerabilitiesTool that validates vulnerability search against real
 * TeamServer.
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
public class SearchVulnerabilitiesToolIT
    extends AbstractIntegrationTest<SearchVulnerabilitiesToolIT.TestData> {

  @Autowired private SearchVulnerabilitiesTool searchVulnerabilitiesTool;

  /** Container for discovered test data - no specific data needed for basic search. */
  static class TestData {
    boolean hasVulnerabilities;

    @Override
    public String toString() {
      return String.format("TestData{hasVulnerabilities=%s}", hasVulnerabilities);
    }
  }

  @Override
  protected String testDisplayName() {
    return "SearchVulnerabilitiesTool Integration Test";
  }

  @Override
  protected Class<TestData> testDataType() {
    return TestData.class;
  }

  @Override
  protected void logTestDataDetails(TestData data) {
    log.info("Test data: hasVulnerabilities={}", data.hasVulnerabilities);
  }

  @Override
  protected TestData performDiscovery() throws IOException {
    // Just check if we can get any vulnerabilities
    var response =
        searchVulnerabilitiesTool.searchVulnerabilities(
            1, 5, null, null, null, null, null, null, null);

    var testData = new TestData();
    testData.hasVulnerabilities = response.isSuccess() && !response.items().isEmpty();

    log.info(
        "Discovery: found {} vulnerabilities (hasVulns={})",
        response.items().size(),
        testData.hasVulnerabilities);

    return testData;
  }

  @Test
  void searchVulnerabilities_should_return_valid_response() {
    var response =
        searchVulnerabilitiesTool.searchVulnerabilities(
            1, 10, null, null, null, null, null, null, null);

    assertThat(response).isNotNull();
    assertThat(response.isSuccess()).isTrue();
    assertThat(response.page()).isEqualTo(1);
    assertThat(response.pageSize()).isEqualTo(10);
    // Items may or may not be present depending on org data
    assertThat(response.items()).isNotNull();
  }

  @Test
  void searchVulnerabilities_should_filter_by_severity() {
    var response =
        searchVulnerabilitiesTool.searchVulnerabilities(
            1, 10, "CRITICAL,HIGH", null, null, null, null, null, null);

    assertThat(response.isSuccess()).isTrue();
    // If we have results, verify they match filter (API returns capitalized: Critical, High)
    if (!response.items().isEmpty()) {
      assertThat(response.items())
          .allMatch(
              v ->
                  "Critical".equalsIgnoreCase(v.severity())
                      || "High".equalsIgnoreCase(v.severity()));
    }
  }

  @Test
  void searchVulnerabilities_should_filter_by_environment() {
    var response =
        searchVulnerabilitiesTool.searchVulnerabilities(
            1, 10, null, null, null, "PRODUCTION", null, null, null);

    assertThat(response.isSuccess()).isTrue();
    // Results may be empty if no production vulns exist
    assertThat(response.items()).isNotNull();
  }

  @Test
  void searchVulnerabilities_should_include_default_status_warning() {
    var response =
        searchVulnerabilitiesTool.searchVulnerabilities(
            1, 10, null, null, null, null, null, null, null);

    assertThat(response.isSuccess()).isTrue();
    assertThat(response.warnings()).anyMatch(w -> w.contains("excluding Fixed and Remediated"));
  }

  @Test
  void searchVulnerabilities_should_handle_pagination() {
    // First page
    var page1 =
        searchVulnerabilitiesTool.searchVulnerabilities(
            1, 5, null, null, null, null, null, null, null);

    assertThat(page1.isSuccess()).isTrue();

    // If there are more pages, fetch page 2
    if (page1.hasMorePages()) {
      var page2 =
          searchVulnerabilitiesTool.searchVulnerabilities(
              2, 5, null, null, null, null, null, null, null);

      assertThat(page2.isSuccess()).isTrue();
      assertThat(page2.page()).isEqualTo(2);
    }
  }

  @Test
  void searchVulnerabilities_should_populate_environments_and_tags() {
    var response =
        searchVulnerabilitiesTool.searchVulnerabilities(
            1, 10, null, null, null, null, null, null, null);

    assertThat(response.isSuccess()).isTrue();

    if (!response.items().isEmpty()) {
      // Verify each vulnerability has non-null environments and tags collections
      for (var vuln : response.items()) {
        assertThat(vuln.environments())
            .as("Environments should never be null for vuln %s", vuln.vulnID())
            .isNotNull();
        assertThat(vuln.tags())
            .as("Tags should never be null for vuln %s", vuln.vulnID())
            .isNotNull();
      }
      log.info(
          "✓ All {} vulnerabilities have non-null environments and tags", response.items().size());
    }
  }

  @Test
  void searchVulnerabilities_should_populate_session_metadata() {
    var response =
        searchVulnerabilitiesTool.searchVulnerabilities(
            1, 10, null, null, null, null, null, null, null);

    assertThat(response.isSuccess()).isTrue();

    if (!response.items().isEmpty()) {
      int withSessionMetadata = 0;

      for (var vuln : response.items()) {
        assertThat(vuln.sessionMetadata())
            .as("Session metadata should never be null for vuln %s", vuln.vulnID())
            .isNotNull();

        if (!vuln.sessionMetadata().isEmpty()) {
          withSessionMetadata++;
        }
      }

      log.info(
          "✓ Session metadata field present on all vulns ({}/{} have session data)",
          withSessionMetadata,
          response.items().size());
    }
  }

  @Test
  void searchVulnerabilities_should_populate_basic_fields() {
    var response =
        searchVulnerabilitiesTool.searchVulnerabilities(
            1, 5, null, null, null, null, null, null, null);

    assertThat(response.isSuccess()).isTrue();

    if (!response.items().isEmpty()) {
      for (var vuln : response.items()) {
        // Core identification fields
        assertThat(vuln.title()).as("Title should not be null").isNotNull();
        assertThat(vuln.type()).as("Type should not be null").isNotNull();
        assertThat(vuln.vulnID()).as("VulnID should not be null").isNotNull();
        assertThat(vuln.severity()).as("Severity should not be null").isNotNull();
        assertThat(vuln.status()).as("Status should not be null").isNotNull();

        // Application correlation fields (from APPLICATION expand)
        assertThat(vuln.appID())
            .as("appID should not be null (APPLICATION expand)")
            .isNotNull()
            .isNotEmpty();
        assertThat(vuln.appName())
            .as("appName should not be null (APPLICATION expand)")
            .isNotNull()
            .isNotEmpty();

        log.info(
            "✓ {}: {} ({}) - App: {} ({})",
            vuln.vulnID(),
            vuln.title(),
            vuln.severity(),
            vuln.appName(),
            vuln.appID());
      }
    }
  }

  @Test
  void searchVulnerabilities_should_handle_vulnTags_with_spaces() {
    // Query with a tag that contains spaces - SDK should handle URL encoding
    var response =
        searchVulnerabilitiesTool.searchVulnerabilities(
            1, 50, null, null, null, null, null, null, "SmartFix Remediated");

    // The query should complete without error
    assertThat(response).as("Response should not be null").isNotNull();
    assertThat(response.isSuccess()).isTrue();

    log.info(
        "✓ Query with 'SmartFix Remediated' tag completed ({} results)", response.items().size());

    // Try with multiple tags including spaces
    var multiTagResponse =
        searchVulnerabilitiesTool.searchVulnerabilities(
            1, 10, null, null, null, null, null, null, "Tag With Spaces,another-tag");

    assertThat(multiTagResponse).as("Response should not be null").isNotNull();
    assertThat(multiTagResponse.isSuccess()).isTrue();
    log.info("✓ Query with multiple tags (including spaces) completed");
  }
}
