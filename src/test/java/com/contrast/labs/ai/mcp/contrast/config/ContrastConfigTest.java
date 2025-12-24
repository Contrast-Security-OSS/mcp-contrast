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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;

import com.contrast.labs.ai.mcp.contrast.sdkextension.SDKHelper;
import com.contrastsecurity.sdk.ContrastSDK;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/** Unit tests for ContrastConfig. */
@ExtendWith(MockitoExtension.class)
class ContrastConfigTest {

  private ContrastConfig config;

  @Mock private ContrastSDK mockContrastSDK;

  private MockedStatic<SDKHelper> mockedSDKHelper;

  private static final String TEST_HOST = "https://test.contrast.local";
  private static final String TEST_API_KEY = "test-api-key";
  private static final String TEST_SERVICE_KEY = "test-service-key";
  private static final String TEST_USERNAME = "test-user";
  private static final String TEST_ORG_ID = "test-org-123";
  private static final String TEST_PROXY_HOST = "proxy.example.com";
  private static final String TEST_PROXY_PORT = "8080";
  private static final String TEST_PROTOCOL = "https";

  @BeforeEach
  void setUp() {
    config = new ContrastConfig();

    mockedSDKHelper = mockStatic(SDKHelper.class);
    mockedSDKHelper
        .when(
            () ->
                SDKHelper.getSDK(
                    anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
        .thenReturn(mockContrastSDK);

    ReflectionTestUtils.setField(config, "hostName", TEST_HOST);
    ReflectionTestUtils.setField(config, "apiKey", TEST_API_KEY);
    ReflectionTestUtils.setField(config, "serviceKey", TEST_SERVICE_KEY);
    ReflectionTestUtils.setField(config, "userName", TEST_USERNAME);
    ReflectionTestUtils.setField(config, "orgId", TEST_ORG_ID);
    ReflectionTestUtils.setField(config, "httpProxyHost", TEST_PROXY_HOST);
    ReflectionTestUtils.setField(config, "httpProxyPort", TEST_PROXY_PORT);
    ReflectionTestUtils.setField(config, "protocol", TEST_PROTOCOL);
  }

  @AfterEach
  void tearDown() {
    if (mockedSDKHelper != null) {
      mockedSDKHelper.close();
    }
  }

  @Test
  void getHostName_should_return_configured_value() {
    assertThat(config.getHostName()).isEqualTo(TEST_HOST);
  }

  @Test
  void getApiKey_should_return_configured_value() {
    assertThat(config.getApiKey()).isEqualTo(TEST_API_KEY);
  }

  @Test
  void getServiceKey_should_return_configured_value() {
    assertThat(config.getServiceKey()).isEqualTo(TEST_SERVICE_KEY);
  }

  @Test
  void getUserName_should_return_configured_value() {
    assertThat(config.getUserName()).isEqualTo(TEST_USERNAME);
  }

  @Test
  void getOrgId_should_return_configured_value() {
    assertThat(config.getOrgId()).isEqualTo(TEST_ORG_ID);
  }

  @Test
  void getHttpProxyHost_should_return_configured_value() {
    assertThat(config.getHttpProxyHost()).isEqualTo(TEST_PROXY_HOST);
  }

  @Test
  void getHttpProxyPort_should_return_configured_value() {
    assertThat(config.getHttpProxyPort()).isEqualTo(TEST_PROXY_PORT);
  }

  @Test
  void getProtocol_should_return_configured_value() {
    assertThat(config.getProtocol()).isEqualTo(TEST_PROTOCOL);
  }

  @Test
  void getSDK_should_return_sdk_instance() {
    var sdk = config.getSDK();
    assertThat(sdk).isNotNull();
    assertThat(sdk).isEqualTo(mockContrastSDK);
  }

  @Test
  void getSDK_should_call_sdkhelper_with_correct_params() {
    config.getSDK();

    mockedSDKHelper.verify(
        () ->
            SDKHelper.getSDK(
                TEST_HOST,
                TEST_API_KEY,
                TEST_SERVICE_KEY,
                TEST_USERNAME,
                TEST_PROXY_HOST,
                TEST_PROXY_PORT));
  }

  @Test
  void getSDK_should_work_without_proxy_settings() {
    ReflectionTestUtils.setField(config, "httpProxyHost", "");
    ReflectionTestUtils.setField(config, "httpProxyPort", "");

    var sdk = config.getSDK();
    assertThat(sdk).isNotNull();

    mockedSDKHelper.verify(
        () -> SDKHelper.getSDK(TEST_HOST, TEST_API_KEY, TEST_SERVICE_KEY, TEST_USERNAME, "", ""));
  }

  @Test
  void getSDK_should_cache_instance_and_return_same_on_subsequent_calls() {
    var sdk1 = config.getSDK();
    var sdk2 = config.getSDK();

    assertThat(sdk1).isSameAs(sdk2);

    // SDKHelper.getSDK should only be called once
    mockedSDKHelper.verify(
        () ->
            SDKHelper.getSDK(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString()));
  }
}
