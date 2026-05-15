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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExecutionResultTest {

  @Test
  void of_should_create_result_with_items_and_total() {
    var items = List.of("a", "b", "c");

    var result = ExecutionResult.of(items, 100);

    assertThat(result.items()).containsExactly("a", "b", "c");
    assertThat(result.totalItems()).isEqualTo(100);
  }

  @Test
  void of_should_create_result_with_items_only() {
    var items = List.of("x", "y");

    var result = ExecutionResult.of(items);

    assertThat(result.items()).containsExactly("x", "y");
    assertThat(result.totalItems()).isNull();
  }

  @Test
  void empty_should_create_empty_result() {
    var result = ExecutionResult.<String>empty();

    assertThat(result.items()).isEmpty();
    assertThat(result.totalItems()).isEqualTo(0);
  }

  @Test
  void constructor_should_handle_null_items() {
    var result = new ExecutionResult<String>(null, 5);

    assertThat(result.items()).isEmpty();
    assertThat(result.totalItems()).isEqualTo(5);
  }

  @Test
  void constructor_should_handle_null_total() {
    var result = new ExecutionResult<>(List.of("item"), null);

    assertThat(result.items()).containsExactly("item");
    assertThat(result.totalItems()).isNull();
  }

  @Test
  void items_should_be_immutable() {
    var result = ExecutionResult.of(List.of("a", "b"), 2);

    assertThatThrownBy(() -> result.items().add("c"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void items_should_be_defensive_copy() {
    var mutableList = new ArrayList<>(List.of("a", "b"));
    var result = ExecutionResult.of(mutableList, 2);

    mutableList.add("c");

    assertThat(result.items()).containsExactly("a", "b");
  }
}
