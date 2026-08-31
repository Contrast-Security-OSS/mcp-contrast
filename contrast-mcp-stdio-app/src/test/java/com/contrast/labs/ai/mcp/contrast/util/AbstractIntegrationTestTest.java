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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import org.junit.jupiter.api.Test;

class AbstractIntegrationTestTest {

  private static final String ORG_ID = "org-1";

  @Test
  void cacheKey_should_distinguish_normalized_seed_app_ids() {
    var integrationTest = new CacheableDiscoveryIntegrationTest();

    assertThat(integrationTest.cacheKey(null)).isEqualTo("CacheableDiscoveryIntegrationTest-v1");
    assertThat(integrationTest.cacheKey("  ")).isEqualTo("CacheableDiscoveryIntegrationTest-v1");
    assertThat(integrationTest.cacheKey(" app-one "))
        .isEqualTo("CacheableDiscoveryIntegrationTest-v1-seed-app-one");
    assertThat(integrationTest.cacheKey("app-two"))
        .isEqualTo("CacheableDiscoveryIntegrationTest-v1-seed-app-two");
  }

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
