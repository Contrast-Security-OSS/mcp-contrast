package com.contrast.labs.ai.mcp.contrast.hints;

import java.util.List;

/**
 * Generator for vulnerability remediation hints.
 * This class uses the RuleHints enum to generate appropriate hints based on rule types.
 */
public class HintGenerator {

    private static final String DEFAULT_HINT = "Where a vulnerable library exists in the stacktrace and it looks like that library may be the cause of the underlying vulnerability, prioritise upgrading that library but only fix the vulnerability explicitly asked by the developer";

    /**
     * Generate vulnerability fix hints for a specific rule.
     * 
     * @param rule The rule type to generate hints for
     * @return A formatted string containing all applicable hints for the rule
     */
    public static String generateVulnerabilityFixHint(String rule) {
        if (rule == null || rule.isEmpty()) {
            return DEFAULT_HINT;
        }
        
        List<String> hints = HintProvider.getAllHintsForRule(rule);
        
        if (hints.isEmpty()) {
            return DEFAULT_HINT + rule;
        }
        
        return HintUtils.formatHints(hints);
    }
}
