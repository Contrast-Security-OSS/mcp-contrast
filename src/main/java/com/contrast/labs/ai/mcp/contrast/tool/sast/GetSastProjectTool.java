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
package com.contrast.labs.ai.mcp.contrast.tool.sast;

import com.contrast.labs.ai.mcp.contrast.tool.base.BaseSingleTool;
import com.contrast.labs.ai.mcp.contrast.tool.base.SingleToolResponse;
import com.contrast.labs.ai.mcp.contrast.tool.sast.params.GetSastProjectParams;
import com.contrastsecurity.sdk.scan.Project;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * MCP tool for retrieving SAST (Contrast Scan) project details by name. Returns project metadata
 * including language, scan counts, and vulnerability severity totals.
 */
@Service
@Slf4j
public class GetSastProjectTool extends BaseSingleTool<GetSastProjectParams, Project> {

  @Tool(
      name = "get_scan_project",
      description =
          """
          Takes a scan project name and returns the project details.

          Returns project metadata including:
          - id: Unique project identifier
          - name: Project name
          - language: Programming language (Java, JavaScript, etc.)
          - lastScanId: ID of the most recent scan
          - lastScanTime: When the last scan completed
          - completedScans: Total number of completed scans
          - Vulnerability counts by severity (critical, high, medium, low, note)

          Usage examples:
          - Get project: projectName="my-application"

          Note: Project names are case-sensitive and must match exactly.

          Related tools:
          - get_scan_results: Get SARIF results for a project's latest scan
          """)
  public SingleToolResponse<Project> getScanProject(
      @ToolParam(description = "Scan project name (case-sensitive, must match exactly)")
          String projectName) {
    return executePipeline(() -> GetSastProjectParams.of(projectName));
  }

  @Override
  protected Project doExecute(GetSastProjectParams params, List<String> warnings) throws Exception {
    var sdk = getContrastSDK();
    var orgId = getOrgId();

    log.debug("Retrieving scan project details for project: {}", params.projectName());

    var projectOptional = sdk.scan(orgId).projects().findByName(params.projectName());

    if (projectOptional.isEmpty()) {
      log.debug("Project not found: {}", params.projectName());
      return null; // BaseGetTool converts this to notFound response
    }

    var project = projectOptional.get();
    log.debug(
        "Successfully found project: {} (id: {}, language: {})",
        project.name(),
        project.id(),
        project.language());

    return project;
  }
}
