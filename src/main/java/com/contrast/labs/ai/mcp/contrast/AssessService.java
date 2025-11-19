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

import com.contrast.labs.ai.mcp.contrast.data.ApplicationData;
import com.contrast.labs.ai.mcp.contrast.data.LibraryLibraryObservation;
import com.contrast.labs.ai.mcp.contrast.data.Metadata;
import com.contrast.labs.ai.mcp.contrast.data.PaginatedResponse;
import com.contrast.labs.ai.mcp.contrast.data.StackLib;
import com.contrast.labs.ai.mcp.contrast.data.VulnLight;
import com.contrast.labs.ai.mcp.contrast.data.Vulnerability;
import com.contrast.labs.ai.mcp.contrast.mapper.VulnerabilityContext;
import com.contrast.labs.ai.mcp.contrast.mapper.VulnerabilityMapper;
import com.contrast.labs.ai.mcp.contrast.sdkextension.SDKExtension;
import com.contrast.labs.ai.mcp.contrast.sdkextension.SDKHelper;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.LibraryExtended;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.application.Application;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.sca.LibraryObservation;
import com.contrast.labs.ai.mcp.contrast.utils.PaginationHandler;
import com.contrastsecurity.http.TraceFilterForm;
import com.contrastsecurity.models.MetadataFilterResponse;
import com.contrastsecurity.models.MetadataItem;
import com.contrastsecurity.models.Rules;
import com.contrastsecurity.models.SessionMetadata;
import com.contrastsecurity.models.Stacktrace;
import com.contrastsecurity.models.TraceFilterBody;
import com.contrastsecurity.models.Traces;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Slf4j
@RequiredArgsConstructor
public class AssessService {
  private final VulnerabilityMapper vulnerabilityMapper;
  private final PaginationHandler paginationHandler;

  @Value("${contrast.host-name:${CONTRAST_HOST_NAME:}}")
  private String hostName;

  @Value("${contrast.api-key:${CONTRAST_API_KEY:}}")
  private String apiKey;

  @Value("${contrast.service-key:${CONTRAST_SERVICE_KEY:}}")
  private String serviceKey;

  @Value("${contrast.username:${CONTRAST_USERNAME:}}")
  private String userName;

  @Value("${contrast.org-id:${CONTRAST_ORG_ID:}}")
  private String orgID;

  @Value("${http.proxy.host:${http_proxy_host:}}")
  private String httpProxyHost;

  @Value("${http.proxy.port:${http_proxy_port:}}")
  private String httpProxyPort;

