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
import com.contrast.labs.ai.mcp.contrast.tool.sast.params.GetSastResultsParams;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * MCP tool for retrieving SAST scan results in SARIF format.
 *
 * @deprecated This tool returns raw SARIF which is often too large for AI consumption. Use future
 *     paginated SAST search tools instead for better AI-friendly access to scan results.
 */
@Deprecated
@Service
@Slf4j
public class GetSastResultsTool extends BaseSingleTool<GetSastResultsParams, String> {

  @Tool(
      name = "get_scan_results",
      description =
          """
          DEPRECATED: Takes a scan project name and returns the latest results in SARIF format.

          WARNING: This tool returns raw SARIF JSON which is often very large (megabytes) and
          may exceed AI context limits. Consider using future paginated SAST search tools instead.

          Returns the complete SARIF 2.1.0 JSON document including:
          - Schema and version information
          - Tool information (Contrast Scan)
          - Results array with all findings
          - Rule definitions

          Usage examples:
          - Get results: projectName="my-application"

          Note: Project names are case-sensitive and must match exactly.
          The project must have at least one completed scan.

          Related tools:
          - get_scan_project: Get project details including scan counts
          """)
  public SingleToolResponse<String> getScanResults(
      @ToolParam(description = "Scan project name (case-sensitive, must match exactly)")
          String projectName) {
    return executePipeline(() -> GetSastResultsParams.of(projectName));
  }

  @Override
  protected String doExecute(GetSastResultsParams params, List<String> warnings) throws Exception {
    var sdk = getContrastSDK();
    var orgId = getOrgId();

    log.debug(
        "Retrieving latest scan results in SARIF format for project: {}", params.projectName());

    // Find the project
    var projectOptional = sdk.scan(orgId).projects().findByName(params.projectName());

    if (projectOptional.isEmpty()) {
      log.debug("Project not found: {}", params.projectName());
      return null; // BaseGetTool converts this to notFound response
    }

    var project = projectOptional.get();
    log.debug("Found project with id: {}", project.id());

    // Check if project has any completed scans
    if (project.lastScanId() == null) {
      warnings.add(
          String.format(
              "No scan results available for project: %s. "
                  + "Project exists but has no completed scans.",
              params.projectName()));
      return null;
    }

    // Get the scan and retrieve SARIF
    var scans = sdk.scan(orgId).scans(project.id());
    log.debug("Retrieved scans for project, last scan id: {}", project.lastScanId());

    var scan = scans.get(project.lastScanId());
    if (scan == null) {
      warnings.add(
          String.format(
              "No scan results available for project: %s. Scan ID %s not found.",
              params.projectName(), project.lastScanId()));
      return null;
    }
    log.debug("Retrieved scan with id: {}", project.lastScanId());

    // Read SARIF content
    try (var sarifStream = scan.sarif();
        var reader = new BufferedReader(new InputStreamReader(sarifStream))) {
      var result = reader.lines().collect(Collectors.joining(System.lineSeparator()));
      log.info("Successfully retrieved SARIF data for project: {}", params.projectName());

      // Add deprecation warning
      warnings.add(
          "DEPRECATED: This tool returns raw SARIF which may be very large. "
              + "Consider using future paginated SAST search tools for better AI-friendly access.");

      return result;
    }
  }
}
