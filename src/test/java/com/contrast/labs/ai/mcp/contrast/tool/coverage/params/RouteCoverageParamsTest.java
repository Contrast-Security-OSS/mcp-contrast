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
package com.contrast.labs.ai.mcp.contrast.tool.coverage.params;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RouteCoverageParamsTest {

  private static final String VALID_APP_ID = "app-123";
  private static final String VALID_METADATA_NAME = "branch";
  private static final String VALID_METADATA_VALUE = "main";

  // ========== appId validation ==========

  @Test
  void of_should_require_appId_when_null() {
    var params = RouteCoverageParams.of(null, null, null, null);

    assertThat(params.isValid()).isFalse();
    assertThat(params.errors()).contains("appId is required");
  }

  @Test
  void of_should_require_appId_when_empty() {
    var params = RouteCoverageParams.of("", null, null, null);

    assertThat(params.isValid()).isFalse();
    assertThat(params.errors()).contains("appId is required");
  }

  @Test
  void of_should_require_appId_when_whitespace() {
    var params = RouteCoverageParams.of("   ", null, null, null);

    assertThat(params.isValid()).isFalse();
    assertThat(params.errors()).contains("appId is required");
  }

  // ========== Minimal valid params ==========

  @Test
  void of_should_accept_valid_appId_only() {
    var params = RouteCoverageParams.of(VALID_APP_ID, null, null, null);

    assertThat(params.isValid()).isTrue();
    assertThat(params.errors()).isEmpty();
    assertThat(params.warnings()).isEmpty();
    assertThat(params.appId()).isEqualTo(VALID_APP_ID);
    assertThat(params.sessionMetadataName()).isNull();
    assertThat(params.sessionMetadataValue()).isNull();
    assertThat(params.useLatestSession()).isNull();
    assertThat(params.isUseLatestSession()).isFalse();
    assertThat(params.hasSessionMetadataFilter()).isFalse();
  }

  // ========== Session metadata paired validation ==========

  @Test
  void of_should_require_paired_metadata_when_name_provided_without_value() {
    var params = RouteCoverageParams.of(VALID_APP_ID, VALID_METADATA_NAME, null, null);

    assertThat(params.isValid()).isFalse();
    assertThat(params.errors()).anyMatch(e -> e.contains("sessionMetadataValue is required"));
  }

  @Test
  void of_should_require_paired_metadata_when_value_provided_without_name() {
    var params = RouteCoverageParams.of(VALID_APP_ID, null, VALID_METADATA_VALUE, null);

    assertThat(params.isValid()).isFalse();
    assertThat(params.errors()).anyMatch(e -> e.contains("sessionMetadataName is required"));
  }

  @Test
  void of_should_require_paired_metadata_when_name_empty_and_value_provided() {
    var params = RouteCoverageParams.of(VALID_APP_ID, "", VALID_METADATA_VALUE, null);

    assertThat(params.isValid()).isFalse();
    assertThat(params.errors()).anyMatch(e -> e.contains("sessionMetadataName is required"));
  }

  @Test
  void of_should_require_paired_metadata_when_value_empty_and_name_provided() {
    var params = RouteCoverageParams.of(VALID_APP_ID, VALID_METADATA_NAME, "", null);

    assertThat(params.isValid()).isFalse();
    assertThat(params.errors()).anyMatch(e -> e.contains("sessionMetadataValue is required"));
  }

  @Test
  void of_should_accept_valid_metadata_params() {
    var params =
        RouteCoverageParams.of(VALID_APP_ID, VALID_METADATA_NAME, VALID_METADATA_VALUE, null);

    assertThat(params.isValid()).isTrue();
    assertThat(params.errors()).isEmpty();
    assertThat(params.sessionMetadataName()).isEqualTo(VALID_METADATA_NAME);
    assertThat(params.sessionMetadataValue()).isEqualTo(VALID_METADATA_VALUE);
    assertThat(params.hasSessionMetadataFilter()).isTrue();
  }

  // ========== Empty strings treated as null ==========

  @Test
  void of_should_treat_empty_strings_as_null_for_metadata() {
    var params = RouteCoverageParams.of(VALID_APP_ID, "", "", null);

    assertThat(params.isValid()).isTrue();
    assertThat(params.errors()).isEmpty();
    assertThat(params.sessionMetadataName()).isNull();
    assertThat(params.sessionMetadataValue()).isNull();
    assertThat(params.hasSessionMetadataFilter()).isFalse();
  }

  @Test
  void of_should_treat_whitespace_strings_as_null_for_metadata() {
    var params = RouteCoverageParams.of(VALID_APP_ID, "   ", "   ", null);

    assertThat(params.isValid()).isTrue();
    assertThat(params.errors()).isEmpty();
    assertThat(params.sessionMetadataName()).isNull();
    assertThat(params.sessionMetadataValue()).isNull();
    assertThat(params.hasSessionMetadataFilter()).isFalse();
  }

  // ========== useLatestSession ==========

  @Test
  void of_should_accept_useLatestSession_true() {
    var params = RouteCoverageParams.of(VALID_APP_ID, null, null, true);

    assertThat(params.isValid()).isTrue();
    assertThat(params.errors()).isEmpty();
    assertThat(params.useLatestSession()).isTrue();
    assertThat(params.isUseLatestSession()).isTrue();
  }

  @Test
  void of_should_accept_useLatestSession_false() {
    var params = RouteCoverageParams.of(VALID_APP_ID, null, null, false);

    assertThat(params.isValid()).isTrue();
    assertThat(params.errors()).isEmpty();
    assertThat(params.useLatestSession()).isFalse();
    assertThat(params.isUseLatestSession()).isFalse();
  }

  // ========== Mutual exclusivity warning ==========

  @Test
  void of_should_warn_when_both_useLatestSession_and_metadata_provided() {
    var params =
        RouteCoverageParams.of(VALID_APP_ID, VALID_METADATA_NAME, VALID_METADATA_VALUE, true);

    assertThat(params.isValid()).isTrue(); // Valid but with warning
    assertThat(params.errors()).isEmpty();
    assertThat(params.warnings()).anyMatch(w -> w.contains("useLatestSession takes precedence"));
  }

  @Test
  void of_should_not_warn_when_useLatestSession_false_and_metadata_provided() {
    var params =
        RouteCoverageParams.of(VALID_APP_ID, VALID_METADATA_NAME, VALID_METADATA_VALUE, false);

    assertThat(params.isValid()).isTrue();
    assertThat(params.warnings()).isEmpty();
  }

  @Test
  void of_should_not_warn_when_useLatestSession_null_and_metadata_provided() {
    var params =
        RouteCoverageParams.of(VALID_APP_ID, VALID_METADATA_NAME, VALID_METADATA_VALUE, null);

    assertThat(params.isValid()).isTrue();
    assertThat(params.warnings()).isEmpty();
  }

  // ========== Multiple validation errors ==========

  @Test
  void of_should_collect_multiple_validation_errors() {
    var params = RouteCoverageParams.of(null, VALID_METADATA_NAME, null, null);

    assertThat(params.isValid()).isFalse();
    assertThat(params.errors()).hasSize(2);
    assertThat(params.errors())
        .anyMatch(e -> e.contains("appId is required"))
        .anyMatch(e -> e.contains("sessionMetadataValue is required"));
  }
}
