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
package com.contrast.labs.ai.mcp.contrast.tool.base;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.contrast.labs.ai.mcp.contrast.config.ContrastConfig;
import com.contrastsecurity.sdk.ContrastSDK;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;

/** Unit tests for {@link BaseContrastTool}. */
class BaseContrastToolTest {

  private TestableContrastTool tool;
  private ContrastConfig config;
  private ContrastSDK sdk;

  @BeforeEach
  void setUp() {
    tool = new TestableContrastTool();
    config = mock();
    sdk = mock();

    when(config.getSDK()).thenReturn(sdk);
    when(config.getOrgId()).thenReturn("test-org-123");

    ReflectionTestUtils.setField(tool, "config", config);
  }

  @Test
  void getContrastSDK_should_return_sdk_from_config() {
    var result = tool.getContrastSDK();

    assertThat(result).isSameAs(sdk);
  }

  @Test
  void getOrgId_should_return_orgId_from_config() {
    var result = tool.getOrgId();

    assertThat(result).isEqualTo("test-org-123");
  }

  @ParameterizedTest
  @CsvSource({
    "401, Authentication failed. Verify API credentials.",
    "403, Access denied. User lacks permission for this resource.",
    "404, Resource not found.",
    "429, Rate limit exceeded. Retry later.",
    "500, Contrast API error. Try again later.",
    "502, Contrast API error. Try again later.",
    "503, Contrast API error. Try again later.",
    "418, API error (HTTP 418)"
  })
  void mapHttpErrorCode_should_return_user_friendly_message(int code, String expectedMessage) {
    var result = tool.mapHttpErrorCode(code);

    assertThat(result).isEqualTo(expectedMessage);
  }

  /** Concrete implementation for testing abstract base class. */
  private static class TestableContrastTool extends BaseContrastTool {
    // Expose protected methods for testing
    @Override
    public ContrastSDK getContrastSDK() {
      return super.getContrastSDK();
    }

    @Override
    public String getOrgId() {
      return super.getOrgId();
    }

    @Override
    public String mapHttpErrorCode(int code) {
      return super.mapHttpErrorCode(code);
    }
  }
}
