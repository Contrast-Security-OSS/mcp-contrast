/*
 * Copyright 2025 Contrast Security
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
package com.contrast.labs.ai.mcp.contrast.tool.library.params;

import com.contrast.labs.ai.mcp.contrast.tool.base.BaseToolParams;
import com.contrast.labs.ai.mcp.contrast.tool.validation.ToolValidationContext;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

/**
 * Validation parameters for ListApplicationsByCveTool. Validates required cveId and format.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * var params = ListApplicationsByCveParams.of("CVE-2021-44228");
 * if (!params.isValid()) {
 *   // Handle errors
 * }
 * }</pre>
 */
public class ListApplicationsByCveParams extends BaseToolParams {

  /** CVE ID format pattern: CVE-YYYY-NNNNN (4+ digit sequence number) */
  private static final Pattern CVE_PATTERN = Pattern.compile("^CVE-\\d{4}-\\d{4,}$");

  private String cveId;

  /** Private constructor - use static factory method {@link #of}. */
  private ListApplicationsByCveParams() {}

  /**
   * Parse and validate CVE lookup parameters.
   *
   * @param cveId CVE identifier (required, e.g., CVE-2021-44228)
   * @return ListApplicationsByCveParams with validation state
   */
  public static ListApplicationsByCveParams of(String cveId) {
    var params = new ListApplicationsByCveParams();
    var ctx = new ToolValidationContext();

    // Validate required field
    ctx.require(cveId, "cveId");

    // Validate CVE format if provided
    if (StringUtils.hasText(cveId) && !CVE_PATTERN.matcher(cveId).matches()) {
      ctx.errorIf(
          true,
          "cveId must be in CVE format (e.g., CVE-2021-44228). "
              + "Format: CVE-YYYY-NNNNN where YYYY is the year and NNNNN is a sequence number.");
    }

    params.cveId = cveId;

    params.setValidationResult(ctx);
    return params;
  }

  public String cveId() {
    return cveId;
  }
}
