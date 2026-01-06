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

import com.contrast.labs.ai.mcp.contrast.tool.sca.ListApplicationLibrariesTool;
import com.contrast.labs.ai.mcp.contrast.tool.sca.ListApplicationsByCveTool;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class McpContrastApplication {

  private static final Logger logger = LoggerFactory.getLogger(McpContrastApplication.class);

  public static void main(String[] args) {
    SpringApplication.run(McpContrastApplication.class, args);
  }

  @Bean
  public ApplicationRunner logVersion(
      org.springframework.beans.factory.ObjectProvider<BuildProperties> buildPropertiesProvider) {
    return args -> {
      BuildProperties buildProperties = buildPropertiesProvider.getIfAvailable();
      logger.info("=".repeat(60));
      if (buildProperties != null) {
        logger.info("Contrast MCP Server - Version {}", buildProperties.getVersion());
      } else {
        logger.info("Contrast MCP Server - Version information not available");
      }
      logger.info("=".repeat(60));
    };
  }

  @Bean
  public List<ToolCallback> tools(
      AssessService assessService,
      SastService sastService,
      ADRService adrService,
      RouteCoverageService routeCoverageService,
      ListApplicationLibrariesTool listApplicationLibrariesTool,
      ListApplicationsByCveTool listApplicationsByCveTool) {
    return of(
        ToolCallbacks.from(
            assessService,
            sastService,
            adrService,
            routeCoverageService,
            listApplicationLibrariesTool,
            listApplicationsByCveTool));
  }
}
