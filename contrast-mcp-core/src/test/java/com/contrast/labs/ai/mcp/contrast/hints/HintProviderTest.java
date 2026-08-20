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

package com.contrast.labs.ai.mcp.contrast.hints;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class HintProviderTest {

  @Test
  public void testGetHintsForRule() {
    // Test getting hints for SQL injection
    assertThat(HintProvider.getHintsForRule("sql-injection")).isNotEmpty();

    // Test getting hints for a non-existent rule
    assertThat(HintProvider.getHintsForRule("non-existent-rule")).isEmpty();
  }

  @Test
  public void testGetGeneralGuidance() {
    // General guidance should not be empty
    assertThat(HintProvider.getGeneralGuidance()).isNotEmpty();
  }

  @Test
  public void testGetAllHintsForRule() {
    // For SQL injection, we should get both general and specific hints
    assertThat(HintProvider.getAllHintsForRule("sql-injection").size())
        .isGreaterThan(HintProvider.getHintsForRule("sql-injection").size());
  }
}
