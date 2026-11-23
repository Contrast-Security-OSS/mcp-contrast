/*
 * Copyright 2026 Contrast Security
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import com.contrastsecurity.exceptions.HttpResponseException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class AbstractIntegrationTestTest {

  private static final String ORG_ID = "org-1";

  @Test
  void setUpTestData_should_not_cache_degraded_discovery_data() {
    var integrationTest = new DegradedDiscoveryIntegrationTest();
    integrationTest.orgId = ORG_ID;

    try (var cache = mockStatic(IntegrationTestDiskCache.class)) {
      integrationTest.setUpTestData();

      cache.verify(
          () ->
              IntegrationTestDiskCache.write(
                  any(String.class), any(String.class), any(String.class)),
          never());
    }
  }

  @Test
  void setUpTestData_should_cache_when_isCacheable_returns_true() {
    var integrationTest = new CacheableDiscoveryIntegrationTest();
    integrationTest.orgId = ORG_ID;

    try (var cache = mockStatic(IntegrationTestDiskCache.class)) {
      integrationTest.setUpTestData();

      cache.verify(
          () ->
              IntegrationTestDiskCache.write(
                  any(String.class), any(String.class), any(String.class)),
          times(1));
    }
  }

  @Test
  void setUpTestData_should_retry_transient_discovery_failure_and_use_the_recovered_data() {
    var integrationTest =
        new FlakyDiscoveryIntegrationTest(new SocketTimeoutException("Read timed out"), 1);
    integrationTest.orgId = ORG_ID;

    try (var cache = mockStatic(IntegrationTestDiskCache.class)) {
      integrationTest.setUpTestData();
      cache.verify(
          () ->
              IntegrationTestDiskCache.write(
                  any(String.class), any(String.class), any(String.class)),
          never());
    }

    assertThat(integrationTest.discoveryCalls).hasValue(2);
    assertThat(integrationTest.testData).isEqualTo("recovered");
  }

  @Test
  void setUpTestData_should_fail_with_exception_type_and_cause_when_retries_are_exhausted() {
    var cause = new SocketTimeoutException("Read timed out");
    var integrationTest = new FlakyDiscoveryIntegrationTest(cause, Integer.MAX_VALUE);
    integrationTest.orgId = ORG_ID;

    try (var cache = mockStatic(IntegrationTestDiskCache.class)) {
      assertThatThrownBy(integrationTest::setUpTestData)
          .isInstanceOf(AssertionError.class)
          .hasMessageStartingWith(AbstractIntegrationTest.DISCOVERY_ERROR_PREFIX)
          .hasMessageContaining("java.net.SocketTimeoutException: Read timed out")
          .hasCause(cause);
      cache.verify(
          () ->
              IntegrationTestDiskCache.write(
                  any(String.class), any(String.class), any(String.class)),
          never());
    }

    assertThat(integrationTest.discoveryCalls).hasValue(TransientRetry.MAX_ATTEMPTS);
  }

  @Test
  void setUpTestData_should_not_retry_a_non_transient_discovery_failure() {
    var cause = new IOException("disk full");
    var integrationTest = new FlakyDiscoveryIntegrationTest(cause, Integer.MAX_VALUE);
    integrationTest.orgId = ORG_ID;

    try (var cache = mockStatic(IntegrationTestDiskCache.class)) {
      assertThatThrownBy(integrationTest::setUpTestData)
          .isInstanceOf(AssertionError.class)
          .hasCause(cause);
      cache.verify(
          () ->
              IntegrationTestDiskCache.write(
                  any(String.class), any(String.class), any(String.class)),
          never());
    }

    assertThat(integrationTest.discoveryCalls).hasValue(1);
  }

  @Test
  void setUpTestData_should_report_http_method_path_and_status_when_discovery_gets_http_error() {
    var cause =
        new HttpResponseException(
            "failed",
            "GET",
            "/ng/org/applications",
            HttpURLConnection.HTTP_UNAVAILABLE,
            "Service Unavailable");
    var integrationTest = new FlakyDiscoveryIntegrationTest(cause, Integer.MAX_VALUE);
    integrationTest.orgId = ORG_ID;

    try (var cache = mockStatic(IntegrationTestDiskCache.class)) {
      assertThatThrownBy(integrationTest::setUpTestData)
          .isInstanceOf(AssertionError.class)
          .hasMessageStartingWith(AbstractIntegrationTest.DISCOVERY_ERROR_PREFIX)
          .hasMessageContaining("GET /ng/org/applications")
          .hasMessageContaining("503 Service Unavailable")
          .hasCause(cause);
      cache.verify(
          () ->
              IntegrationTestDiskCache.write(
                  any(String.class), any(String.class), any(String.class)),
          never());
    }

    assertThat(integrationTest.discoveryCalls).hasValue(TransientRetry.MAX_ATTEMPTS);
  }

  private static final class FlakyDiscoveryIntegrationTest extends AbstractIntegrationTest<String> {

    private final AtomicInteger discoveryCalls = new AtomicInteger();
    private final Exception failure;
    private final int failuresBeforeSuccess;

    private FlakyDiscoveryIntegrationTest(Exception failure, int failuresBeforeSuccess) {
      this.failure = failure;
      this.failuresBeforeSuccess = failuresBeforeSuccess;
    }

    @Override
    protected TransientRetry discoveryRetry() {
      return new TransientRetry(TransientRetry.MAX_ATTEMPTS, 0L, 0L, ms -> {}, ceiling -> 0L);
    }

    @Override
    protected String testDisplayName() {
      return "Flaky Discovery Integration Test";
    }

    @Override
    protected Class<String> testDataType() {
      return String.class;
    }

    @Override
    protected String performDiscovery() throws Exception {
      if (discoveryCalls.incrementAndGet() <= failuresBeforeSuccess) {
        throw failure;
      }
      return "recovered";
    }

    @Override
    protected boolean isCacheable(String data) {
      return false;
    }

    @Override
    protected void logTestDataDetails(String data) {}
  }

  private static final class CacheableDiscoveryIntegrationTest
      extends AbstractIntegrationTest<String> {

    @Override
    protected String testDisplayName() {
      return "Cacheable Discovery Integration Test";
    }

    @Override
    protected Class<String> testDataType() {
      return String.class;
    }

    @Override
    protected String performDiscovery() {
      return "cacheable";
    }

    @Override
    protected void logTestDataDetails(String data) {}
  }

  private static final class DegradedDiscoveryIntegrationTest
      extends AbstractIntegrationTest<String> {

    @Override
    protected String testDisplayName() {
      return "Degraded Discovery Integration Test";
    }

    @Override
    protected Class<String> testDataType() {
      return String.class;
    }

    @Override
    protected String performDiscovery() {
      return "degraded";
    }

    @Override
    protected boolean isCacheable(String data) {
      return false;
    }

    @Override
    protected void logTestDataDetails(String data) {}
  }
}