  @Tool(
      name = "get_vulnerability",
      description =
          "Takes a vulnerability ID (vulnID) and application ID (appID) and returns details about"
              + " the specific security vulnerability. Use search_applications(name=...) to find"
              + " the application ID from a name. If based on the stacktrace, the vulnerability"
              + " looks like it is in code that is not in the codebase, the vulnerability may be in"
              + " a 3rd party library, review the CVE data attached to that stackframe you believe"
              + " the vulnerability exists in and if possible upgrade that library to the next non"
              + " vulnerable version based on the remediation guidance.")
  public Vulnerability getVulnerabilityById(
      @ToolParam(description = "Vulnerability ID (UUID format)") String vulnID,
      @ToolParam(description = "Application ID") String appID)
      throws IOException {
    log.info(
        "Retrieving vulnerability details for vulnID: {} in application ID: {}", vulnID, appID);
    var contrastSDK =
        SDKHelper.getSDK(hostName, apiKey, serviceKey, userName, httpProxyHost, httpProxyPort);
    log.debug("ContrastSDK initialized with host: {}", hostName);

    try {
      var trace =
          contrastSDK.getTraces(orgID, appID, new TraceFilterBody()).getTraces().stream()
              .filter(t -> t.getUuid().equalsIgnoreCase(vulnID))
              .findFirst()
              .orElseThrow();
      log.debug("Found trace with title: {} and rule: {}", trace.getTitle(), trace.getRule());

      var recommendationResponse = contrastSDK.getRecommendation(orgID, vulnID);
      var requestResponse = contrastSDK.getHttpRequest(orgID, vulnID);
      var eventSummaryResponse = contrastSDK.getEventSummary(orgID, vulnID);

      var triggerEvent =
          eventSummaryResponse.getEvents().stream()
              .filter(e -> e.getType().equalsIgnoreCase("trigger"))
              .findFirst();

      var stackTraces = new ArrayList<String>();
      if (triggerEvent.isPresent()) {
        var sTrace = triggerEvent.get().getEvent().getStacktraces();
        if (sTrace != null) {
          stackTraces.addAll(sTrace.stream().map(Stacktrace::getDescription).toList());
          log.debug("Found {} stack traces for vulnerability", stackTraces.size());
        }
      }
      var libs = SDKHelper.getLibsForID(appID, orgID, new SDKExtension(contrastSDK));
      var lobs = new ArrayList<LibraryLibraryObservation>();
      for (LibraryExtended lib : libs) {
        var llob =
            new LibraryLibraryObservation(
                lib,
                SDKHelper.getLibraryObservationsWithCache(
                    lib.getHash(), appID, orgID, 50, new SDKExtension(contrastSDK)));
        lobs.add(llob);
      }
      var stackLibs = new ArrayList<StackLib>();
      var libsToReturn = new HashSet<LibraryExtended>();
      for (String stackTrace : stackTraces) {
        var matchingLlobOpt = findMatchingLibraryData(stackTrace, lobs);
        if (matchingLlobOpt.isPresent()) {
          var llob = matchingLlobOpt.get();
          var library = llob.library();
          if (!library.getVulnerabilities().isEmpty()) {
            libsToReturn.add(library); // Set.add() handles uniqueness efficiently
            stackLibs.add(new StackLib(stackTrace, library.getHash()));
          } else {
            stackLibs.add(new StackLib(stackTrace, null));
          }
        } else {
          stackLibs.add(new StackLib(stackTrace, null));
        }
      }

      String httpRequestText = null;
      if (requestResponse.getHttpRequest() != null) {
        httpRequestText = requestResponse.getHttpRequest().getText();
      }

      var context =
          VulnerabilityContext.builder()
              .recommendation(recommendationResponse.getRecommendation().getText())
              .stackLibs(stackLibs)
              .libraries(new ArrayList<>(libsToReturn)) // Convert Set to List
              .httpRequest(httpRequestText)
              .build();

      log.info("Successfully retrieved vulnerability details for vulnID: {}", vulnID);
      return vulnerabilityMapper.toFullVulnerability(trace, context);
    } catch (Exception e) {
      log.error("Error retrieving vulnerability details for vulnID: {}", vulnID, e);
      throw new IOException("Failed to retrieve vulnerability details: " + e.getMessage(), e);
    }
  }

  private Optional<LibraryLibraryObservation> findMatchingLibraryData(
      String stackTrace, List<LibraryLibraryObservation> lobs) {
    var lowerStackTrace = stackTrace.toLowerCase();
    for (LibraryLibraryObservation llob : lobs) {
      for (LibraryObservation lob : llob.libraryObservation()) {
        if (lob.getName() != null && lowerStackTrace.startsWith(lob.getName().toLowerCase())) {
          return Optional.of(llob);
        }
      }
    }
    return Optional.empty();
  }

