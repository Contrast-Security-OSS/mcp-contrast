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

import com.contrast.labs.ai.mcp.contrast.client.ContrastApiClient;
import com.contrast.labs.ai.mcp.contrast.result.ScanProject;
import com.contrast.labs.ai.mcp.contrast.tool.base.SingleTool;
import com.contrast.labs.ai.mcp.contrast.tool.base.SingleToolResponse;
import com.contrast.labs.ai.mcp.contrast.tool.base.WarningCollector;
import com.contrast.labs.ai.mcp.contrast.tool.sast.params.GetSastProjectParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * MCP tool for retrieving SAST (Contrast Scan) project details by name. Returns project metadata
 * including language, scan counts, and vulnerability severity totals.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GetSastProjectTool extends SingleTool<GetSastProjectParams, ScanProject> {

  private final ContrastApiClient contrastApiClient;

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

          Note: Project name matching is case-insensitive. The returned project's name field
          reflects the canonical casing as stored in Contrast.
          """)
  public SingleToolResponse<ScanProject> getScanProject(
      @ToolParam(description = "Scan project name (matched case-insensitively)") String projectName,
      ToolContext toolContext) {
    return executePipeline(() -> GetSastProjectParams.of(projectName), toolContext);
  }

  public SingleToolResponse<ScanProject> getScanProject(String projectName) {
    return getScanProject(projectName, null);
  }

  @Override
  protected ScanProject doExecute(GetSastProjectParams params, WarningCollector collector)
      throws Exception {
    log.debug("Retrieving scan project details for project: {}", params.projectName());

    var project = contrastApiClient.getScanProject(params.projectName());

    if (project == null) {
      log.debug("Project not found: {}", params.projectName());
      return null; // SingleTool converts this to notFound response
    }

    log.debug(
        "Successfully found project: {} (id: {}, language: {})",
        project.name(),
        project.id(),
        project.language());

    return project;
  }
}
