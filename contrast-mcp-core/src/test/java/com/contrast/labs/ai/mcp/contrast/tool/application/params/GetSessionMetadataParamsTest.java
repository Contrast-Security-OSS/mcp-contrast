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
package com.contrast.labs.ai.mcp.contrast.tool.application.params;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GetSessionMetadataParamsTest {

  @Test
  void of_should_accept_valid_app_id() {
    var params = GetSessionMetadataParams.of("app-123");

    assertThat(params.isValid()).isTrue();
    assertThat(params.appId()).isEqualTo("app-123");
    assertThat(params.errors()).isEmpty();
    assertThat(params.warnings()).isEmpty();
  }

  @Test
  void of_should_reject_null_app_id() {
    var params = GetSessionMetadataParams.of(null);

    assertThat(params.isValid()).isFalse();
    assertThat(params.errors()).anyMatch(e -> e.contains("appId") && e.contains("required"));
  }

  @Test
  void of_should_reject_empty_app_id() {
    var params = GetSessionMetadataParams.of("");

    assertThat(params.isValid()).isFalse();
    assertThat(params.errors()).anyMatch(e -> e.contains("appId") && e.contains("required"));
  }

  @Test
  void of_should_reject_blank_app_id() {
    var params = GetSessionMetadataParams.of("   ");

    assertThat(params.isValid()).isFalse();
    assertThat(params.errors()).anyMatch(e -> e.contains("appId") && e.contains("required"));
  }
}
