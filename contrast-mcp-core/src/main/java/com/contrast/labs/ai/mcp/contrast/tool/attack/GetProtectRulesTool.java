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
package com.contrast.labs.ai.mcp.contrast.tool.attack;

import com.contrast.labs.ai.mcp.contrast.client.ContrastApiClient;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.ProtectData;
import com.contrast.labs.ai.mcp.contrast.tool.attack.params.GetProtectRulesParams;
import com.contrast.labs.ai.mcp.contrast.tool.base.SingleTool;
import com.contrast.labs.ai.mcp.contrast.tool.base.SingleToolResponse;
import com.contrast.labs.ai.mcp.contrast.tool.base.WarningCollector;
import java.util.List;
import java.util.Optional;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * MCP tool for retrieving Protect rules for an application. The legacy {@code get_protect_rules}
 * tool name is retained for AIML-110 local/remote parity.
 */
@Service
public class GetProtectRulesTool extends SingleTool<GetProtectRulesParams, ProtectData> {

  private final ContrastApiClient contrastApiClient;

  public GetProtectRulesTool(ContrastApiClient contrastApiClient) {
    this.contrastApiClient = contrastApiClient;
  }

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

          Response shape note:
          - Protect Rules use development/qa/production mode strings and populate uuid.
          - Virtual Patches use enabledDev/enabledQa/enabledProd booleans instead; their
            development/qa/production mode fields and uuid may be null.

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
      @ToolParam(description = "Application ID (use search_applications to find)") String appId,
      ToolContext toolContext) {
    return executePipeline(() -> GetProtectRulesParams.of(appId), toolContext);
  }

  public SingleToolResponse<ProtectData> getProtectRules(String appId) {
    return getProtectRules(appId, null);
  }

  @Override
  protected ProtectData doExecute(GetProtectRulesParams params, WarningCollector collector)
      throws Exception {
    var protectData = contrastApiClient.getProtectRules(params.appId());

    if (protectData == null) {
      return null;
    }

    var ruleCount = Optional.ofNullable(protectData.getRules()).map(List::size).orElse(0);
    if (ruleCount == 0) {
      collector.warn("Application has Protect enabled but no rules are configured.");
    }

    return protectData;
  }
}
