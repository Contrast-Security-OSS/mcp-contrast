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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

/** Unit tests for ContrastSDKFactory. */
@ExtendWith(MockitoExtension.class)
class ContrastSDKFactoryTest {

  @Mock private ContrastSDK mockSDK;

  private MockedStatic<SDKHelper> mockedSDKHelper;
  private ContrastSDKFactory factory;

  private static final ContrastProperties TEST_PROPS =
      new ContrastProperties(
          "test.contrast.com",
          "api-key-123",
          "service-key-456",
          "testuser",
          "org-789",
          "proxy.example.com",
          "8080",
          "https");

  @BeforeEach
  void setUp() {
    mockedSDKHelper = mockStatic(SDKHelper.class);
    mockedSDKHelper
        .when(
            () ->
                SDKHelper.getSDK(
                    TEST_PROPS.hostName(),
                    TEST_PROPS.apiKey(),
                    TEST_PROPS.serviceKey(),
                    TEST_PROPS.userName(),
                    TEST_PROPS.httpProxyHost(),
                    TEST_PROPS.httpProxyPort(),
                    TEST_PROPS.protocol()))
        .thenReturn(mockSDK);

    factory = new ContrastSDKFactory(TEST_PROPS);
  }

  @AfterEach
  void tearDown() {
    if (mockedSDKHelper != null) {
      mockedSDKHelper.close();
    }
  }

  @Test
  void getSDK_should_return_sdk_instance() {
    var sdk = factory.getSDK();

    assertThat(sdk).isSameAs(mockSDK);
  }

  @Test
  void getSDK_should_cache_instance_on_subsequent_calls() {
    var sdk1 = factory.getSDK();
    var sdk2 = factory.getSDK();

    assertThat(sdk1).isSameAs(sdk2);
    mockedSDKHelper.verify(
        () ->
            SDKHelper.getSDK(
                TEST_PROPS.hostName(),
                TEST_PROPS.apiKey(),
                TEST_PROPS.serviceKey(),
                TEST_PROPS.userName(),
                TEST_PROPS.httpProxyHost(),
                TEST_PROPS.httpProxyPort(),
                TEST_PROPS.protocol()),
        org.mockito.Mockito.times(1));
  }

  @Test
  void getOrgId_should_return_orgId_from_properties() {
    var orgId = factory.getOrgId();

    assertThat(orgId).isEqualTo("org-789");
  }

  @Test
  void validateConfiguration_should_throw_when_required_property_is_blank() {
    var invalidProps =
        new ContrastProperties(
            "", // blank hostName
            "api-key",
            "service-key",
            "user",
            "org",
            null,
            null,
            "https");

    var factoryWithInvalidProps = new ContrastSDKFactory(invalidProps);

    assertThatThrownBy(factoryWithInvalidProps::validateConfiguration)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("CONTRAST_HOST_NAME");
  }

  @Test
  void validateConfiguration_should_list_all_missing_credentials() {
    var invalidProps =
        new ContrastProperties(
            "", // blank hostName
            "", // blank apiKey
            "service-key",
            "", // blank userName
            "org",
            null,
            null,
            "https");

    var factoryWithInvalidProps = new ContrastSDKFactory(invalidProps);

    assertThatThrownBy(factoryWithInvalidProps::validateConfiguration)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("CONTRAST_HOST_NAME")
        .hasMessageContaining("CONTRAST_API_KEY")
        .hasMessageContaining("CONTRAST_USERNAME");
  }

  @Test
  void validateConfiguration_should_pass_when_all_required_properties_are_set() {
    // Factory created with TEST_PROPS should validate without exception
    factory.validateConfiguration();
    // No exception means success
  }

  @Test
  void validateConfiguration_should_throw_when_protocol_is_http() {
    var props = buildProps("test.contrast.com", "http");
    var factoryWithInvalidProps = new ContrastSDKFactory(props);

    assertThatThrownBy(factoryWithInvalidProps::validateConfiguration)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Invalid or insecure Contrast TeamServer URL")
        .hasMessageContaining("contrast.api.protocol='http'")
        .hasMessageContaining("https://");
  }

