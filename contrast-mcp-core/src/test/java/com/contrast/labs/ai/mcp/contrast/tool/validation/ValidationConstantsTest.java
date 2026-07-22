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
package com.contrast.labs.ai.mcp.contrast.tool.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ValidationConstantsTest {

  @Test
  void pagination_constants_should_have_expected_values() {
    assertThat(ValidationConstants.DEFAULT_PAGE).isEqualTo(1);
    assertThat(ValidationConstants.DEFAULT_PAGE_SIZE).isEqualTo(50);
    assertThat(ValidationConstants.MAX_PAGE_SIZE).isEqualTo(100);
    assertThat(ValidationConstants.MIN_PAGE).isEqualTo(1);
  }
}
