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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class CursorToolResponseTest {

  @Test
  void success_should_include_cursor_pagination_metadata_only() {
    var response =
        CursorToolResponse.success(List.of("item1"), 50, "next-opaque-token", true, List.of(), 10L);

    assertThat(response.items()).containsExactly("item1");
    assertThat(response.pageSize()).isEqualTo(50);
    assertThat(response.nextCursor()).isEqualTo("next-opaque-token");
    assertThat(response.hasMore()).isTrue();
    assertThat(response.errors()).isEmpty();
    assertThat(response.warnings()).isEmpty();
    assertThat(response.durationMs()).isEqualTo(10L);
    assertThat(response.isSuccess()).isTrue();
  }

  @Test
  void validationError_should_create_error_without_page_fields() {
    var response = CursorToolResponse.validationError(50, List.of("issueId is required"));

    assertThat(response.items()).isEmpty();
    assertThat(response.pageSize()).isEqualTo(50);
    assertThat(response.nextCursor()).isNull();
    assertThat(response.hasMore()).isFalse();
    assertThat(response.errors()).containsExactly("issueId is required");
    assertThat(response.durationMs()).isNull();
    assertThat(response.isSuccess()).isFalse();
  }

  @Test
  void error_should_create_single_error_response() {
    var response = CursorToolResponse.error(25, "Invalid cursor");

    assertThat(response.items()).isEmpty();
    assertThat(response.pageSize()).isEqualTo(25);
    assertThat(response.nextCursor()).isNull();
    assertThat(response.hasMore()).isFalse();
    assertThat(response.errors()).containsExactly("Invalid cursor");
    assertThat(response.warnings()).isEmpty();
  }

  @Test
  void compact_constructor_should_copy_lists() {
    var mutableItems = new ArrayList<String>();
    mutableItems.add("item1");

    var response =
        new CursorToolResponse<>(mutableItems, 50, null, false, List.of(), List.of(), null);
    mutableItems.add("item2");

    assertThat(response.items()).containsExactly("item1");
  }

  @Test
  void record_components_should_exclude_total_and_random_access_page_fields() {
    var componentNames =
        Arrays.stream(CursorToolResponse.class.getRecordComponents())
            .map(component -> component.getName())
            .toList();

    assertThat(componentNames)
        .containsExactly(
            "items", "pageSize", "nextCursor", "hasMore", "errors", "warnings", "durationMs")
        .doesNotContain("page", "offset", "totalPages", "totalItems", "hasMorePages");
  }
}
