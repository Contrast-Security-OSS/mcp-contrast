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

package com.contrast.labs.ai.mcp.contrast.sdkextension.data.adr;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AttacksResponseTest {

  @Test
  void getTotalCount_should_prefer_count_when_both_fields_are_present() {
    var response = new AttacksResponse();
    response.setCount(7);
    response.setTotal(99);

    assertThat(response.getTotalCount()).isEqualTo(7);
  }

  @Test
  void getTotalCount_should_fall_back_to_total_when_count_is_absent() {
    var response = new AttacksResponse();
    response.setTotal(99);

    assertThat(response.getTotalCount()).isEqualTo(99);
  }

  @Test
  void getTotalCount_should_return_null_when_neither_field_is_present() {
    assertThat(new AttacksResponse().getTotalCount()).isNull();
  }

  @Test
  void getTotalCount_should_return_zero_rather_than_falling_back_when_count_is_zero() {
    var response = new AttacksResponse();
    response.setCount(0);
    response.setTotal(99);

    assertThat(response.getTotalCount()).isZero();
  }
}