  @Test
  void validateConfiguration_should_throw_when_protocol_is_HTTP_uppercase() {
    var props = buildProps("test.contrast.com", "HTTP");
    var factoryWithInvalidProps = new ContrastSDKFactory(props);

    assertThatThrownBy(factoryWithInvalidProps::validateConfiguration)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Invalid or insecure Contrast TeamServer URL")
        .hasMessageContaining("contrast.api.protocol='HTTP'");
  }

  @Test
  void validateConfiguration_should_throw_when_protocol_is_ftp() {
    var props = buildProps("test.contrast.com", "ftp");
    var factoryWithInvalidProps = new ContrastSDKFactory(props);

    assertThatThrownBy(factoryWithInvalidProps::validateConfiguration)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Invalid or insecure Contrast TeamServer URL")
        .hasMessageContaining("contrast.api.protocol='ftp'");
  }

  @Test
  void validateConfiguration_should_throw_when_protocol_has_scheme_suffix() {
    var props = buildProps("test.contrast.com", "https://");
    var factoryWithInvalidProps = new ContrastSDKFactory(props);

    assertThatThrownBy(factoryWithInvalidProps::validateConfiguration)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Invalid or insecure Contrast TeamServer URL")
        .hasMessageContaining("contrast.api.protocol='https://'");
  }

  @Test
  void validateConfiguration_should_pass_when_protocol_is_https() {
    var props = buildProps("test.contrast.com", "https");
    var factoryWithValidProps = new ContrastSDKFactory(props);

    assertThatCode(factoryWithValidProps::validateConfiguration).doesNotThrowAnyException();
  }

  @Test
  void validateConfiguration_should_pass_when_protocol_is_HTTPS_uppercase() {
    var props = buildProps("test.contrast.com", "HTTPS");
    var factoryWithValidProps = new ContrastSDKFactory(props);

    assertThatCode(factoryWithValidProps::validateConfiguration).doesNotThrowAnyException();
  }

  @Test
  void validateConfiguration_should_pass_when_protocol_is_blank() {
    var props = buildProps("test.contrast.com", "");
    var factoryWithValidProps = new ContrastSDKFactory(props);

    assertThatCode(factoryWithValidProps::validateConfiguration).doesNotThrowAnyException();
  }

  @Test
  void validateConfiguration_should_pass_when_protocol_is_null() {
    var props = buildProps("test.contrast.com", null);
    var factoryWithValidProps = new ContrastSDKFactory(props);

    assertThatCode(factoryWithValidProps::validateConfiguration).doesNotThrowAnyException();
  }

  @Test
  void validateConfiguration_should_throw_when_hostname_has_http_scheme() {
    var props = buildProps("http://app.example.com", "https");
    var factoryWithInvalidProps = new ContrastSDKFactory(props);

    assertThatThrownBy(factoryWithInvalidProps::validateConfiguration)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Invalid or insecure Contrast TeamServer URL")
        .hasMessageContaining("CONTRAST_HOST_NAME='http://app.example.com'");
  }

  @Test
  void validateConfiguration_should_throw_when_hostname_has_HTTP_uppercase_scheme() {
    var props = buildProps("HTTP://app.example.com", "https");
    var factoryWithInvalidProps = new ContrastSDKFactory(props);

    assertThatThrownBy(factoryWithInvalidProps::validateConfiguration)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Invalid or insecure Contrast TeamServer URL")
        .hasMessageContaining("CONTRAST_HOST_NAME='HTTP://app.example.com'");
  }

  @Test
  void validateConfiguration_should_pass_when_hostname_has_https_scheme() {
    var props = buildProps("https://app.example.com", "https");
    var factoryWithValidProps = new ContrastSDKFactory(props);

    assertThatCode(factoryWithValidProps::validateConfiguration).doesNotThrowAnyException();
  }

  private ContrastProperties buildProps(String hostName, String protocol) {
    return new ContrastProperties(
        hostName,
        "api-key-123",
        "service-key-456",
        "testuser",
        "org-789",
        "proxy.example.com",
        "8080",
        protocol);
  }
}
