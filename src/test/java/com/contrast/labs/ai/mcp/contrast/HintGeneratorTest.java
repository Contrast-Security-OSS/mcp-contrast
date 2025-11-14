package com.contrast.labs.ai.mcp.contrast;

import static org.junit.jupiter.api.Assertions.*;

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
    assertTrue(
        sqlInjectionHints.contains("allow list"),
        "SQL injection hints should contain 'allow list' guidance");
  }

  @Test
  @DisplayName("Test with null rule")
  public void testGenerateVulnerabilityFixHintForNullRule() {
    var nullRuleHints = HintGenerator.generateVulnerabilityFixHint(null);
    assertTrue(
        nullRuleHints.contains("Where a vulnerable library exists"),
        "Null rule should return the default hint");
  }

  @Test
  @DisplayName("Test with empty rule")
  public void testGenerateVulnerabilityFixHintForEmptyRule() {
    var emptyRuleHints = HintGenerator.generateVulnerabilityFixHint("");
    assertTrue(
        emptyRuleHints.contains("Where a vulnerable library exists"),
        "Empty rule should return the default hint");
  }

  @Test
  @DisplayName("Test with non-existent rule")
  public void testGenerateVulnerabilityFixHintForNonExistentRule() {
    var nonExistentRuleHints = HintGenerator.generateVulnerabilityFixHint("non-existent-rule");
    assertTrue(
        nonExistentRuleHints.contains("Where a vulnerable library exists"),
        "Non-existent rule should return the default hint");
  }
}
