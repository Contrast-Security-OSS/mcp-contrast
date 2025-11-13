package com.contrast.labs.ai.mcp.contrast;

import static org.assertj.core.api.Assertions.assertThat;

import com.contrast.labs.ai.mcp.contrast.hints.HintGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Test class for the HintGenerator. Tests the generation of vulnerability fix hints for different
 * scenarios.
 */
public class HintGeneratorTest {

  @Test
  @DisplayName("Test with a valid SQL injection rule")
  public void testGenerateVulnerabilityFixHintForSqlInjection() {
    var sqlInjectionHints = HintGenerator.generateVulnerabilityFixHint("sql-injection");
    assertThat(sqlInjectionHints)
        .as("SQL injection hints should contain 'allow list' guidance")
        .contains("allow list");
  }

  @Test
  @DisplayName("Test with null rule")
  public void testGenerateVulnerabilityFixHintForNullRule() {
    var nullRuleHints = HintGenerator.generateVulnerabilityFixHint(null);
    assertThat(nullRuleHints)
        .as("Null rule should return the default hint")
        .contains("Where a vulnerable library exists");
  }

  @Test
  @DisplayName("Test with empty rule")
  public void testGenerateVulnerabilityFixHintForEmptyRule() {
    var emptyRuleHints = HintGenerator.generateVulnerabilityFixHint("");
    assertThat(emptyRuleHints)
        .as("Empty rule should return the default hint")
        .contains("Where a vulnerable library exists");
  }

  @Test
  @DisplayName("Test with non-existent rule")
  public void testGenerateVulnerabilityFixHintForNonExistentRule() {
    var nonExistentRuleHints = HintGenerator.generateVulnerabilityFixHint("non-existent-rule");
    assertThat(nonExistentRuleHints)
        .as("Non-existent rule should return the default hint")
        .contains("Where a vulnerable library exists");
  }
}
