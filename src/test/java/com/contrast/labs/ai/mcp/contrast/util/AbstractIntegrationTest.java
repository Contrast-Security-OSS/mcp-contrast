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
package com.contrast.labs.ai.mcp.contrast.util;

import static org.assertj.core.api.Assertions.fail;

import com.contrast.labs.ai.mcp.contrast.sdkextension.SDKExtension;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 * Shared lifecycle logic for integration tests.
 *
 * <p>Handles test data discovery (with disk caching), timing instrumentation, and summary
 * reporting. Concrete tests only implement discovery specifics and domain assertions.
 *
 * @param <T> Type of the discovered test data container
 */
@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractIntegrationTest<T> {

  @Autowired protected SDKExtension sdkExtension;

  @Value("${contrast.org-id:${CONTRAST_ORG_ID:}}")
  protected String orgID;

  protected T testData;
  private long discoveryDurationMs;
  private long testStartTimeMs;
  private long totalTestTimeMs;
  private int testCount;

  /** Provides a human-readable name for the test class (used in logging). */
  protected abstract String testDisplayName();

  /** Returns the class literal for the cached test data. */
  protected abstract Class<T> testDataType();

  /**
   * Discovers the test data required for the integration test. Implementations should either return
   * discovered data or throw {@link NoTestDataException} when requirements are not met.
   */
  protected abstract T performDiscovery() throws Exception;

  /** Logs important details about the discovered test data. */
  protected abstract void logTestDataDetails(T data);

  /** Optional hook invoked after a cache hit has been loaded. */
  protected void afterCacheHit(T data) {}

  /** Optional hook invoked after new discovery completes successfully. */
  protected void afterDiscovery(T data) {}

  /** Optional hook executed before discovery starts. */
  protected void onDiscoveryStart() {
    log.info("Starting test data discovery (using shared SDK)...");
  }

  @BeforeAll
  void setUpTestData() {
    logBanner(testDisplayName() + " - Discovering Test Data");

    if (IntegrationTestDiskCache.loadIfPresent(
        cacheKey(),
        orgID,
        testDataType(),
        cached -> {
          testData = cached;
          discoveryDurationMs = 0;
          log.info("✓ Loaded cached test data: {}", cached);
          afterCacheHit(cached);
        })) {
      return;
    }

    onDiscoveryStart();
    long startTime = System.currentTimeMillis();
    try {
      testData = performDiscovery();
      discoveryDurationMs = System.currentTimeMillis() - startTime;
      logDiscoverySuccess(testData, discoveryDurationMs);
      IntegrationTestDiskCache.write(cacheKey(), orgID, testData);
      afterDiscovery(testData);
    } catch (NoTestDataException e) {
      log.error(e.getMessage());
      fail(e.getMessage());
    } catch (Exception e) {
      String message = "❌ ERROR during test data discovery: " + e.getMessage();
      log.error("\n{}", message, e);
      fail(message);
    }
  }

  @BeforeEach
  void logTestStart(TestInfo testInfo) {
    log.info("\n▶ Starting test: {}", testInfo.getDisplayName());
    testStartTimeMs = System.currentTimeMillis();
  }

  @AfterEach
  void logTestEnd(TestInfo testInfo) {
    long duration = System.currentTimeMillis() - testStartTimeMs;
    totalTestTimeMs += duration;
    testCount++;
    log.info("✓ Test completed in {}ms: {}\n", duration, testInfo.getDisplayName());
  }

  @AfterAll
  void logSummary() {
    logBanner("Integration Test Performance Summary");
    log.info("Discovery time: {}ms", discoveryDurationMs);
    log.info("Total test time: {}ms", totalTestTimeMs);
    log.info("Tests executed: {}", testCount);
    if (testCount > 0) {
      log.info("Average per test: {}ms", totalTestTimeMs / testCount);
    }
    logFooter();
  }

  private void logDiscoverySuccess(T data, long durationMs) {
    logBanner("Test Data Discovery Complete");
    logTestDataDetails(data);
    log.info("✓ Test data discovery completed in {}ms", durationMs);
    log.info("");
  }

  private void logBanner(String message) {
    log.info(
        "\n╔════════════════════════════════════════════════════════════════════════════════╗");
    log.info("║   {}║", padMessage(message));
    log.info("╚════════════════════════════════════════════════════════════════════════════════╝");
  }

  private void logFooter() {
    log.info(
        "╚════════════════════════════════════════════════════════════════════════════════╝\n");
  }

  private String padMessage(String message) {
    int width = 76; // total width inside the box
    if (message.length() >= width) {
      return message;
    }
    StringBuilder builder = new StringBuilder(message);
    while (builder.length() < width) {
      builder.append(' ');
    }
    return builder.toString();
  }

  private String cacheKey() {
    return testDisplayName().replaceAll("\\s+", "");
  }

  /**
   * Runtime exception used to signal that no suitable test data could be discovered.
   *
   * <p>Allows concrete implementations to provide detailed error messages without duplicating error
   * handling logic.
   */
  protected static class NoTestDataException extends RuntimeException {
    public NoTestDataException(String message) {
      super(message);
    }
  }
}