  @Tool(
      name = "get_session_metadata",
      description =
          "Retrieves session metadata for a specific application by its ID. Returns the latest"
              + " session metadata for the application. Use search_applications(name=...) to find"
              + " the application ID from a name.")
  public MetadataFilterResponse getSessionMetadata(
      @ToolParam(description = "Application ID") String appId) throws IOException {
    var contrastSDK =
        SDKHelper.getSDK(hostName, apiKey, serviceKey, userName, httpProxyHost, httpProxyPort);
    return contrastSDK.getSessionMetadataForApplication(orgID, appId, null);
  }

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
          """)
  public List<ApplicationData> search_applications(
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
          String metadataValue)
      throws IOException {
    log.info(
        "Searching applications with filters - name: {}, tag: {}, metadataName: {}, metadataValue:"
            + " {}",
        name,
        tag,
        metadataName,
        metadataValue);

    // Validate metadata parameters
    var hasMetadataName = StringUtils.hasText(metadataName);
    var hasMetadataValue = StringUtils.hasText(metadataValue);

    if (hasMetadataValue && !hasMetadataName) {
      var errorMsg =
          "metadataValue requires metadataName. Valid combinations: both, metadataName only, or"
              + " neither.";
      log.error(errorMsg);
      throw new IllegalArgumentException(errorMsg);
    }

    var contrastSDK =
        SDKHelper.getSDK(hostName, apiKey, serviceKey, userName, httpProxyHost, httpProxyPort);

    try {
      var applications = SDKHelper.getApplicationsWithCache(orgID, contrastSDK);
      log.debug("Retrieved {} total applications from Contrast", applications.size());

      var filteredApps = new ArrayList<ApplicationData>();

      for (Application app : applications) {
        // Apply all filters - skip if any filter doesn't match
        if (!matchesNameFilter(app, name)
            || !matchesTagFilter(app, tag)
            || !matchesMetadataFilter(app, metadataName, metadataValue)) {
          continue;
        }

        // Application passed all filters
        filteredApps.add(
            new ApplicationData(
                app.getName(),
                app.getStatus(),
                app.getAppId(),
                FilterHelper.formatTimestamp(app.getLastSeen()),
                app.getLanguage(),
                getMetadataFromApp(app),
                app.getTags(),
                app.getTechs()));

        log.debug(
            "Application matches filters - ID: {}, Name: {}, Status: {}",
            app.getAppId(),
            app.getName(),
            app.getStatus());
      }

      log.info(
          "Found {} applications matching filters - name: {}, tag: {}, metadataName: {},"
              + " metadataValue: {}",
          filteredApps.size(),
          name,
          tag,
          metadataName,
          metadataValue);
      return filteredApps;

    } catch (Exception e) {
      log.error("Error searching applications", e);
      throw new IOException("Failed to search applications: " + e.getMessage(), e);
    }
  }

  /**
   * Check if an application matches the name filter (partial, case-insensitive).
   *
   * @param app The application to check
   * @param name The name filter to match against (null/empty means no filter)
   * @return true if the application matches the filter or no filter is specified
   */
  private boolean matchesNameFilter(Application app, String name) {
    if (!StringUtils.hasText(name)) {
      return true; // No filter specified
    }
    return app.getName().toLowerCase().contains(name.toLowerCase());
  }

  /**
   * Check if an application matches the tag filter (exact, case-sensitive).
   *
   * @param app The application to check
   * @param tag The tag filter to match against (null/empty means no filter)
   * @return true if the application matches the filter or no filter is specified
   */
  private boolean matchesTagFilter(Application app, String tag) {
    if (!StringUtils.hasText(tag)) {
      return true; // No filter specified
    }
    return app.getTags().contains(tag);
  }

  /**
   * Check if an application matches the metadata filter (exact, case-insensitive).
   *
   * @param app The application to check
   * @param metadataName The metadata field name to match (null/empty means no filter)
   * @param metadataValue The metadata field value to match (null/empty means any value)
   * @return true if the application matches the filter or no filter is specified
   */
  private boolean matchesMetadataFilter(
      Application app, String metadataName, String metadataValue) {
    if (!StringUtils.hasText(metadataName)) {
      return true; // No filter specified
    }

    var hasMetadataValue = StringUtils.hasText(metadataValue);

    if (app.getMetadataEntities() == null) {
      return false; // No metadata to match against
    }

    for (var metadata : app.getMetadataEntities()) {
      if (metadata == null || metadata.getName() == null) {
        continue;
      }

      var nameMatches = metadata.getName().equalsIgnoreCase(metadataName);

      if (hasMetadataValue) {
        // Both name and value must match
        if (nameMatches
            && metadata.getValue() != null
            && metadata.getValue().equalsIgnoreCase(metadataValue)) {
          return true;
        }
      } else {
        // Name only - any value is acceptable
        if (nameMatches) {
          return true;
        }
      }
    }

    return false; // No matching metadata found
  }

  @Tool(
      name = "search_vulnerabilities",
      description =
          """
          Search vulnerabilities across all applications in your organization with optional filtering by
          severity, status, environment, vulnerability type, date range, and tags.

