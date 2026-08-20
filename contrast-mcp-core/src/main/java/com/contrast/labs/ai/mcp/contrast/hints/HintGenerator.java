package com.contrast.labs.ai.mcp.contrast.hints;

import lombok.experimental.UtilityClass;
import org.springframework.util.StringUtils;

/**
 * Generator for vulnerability remediation hints. This class uses the RuleHints enum to generate
 * appropriate hints based on rule types.
 */
@UtilityClass
public class HintGenerator {

  /**
   * Generate vulnerability fix hints for a specific rule.
   *
   * @param rule The rule type to generate hints for
   * @return A formatted string containing all applicable hints for the rule
   */
  public static String generateVulnerabilityFixHint(String rule) {
    if (!StringUtils.hasText(rule)) {
      return HintUtils.formatHints(HintProvider.getGeneralGuidance());
    }

    var hints = HintProvider.getAllHintsForRule(rule);

    if (hints.isEmpty()) {
      return HintUtils.formatHints(HintProvider.getGeneralGuidance());
    }

    return HintUtils.formatHints(hints);
  }
}
