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

  /** Container for discovered test data. */
  static class TestData {
    boolean hasApplications;
    String sampleAppId;

    @Override
    public String toString() {
      return String.format(
          "TestData{hasApplications=%s, sampleAppId='%s'}", hasApplications, sampleAppId);
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
    log.info(
        "Test data: hasApplications={}, sampleAppId={}", data.hasApplications, data.sampleAppId);
  }

  @Override
  protected TestData performDiscovery() throws IOException {
    var response = searchApplicationsTool.searchApplications(1, 5, null, null, null, null);

    var testData = new TestData();
    testData.hasApplications = response.isSuccess() && !response.items().isEmpty();

    if (testData.hasApplications) {
      testData.sampleAppId = response.items().get(0).appID();
    }

    log.info(
        "Discovery: found {} applications (hasApps={})",
        response.items().size(),
        testData.hasApplications);

    return testData;
  }

  @Test
  void getSessionMetadata_should_return_validation_error_for_missing_app_id() {
    var result = getSessionMetadataTool.getSessionMetadata(null);

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors()).anyMatch(e -> e.contains("appId") && e.contains("required"));
  }

  @Test
  void getSessionMetadata_should_return_validation_error_for_empty_app_id() {
    var result = getSessionMetadataTool.getSessionMetadata("");

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors()).anyMatch(e -> e.contains("appId") && e.contains("required"));
  }

  @Test
  void getSessionMetadata_should_handle_valid_app_id() {
    // First discover an app ID
    var appsResponse = searchApplicationsTool.searchApplications(1, 1, null, null, null, null);

    if (appsResponse.isSuccess() && !appsResponse.items().isEmpty()) {
      var appId = appsResponse.items().get(0).appID();

      var result = getSessionMetadataTool.getSessionMetadata(appId);

      // Should succeed without errors (may or may not have session data)
      assertThat(result.isSuccess()).isTrue();
      assertThat(result.errors()).isEmpty();

      // If no session metadata, should have a warning
      if (!result.found() || result.data() == null) {
        assertThat(result.warnings()).anyMatch(w -> w.contains("session metadata"));
      }
    } else {
      log.warn("No applications found in org - skipping session metadata test");
    }
  }

  @Test
  void getSessionMetadata_should_handle_nonexistent_app_id_gracefully() {
    var result = getSessionMetadataTool.getSessionMetadata("nonexistent-app-id-12345");

    // Should not throw an exception - should return an error or not-found response
    assertThat(result).isNotNull();
    // The API may return an error for invalid app IDs
    // or return null/empty response - either is acceptable
  }
}