          This is an organization-level search tool. For application-scoped searches with session filtering
          capabilities, use the search_app_vulnerabilities tool instead.

          Common usage examples:
          - Critical vulnerabilities only: severities="CRITICAL"
          - High-priority open issues: severities="CRITICAL,HIGH", statuses="Reported,Confirmed"
          - Production vulnerabilities: environments="PRODUCTION"
          - Recent activity: lastSeenAfter="2025-01-01"
          - Production critical issues with recent activity: environments="PRODUCTION", severities="CRITICAL", lastSeenAfter="2025-01-01"
          - SmartFix remediated vulnerabilities: vulnTags="SmartFix Remediated", statuses="Remediated"
          - Reviewed critical vulnerabilities: vulnTags="reviewed", severities="CRITICAL"

          Note: This tool requires Contrast Platform Admin or Org Admin permissions to access organization-level
          vulnerability data.

          Returns paginated results with metadata including totalItems (when available) and hasMorePages.
          Check 'message' field for validation warnings or empty result info.

          Response fields:
          - environments: List of all environments (DEVELOPMENT, QA, PRODUCTION) where this vulnerability
                         has been seen over time. Shows historical presence across environments.
          - application: Application name and ID where the vulnerability was found.

          Related tools:
          - search_app_vulnerabilities: For app-scoped searches with session filtering
          - search_applications: To find application IDs by name, tag, or metadata
          """)
  public PaginatedResponse<VulnLight> searchVulnerabilities(
      @ToolParam(description = "Page number (1-based), default: 1", required = false) Integer page,
      @ToolParam(description = "Items per page (max 100), default: 50", required = false)
          Integer pageSize,
      @ToolParam(
              description =
                  "Comma-separated severities: CRITICAL,HIGH,MEDIUM,LOW,NOTE. Default: all"
                      + " severities",
              required = false)
          String severities,
      @ToolParam(
              description =
                  "Comma-separated statuses: Reported,Suspicious,Confirmed,Remediated,Fixed."
                      + " Default: Reported,Suspicious,Confirmed (excludes Fixed and Remediated to"
                      + " focus on actionable items)",
              required = false)
          String statuses,
      @ToolParam(
              description =
                  "Comma-separated vulnerability types (e.g., sql-injection,xss-reflected). Use"
                      + " list_vulnerability_types tool for complete list. Default: all types",
              required = false)
          String vulnTypes,
      @ToolParam(
              description =
                  "Comma-separated environments: DEVELOPMENT,QA,PRODUCTION. Default: all"
                      + " environments",
              required = false)
          String environments,
      @ToolParam(
              description =
                  "Only include vulnerabilities with LAST ACTIVITY after this date (ISO format:"
                      + " YYYY-MM-DD or epoch timestamp). Filters on lastTimeSeen, not discovery"
                      + " date",
              required = false)
          String lastSeenAfter,
      @ToolParam(
              description =
                  "Only include vulnerabilities with LAST ACTIVITY before this date (ISO format:"
                      + " YYYY-MM-DD or epoch timestamp). Filters on lastTimeSeen, not discovery"
                      + " date",
              required = false)
          String lastSeenBefore,
      @ToolParam(
              description =
                  "Comma-separated VULNERABILITY-LEVEL tags (e.g., 'SmartFix Remediated,reviewed')."
                      + " Note: These are vulnerability tags, not application tags. Use to find"
                      + " vulnerabilities tagged during remediation workflows",
              required = false)
          String vulnTags)
      throws IOException {
    log.info(
        "Searching org vulnerabilities - page: {}, pageSize: {}, filters: severities={},"
            + " statuses={}, vulnTypes={}, environments={}, lastSeenAfter={}, lastSeenBefore={},"
            + " vulnTags={}",
        page,
        pageSize,
        severities,
        statuses,
        vulnTypes,
        environments,
        lastSeenAfter,
        lastSeenBefore,
        vulnTags);
    long startTime = System.currentTimeMillis();

    // Parse and validate inputs
    var pagination = PaginationParams.of(page, pageSize);
    var filters =
        VulnerabilityFilterParams.of(
            severities,
            statuses,
            null, // No appId for org-level search
            vulnTypes,
            environments,
            lastSeenAfter,
            lastSeenBefore,
            vulnTags);

    // Check for hard failures - return error immediately if invalid
    if (!filters.isValid()) {
      var errorMessage = String.join(" ", filters.errors());
      log.error("Validation errors: {}", errorMessage);
      return PaginatedResponse.error(pagination.page(), pagination.pageSize(), errorMessage);
    }

    var contrastSDK =
        SDKHelper.getSDK(hostName, apiKey, serviceKey, userName, httpProxyHost, httpProxyPort);

    try {
      // Configure SDK request with validated params
      var filterForm = filters.toTraceFilterForm();
      filterForm.setLimit(pagination.limit());
      filterForm.setOffset(pagination.offset());
      filterForm.setExpand(
          EnumSet.of(
              TraceFilterForm.TraceExpandValue.SERVER_ENVIRONMENTS,
              TraceFilterForm.TraceExpandValue.SESSION_METADATA,
              TraceFilterForm.TraceExpandValue.APPLICATION));

      // Use organization-level API
      Traces traces = contrastSDK.getTracesInOrg(orgID, filterForm);

      if (traces != null && traces.getTraces() != null) {
        // Organization API worked (empty list with count=0 is valid - means no vulnerabilities or
        // no EAC access)
        var vulnerabilities =
            traces.getTraces().stream().map(vulnerabilityMapper::toVulnLight).toList();

        // Get totalItems if available from SDK response (don't make extra query)
        var totalItems = (traces.getCount() != null) ? traces.getCount() : null;

        // Use PaginationHandler to create paginated response with all warnings
        var response =
            paginationHandler.createPaginatedResponse(
                vulnerabilities, pagination, totalItems, filters.warnings());

        long duration = System.currentTimeMillis() - startTime;
        log.info(
            "Retrieved {} vulnerabilities for page {} (pageSize: {}, totalItems: {}, took {} ms)",
            response.items().size(),
            response.page(),
            response.pageSize(),
            response.totalItems(),
            duration);

        return response;

      } else {
        // Org-level API returned null - unexpected condition
        var errorMsg =
            String.format(
                "Org-level vulnerability API returned null for org %s. This is unexpected - the API"
                    + " should return an empty list if no vulnerabilities exist. Please check API"
                    + " connectivity and permissions.",
                orgID);
        log.error(errorMsg);
        return PaginatedResponse.error(pagination.page(), pagination.pageSize(), errorMsg);
      }

    } catch (Exception e) {
      log.error("Error listing all vulnerabilities", e);
      throw new IOException("Failed to list all vulnerabilities: " + e.getMessage(), e);
    }
  }

  @Tool(
      name = "search_app_vulnerabilities",
      description =
          """
          Application-scoped vulnerability search with all filters plus session-based filtering.

