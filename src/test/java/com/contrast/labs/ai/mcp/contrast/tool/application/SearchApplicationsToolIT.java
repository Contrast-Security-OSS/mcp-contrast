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
 * Integration test for SearchApplicationsTool that validates application search against real
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
public class SearchApplicationsToolIT
    extends AbstractIntegrationTest<SearchApplicationsToolIT.TestData> {

  @Autowired private SearchApplicationsTool searchApplicationsTool;

  /** Container for discovered test data. */
  static class TestData {
    boolean hasApplications;
    String sampleAppName;
    String sampleAppId;

    @Override
    public String toString() {
      return String.format(
          "TestData{hasApplications=%s, sampleAppName='%s', sampleAppId='%s'}",
          hasApplications, sampleAppName, sampleAppId);
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
    log.info(
        "Test data: hasApplications={}, sampleAppName={}, sampleAppId={}",
        data.hasApplications,
        data.sampleAppName,
        data.sampleAppId);
  }

  @Override
  protected TestData performDiscovery() throws IOException {
    var response = searchApplicationsTool.searchApplications(1, 5, null, null, null, null);

    var testData = new TestData();
    testData.hasApplications = response.isSuccess() && !response.items().isEmpty();

    if (testData.hasApplications) {
      var firstApp = response.items().get(0);
      testData.sampleAppName = firstApp.name();
      testData.sampleAppId = firstApp.appID();
    }

    log.info(
        "Discovery: found {} applications (hasApps={})",
        response.items().size(),
        testData.hasApplications);

    return testData;
  }

  @Test
  void searchApplications_should_return_valid_response() {
    var response = searchApplicationsTool.searchApplications(1, 10, null, null, null, null);

    assertThat(response).isNotNull();
    assertThat(response.isSuccess()).isTrue();
    assertThat(response.page()).isEqualTo(1);
    assertThat(response.pageSize()).isEqualTo(10);
    assertThat(response.items()).isNotNull();
  }

  @Test
  void searchApplications_should_include_application_details() {
    var response = searchApplicationsTool.searchApplications(1, 10, null, null, null, null);

    assertThat(response.isSuccess()).isTrue();

    if (!response.items().isEmpty()) {
      var app = response.items().get(0);
      assertThat(app.name()).isNotBlank();
      assertThat(app.appID()).isNotBlank();
      // Status and language should be present for most apps
      assertThat(app.status()).isNotNull();
    }
  }

  @Test
  void searchApplications_should_filter_by_name() {
    // First get an app to search for
    var allApps = searchApplicationsTool.searchApplications(1, 1, null, null, null, null);

    if (allApps.isSuccess() && !allApps.items().isEmpty()) {
      var appName = allApps.items().get(0).name();

      // Search for it by partial name (first 3 chars)
      var partialName = appName.substring(0, Math.min(3, appName.length()));
      var filteredResponse =
          searchApplicationsTool.searchApplications(1, 50, partialName, null, null, null);

      assertThat(filteredResponse.isSuccess()).isTrue();
      // The original app should be in the filtered results
      assertThat(filteredResponse.items())
          .anyMatch(a -> a.name().toLowerCase().contains(partialName.toLowerCase()));
    }
  }

  @Test
  void searchApplications_should_handle_pagination() {
    // First page with small page size
    var page1 = searchApplicationsTool.searchApplications(1, 2, null, null, null, null);

    assertThat(page1.isSuccess()).isTrue();

    // If there are more pages, fetch page 2
    if (page1.hasMorePages()) {
      var page2 = searchApplicationsTool.searchApplications(2, 2, null, null, null, null);

      assertThat(page2.isSuccess()).isTrue();
      assertThat(page2.page()).isEqualTo(2);
    }
  }

  @Test
  void searchApplications_should_return_total_count() {
    var response = searchApplicationsTool.searchApplications(1, 10, null, null, null, null);

    assertThat(response.isSuccess()).isTrue();
    assertThat(response.totalItems()).isNotNull();
    assertThat(response.totalItems()).isGreaterThanOrEqualTo(0);
  }

  @Test
  void searchApplications_should_reject_metadata_value_without_name() {
    var response = searchApplicationsTool.searchApplications(1, 10, null, null, null, "someValue");

    assertThat(response.isSuccess()).isFalse();
    assertThat(response.errors())
        .anyMatch(e -> e.contains("metadataValue") && e.contains("metadataName"));
  }
}
