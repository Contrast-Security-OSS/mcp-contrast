package com.contrast.labs.ai.mcp.contrast.hints;

import java.util.Collections;
import java.util.List;

/**
 * Utility class to provide hints for vulnerability remediation based on rule types.
 * This class serves as a helper to retrieve hints from the RuleHints enum.
 */
public class HintProvider {

    /**
     * Get the hints for a specific rule type.
     * 
     * @param ruleName The name of the rule to get hints for
     * @return A list of hints for the specified rule, or an empty list if no hints are available
     */
    public static List<String> getHintsForRule(String ruleName) {
        RuleHints ruleHints = RuleHints.findByRuleName(ruleName);
        if (ruleHints != null) {
            return ruleHints.getHints();
        }
        return Collections.emptyList();
    }
    
    /**
     * Get general guidance that applies to all rule types.
     * 
     * @return A list of general guidance hints
     */
    public static List<String> getGeneralGuidance() {
        return RuleHints.GENERAL_GUIDANCE.getHints();
    }
    
    /**
     * Get both general guidance and rule-specific hints.
     * 
     * @param ruleName The name of the rule to get hints for
     * @return A combined list of general guidance and rule-specific hints,
     *         or empty list if the rule doesn't exist
     */
    public static List<String> getAllHintsForRule(String ruleName) {
        // For non-existent rules, return empty list
        RuleHints ruleHints = RuleHints.findByRuleName(ruleName);
        if (ruleHints == null) {
            return Collections.emptyList();
        }
        
        List<String> generalHints = getGeneralGuidance();
        List<String> specificRuleHints = getHintsForRule(ruleName);
        
        // Combine both lists (first the general guidance, then specific rule hints)
        return HintUtils.combineHints(generalHints, specificRuleHints);
    }
}
