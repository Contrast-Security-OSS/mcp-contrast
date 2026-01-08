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
package com.contrast.labs.ai.mcp.contrast.tool.assess;

import com.contrast.labs.ai.mcp.contrast.PaginationParams;
import com.contrast.labs.ai.mcp.contrast.data.VulnLight;
import com.contrast.labs.ai.mcp.contrast.mapper.VulnerabilityMapper;
import com.contrast.labs.ai.mcp.contrast.sdkextension.SDKExtension;
import com.contrast.labs.ai.mcp.contrast.tool.assess.params.SearchAppVulnerabilitiesParams;
import com.contrast.labs.ai.mcp.contrast.tool.base.BasePaginatedTool;
import com.contrast.labs.ai.mcp.contrast.tool.base.ExecutionResult;
import com.contrast.labs.ai.mcp.contrast.tool.base.PaginatedToolResponse;
import com.contrastsecurity.http.TraceFilterForm.TraceExpandValue;
import com.contrastsecurity.models.MetadataItem;
import com.contrastsecurity.models.SessionMetadata;
import com.contrastsecurity.models.Trace;
import com.contrastsecurity.models.TraceFilterBody;
import com.contrastsecurity.models.Traces;
import com.contrastsecurity.sdk.ContrastSDK;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * MCP tool for searching vulnerabilities within a specific application. Supports session metadata
 * filtering and latest session filtering in addition to standard vulnerability filters.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SearchAppVulnerabilitiesTool
    extends BasePaginatedTool<SearchAppVulnerabilitiesParams, VulnLight> {

  private final VulnerabilityMapper vulnerabilityMapper;

  /**
   * Maximum traces to fetch for session filtering to prevent memory exhaustion. Configurable via
   * property for testing. Default: 50,000.
   */
  @Value("${contrast.max-traces-for-session-filtering:50000}")
  private int maxTracesForSessionFiltering = 50_000;

  @Tool(
      name = "search_app_vulnerabilities",
      description =
          """
          Application-scoped vulnerability search with all filters plus session-based filtering.

          Required: appId parameter. Use search_applications tool to find application IDs by name.

          Supports all standard filters (severity, status, environment, dates, tags) PLUS:
          - Session metadata filtering: sessionMetadataName, sessionMetadataValue
          - Latest session filtering: useLatestSession=true

          Notes:
          - If useLatestSession=true and no sessions exist, returns all vulnerabilities
            for the application with a warning message.
          - If an API error occurs during multi-page fetching (for session filtering),
            partial results are returned with a warning instead of failing completely.
            Check the warnings field in the response for data completeness.

          Common usage examples:
          - All vulns in app: appId="abc123"
          - Latest session vulns: appId="abc123", useLatestSession=true
          - Session metadata: appId="abc123", sessionMetadataName="branch", sessionMetadataValue="main"
          - Production critical: appId="abc123", severities="CRITICAL", environments="PRODUCTION"
          """)
  public PaginatedToolResponse<VulnLight> searchAppVulnerabilities(
      @ToolParam(
              description =
                  "Application ID (required). Use search_applications to find app IDs by name.")
          String appId,
      @ToolParam(description = "Page number (1-based), default: 1", required = false) Integer page,
      @ToolParam(description = "Items per page (max 100), default: 50", required = false)
          Integer pageSize,
      @ToolParam(
              description = "Comma-separated severities: CRITICAL,HIGH,MEDIUM,LOW,NOTE",
              required = false)
          String severities,
      @ToolParam(
              description =
                  "Comma-separated statuses: Reported,Suspicious,Confirmed,Remediated,Fixed."
                      + " Default: Reported,Suspicious,Confirmed",
              required = false)
          String statuses,
      @ToolParam(
              description =
                  "Comma-separated vulnerability types. Use list_vulnerability_types for complete"
                      + " list",
              required = false)
          String vulnTypes,
      @ToolParam(
              description = "Comma-separated environments: DEVELOPMENT,QA,PRODUCTION",
              required = false)
          String environments,
      @ToolParam(
              description =
                  "Only include vulnerabilities with LAST ACTIVITY after this date (ISO: YYYY-MM-DD"
                      + " or epoch)",
              required = false)
          String lastSeenAfter,
      @ToolParam(
              description =
                  "Only include vulnerabilities with LAST ACTIVITY before this date (ISO:"
                      + " YYYY-MM-DD or epoch)",
              required = false)
          String lastSeenBefore,
      @ToolParam(description = "Comma-separated vulnerability tags", required = false)
          String vulnTags,
      @ToolParam(
              description =
                  "Filter by session metadata field name. Matching is case-insensitive: 'branch',"
                      + " 'Branch', and 'BRANCH' all match. Use get_session_metadata(appId) to"
                      + " discover available field names for this application. When specified"
                      + " without sessionMetadataValue, returns vulnerabilities that have this"
                      + " metadata field with any value.",
              required = false)
          String sessionMetadataName,
      @ToolParam(
              description =
                  "Filter by session metadata field value. Matching is case-insensitive: 'Main',"
                      + " 'main', and 'MAIN' all match. Use get_session_metadata(appId) to discover"
                      + " available values for the specified field name. Requires"
                      + " sessionMetadataName to be specified.",
              required = false)
          String sessionMetadataValue,
      @ToolParam(description = "Filter to latest session only", required = false)
          Boolean useLatestSession) {

    return executePipeline(
        page,
        pageSize,
        () ->
            SearchAppVulnerabilitiesParams.of(
                appId,
                severities,
                statuses,
                vulnTypes,
                environments,
                lastSeenAfter,
                lastSeenBefore,
                vulnTags,
                sessionMetadataName,
                sessionMetadataValue,
                useLatestSession));
  }

  @Override
  protected ExecutionResult<VulnLight> doExecute(
      PaginationParams pagination, SearchAppVulnerabilitiesParams params, List<String> warnings)
      throws Exception {

    var sdk = getContrastSDK();
    var orgId = getOrgId();
    var appId = params.appId();

    if (params.needsSessionFiltering()) {
      return executeWithInMemorySessionFiltering(sdk, orgId, appId, params, pagination, warnings);
    } else {
      return executeWithServerSidePagination(sdk, orgId, appId, params, pagination, warnings);
    }
  }

  private ExecutionResult<VulnLight> executeWithServerSidePagination(
      ContrastSDK sdk,
      String orgId,
      String appId,
      SearchAppVulnerabilitiesParams params,
      PaginationParams pagination,
      List<String> warnings)
      throws Exception {

    var sdkExtension = new SDKExtension(sdk);
    var filterBody = params.toTraceFilterBody();

    var expand =
        EnumSet.of(
            TraceExpandValue.SESSION_METADATA,
            TraceExpandValue.SERVER_ENVIRONMENTS,
            TraceExpandValue.APPLICATION);

    var traces =
        sdkExtension.getTraces(
            orgId, appId, filterBody, pagination.limit(), pagination.offset(), expand);

    if (traces == null || traces.getTraces() == null) {
      warnings.add("API returned no trace data. Verify permissions and filters.");
      return ExecutionResult.empty();
    }

    var vulnerabilities =
        traces.getTraces().stream().map(vulnerabilityMapper::toVulnLight).toList();

    return ExecutionResult.of(vulnerabilities, traces.getCount());
  }

  private ExecutionResult<VulnLight> executeWithInMemorySessionFiltering(
      ContrastSDK sdk,
      String orgId,
      String appId,
      SearchAppVulnerabilitiesParams params,
      PaginationParams pagination,
      List<String> warnings)
      throws Exception {

    var sdkExtension = new SDKExtension(sdk);

    // Fetch agent session ID if useLatestSession requested
    String agentSessionId = null;
    if (Boolean.TRUE.equals(params.getUseLatestSession())) {
      var latestSession = sdkExtension.getLatestSessionMetadata(orgId, appId);

      if (latestSession != null
          && latestSession.getAgentSession() != null
          && latestSession.getAgentSession().getAgentSessionId() != null) {
        agentSessionId = latestSession.getAgentSession().getAgentSessionId();
        log.debug("Using latest session ID: {}", agentSessionId);
      } else {
        warnings.add(
            "No sessions found for this application. Returning all vulnerabilities across all"
                + " sessions for this application.");
        log.warn("No sessions found for application: {}", appId);
      }
    }

    // Build TraceFilterBody with session parameters
    var filterBody = params.toTraceFilterBody(agentSessionId);

    // Build filter predicate for in-memory filtering (case-insensitive name/value matching)
    var filterPredicate =
        buildSessionFilterPredicate(
            agentSessionId, params.getSessionMetadataName(), params.getSessionMetadataValue());

    // Determine if we have a selective filter (one that will actually filter results)
    boolean hasSelectiveFilter =
        agentSessionId != null || StringUtils.hasText(params.getSessionMetadataName());

    // Calculate target count for early termination
    int targetCount =
        hasSelectiveFilter
            ? pagination.offset() + pagination.pageSize()
            : maxTracesForSessionFiltering;

    // Fetch with early termination using POST endpoint
    var fetchResult =
        fetchTracesWithEarlyTermination(
            sdkExtension, orgId, appId, filterBody, filterPredicate, targetCount);

    if (fetchResult.wasTruncated()) {
      warnings.add(
          String.format(
              "IMPORTANT: Results were truncated due to limits (max %d traces or 100 pages). "
                  + "This application may have more matching vulnerabilities than returned. "
                  + "To get complete results, narrow your search using filters: "
                  + "severity (e.g., 'CRITICAL,HIGH'), status (e.g., 'Confirmed'), "
                  + "or environment (e.g., 'PRODUCTION'). "
                  + "Without narrower filters, you may be missing critical security findings.",
              maxTracesForSessionFiltering));
    }

    if (fetchResult.hadFetchError()) {
      warnings.add(
          String.format(
              "WARNING: Partial data returned due to API error during multi-page fetch. "
                  + "Retrieved %d matching vulnerabilities before error occurred. "
                  + "Additional vulnerabilities may exist. Details: %s",
              fetchResult.traces().size(), fetchResult.errorMessage()));
    }

    // Convert to VulnLight
    var allVulnerabilities =
        fetchResult.traces().stream().map(vulnerabilityMapper::toVulnLight).toList();

    // Apply pagination to in-memory filtered results
    var startIndex = pagination.offset();
    var endIndex = Math.min(startIndex + pagination.pageSize(), allVulnerabilities.size());
    var pagedVulns =
        (startIndex < allVulnerabilities.size())
            ? allVulnerabilities.subList(startIndex, endIndex)
            : List.<VulnLight>of();

    return ExecutionResult.of(pagedVulns, allVulnerabilities.size());
  }

  /**
   * Result of fetching traces for session filtering.
   *
   * @param traces The fetched traces (may be partial if error occurred)
   * @param wasTruncated True if results hit the max limit
   * @param hadFetchError True if an API error occurred during multi-page fetch
   * @param errorMessage Description of the fetch error (null if no error)
   */
  private record SessionFilteringResult(
      List<Trace> traces, boolean wasTruncated, boolean hadFetchError, String errorMessage) {

    /** Creates a successful result (no errors). */
    static SessionFilteringResult success(List<Trace> traces, boolean wasTruncated) {
      return new SessionFilteringResult(traces, wasTruncated, false, null);
    }

    /** Creates a partial result due to fetch error. */
    static SessionFilteringResult partial(List<Trace> traces, String errorMessage) {
      return new SessionFilteringResult(traces, false, true, errorMessage);
    }
  }

  /** Fetches traces with early termination when enough filtered results are found. */
  private SessionFilteringResult fetchTracesWithEarlyTermination(
      SDKExtension sdkExtension,
      String orgId,
      String appId,
      TraceFilterBody filterBody,
      Predicate<Trace> filterPredicate,
      int targetCount)
      throws IOException {

    final int PAGE_SIZE = 500;
    final int MAX_PAGES = 100;
    final var expand =
        EnumSet.of(
            TraceExpandValue.SESSION_METADATA,
            TraceExpandValue.SERVER_ENVIRONMENTS,
            TraceExpandValue.APPLICATION);
    var matchingTraces = new ArrayList<Trace>(Math.min(targetCount, 1000));
    int offset = 0;
    int pagesChecked = 0;
    int totalTracesFetched = 0;

    while (matchingTraces.size() < targetCount && pagesChecked < MAX_PAGES) {
      if (matchingTraces.size() >= maxTracesForSessionFiltering) {
        break;
      }

      Traces pageResult;
      try {
        pageResult = sdkExtension.getTraces(orgId, appId, filterBody, PAGE_SIZE, offset, expand);
      } catch (Exception e) {
        String errorMsg =
            String.format(
                "API error during multi-page fetch at offset %d. Returning %d partial results."
                    + " Error: %s",
                offset, matchingTraces.size(), e.getMessage());
        log.warn(errorMsg, e);
        return SessionFilteringResult.partial(matchingTraces, errorMsg);
      }

      if (pageResult == null
          || pageResult.getTraces() == null
          || pageResult.getTraces().isEmpty()) {
        break;
      }

      pagesChecked++;
      totalTracesFetched += pageResult.getTraces().size();

      // Apply filter and collect matching traces with early termination
      for (Trace trace : pageResult.getTraces()) {
        if (filterPredicate.test(trace)) {
          matchingTraces.add(trace);
          if (matchingTraces.size() >= targetCount) {
            log.debug(
                "Early termination: found {} matching traces after checking {} pages ({} total"
                    + " traces)",
                matchingTraces.size(),
                pagesChecked,
                totalTracesFetched);
            return SessionFilteringResult.success(matchingTraces, false);
          }
          if (matchingTraces.size() >= maxTracesForSessionFiltering) {
            break;
          }
        }
      }

      log.debug(
          "Page {}: fetched {} traces, {} matching so far (target: {})",
          pagesChecked,
          pageResult.getTraces().size(),
          matchingTraces.size(),
          targetCount);

      // If we got fewer than PAGE_SIZE, we've reached the last page
      if (pageResult.getTraces().size() < PAGE_SIZE) {
        break;
      }

      offset += PAGE_SIZE;
    }

    // Check if we hit limits
    boolean wasTruncated = false;
    if (pagesChecked >= MAX_PAGES) {
      log.atWarn()
          .addKeyValue("appId", appId)
          .addKeyValue("pagesChecked", pagesChecked)
          .addKeyValue("matchingTraces", matchingTraces.size())
          .setMessage(
              "Reached MAX_PAGES limit during session filtering. Results may be incomplete.")
          .log();
      wasTruncated = true;
    } else if (matchingTraces.size() >= maxTracesForSessionFiltering) {
      log.atWarn()
          .addKeyValue("appId", appId)
          .addKeyValue("maxTraces", maxTracesForSessionFiltering)
          .setMessage("Reached maximum trace limit for session filtering. Results are incomplete.")
          .log();
      wasTruncated = true;
    }

    log.debug(
        "Session filtering complete: {} matching traces from {} total fetched ({} pages)",
        matchingTraces.size(),
        totalTracesFetched,
        pagesChecked);
    return SessionFilteringResult.success(matchingTraces, wasTruncated);
  }

  /** Builds a predicate for filtering traces based on session criteria. */
  private Predicate<Trace> buildSessionFilterPredicate(
      String agentSessionId, String sessionMetadataName, String sessionMetadataValue) {

    Predicate<Trace> predicate = trace -> true;

    // Add agent session ID filter if specified
    if (agentSessionId != null) {
      final String sessionIdToMatch = agentSessionId;
      predicate =
          predicate.and(
              trace ->
                  trace.getSessionMetadata() != null
                      && trace.getSessionMetadata().stream()
                          .anyMatch(sm -> sessionIdToMatch.equals(sm.getSessionId())));
    }

    // Add session metadata name/value filter if specified
    if (StringUtils.hasText(sessionMetadataName)) {
      final String nameToMatch = sessionMetadataName;
      final String valueToMatch = sessionMetadataValue;
      predicate =
          predicate.and(
              trace -> {
                if (trace.getSessionMetadata() == null) {
                  return false;
                }
                for (SessionMetadata sm : trace.getSessionMetadata()) {
                  if (sm.getMetadata() == null) {
                    continue;
                  }
                  for (MetadataItem item : sm.getMetadata()) {
                    boolean nameMatches =
                        item.getDisplayLabel() != null
                            && item.getDisplayLabel().equalsIgnoreCase(nameToMatch);
                    boolean valueMatches =
                        valueToMatch == null
                            || (item.getValue() != null
                                && item.getValue().equalsIgnoreCase(valueToMatch));
                    if (nameMatches && valueMatches) {
                      return true;
                    }
                  }
                }
                return false;
              });
    }

    return predicate;
  }
}
