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

class ApplicationFilterParamsTest {

  // -- Validation tests --

  @Test
  void of_should_accept_all_null_filters() {
    var params = ApplicationFilterParams.of(null, null, null);

    assertThat(params.isValid()).isTrue();
    assertThat(params.getName()).isNull();
    assertThat(params.getTag()).isNull();
    assertThat(params.getMetadataFilters()).isNull();
  }

  @Test
  void of_should_accept_all_empty_filters() {
    var params = ApplicationFilterParams.of("", "", "");

    assertThat(params.isValid()).isTrue();
    assertThat(params.getName()).isNull();
    assertThat(params.getTag()).isNull();
    assertThat(params.getMetadataFilters()).isNull();
  }

  @Test
  void of_should_parse_metadata_filters_json() {
    var params = ApplicationFilterParams.of(null, null, "{\"env\":\"prod\"}");

    assertThat(params.isValid()).isTrue();
    assertThat(params.getMetadataFilters()).hasSize(1);
    assertThat(params.getMetadataFilters().get(0).fieldName()).isEqualTo("env");
    assertThat(params.getMetadataFilters().get(0).values()).containsExactly("prod");
  }

  @Test
  void of_should_parse_metadata_filters_with_multiple_values() {
    var params = ApplicationFilterParams.of(null, null, "{\"env\":[\"prod\",\"staging\"]}");

    assertThat(params.isValid()).isTrue();
    assertThat(params.getMetadataFilters()).hasSize(1);
    assertThat(params.getMetadataFilters().get(0).fieldName()).isEqualTo("env");
    assertThat(params.getMetadataFilters().get(0).values()).containsExactly("prod", "staging");
  }

  @Test
  void of_should_parse_metadata_filters_with_multiple_fields() {
    var params = ApplicationFilterParams.of(null, null, "{\"env\":\"prod\",\"team\":\"backend\"}");

    assertThat(params.isValid()).isTrue();
    assertThat(params.getMetadataFilters()).hasSize(2);
  }

  @Test
  void of_should_reject_invalid_metadata_filters_json() {
    var params = ApplicationFilterParams.of(null, null, "{invalid json}");

    assertThat(params.isValid()).isFalse();
    assertThat(params.errors())
        .anyMatch(e -> e.contains("Invalid JSON") && e.contains("metadataFilters"));
  }

  @Test
  void of_should_store_name_filter() {
    var params = ApplicationFilterParams.of("myapp", null, null);

    assertThat(params.isValid()).isTrue();
    assertThat(params.getName()).isEqualTo("myapp");
  }

  @Test
  void of_should_store_tag_filter() {
    var params = ApplicationFilterParams.of(null, "Production", null);

    assertThat(params.isValid()).isTrue();
    assertThat(params.getTag()).isEqualTo("Production");
  }

  @Test
  void of_should_accept_all_filters_together() {
    var params = ApplicationFilterParams.of("myapp", "Production", "{\"env\":\"prod\"}");

    assertThat(params.isValid()).isTrue();
    assertThat(params.getName()).isEqualTo("myapp");
    assertThat(params.getTag()).isEqualTo("Production");
    assertThat(params.getMetadataFilters()).hasSize(1);
  }
}
