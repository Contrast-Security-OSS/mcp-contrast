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
package com.contrast.labs.ai.mcp.contrast.tool.server;

import com.contrast.labs.ai.mcp.contrast.client.ContrastApiClient;
import com.contrast.labs.ai.mcp.contrast.result.ServerSummary;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.server.ServersResponseEnvelope;
import com.contrast.labs.ai.mcp.contrast.tool.base.ExecutionResult;
import com.contrast.labs.ai.mcp.contrast.tool.base.PaginatedTool;
import com.contrast.labs.ai.mcp.contrast.tool.base.PaginatedToolResponse;
import com.contrast.labs.ai.mcp.contrast.tool.base.PaginationParams;
import com.contrast.labs.ai.mcp.contrast.tool.base.WarningCollector;
import com.contrast.labs.ai.mcp.contrast.tool.server.params.ServerFilterParams;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/** Searches the EAC-visible server inventory through the current TeamServer filter API. */
@Service
@RequiredArgsConstructor
public class SearchServersTool extends PaginatedTool<ServerFilterParams, ServerSummary> {

  private final ContrastApiClient contrastApiClient;

  @Tool(
      name = "search_servers",
      description =
          """
          Searches servers visible to the current credentials for inventory, agent health, and
          Protect coverage. Values within a comma-separated parameter are ORed; different
          parameters and the single-valued quickFilter are ANDed. To combine two quick-filter
          dimensions, filter by one and inspect item fields for the other.

          ONLINE/OFFLINE use TeamServer's activity threshold (typically about 50 minutes).
          OUT_OF_DATE means older than the newest agent TeamServer can serve for that language.
          protectEnabled=null means unknown or unavailable; non-null Assess/Protect state may
          reflect the first visible application's effective configuration. agentOutOfDate=null
          means the latest agent version could not be determined. Counts and applications are
          EAC-scoped; servers without applications require the corresponding organization access.

          Examples: quickFilter="ONLINE" to inspect Protect state; quickFilter="UNPROTECTED" with
          environments="PRODUCTION"; environments="QA" with includeApplications=true.
          Related: search_applications, search_attacks, get_route_coverage, get_session_metadata.
          """)
  public PaginatedToolResponse<ServerSummary> searchServers(
      @ToolParam(description = "Page number (1-based), default: 1", required = false) Integer page,
      @ToolParam(description = "Items per page (max 100), default: 50", required = false)
          Integer pageSize,
      @ToolParam(
              description = "Substring match over server name, hostname, path, or tag",
              required = false)
          String keyword,
      @ToolParam(
              description = "Comma-separated environments. Valid: DEVELOPMENT, QA, PRODUCTION",
              required = false)
          String environments,
      @ToolParam(
              description =
                  "Single condition. Valid: ALL, ONLINE, OFFLINE, PROTECTED, UNPROTECTED,"
                      + " OUT_OF_DATE. Default: ALL",
              required = false)
          String quickFilter,
      @ToolParam(
              description = "Comma-separated log levels. Valid: ERROR, WARN, INFO, DEBUG, TRACE",
              required = false)
          String logLevels,
      @ToolParam(description = "Comma-separated exact server tags", required = false) String tags,
      @ToolParam(description = "Comma-separated application IDs", required = false)
          String applicationIds,
      @ToolParam(
              description =
                  "Only servers with no applications; mutually exclusive with applicationIds",
              required = false)
          Boolean withoutApplications,
      @ToolParam(description = "Comma-separated exact agent versions", required = false)
          String agentVersions,
      @ToolParam(
              description =
                  "Include visible application identities; default false returns only app counts",
              required = false)
          Boolean includeApplications,
      @ToolParam(
              description =
                  "Sort as property,DIRECTION. Properties: name, environment, lastActivity,"
                      + " agentVersion. Directions: ASC, DESC. Default: lastActivity,DESC",
              required = false)
          String sort,
      ToolContext toolContext) {
    return executePipeline(
        page,
        pageSize,
        () ->
            ServerFilterParams.of(
                keyword,
                environments,
                quickFilter,
                logLevels,
                tags,
                applicationIds,
                withoutApplications,
                agentVersions,
                includeApplications,
                sort),
        toolContext);
  }

  public PaginatedToolResponse<ServerSummary> searchServers(
      Integer page,
      Integer pageSize,
      String keyword,
      String environments,
      String quickFilter,
      String logLevels,
      String tags,
      String applicationIds,
      Boolean withoutApplications,
      String agentVersions,
      Boolean includeApplications,
      String sort) {
    return searchServers(
        page,
        pageSize,
        keyword,
        environments,
        quickFilter,
        logLevels,
        tags,
        applicationIds,
        withoutApplications,
        agentVersions,
        includeApplications,
        sort,
        null);
  }

  @Override
  protected ExecutionResult<ServerSummary> doExecute(
      PaginationParams pagination, ServerFilterParams params, WarningCollector collector)
      throws Exception {
    var filterBody = params.toServerFilterBody();
    var response =
        contrastApiClient.searchServers(
            filterBody,
            pagination.limit(),
            pagination.offset(),
            params.getSort(),
            params.isIncludeApplications());

    ServersResponseEnvelope.validateAndNormalize(response, filterBody);

    var summaries =
        response.getServers().stream()
            .map(server -> ServerSummary.fromServer(server, params.isIncludeApplications()))
            .toList();
    return ExecutionResult.of(summaries, Math.toIntExact(response.getCount()));
  }
}
