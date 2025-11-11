package com.contrast.labs.ai.mcp.contrast;

import com.contrast.labs.ai.mcp.contrast.sdkexstension.SDKExtension;
import com.contrast.labs.ai.mcp.contrast.sdkexstension.SDKHelper;
import com.contrast.labs.ai.mcp.contrast.sdkexstension.data.routecoverage.Route;
import com.contrast.labs.ai.mcp.contrast.sdkexstension.data.routecoverage.RouteCoverageBySessionIDAndMetadataRequestExtended;
import com.contrast.labs.ai.mcp.contrast.sdkexstension.data.routecoverage.RouteCoverageResponse;
import com.contrast.labs.ai.mcp.contrast.sdkexstension.data.routecoverage.RouteDetailsResponse;
import com.contrast.labs.ai.mcp.contrast.sdkexstension.data.sessionmetadata.SessionMetadataResponse;
import com.contrastsecurity.models.RouteCoverageMetadataLabelValues;
import com.contrastsecurity.sdk.ContrastSDK;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class RouteCoverageService {

    private static final Logger logger = LoggerFactory.getLogger(RouteCoverageService.class);

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
     * Routes can have two statuses:
     * - DISCOVERED: Found by Contrast Assess but has not received any HTTP requests
     * - EXERCISED: Has received at least one HTTP request
     *
     * @param appId Required - The application ID to retrieve route coverage for
     * @param sessionMetadataName Optional - Filter by session metadata field name (e.g., "branch")
     * @param sessionMetadataValue Optional - Filter by session metadata field value (e.g., "main").
     *                             Required if sessionMetadataName is provided.
     * @param useLatestSession Optional - If true, only return routes from the latest session
     * @return RouteCoverageResponse containing route coverage data with details for each route
     * @throws IOException If an error occurs while retrieving data from Contrast
     * @throws IllegalArgumentException If sessionMetadataName is provided without sessionMetadataValue
     */
    @Tool(name = "get_route_coverage",
          description = "Retrieves route coverage data for an application. Routes can be DISCOVERED (found but not exercised) " +
                  "or EXERCISED (received HTTP traffic). Supports optional filtering by session metadata name/value or latest session. " +
                  "Parameters: appId (required), sessionMetadataName (optional), sessionMetadataValue (optional - required if " +
                  "sessionMetadataName provided), useLatestSession (optional).")
    public RouteCoverageResponse getRouteCoverage(
            String appId,
            String sessionMetadataName,
            String sessionMetadataValue,
            Boolean useLatestSession) throws IOException {

        logger.info("Retrieving route coverage for application ID: {}", appId);

        // Validate parameters
        if (sessionMetadataName != null && sessionMetadataValue == null) {
            String errorMsg = "sessionMetadataValue is required when sessionMetadataName is provided";
            logger.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        // Initialize SDK
        ContrastSDK contrastSDK = SDKHelper.getSDK(hostName, apiKey, serviceKey, userName, httpProxyHost, httpProxyPort);
        SDKExtension sdkExtension = new SDKExtension(contrastSDK);

        // Build request based on parameters
        RouteCoverageBySessionIDAndMetadataRequestExtended requestExtended = null;

        if (useLatestSession != null && useLatestSession) {
            // Filter by latest session
            logger.debug("Fetching latest session metadata for application ID: {}", appId);
            SessionMetadataResponse latest = sdkExtension.getLatestSessionMetadata(orgID, appId);

            if (latest == null || latest.getAgentSession() == null) {
                logger.error("No session metadata found for application ID: {}", appId);
                RouteCoverageResponse noRouteCoverageResponse = new RouteCoverageResponse();
                noRouteCoverageResponse.setSuccess(false);
                logger.debug("No Agent session found in latest session metadata response for application ID: {}", appId);
                return noRouteCoverageResponse;
            }

            requestExtended = new RouteCoverageBySessionIDAndMetadataRequestExtended();
            requestExtended.setSessionId(latest.getAgentSession().getAgentSessionId());
            logger.debug("Using latest session ID: {}", latest.getAgentSession().getAgentSessionId());

        } else if (sessionMetadataName != null) {
            // Filter by session metadata
            logger.debug("Filtering by session metadata: {}={}", sessionMetadataName, sessionMetadataValue);
            requestExtended = new RouteCoverageBySessionIDAndMetadataRequestExtended();
            RouteCoverageMetadataLabelValues metadataLabelValue = new RouteCoverageMetadataLabelValues();
            metadataLabelValue.setLabel(sessionMetadataName);
            metadataLabelValue.getValues().add(sessionMetadataValue);
            requestExtended.getValues().add(metadataLabelValue);
        } else {
            logger.debug("No filters applied - retrieving all route coverage");
        }

        // Call SDK to get route coverage
        logger.debug("Fetching route coverage data for application ID: {}", appId);
        RouteCoverageResponse response = sdkExtension.getRouteCoverage(orgID, appId, requestExtended);
        logger.debug("Found {} routes for application", response.getRoutes().size());

        // Fetch route details for each route
        logger.debug("Retrieving route details for each route");
        for (Route route : response.getRoutes()) {
            logger.trace("Fetching details for route: {}", route.getSignature());
            RouteDetailsResponse routeDetailsResponse = sdkExtension.getRouteDetails(orgID, appId, route.getRouteHash());
            route.setRouteDetailsResponse(routeDetailsResponse);
        }

        logger.info("Successfully retrieved route coverage for application ID: {} ({} routes)", appId, response.getRoutes().size());
        return response;
    }
}
