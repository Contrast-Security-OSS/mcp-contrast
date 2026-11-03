/*
 * Copyright 2026 Contrast Security
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
package com.contrast.labs.ai.mcp.contrast.sdkextension.data;

import static org.assertj.core.api.Assertions.assertThat;

import com.contrastsecurity.models.Recommendation;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RecommendationDataTest {

  @Test
  void from_should_map_all_fields_from_sdk_recommendation() {
    var sdk = new Recommendation();
    sdk.setText("plain text");
    sdk.setFormattedText("{{#bold}}formatted{{/bold}}");
    sdk.setFormattedTextVariables(Map.of("key", "value"));

    var data = RecommendationData.from(sdk);

    assertThat(data.text()).isEqualTo("plain text");
    assertThat(data.formattedText()).isEqualTo("{{#bold}}formatted{{/bold}}");
    assertThat(data.formattedTextVariables()).containsEntry("key", "value");
  }

  @Test
  void from_should_return_null_for_null_input() {
    assertThat(RecommendationData.from(null)).isNull();
  }

  @Test
  void from_should_handle_null_fields_in_sdk_recommendation() {
    var sdk = new Recommendation();

    var data = RecommendationData.from(sdk);

    assertThat(data.text()).isNull();
    assertThat(data.formattedText()).isNull();
    assertThat(data.formattedTextVariables()).isNull();
  }
}
