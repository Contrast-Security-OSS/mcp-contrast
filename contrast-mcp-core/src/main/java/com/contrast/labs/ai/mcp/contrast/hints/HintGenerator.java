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
