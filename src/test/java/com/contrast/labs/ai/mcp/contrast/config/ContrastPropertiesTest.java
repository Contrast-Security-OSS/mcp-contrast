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
package com.contrast.labs.ai.mcp.contrast.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Unit tests for ContrastProperties record. */
class ContrastPropertiesTest {

  @Test
  void record_should_store_all_required_fields() {
    var props =
        new ContrastProperties(
            "test.contrast.com",
            "api-key-123",
            "service-key-456",
            "testuser",
            "org-789",
            null,
            null,
            "https");

    assertThat(props.hostName()).isEqualTo("test.contrast.com");
    assertThat(props.apiKey()).isEqualTo("api-key-123");
    assertThat(props.serviceKey()).isEqualTo("service-key-456");
    assertThat(props.userName()).isEqualTo("testuser");
    assertThat(props.orgId()).isEqualTo("org-789");
    assertThat(props.protocol()).isEqualTo("https");
  }

  @Test
  void record_should_store_optional_proxy_fields() {
    var props =
        new ContrastProperties(
            "test.contrast.com",
            "api-key",
            "service-key",
            "user",
            "org",
            "proxy.example.com",
            "8080",
            "https");

    assertThat(props.httpProxyHost()).isEqualTo("proxy.example.com");
    assertThat(props.httpProxyPort()).isEqualTo("8080");
  }

  @Test
  void record_should_be_immutable() {
    var props1 =
        new ContrastProperties("host1", "key1", "svc1", "user1", "org1", null, null, "https");
    var props2 =
        new ContrastProperties("host1", "key1", "svc1", "user1", "org1", null, null, "https");

    assertThat(props1).isEqualTo(props2);
    assertThat(props1.hashCode()).isEqualTo(props2.hashCode());
  }
}
