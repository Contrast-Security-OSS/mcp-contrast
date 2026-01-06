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
package com.contrast.labs.ai.mcp.contrast.tool.coverage;

import com.contrast.labs.ai.mcp.contrast.sdkextension.SDKExtension;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.routecoverage.Route;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.routecoverage.RouteCoverageBySessionIDAndMetadataRequestExtended;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.routecoverage.RouteCoverageResponse;
import com.contrast.labs.ai.mcp.contrast.tool.base.BaseSingleTool;
import com.contrast.labs.ai.mcp.contrast.tool.base.SingleToolResponse;
import com.contrast.labs.ai.mcp.contrast.tool.coverage.params.RouteCoverageParams;
import com.contrastsecurity.models.RouteCoverageMetadataLabelValues;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * MCP tool for retrieving route coverage data for an application. Demonstrates the tool-per-class
 * pattern with BaseSingleTool for non-paginated retrieval.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GetRouteCoverageTool
    extends BaseSingleTool<RouteCoverageParams, RouteCoverageResponse> {

  @Tool(
      name = "get_route_coverage",
      description =
          """
          Retrieves route coverage data for an application.

          Routes can have two statuses:
          - DISCOVERED: Found by Contrast Assess but has not received any HTTP requests
          - EXERCISED: Has received at least one HTTP request

          Response fields:
          - routes: List of routes with coverage status and details
          - success: Whether the request completed successfully

          Filtering options (mutually exclusive):
          - No filter: Returns all routes across all sessions
          - sessionMetadataName + sessionMetadataValue: Filter by session metadata (e.g., branch=main)
          - useLatestSession: Filter by the most recent session only

          NOTE: sessionMetadataName and sessionMetadataValue must be provided together or both omitted.
          If both useLatestSession and session metadata are provided, useLatestSession takes precedence.

          Usage examples:
          - Get all routes: appId="app-123"
          - Filter by branch: appId="app-123", sessionMetadataName="branch", sessionMetadataValue="main"
          - Latest session only: appId="app-123", useLatestSession=true

          Related tools:
          - search_applications: Find application IDs by name or tag
          - get_session_metadata: View available session metadata fields
          """)
  public SingleToolResponse<RouteCoverageResponse> getRouteCoverage(
      @ToolParam(description = "Application ID (use search_applications to find)") String appId,
      @ToolParam(
              description =
                  "Session metadata field name to filter by (e.g., 'branch'). Must be provided with"
                      + " sessionMetadataValue.",
              required = false)
          String sessionMetadataName,
      @ToolParam(
              description =
                  "Session metadata field value to filter by (e.g., 'main'). Must be provided with"
                      + " sessionMetadataName.",
              required = false)
          String sessionMetadataValue,
      @ToolParam(
              description =
                  "If true, only return routes from the latest session. Mutually exclusive with"
                      + " session metadata filter.",
              required = false)
          Boolean useLatestSession) {
    return executePipeline(
        () ->
            RouteCoverageParams.of(
                appId, sessionMetadataName, sessionMetadataValue, useLatestSession));
  }

  @Override
  protected RouteCoverageResponse doExecute(RouteCoverageParams params, List<String> warnings)
      throws Exception {
    var sdk = getContrastSDK();
    var orgId = getOrgId();
    var sdkExtension = new SDKExtension(sdk);

    // Build request based on parameters
    RouteCoverageBySessionIDAndMetadataRequestExtended request = null;

    if (params.isUseLatestSession()) {
      // Filter by latest session
      log.debug("Fetching latest session metadata for application ID: {}", params.appId());
      var latest = sdkExtension.getLatestSessionMetadata(orgId, params.appId());

      if (latest == null) {
        log.warn("No session metadata found for application ID: {}", params.appId());
        return null; // BaseSingleTool converts this to notFound response
      }

      if (latest.getAgentSession() == null) {
        log.warn("No agent session found for application ID: {}", params.appId());
        return null; // BaseSingleTool converts this to notFound response
      }

      request = new RouteCoverageBySessionIDAndMetadataRequestExtended();
      request.setSessionId(latest.getAgentSession().getAgentSessionId());
      log.debug("Using latest session ID: {}", latest.getAgentSession().getAgentSessionId());

    } else if (params.hasSessionMetadataFilter()) {
      // Filter by session metadata
      log.debug(
          "Filtering by session metadata: {}={}",
          params.sessionMetadataName(),
          params.sessionMetadataValue());
      request = new RouteCoverageBySessionIDAndMetadataRequestExtended();
      var metadataLabelValue = new RouteCoverageMetadataLabelValues();
      metadataLabelValue.setLabel(params.sessionMetadataName());
      metadataLabelValue.getValues().add(params.sessionMetadataValue());
      request.getValues().add(metadataLabelValue);
    } else {
      log.debug("No filters applied - retrieving all route coverage");
    }

    // Call SDK to get route coverage
    log.debug("Fetching route coverage data for application ID: {}", params.appId());
    var response = sdkExtension.getRouteCoverage(orgId, params.appId(), request);

    // Defensive null checks - API may return null on errors or permission issues
    if (response == null) {
      log.warn("Route coverage API returned null for app {}", params.appId());
      return null; // BaseSingleTool converts this to notFound response
    }

    if (response.getRoutes() == null) {
      log.debug("No routes returned for app {} - returning empty response", params.appId());
      response.setRoutes(Collections.emptyList());
    }

    log.debug("Found {} routes for application", response.getRoutes().size());

    // Fetch route details for each route (N+1 API call pattern)
    log.debug("Retrieving route details for each route");
    for (Route route : response.getRoutes()) {
      log.trace("Fetching details for route: {}", route.getSignature());
      var routeDetailsResponse =
          sdkExtension.getRouteDetails(orgId, params.appId(), route.getRouteHash());
      route.setRouteDetailsResponse(routeDetailsResponse);
    }

    log.info(
        "Successfully retrieved route coverage for application ID: {} ({} routes)",
        params.appId(),
        response.getRoutes().size());

    return response;
  }
}
