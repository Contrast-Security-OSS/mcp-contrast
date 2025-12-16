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
package com.contrast.labs.ai.mcp.contrast.tool.adr.params;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Unit tests for GetProtectRulesParams validation. */
class GetProtectRulesParamsTest {

  @Test
  void of_should_accept_valid_appId() {
    var params = GetProtectRulesParams.of("app-123");

    assertThat(params.isValid()).isTrue();
    assertThat(params.errors()).isEmpty();
    assertThat(params.appId()).isEqualTo("app-123");
  }

  @Test
  void of_should_reject_null_appId() {
    var params = GetProtectRulesParams.of(null);

    assertThat(params.isValid()).isFalse();
    assertThat(params.errors()).anyMatch(e -> e.contains("appId is required"));
  }

  @Test
  void of_should_reject_empty_appId() {
    var params = GetProtectRulesParams.of("");

    assertThat(params.isValid()).isFalse();
    assertThat(params.errors()).anyMatch(e -> e.contains("appId is required"));
  }

  @Test
  void of_should_reject_blank_appId() {
    var params = GetProtectRulesParams.of("   ");

    assertThat(params.isValid()).isFalse();
    assertThat(params.errors()).anyMatch(e -> e.contains("appId is required"));
  }

  @Test
  void of_should_have_no_warnings_for_valid_input() {
    var params = GetProtectRulesParams.of("valid-app-id");

    assertThat(params.isValid()).isTrue();
    assertThat(params.warnings()).isEmpty();
  }
}
