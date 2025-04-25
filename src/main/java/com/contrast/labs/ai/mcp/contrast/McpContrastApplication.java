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
package com.contrast.labs.ai.mcp.contrast;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbacks;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.Collections;
import java.util.List;

import static com.contrast.labs.ai.mcp.contrast.PromptRegistration.PROMPT_TEMPLATE;
import static java.util.List.of;

@SpringBootApplication
public class McpContrastApplication {

	public static void main(String[] args)  {
		SpringApplication.run(McpContrastApplication.class, args);
	}


	@Bean
	public List<ToolCallback> tools(AssessService assessService, SastService sastService,SCAService scaService,ADRService adrService) {
		return of(ToolCallbacks.from(assessService,sastService,scaService,adrService));
	}

	@Bean
	public List<McpServerFeatures.SyncPromptRegistration> prompts() {
		// AssessService prompts
		var getVulnPrompt = new McpSchema.Prompt("get_vulnerability_prompt", "Get details about an Assess vulnerability",
				List.of(
						new McpSchema.PromptArgument("vulnID", "The vulnerability ID", true),
						new McpSchema.PromptArgument("appID", "The application ID", true)
				));

		var getVulnByAppNamePrompt = new McpSchema.Prompt("get_vulnerability_by_app_name_prompt", "Get details about an Assess vulnerability",
				List.of(
						new McpSchema.PromptArgument("vulnID", "The vulnerability ID", true),
						new McpSchema.PromptArgument("appName", "The application name", true)
				));

		var listVulnsPrompt = new McpSchema.Prompt("list_vulnerabilities_prompt", "List vulnerabilities for an application",
				List.of(new McpSchema.PromptArgument("appID", "The application ID", true)));

		var listVulnsByAppNamePrompt = new McpSchema.Prompt("list_vulnerabilities_with_app_name_prompt", "List vulnerabilities by app name",
				List.of(new McpSchema.PromptArgument("appName", "The application name", true)));

		var listAppsPrompt = new McpSchema.Prompt("list_applications_prompt", "List active applications by name",
				List.of(new McpSchema.PromptArgument("appName", "The application name to filter by", true)));

		// SastService prompts
		var scanProjectPrompt = new McpSchema.Prompt("list_scan_project_prompt", "Get scan project details",
				List.of(new McpSchema.PromptArgument("projectName", "The scan project name", true)));

		var scanResultsPrompt = new McpSchema.Prompt("list_scan_results_prompt", "Get latest scan results for project name",
				List.of(new McpSchema.PromptArgument("projectName", "The scan project name", true)));

		// SCAService prompts
		var appLibsByIdPrompt = new McpSchema.Prompt("list_application_libraries_by_app_id_prompt", "Get libraries used by app ID",
				List.of(new McpSchema.PromptArgument("appID", "The application ID", true)));

		var appLibsPrompt = new McpSchema.Prompt("list_application_libraries_prompt", "Get libraries used by app name",
				List.of(new McpSchema.PromptArgument("appName", "The application name", true)));

		var cveAppsPrompt = new McpSchema.Prompt("list_applications_vulnerable_to_cve_prompt", "Find apps vulnerable to a CVE",
				List.of(new McpSchema.PromptArgument("cveid", "The CVE ID", true)));

		// ADRService prompts
		var protectRulesPrompt = new McpSchema.Prompt("get_adr_protect_rules_prompt", "Get protect/ADR rules by app name",
				List.of(new McpSchema.PromptArgument("applicationName", "The application name", true)));

		var protectRulesByIdPrompt = new McpSchema.Prompt("get_adr_protect_rules_by_app_id_prompt", "Get protect/ADR rules by app ID",
				List.of(new McpSchema.PromptArgument("appID", "The application ID", true)));

		// Create generic message handler for all prompts
		return List.of(
				getAssistantPrompt(),
				createToolPromptRegistration(getVulnPrompt, "get_vulnerability"),
				createToolPromptRegistration(getVulnByAppNamePrompt, "get_vulnerability_by_app_name"),
				createToolPromptRegistration(listVulnsPrompt, "list_vulnerabilities"),
				createToolPromptRegistration(listVulnsByAppNamePrompt, "list_vulnerabilities_with_app_name"),
				createToolPromptRegistration(listAppsPrompt, "list_applications"),
				createToolPromptRegistration(scanProjectPrompt, "list_Scan_Project"),
				createToolPromptRegistration(scanResultsPrompt, "list_Scan_Results"),
				createToolPromptRegistration(appLibsByIdPrompt, "list_application_libraries_by_app_id"),
				createToolPromptRegistration(appLibsPrompt, "list_application_libraries"),
				createToolPromptRegistration(cveAppsPrompt, "list_applications_vulnerable_to_cve"),
				createToolPromptRegistration(protectRulesPrompt, "get_ADR_Protect_Rules"),
				createToolPromptRegistration(protectRulesByIdPrompt, "get_ADR_Protect_Rules_by_app_id")
		);
	}



	private McpServerFeatures.SyncPromptRegistration getAssistantPrompt() {
		var prompt = new McpSchema.Prompt("default-contrast-prompt", "A prompt to seed system prompt for Contrast chat with mcp.", Collections.emptyList());
		return new McpServerFeatures.SyncPromptRegistration(prompt, getPromptRequest -> {
			var promptMessage = new McpSchema.PromptMessage(McpSchema.Role.ASSISTANT,
					new McpSchema.TextContent(PROMPT_TEMPLATE));
			return new McpSchema.GetPromptResult("default-contrast-prompt", List.of(promptMessage));
		});
	}


	// Helper method to create tool prompt registrations with consistent formatting
	private McpServerFeatures.SyncPromptRegistration createToolPromptRegistration(McpSchema.Prompt prompt, String toolName) {
		return new McpServerFeatures.SyncPromptRegistration(prompt, getPromptRequest -> {
			var userMessage = new McpSchema.PromptMessage(McpSchema.Role.USER,
					new McpSchema.TextContent("Please use the " + toolName + " tool with the following parameters: " + getPromptRequest.arguments()));
			return new McpSchema.GetPromptResult("A prompt to use the " + toolName + " tool", List.of(userMessage));
		});
	}
}
