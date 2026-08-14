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

import static com.contrast.labs.ai.mcp.contrast.tool.validation.ValidationConstants.MAX_PAGE_SIZE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class CursorPaginationParamsTest {

  private static final int MIN_PAGE_SIZE = 1;
  private static final String OPAQUE_CURSOR = "opaque.cursor/with+symbols==";

  @Test
  void of_should_default_page_size_and_absent_cursor_when_inputs_null() {
    var params = CursorPaginationParams.of(null, null);

    assertThat(params.cursor()).isNull();
    assertThat(params.pageSize()).isEqualTo(50);
    assertThat(params.limit()).isEqualTo(50);
    assertThat(params.cursorPresence()).isEqualTo("absent");
    assertThat(params.notices()).isEmpty();
    assertThat(params.isValid()).isTrue();
  }

  @Test
  void of_should_preserve_opaque_cursor_without_parsing() {
    var params = CursorPaginationParams.of(OPAQUE_CURSOR, 25);

    assertThat(params.cursor()).isEqualTo(OPAQUE_CURSOR);
    assertThat(params.cursorPresence()).isEqualTo("present");
    assertThat(params.pageSize()).isEqualTo(25);
    assertThat(params.limit()).isEqualTo(25);
    assertThat(params.notices()).isEmpty();
  }

  @Test
  void of_should_accept_minimum_page_size_without_notice_when_page_size_is_one() {
    var params = CursorPaginationParams.of(OPAQUE_CURSOR, MIN_PAGE_SIZE);

    assertThat(params.pageSize()).isEqualTo(MIN_PAGE_SIZE);
    assertThat(params.limit()).isEqualTo(MIN_PAGE_SIZE);
    assertThat(params.notices()).isEmpty();
  }

  @Test
  void of_should_accept_default_maximum_without_notice_when_page_size_is_maximum() {
    var params = CursorPaginationParams.of(OPAQUE_CURSOR, MAX_PAGE_SIZE);

    assertThat(params.pageSize()).isEqualTo(MAX_PAGE_SIZE);
    assertThat(params.limit()).isEqualTo(MAX_PAGE_SIZE);
    assertThat(params.notices()).isEmpty();
  }

  @Test
  void of_should_treat_blank_cursor_as_absent() {
    var params = CursorPaginationParams.of("   ", 25);

    assertThat(params.cursor()).isNull();
    assertThat(params.cursorPresence()).isEqualTo("absent");
  }

  @Test
  void of_should_default_invalid_page_size_without_exposing_cursor() {
    var params = CursorPaginationParams.of(OPAQUE_CURSOR, 0);

    assertThat(params.pageSize()).isEqualTo(50);
    assertThat(params.notices())
        .singleElement()
        .satisfies(notice -> assertThat(notice).contains("Invalid pageSize 0"));
    assertThat(params.notices()).noneMatch(notice -> notice.contains(OPAQUE_CURSOR));
  }

  @Test
  void of_should_cap_page_size_at_default_max_without_exposing_cursor() {
    var params = CursorPaginationParams.of(OPAQUE_CURSOR, 200);

    assertThat(params.pageSize()).isEqualTo(100);
    assertThat(params.notices())
        .singleElement()
        .satisfies(
            notice -> assertThat(notice).contains("Requested pageSize 200 exceeds maximum 100"));
    assertThat(params.notices()).noneMatch(notice -> notice.contains(OPAQUE_CURSOR));
  }

  @Test
  void of_should_support_narrower_endpoint_max_page_size() {
    var params = CursorPaginationParams.of(OPAQUE_CURSOR, 100, 50);

    assertThat(params.pageSize()).isEqualTo(50);
    assertThat(params.notices())
        .singleElement()
        .satisfies(
            notice -> assertThat(notice).contains("Requested pageSize 100 exceeds maximum 50"));
  }

  @Test
  void record_components_should_not_expose_random_access_pagination_fields() {
    var componentNames =
        Arrays.stream(CursorPaginationParams.class.getRecordComponents())
            .map(component -> component.getName())
            .toList();

    assertThat(componentNames)
        .containsExactly("cursor", "pageSize", "limit", "notices")
        .doesNotContain("page", "offset", "totalPages", "totalItems");
  }

  @Test
  void notices_should_be_immutable() {
    var params = CursorPaginationParams.of(null, -1);

    assertThatThrownBy(() -> params.notices().add("mutated"))
        .isInstanceOf(UnsupportedOperationException.class);
  }
}
