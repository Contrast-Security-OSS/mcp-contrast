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
package com.contrast.labs.ai.mcp.contrast.tool.adr;

import com.contrast.labs.ai.mcp.contrast.sdkextension.SDKExtension;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.ProtectData;
import com.contrast.labs.ai.mcp.contrast.tool.adr.params.GetProtectRulesParams;
import com.contrast.labs.ai.mcp.contrast.tool.base.BaseSingleTool;
import com.contrast.labs.ai.mcp.contrast.tool.base.SingleToolResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * MCP tool for retrieving Protect rules for an application. Demonstrates the tool-per-class pattern
 * with BaseGetTool for non-paginated single-item retrieval.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GetProtectRulesTool extends BaseSingleTool<GetProtectRulesParams, ProtectData> {

  @Tool(
      name = "get_protect_rules",
      description =
          """
          Takes an application ID and returns the Protect rules for the application.
          Use search_applications first to get the application ID from a name.

          Returns protection configuration including:
          - Rule names (e.g., sql-injection, xss-reflected, path-traversal)
          - Production mode for each rule (block, monitor, or off)
          - Rule-specific configuration settings

          Usage examples:
          - Get protect rules: appId="app-123"

          Note: Protect/ADR is a premium feature. The application must have Protect enabled
          and at least one rule configured. If the application has no Protect rules configured,
          an empty rules list will be returned.

          Related tools:
          - search_applications: Find application IDs by name or tag
          - search_attacks: Search for attacks across the organization
          """)
  public SingleToolResponse<ProtectData> getProtectRules(
      @ToolParam(description = "Application ID (use search_applications to find)") String appId) {
    return executePipeline(() -> GetProtectRulesParams.of(appId));
  }

  @Override
  protected ProtectData doExecute(GetProtectRulesParams params, List<String> warnings)
      throws Exception {
    var sdk = getContrastSDK();
    var extendedSDK = new SDKExtension(sdk);

    log.debug("Retrieving protection configuration for application ID: {}", params.appId());

    var protectData = extendedSDK.getProtectConfig(getOrgId(), params.appId());

    if (protectData == null) {
      log.debug("No protection data returned for application ID: {}", params.appId());
      return null; // BaseGetTool converts this to notFound response
    }

    int ruleCount = protectData.getRules() != null ? protectData.getRules().size() : 0;
    log.debug(
        "Successfully retrieved {} protection rules for application ID: {}",
        ruleCount,
        params.appId());

    if (ruleCount == 0) {
      warnings.add("Application has Protect enabled but no rules are configured.");
    }

    return protectData;
  }
}