          Required: appId parameter. Use search_applications tool to find application IDs by name.

          Supports all standard filters (severity, status, environment, dates, tags) PLUS:
          - Session metadata filtering: sessionMetadataName, sessionMetadataValue
          - Latest session filtering: useLatestSession=true

          Note: If useLatestSession=true and no sessions exist, returns all vulnerabilities
          for the application with a warning message.

          Common usage examples:
          - All vulns in app: appId="abc123"
          - Latest session vulns: appId="abc123", useLatestSession=true
          - Session metadata: appId="abc123", sessionMetadataName="branch", sessionMetadataValue="main"
          - Production critical: appId="abc123", severities="CRITICAL", environments="PRODUCTION"
          """)
  public PaginatedResponse<VulnLight> searchAppVulnerabilities(
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
      @ToolParam(description = "Session metadata field name for filtering", required = false)
          String sessionMetadataName,
      @ToolParam(description = "Session metadata field value for filtering", required = false)
          String sessionMetadataValue,
      @ToolParam(description = "Filter to latest session only", required = false)
          Boolean useLatestSession)
      throws IOException {
    log.info(
        "Searching app vulnerabilities - appId: {}, page: {}, pageSize: {}, filters:"
            + " severities={}, statuses={}, vulnTypes={}, environments={}, lastSeenAfter={},"
            + " lastSeenBefore={}, vulnTags={}, sessionMetadataName={}, sessionMetadataValue={},"
            + " useLatestSession={}",
        appId,
        page,
        pageSize,
        severities,
        statuses,
        vulnTypes,
        environments,
        lastSeenAfter,
        lastSeenBefore,
        vulnTags,
        sessionMetadataName,
        sessionMetadataValue,
        useLatestSession);
    long startTime = System.currentTimeMillis();

    // Validate appId is required
    if (!StringUtils.hasText(appId)) {
      var errorMessage = "appId parameter is required";
      log.error("Validation error: {}", errorMessage);
      return PaginatedResponse.error(1, 50, errorMessage);
    }

    // Validate conflicting parameters: useLatestSession + sessionMetadataName
    if (Boolean.TRUE.equals(useLatestSession) && StringUtils.hasText(sessionMetadataName)) {
      var errorMessage =
          "Cannot use both useLatestSession=true and sessionMetadataName. Choose one session"
              + " filtering strategy.";
      log.error("Validation error: {}", errorMessage);
      return PaginatedResponse.error(1, 50, errorMessage);
    }

    // Validate incomplete parameters: sessionMetadataValue without sessionMetadataName
    if (StringUtils.hasText(sessionMetadataValue) && !StringUtils.hasText(sessionMetadataName)) {
      var errorMessage =
          "sessionMetadataValue requires sessionMetadataName. Both must be provided together.";
      log.error("Validation error: {}", errorMessage);
      return PaginatedResponse.error(1, 50, errorMessage);
    }

    // Parse and validate inputs
    var pagination = PaginationParams.of(page, pageSize);
    var filters =
        VulnerabilityFilterParams.of(
            severities,
            statuses,
            appId, // Pass appId to filters for consistency
            vulnTypes,
            environments,
            lastSeenAfter,
            lastSeenBefore,
            vulnTags);

    // Check for hard failures - return error immediately if invalid
    if (!filters.isValid()) {
      var errorMessage = String.join(" ", filters.errors());
      log.error("Validation errors: {}", errorMessage);
      return PaginatedResponse.error(pagination.page(), pagination.pageSize(), errorMessage);
    }

    var contrastSDK =
        SDKHelper.getSDK(hostName, apiKey, serviceKey, userName, httpProxyHost, httpProxyPort);
    var allWarnings = new ArrayList<String>();
    allWarnings.addAll(filters.warnings());

    try {
      // Build TraceFilterBody from TraceFilterForm
      var filterForm = filters.toTraceFilterForm();
      var filterBody = new TraceFilterBody();

      // Copy filters from TraceFilterForm to TraceFilterBody
      if (filterForm.getSeverities() != null && !filterForm.getSeverities().isEmpty()) {
        filterBody.setSeverities(new ArrayList<>(filterForm.getSeverities()));
      }
      if (filterForm.getEnvironments() != null && !filterForm.getEnvironments().isEmpty()) {
        filterBody.setEnvironments(new ArrayList<>(filterForm.getEnvironments()));
      }
      if (filterForm.getVulnTypes() != null && !filterForm.getVulnTypes().isEmpty()) {
        filterBody.setVulnTypes(filterForm.getVulnTypes());
      }
      if (filterForm.getStartDate() != null) {
        filterBody.setStartDate(filterForm.getStartDate());
      }
      if (filterForm.getEndDate() != null) {
        filterBody.setEndDate(filterForm.getEndDate());
      }
      if (filterForm.getFilterTags() != null && !filterForm.getFilterTags().isEmpty()) {
        filterBody.setFilterTags(filterForm.getFilterTags());
      }

      // Handle useLatestSession logic
      if (Boolean.TRUE.equals(useLatestSession)) {
        var extension = new SDKExtension(contrastSDK);
        var latestSession = extension.getLatestSessionMetadata(orgID, appId);

        if (latestSession != null
            && latestSession.getAgentSession() != null
            && latestSession.getAgentSession().getAgentSessionId() != null) {
          filterBody.setAgentSessionId(latestSession.getAgentSession().getAgentSessionId());
          log.debug(
              "Using latest session ID: {}", latestSession.getAgentSession().getAgentSessionId());
        } else {
          // No session found - add warning and continue with all vulnerabilities
          allWarnings.add(
              "No sessions found for this application. Returning all vulnerabilities across all"
                  + " sessions for this application.");
          log.warn("No sessions found for application: {}", appId);
        }
      }

      // Make API call - always use app-specific endpoint
      // Note: When using session metadata filtering, we get all results (no SDK pagination)
      // because we need to filter in-memory before paginating
      Traces traces;
      if (StringUtils.hasText(sessionMetadataName)) {
        // Get all results for in-memory filtering (no SDK pagination)
        traces =
            contrastSDK.getTraces(
                orgID,
                appId,
                filterBody,
                EnumSet.of(
                    TraceFilterForm.TraceExpandValue.SESSION_METADATA,
                    TraceFilterForm.TraceExpandValue.SERVER_ENVIRONMENTS,
                    TraceFilterForm.TraceExpandValue.APPLICATION));
      } else {
        // Use SDK pagination when no in-memory filtering needed
        filterForm.setLimit(pagination.limit());
        filterForm.setOffset(pagination.offset());
        filterForm.setExpand(
            EnumSet.of(
                TraceFilterForm.TraceExpandValue.SESSION_METADATA,
                TraceFilterForm.TraceExpandValue.SERVER_ENVIRONMENTS,
                TraceFilterForm.TraceExpandValue.APPLICATION));
        traces = contrastSDK.getTraces(orgID, appId, filterForm);
      }

      if (traces == null || traces.getTraces() == null) {
        var errorMsg =
            String.format(
                "App-level vulnerability API returned null for app %s. Please check API"
                    + " connectivity and permissions.",
                appId);
        log.error(errorMsg);
        return PaginatedResponse.error(pagination.page(), pagination.pageSize(), errorMsg);
      }

      // Convert to VulnLight
      var vulnerabilities =
          traces.getTraces().stream().map(vulnerabilityMapper::toVulnLight).toList();

      // Apply in-memory session metadata filtering if requested
      List<VulnLight> finalVulns = vulnerabilities;
      if (StringUtils.hasText(sessionMetadataName)) {
        var filteredVulns = new ArrayList<VulnLight>();
        for (VulnLight vuln : vulnerabilities) {
          if (vuln.sessionMetadata() != null) {
            for (SessionMetadata sm : vuln.sessionMetadata()) {
              for (MetadataItem metadataItem : sm.getMetadata()) {
                if (metadataItem.getDisplayLabel().equalsIgnoreCase(sessionMetadataName)
                    && metadataItem.getValue().equalsIgnoreCase(sessionMetadataValue)) {
                  filteredVulns.add(vuln);
                  log.debug(
                      "Found matching vulnerability with ID: {} for session metadata {}={}",
                      vuln.vulnID(),
                      sessionMetadataName,
                      sessionMetadataValue);
                  break;
                }
              }
            }
          }
        }

        // Apply pagination to filtered results
        var startIndex = pagination.offset();
        var endIndex = Math.min(startIndex + pagination.pageSize(), filteredVulns.size());
        finalVulns =
            (startIndex < filteredVulns.size())
                ? filteredVulns.subList(startIndex, endIndex)
                : List.<VulnLight>of();

        // Use filtered count as totalItems for paginated response
        var response =
            paginationHandler.createPaginatedResponse(
                finalVulns, pagination, filteredVulns.size(), allWarnings);

        long duration = System.currentTimeMillis() - startTime;
        log.info(
            "Retrieved {} vulnerabilities for app {} page {} after filtering (pageSize: {},"
                + " totalFiltered: {}, took {} ms)",
            response.items().size(),
            appId,
            response.page(),
            response.pageSize(),
            filteredVulns.size(),
            duration);

        return response;
      }

      // No session metadata filtering - use SDK results directly with SDK pagination
      var totalItems = traces.getCount();
      var response =
          paginationHandler.createPaginatedResponse(
              finalVulns, pagination, totalItems, allWarnings);

      long duration = System.currentTimeMillis() - startTime;
      log.info(
          "Retrieved {} vulnerabilities for app {} page {} (pageSize: {}, totalItems: {}, took {}"
              + " ms)",
          response.items().size(),
          appId,
          response.page(),
          response.pageSize(),
          response.totalItems(),
          duration);

      return response;

    } catch (Exception e) {
      log.error("Error searching app vulnerabilities for appId: {}", appId, e);
      throw new IOException("Failed to search app vulnerabilities: " + e.getMessage(), e);
    }
  }

  private List<Metadata> getMetadataFromApp(Application app) {
    var metadata = new ArrayList<Metadata>();
    app.getMetadataEntities().stream()
        .map(m -> new Metadata(m.getName(), m.getValue()))
        .forEach(metadata::add);
    return metadata;
  }

  @Tool(
      name = "list_vulnerability_types",
      description =
          """
          Returns the complete list of vulnerability types (rule names) available in Contrast.

