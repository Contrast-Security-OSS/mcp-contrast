package com.contrast.labs.ai.mcp.contrast;

import com.contrast.labs.ai.mcp.contrast.sdkextension.SDKExtension;
import com.contrast.labs.ai.mcp.contrast.sdkextension.SDKHelper;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.routecoverage.Route;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.routecoverage.RouteCoverageBySessionIDAndMetadataRequestExtended;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.routecoverage.RouteCoverageResponse;
import com.contrastsecurity.models.RouteCoverageMetadataLabelValues;
import java.io.IOException;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class RouteCoverageService {
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

  /**
   * Retrieves route coverage data for an application with optional filtering.
   *
   * <p>Routes can have two statuses: - DISCOVERED: Found by Contrast Assess but has not received
   * any HTTP requests - EXERCISED: Has received at least one HTTP request
   *
   * @param appId Required - The application ID to retrieve route coverage for
   * @param sessionMetadataName Optional - Filter by session metadata field name (e.g., "branch").
   *     Must be provided with sessionMetadataValue. Empty strings are treated as null (no filter).
   * @param sessionMetadataValue Optional - Filter by session metadata field value (e.g., "main").
   *     Must be provided with sessionMetadataName. Empty strings are treated as null.
   * @param useLatestSession Optional - If true, only return routes from the latest session
   * @return RouteCoverageResponse containing route coverage data with details for each route
   * @throws IOException If an error occurs while retrieving data from Contrast
   * @throws IllegalArgumentException If sessionMetadataName is provided without
   *     sessionMetadataValue, or if sessionMetadataValue is provided without sessionMetadataName
   */
  @Tool(
      name = "get_route_coverage",
      description =
          "Retrieves route coverage data for an application. Routes can be DISCOVERED (found but"
              + " not exercised) or EXERCISED (received HTTP traffic). All filter parameters are"
              + " truly optional - if none provided (null or empty strings), returns all routes"
              + " across all sessions. Parameters: appId (required), sessionMetadataName"
              + " (optional), sessionMetadataValue (optional), useLatestSession (optional). NOTE:"
              + " sessionMetadataName and sessionMetadataValue must be provided together or both"
              + " omitted. IMPORTANT: useLatestSession and sessionMetadataName/Value are mutually"
              + " exclusive - if both are provided, useLatestSession takes precedence and the"
              + " session metadata filter is ignored.")
  public RouteCoverageResponse getRouteCoverage(
      String appId,
      String sessionMetadataName,
      String sessionMetadataValue,
      Boolean useLatestSession)
      throws IOException {

    // Validate appId is required
    if (!StringUtils.hasText(appId)) {
      log.error("appId parameter is required and cannot be empty");
      var errorResponse = new RouteCoverageResponse();
      errorResponse.setSuccess(false);
      return errorResponse;
    }

    log.info("Retrieving route coverage for application ID: {}", appId);

    // Validate parameters - treat empty strings as null
    if (StringUtils.hasText(sessionMetadataName) && !StringUtils.hasText(sessionMetadataValue)) {
      var errorMsg = "sessionMetadataValue is required when sessionMetadataName is provided";
      log.error(errorMsg);
      throw new IllegalArgumentException(errorMsg);
    }

    // Validate sessionMetadataValue requires sessionMetadataName
    if (StringUtils.hasText(sessionMetadataValue) && !StringUtils.hasText(sessionMetadataName)) {
      var errorMsg = "sessionMetadataName is required when sessionMetadataValue is provided";
      log.error(errorMsg);
      throw new IllegalArgumentException(errorMsg);
    }

    // Warn if mutually exclusive parameters are both provided
    if (Boolean.TRUE.equals(useLatestSession) && StringUtils.hasText(sessionMetadataName)) {
      log.atWarn()
          .setMessage(
              "Both useLatestSession and sessionMetadataName provided - these are mutually"
                  + " exclusive. Using useLatestSession and ignoring sessionMetadataName/Value.")
          .addKeyValue("sessionMetadataName", sessionMetadataName)
          .addKeyValue("sessionMetadataValue", sessionMetadataValue)
          .log();
    }

    // Initialize SDK
    var contrastSDK =
        SDKHelper.getSDK(hostName, apiKey, serviceKey, userName, httpProxyHost, httpProxyPort);
    var sdkExtension = new SDKExtension(contrastSDK);

    // Build request based on parameters
    RouteCoverageBySessionIDAndMetadataRequestExtended requestExtended = null;

    if (useLatestSession != null && useLatestSession) {
      // Filter by latest session
      log.debug("Fetching latest session metadata for application ID: {}", appId);
      var latest = sdkExtension.getLatestSessionMetadata(orgID, appId);

      if (latest == null) {
        log.error("No session metadata found for application ID: {}", appId);
        var noRouteCoverageResponse = new RouteCoverageResponse();
        noRouteCoverageResponse.setSuccess(false);
        return noRouteCoverageResponse;
      }

      if (latest.getAgentSession() == null) {
        log.error("No agent session found for application ID: {}", appId);
        var noRouteCoverageResponse = new RouteCoverageResponse();
        noRouteCoverageResponse.setSuccess(false);
        return noRouteCoverageResponse;
      }

      requestExtended = new RouteCoverageBySessionIDAndMetadataRequestExtended();
      requestExtended.setSessionId(latest.getAgentSession().getAgentSessionId());
      log.debug("Using latest session ID: {}", latest.getAgentSession().getAgentSessionId());

    } else if (StringUtils.hasText(sessionMetadataName)) {
      // Filter by session metadata
      log.debug("Filtering by session metadata: {}={}", sessionMetadataName, sessionMetadataValue);
      requestExtended = new RouteCoverageBySessionIDAndMetadataRequestExtended();
      var metadataLabelValue = new RouteCoverageMetadataLabelValues();
      metadataLabelValue.setLabel(sessionMetadataName);
      metadataLabelValue.getValues().add(sessionMetadataValue);
      requestExtended.getValues().add(metadataLabelValue);
    } else {
      log.debug("No filters applied - retrieving all route coverage");
    }

    // Call SDK to get route coverage
    log.debug("Fetching route coverage data for application ID: {}", appId);
    var response = sdkExtension.getRouteCoverage(orgID, appId, requestExtended);

    // Defensive null checks - API may return null on errors or permission issues
    if (response == null) {
      log.error("Route coverage API returned null for app {}", appId);
      var errorResponse = new RouteCoverageResponse();
      errorResponse.setSuccess(false);
      return errorResponse;
    }

    if (response.getRoutes() == null) {
      log.warn("No routes returned for app {} - returning empty response", appId);
      response.setRoutes(Collections.emptyList());
    }

    log.debug("Found {} routes for application", response.getRoutes().size());

    // Fetch route details for each route
    log.debug("Retrieving route details for each route");
    for (Route route : response.getRoutes()) {
      log.trace("Fetching details for route: {}", route.getSignature());
      var routeDetailsResponse = sdkExtension.getRouteDetails(orgID, appId, route.getRouteHash());
      route.setRouteDetailsResponse(routeDetailsResponse);
    }

    log.info(
        "Successfully retrieved route coverage for application ID: {} ({} routes)",
        appId,
        response.getRoutes().size());
    return response;
  }
}
