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

package com.contrast.labs.ai.mcp.contrast;

import static org.assertj.core.api.Assertions.assertThat;

import com.contrast.labs.ai.mcp.contrast.hints.HintGenerator;
import com.contrast.labs.ai.mcp.contrast.hints.HintProvider;
import com.contrast.labs.ai.mcp.contrast.hints.HintUtils;
import org.junit.jupiter.api.Test;

/**
 * Test class for the HintGenerator. Tests the generation of vulnerability fix hints for different
 * scenarios.
 */
public class HintGeneratorTest {

  @Test
  public void generateVulnerabilityFixHint_should_include_specific_hints_when_rule_is_mapped() {
    var expected = HintUtils.formatHints(HintProvider.getAllHintsForRule("sql-injection"));

    var result = HintGenerator.generateVulnerabilityFixHint("sql-injection");

    assertThat(result).isEqualTo(expected).contains("allow list", "prepared statement");
  }

  @Test
  public void generateVulnerabilityFixHint_should_return_full_general_guidance_when_rule_is_null() {
    var expected = HintUtils.formatHints(HintProvider.getGeneralGuidance());

    var result = HintGenerator.generateVulnerabilityFixHint(null);

    assertThat(result).isEqualTo(expected).contains("dompurify, esapi");
  }

  @Test
  public void
      generateVulnerabilityFixHint_should_return_full_general_guidance_when_rule_is_blank() {
    var expected = HintUtils.formatHints(HintProvider.getGeneralGuidance());

    var result = HintGenerator.generateVulnerabilityFixHint(" \t");

    assertThat(result).isEqualTo(expected).contains("dompurify, esapi");
  }

  @Test
  public void generateVulnerabilityFixHint_should_omit_unmapped_rule_from_full_general_guidance() {
    var expected = HintUtils.formatHints(HintProvider.getGeneralGuidance());

    var result = HintGenerator.generateVulnerabilityFixHint("jndi-injection");

    assertThat(result)
        .isEqualTo(expected)
        .contains("dompurify, esapi")
        .doesNotContain("jndi-injection");
  }
}
