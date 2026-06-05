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
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

/** Validation parameters for ListApplicationsByCveTool. */
public class ListApplicationsByCveParams extends BaseToolParams {

  private static final Pattern CVE_PATTERN = Pattern.compile("^CVE-\\d{4}-\\d{4,}$");

  private String cveId;

  private ListApplicationsByCveParams() {}

  public static ListApplicationsByCveParams of(String cveId) {
    var params = new ListApplicationsByCveParams();
    var ctx = new ToolValidationContext();
    var normalizedCveId =
        StringUtils.hasText(cveId) ? cveId.trim().toUpperCase(Locale.ROOT) : cveId;

    ctx.require(normalizedCveId, "cveId");
    if (StringUtils.hasText(normalizedCveId) && !CVE_PATTERN.matcher(normalizedCveId).matches()) {
      ctx.errorIf(
          true,
          "cveId must be in CVE format (e.g., CVE-2021-44228). "
              + "Format: CVE-YYYY-NNNNN where YYYY is the year and NNNNN is a sequence number.");
    }

    params.cveId = normalizedCveId;
    params.setValidationResult(ctx);
    return params;
  }

  public String cveId() {
    return cveId;
  }
}
