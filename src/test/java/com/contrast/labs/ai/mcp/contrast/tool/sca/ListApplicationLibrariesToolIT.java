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
package com.contrast.labs.ai.mcp.contrast.tool.sca;

import static org.assertj.core.api.Assertions.assertThat;

import com.contrast.labs.ai.mcp.contrast.config.IntegrationTestConfig;
import com.contrast.labs.ai.mcp.contrast.util.AbstractIntegrationTest;
import com.contrast.labs.ai.mcp.contrast.util.TestDataDiscoveryHelper;
import java.io.IOException;
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

  @Override
  protected TestData performDiscovery() throws IOException {
    var appWithLibraries =
        TestDataDiscoveryHelper.findApplicationWithLibraries(orgID, sdkExtension);

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
    log.info("\n=== Integration Test: Validate test data discovery ===");

    assertThat(testData).as("Test data should have been discovered").isNotNull();
    assertThat(testData.appId).as("App ID should be set").isNotNull();
    assertThat(testData.expectedLibraryCount).as("Should have libraries").isGreaterThan(0);
  }

  @Test
  void listApplicationLibraries_should_return_libraries() {
    log.info("\n=== Integration Test: list_application_libraries ===");

    var result = tool.listApplicationLibraries(testData.appId);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.found()).isTrue();
    assertThat(result.data()).isNotEmpty();
    assertThat(result.errors()).isEmpty();

    log.info("Retrieved {} libraries for application: {}", result.data().size(), testData.appName);

    // Verify library structure
    var firstLib = result.data().get(0);
    assertThat(firstLib.getFilename()).as("Library filename should not be null").isNotNull();
    assertThat(firstLib.getHash()).as("Library hash should not be null").isNotNull();
    assertThat(firstLib.getClassCount()).as("Class count should be non-negative").isNotNegative();
  }

  @Test
  void listApplicationLibraries_should_handle_invalid_app_id() {
    log.info("\n=== Integration Test: Invalid app ID handling ===");

    var result = tool.listApplicationLibraries("invalid-app-id-12345");

    // API should handle gracefully - either empty list or API error
    if (result.isSuccess()) {
      log.info("API handled invalid app ID gracefully - returned empty or data");
    } else {
      log.info("API rejected invalid app ID with error: {}", result.errors());
    }
  }

  @Test
  void listApplicationLibraries_should_include_class_usage_data() {
    log.info("\n=== Integration Test: Class usage statistics ===");

    var result = tool.listApplicationLibraries(testData.appId);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.data()).isNotEmpty();

    // Check class usage is populated
    var libraries = result.data();
    long activeLibs = libraries.stream().filter(lib -> lib.getClassedUsed() > 0).count();
    long unusedLibs = libraries.stream().filter(lib -> lib.getClassedUsed() == 0).count();

    log.info("Active libraries (classes used > 0): {}", activeLibs);
    log.info("Unused libraries (classes used = 0): {}", unusedLibs);

    // Verify class usage consistency
    for (var lib : libraries) {
      assertThat(lib.getClassedUsed())
          .as("Classes used should not exceed class count for " + lib.getFilename())
          .isLessThanOrEqualTo(lib.getClassCount());
    }
  }
}