          Use this tool to discover all possible values for the vulnTypes filter parameter
          when calling list_all_vulnerabilities or other vulnerability filtering tools.

          Returns rule names like: sql-injection, xss-reflected, path-traversal, cmd-injection, etc.

          These rule names can be used with the vulnTypes parameter to filter vulnerabilities
          by specific vulnerability classes.

          Note: The list is fetched dynamically from Contrast and reflects the rules
          configured for your organization, so it's always current.
          """)
  public List<String> listVulnerabilityTypes() throws IOException {
    log.info("Retrieving all vulnerability types (rule names) for organization: {}", orgID);
    var contrastSDK =
        SDKHelper.getSDK(hostName, apiKey, serviceKey, userName, httpProxyHost, httpProxyPort);

    try {
      var rules = contrastSDK.getRules(orgID);

      if (rules == null || rules.getRules() == null) {
        log.warn("No rules returned from Contrast API");
        return new ArrayList<>();
      }

      // Extract rule names, trim whitespace, filter out null/empty, and sort alphabetically
      var ruleNames =
          rules.getRules().stream()
              .map(Rules.Rule::getName)
              .filter(name -> name != null && !name.trim().isEmpty())
              .map(String::trim)
              .sorted()
              .toList();

      log.info("Retrieved {} vulnerability types", ruleNames.size());
      return ruleNames;

    } catch (Exception e) {
      log.error("Error retrieving vulnerability types", e);
      throw new IOException("Failed to retrieve vulnerability types: " + e.getMessage(), e);
    }
  }
}
