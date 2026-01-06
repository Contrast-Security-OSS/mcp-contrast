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
package com.contrast.labs.ai.mcp.contrast.tool.applications;

import com.contrast.labs.ai.mcp.contrast.FilterHelper;
import com.contrast.labs.ai.mcp.contrast.PaginationParams;
import com.contrast.labs.ai.mcp.contrast.data.ApplicationData;
import com.contrast.labs.ai.mcp.contrast.data.Metadata;
import com.contrast.labs.ai.mcp.contrast.sdkextension.SDKHelper;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.application.Application;
import com.contrast.labs.ai.mcp.contrast.tool.applications.params.ApplicationFilterParams;
import com.contrast.labs.ai.mcp.contrast.tool.base.BasePaginatedTool;
import com.contrast.labs.ai.mcp.contrast.tool.base.ExecutionResult;
import com.contrast.labs.ai.mcp.contrast.tool.base.PaginatedToolResponse;
import java.util.List;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * MCP tool for searching applications in an organization. Demonstrates the tool-per-class pattern
 * with BasePaginatedTool and in-memory filtering.
 */
@Service
public class SearchApplicationsTool
    extends BasePaginatedTool<ApplicationFilterParams, ApplicationData> {

  @Tool(
      name = "search_applications",
      description =
          """
          Search applications with optional filters. Returns all applications if no filters specified.

          Filtering behavior:
          - name: Partial, case-insensitive matching (finds "app" in "MyApp")
          - tag: Exact, case-sensitive matching (CASE-SENSITIVE - 'Production' != 'production')
          - metadataName + metadataValue: Exact, case-insensitive matching for both
          - metadataName only: Returns apps with that metadata field (any value)

          Note: Application data is cached for 5 minutes. If you recently created/modified
          applications in TeamServer and don't see changes, wait 5 minutes and retry.

          Related tools:
          - get_session_metadata: Get session metadata for an application
          - search_vulnerabilities: Search vulnerabilities across applications
          """)
  public PaginatedToolResponse<ApplicationData> searchApplications(
      @ToolParam(description = "Page number (1-based), default: 1", required = false) Integer page,
      @ToolParam(description = "Items per page (max 100), default: 50", required = false)
          Integer pageSize,
      @ToolParam(
              description = "Application name filter (partial, case-insensitive)",
              required = false)
          String name,
      @ToolParam(
              description = "Tag filter (CASE-SENSITIVE - 'Production' != 'production')",
              required = false)
          String tag,
      @ToolParam(description = "Metadata field name (case-insensitive)", required = false)
          String metadataName,
      @ToolParam(
              description = "Metadata field value (case-insensitive, requires metadataName)",
              required = false)
          String metadataValue) {

    return executePipeline(
        page, pageSize, () -> ApplicationFilterParams.of(name, tag, metadataName, metadataValue));
  }

  @Override
  protected ExecutionResult<ApplicationData> doExecute(
      PaginationParams pagination, ApplicationFilterParams params, List<String> warnings)
      throws Exception {

    var sdk = getContrastSDK();
    var orgId = getOrgId();

    // Get all applications (cached for 5 minutes)
    var applications = SDKHelper.getApplicationsWithCache(orgId, sdk);

    // Apply filters in memory
    var filteredApps =
        applications.stream().filter(params::matches).map(this::toApplicationData).toList();

    // Apply pagination to filtered results
    int startIndex = pagination.offset();
    int endIndex = Math.min(startIndex + pagination.pageSize(), filteredApps.size());

    var pagedApps =
        (startIndex < filteredApps.size())
            ? filteredApps.subList(startIndex, endIndex)
            : List.<ApplicationData>of();

    return ExecutionResult.of(pagedApps, filteredApps.size());
  }

  /**
   * Converts an Application SDK model to ApplicationData response record.
   *
   * @param app the SDK application model
   * @return ApplicationData response record
   */
  private ApplicationData toApplicationData(Application app) {
    var metadata =
        app.getMetadataEntities().stream()
            .map(m -> new Metadata(m.getName(), m.getValue()))
            .toList();

    return new ApplicationData(
        app.getName(),
        app.getStatus(),
        app.getAppId(),
        FilterHelper.formatTimestamp(app.getLastSeen()),
        app.getLanguage(),
        metadata,
        app.getTags(),
        app.getTechs());
  }
}
