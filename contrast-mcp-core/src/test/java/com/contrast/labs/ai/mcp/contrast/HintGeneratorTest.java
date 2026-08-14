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
