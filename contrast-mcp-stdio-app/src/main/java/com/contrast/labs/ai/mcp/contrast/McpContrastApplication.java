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

import static java.util.List.of;

import com.contrast.labs.ai.mcp.contrast.tool.application.GetSessionMetadataTool;
import com.contrast.labs.ai.mcp.contrast.tool.application.SearchApplicationsTool;
import com.contrast.labs.ai.mcp.contrast.tool.attack.GetProtectRulesTool;
import com.contrast.labs.ai.mcp.contrast.tool.attack.SearchAttacksTool;
import com.contrast.labs.ai.mcp.contrast.tool.coverage.GetRouteCoverageTool;
import com.contrast.labs.ai.mcp.contrast.tool.library.ListApplicationLibrariesTool;
import com.contrast.labs.ai.mcp.contrast.tool.library.ListApplicationsByCveTool;
import com.contrast.labs.ai.mcp.contrast.tool.sast.GetSastProjectTool;
import com.contrast.labs.ai.mcp.contrast.tool.sast.GetSastResultsTool;
import com.contrast.labs.ai.mcp.contrast.tool.vulnerability.GetVulnerabilityTool;
import com.contrast.labs.ai.mcp.contrast.tool.vulnerability.ListVulnerabilityTypesTool;
import com.contrast.labs.ai.mcp.contrast.tool.vulnerability.SearchAppVulnerabilitiesTool;
import com.contrast.labs.ai.mcp.contrast.tool.vulnerability.SearchVulnerabilitiesTool;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;

@Slf4j
@SpringBootApplication
@ConfigurationPropertiesScan
public class McpContrastApplication {

  private static final int SEPARATOR_WIDTH = 60;

  public static void main(String[] args) {
    SpringApplication.run(McpContrastApplication.class, args);
  }

  @Bean
  public ApplicationRunner logVersion(
      org.springframework.beans.factory.ObjectProvider<BuildProperties> buildPropertiesProvider) {
    return args -> {
      BuildProperties buildProperties = buildPropertiesProvider.getIfAvailable();
      log.info("=".repeat(SEPARATOR_WIDTH));
      if (buildProperties != null) {
        log.info("Contrast MCP Server - Version {}", buildProperties.getVersion());
      } else {
        log.info("Contrast MCP Server - Version information not available");
      }
      log.info("=".repeat(SEPARATOR_WIDTH));
    };
  }

  @Bean
  public List<ToolCallback> tools(
      // Assess tools
      GetVulnerabilityTool getVulnerabilityTool,
      SearchVulnerabilitiesTool searchVulnerabilitiesTool,
      SearchAppVulnerabilitiesTool searchAppVulnerabilitiesTool,
      ListVulnerabilityTypesTool listVulnerabilityTypesTool,
      // Application tools
      SearchApplicationsTool searchApplicationsTool,
      GetSessionMetadataTool getSessionMetadataTool,
      // ADR tools
      GetProtectRulesTool getProtectRulesTool,
      SearchAttacksTool searchAttacksTool,
      // SAST tools
      GetSastProjectTool getSastProjectTool,
      GetSastResultsTool getSastResultsTool,
      // SCA tools
      ListApplicationLibrariesTool listApplicationLibrariesTool,
      ListApplicationsByCveTool listApplicationsByCveTool,
      // Coverage tools
      GetRouteCoverageTool getRouteCoverageTool) {
    return of(
        ToolCallbacks.from(
            getVulnerabilityTool,
            searchVulnerabilitiesTool,
            searchAppVulnerabilitiesTool,
            listVulnerabilityTypesTool,
            searchApplicationsTool,
            getSessionMetadataTool,
            getProtectRulesTool,
            searchAttacksTool,
            getSastProjectTool,
            getSastResultsTool,
            listApplicationLibrariesTool,
            listApplicationsByCveTool,
            getRouteCoverageTool));
  }
}
