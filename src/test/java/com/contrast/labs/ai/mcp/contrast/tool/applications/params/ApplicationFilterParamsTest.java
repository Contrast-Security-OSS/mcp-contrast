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
package com.contrast.labs.ai.mcp.contrast.tool.applications.params;

import static org.assertj.core.api.Assertions.assertThat;

import com.contrast.labs.ai.mcp.contrast.sdkextension.data.application.Application;
import java.util.List;
import org.junit.jupiter.api.Test;

class ApplicationFilterParamsTest {

  // -- Validation tests --

  @Test
  void of_should_accept_all_null_filters() {
    var params = ApplicationFilterParams.of(null, null, null, null);

    assertThat(params.isValid()).isTrue();
    assertThat(params.getName()).isNull();
    assertThat(params.getTag()).isNull();
    assertThat(params.getMetadataName()).isNull();
    assertThat(params.getMetadataValue()).isNull();
  }

  @Test
  void of_should_accept_all_empty_filters() {
    var params = ApplicationFilterParams.of("", "", "", "");

    assertThat(params.isValid()).isTrue();
    assertThat(params.getName()).isNull();
    assertThat(params.getTag()).isNull();
    assertThat(params.getMetadataName()).isNull();
    assertThat(params.getMetadataValue()).isNull();
  }

  @Test
  void of_should_reject_metadata_value_without_name() {
    var params = ApplicationFilterParams.of(null, null, null, "someValue");

    assertThat(params.isValid()).isFalse();
    assertThat(params.errors())
        .anyMatch(e -> e.contains("metadataValue") && e.contains("metadataName"));
  }

  @Test
  void of_should_accept_metadata_name_without_value() {
    var params = ApplicationFilterParams.of(null, null, "environment", null);

    assertThat(params.isValid()).isTrue();
    assertThat(params.getMetadataName()).isEqualTo("environment");
    assertThat(params.getMetadataValue()).isNull();
  }

  @Test
  void of_should_accept_metadata_name_and_value() {
    var params = ApplicationFilterParams.of(null, null, "environment", "production");

    assertThat(params.isValid()).isTrue();
    assertThat(params.getMetadataName()).isEqualTo("environment");
    assertThat(params.getMetadataValue()).isEqualTo("production");
  }

  @Test
  void of_should_store_name_filter() {
    var params = ApplicationFilterParams.of("myapp", null, null, null);

    assertThat(params.isValid()).isTrue();
    assertThat(params.getName()).isEqualTo("myapp");
  }

  @Test
  void of_should_store_tag_filter() {
    var params = ApplicationFilterParams.of(null, "Production", null, null);

    assertThat(params.isValid()).isTrue();
    assertThat(params.getTag()).isEqualTo("Production");
  }

  // -- Filter matching tests --

  @Test
  void matches_should_return_true_when_no_filters() {
    var params = ApplicationFilterParams.of(null, null, null, null);
    var app = createApp("MyApp", "Active");

    assertThat(params.matches(app)).isTrue();
  }

  @Test
  void matches_should_filter_by_name_case_insensitive() {
    var params = ApplicationFilterParams.of("prod", null, null, null);
    var matchingApp = createApp("MyProductionApp", "Active");
    var nonMatchingApp = createApp("TestApp", "Active");

    assertThat(params.matches(matchingApp)).isTrue();
    assertThat(params.matches(nonMatchingApp)).isFalse();
  }

  @Test
  void matches_should_filter_by_name_partial() {
    var params = ApplicationFilterParams.of("App", null, null, null);
    var matchingApp = createApp("MyApplication", "Active");

    assertThat(params.matches(matchingApp)).isTrue();
  }

  @Test
  void matches_should_filter_by_tag_case_sensitive() {
    var params = ApplicationFilterParams.of(null, "Production", null, null);
    var matchingApp = createAppWithTags("App1", List.of("Production", "Critical"));
    var nonMatchingApp = createAppWithTags("App2", List.of("production", "Critical"));

    assertThat(params.matches(matchingApp)).isTrue();
    assertThat(params.matches(nonMatchingApp)).isFalse();
  }

  @Test
  void matches_should_filter_by_metadata_name_only() {
    var params = ApplicationFilterParams.of(null, null, "environment", null);
    var matchingApp = createAppWithMetadata("App1", "Environment", "prod");
    var nonMatchingApp = createAppWithMetadata("App2", "team", "backend");

    assertThat(params.matches(matchingApp)).isTrue();
    assertThat(params.matches(nonMatchingApp)).isFalse();
  }

  @Test
  void matches_should_filter_by_metadata_name_case_insensitive() {
    var params = ApplicationFilterParams.of(null, null, "ENVIRONMENT", null);
    var matchingApp = createAppWithMetadata("App1", "environment", "prod");

    assertThat(params.matches(matchingApp)).isTrue();
  }

  @Test
  void matches_should_filter_by_metadata_name_and_value() {
    var params = ApplicationFilterParams.of(null, null, "environment", "production");
    var matchingApp = createAppWithMetadata("App1", "Environment", "Production");
    var nonMatchingApp = createAppWithMetadata("App2", "Environment", "Development");

    assertThat(params.matches(matchingApp)).isTrue();
    assertThat(params.matches(nonMatchingApp)).isFalse();
  }

  @Test
  void matches_should_combine_filters_with_and_logic() {
    var params = ApplicationFilterParams.of("prod", "Critical", "team", "backend");
    var matchingApp = createFullApp("MyProdApp", List.of("Critical"), "team", "backend");
    var nonMatchingName = createFullApp("TestApp", List.of("Critical"), "team", "backend");
    var nonMatchingTag = createFullApp("MyProdApp", List.of("Normal"), "team", "backend");
    var nonMatchingMetadata = createFullApp("MyProdApp", List.of("Critical"), "team", "frontend");

    assertThat(params.matches(matchingApp)).isTrue();
    assertThat(params.matches(nonMatchingName)).isFalse();
    assertThat(params.matches(nonMatchingTag)).isFalse();
    assertThat(params.matches(nonMatchingMetadata)).isFalse();
  }

  // -- Helper methods --

  private Application createApp(String name, String status) {
    var app = new Application();
    app.setName(name);
    app.setStatus(status);
    app.setAppId("app-" + name.toLowerCase());
    return app;
  }

  private Application createAppWithTags(String name, List<String> tags) {
    var app = createApp(name, "Active");
    app.setTags(tags);
    return app;
  }

  private Application createAppWithMetadata(String name, String metaName, String metaValue) {
    var app = createApp(name, "Active");
    var metadata = new com.contrast.labs.ai.mcp.contrast.sdkextension.data.application.Metadata();
    metadata.setName(metaName);
    metadata.setValue(metaValue);
    app.setMetadataEntities(List.of(metadata));
    return app;
  }

  private Application createFullApp(
      String name, List<String> tags, String metaName, String metaValue) {
    var app = createAppWithTags(name, tags);
    var metadata = new com.contrast.labs.ai.mcp.contrast.sdkextension.data.application.Metadata();
    metadata.setName(metaName);
    metadata.setValue(metaValue);
    app.setMetadataEntities(List.of(metadata));
    return app;
  }
}
