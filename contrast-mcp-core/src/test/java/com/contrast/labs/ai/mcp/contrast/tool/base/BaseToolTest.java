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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Unit tests for {@link BaseTool}. */
class BaseToolTest {

  @ParameterizedTest
  @CsvSource({
    "401, Authentication failed or resource not found. Verify credentials and that the resource ID"
        + " is correct.",
    "403, Access denied. User lacks permission for this resource.",
    "404, Resource not found.",
    "429, Rate limit exceeded. Retry after a brief pause.",
    "500, 'The service returned an error. Narrow filters or reduce page size, then retry.'",
    "502, 'The service returned an error. Narrow filters or reduce page size, then retry.'",
    "503, 'The service returned an error. Narrow filters or reduce page size, then retry.'",
    "418, API error (HTTP 418)"
  })
  void mapHttpErrorCode_should_return_user_friendly_message(int code, String expectedMessage) {
    var tool = new TestableTool();

    var result = tool.mapHttpErrorCode(code);

    assertThat(result).isEqualTo(expectedMessage);
  }

  @Test
  void isAuthenticationStrategyConfigured_should_default_to_false() {
    var tool = new TestableTool();

    assertThat(tool.isAuthenticationStrategyConfigured()).isFalse();
  }

  /** Concrete implementation for testing abstract base class. */
  private static class TestableTool extends BaseTool {
    @Override
    public String mapHttpErrorCode(int code) {
      return super.mapHttpErrorCode(code);
    }
  }
}
