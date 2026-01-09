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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class UnresolvedMetadataFilterTest {

  @Test
  void constructor_should_reject_null_field_name() {
    assertThatThrownBy(() -> new UnresolvedMetadataFilter(null, List.of("value")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("fieldName");
  }

  @Test
  void constructor_should_reject_blank_field_name() {
    assertThatThrownBy(() -> new UnresolvedMetadataFilter("  ", List.of("value")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("fieldName");
  }

  @Test
  void constructor_should_make_defensive_copy_of_values() {
    var mutableList = new ArrayList<>(List.of("a", "b"));
    var filter = new UnresolvedMetadataFilter("field", mutableList);
    mutableList.add("c");

    assertThat(filter.values()).containsExactly("a", "b");
  }

  @Test
  void constructor_should_handle_null_values_as_empty_list() {
    var filter = new UnresolvedMetadataFilter("field", null);

    assertThat(filter.values()).isEmpty();
  }

  @Test
  void record_should_store_field_name_and_values() {
    var filter = new UnresolvedMetadataFilter("branch", List.of("main", "develop"));

    assertThat(filter.fieldName()).isEqualTo("branch");
    assertThat(filter.values()).containsExactly("main", "develop");
  }
}
