package com.contrast.labs.ai.mcp.contrast;

import com.contrast.labs.ai.mcp.contrast.sdkexstension.SDKExtension;
import com.contrast.labs.ai.mcp.contrast.sdkexstension.SDKHelper;
import com.contrast.labs.ai.mcp.contrast.sdkexstension.data.application.Application;
import com.contrast.labs.ai.mcp.contrast.sdkexstension.data.routecoverage.Route;
import com.contrast.labs.ai.mcp.contrast.sdkexstension.data.routecoverage.RouteCoverageBySessionIDAndMetadataRequestExtended;
import com.contrast.labs.ai.mcp.contrast.sdkexstension.data.routecoverage.RouteCoverageResponse;
import com.contrast.labs.ai.mcp.contrast.sdkexstension.data.routecoverage.RouteDetailsResponse;
import com.contrast.labs.ai.mcp.contrast.sdkexstension.data.sessionmetadata.SessionMetadataResponse;
import com.contrastsecurity.models.RouteCoverageBySessionIDAndMetadataRequest;
import com.contrastsecurity.models.RouteCoverageMetadataLabelValues;
import com.contrastsecurity.sdk.ContrastSDK;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Optional;

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



    @Tool(name = "get_application_route_coverage", description = "takes a application name and return the route coverage data for that application. " +
            "If a route/endpoint is DISCOVERED, it means it has been found by Assess but that route has had no inbound http requests. If it is EXERCISED, it means it has had atleast one inbound http request to that route/endpoint.")
    public RouteCoverageResponse getRouteCoverage(String app_name) throws IOException {
        logger.info("Retrieving route coverage for application by name: {}", app_name);
        ContrastSDK contrastSDK = SDKHelper.getSDK(hostName, apiKey, serviceKey, userName,httpProxyHost, httpProxyPort);
        SDKExtension sdkExtension = new SDKExtension(contrastSDK);
        logger.debug("Searching for application ID matching name: {}", app_name);

        Optional<Application> application = SDKHelper.getApplicationByName(app_name, orgID, contrastSDK);

        if (!application.isPresent()) {
            logger.error("Application not found: {}", app_name);
            throw new IOException("Application not found: " + app_name);
        }

        logger.debug("Fetching route coverage data for application ID: {}", application.get().getAppId());
        RouteCoverageResponse response = sdkExtension.getRouteCoverage(orgID, application.get().getAppId(), null);
        logger.debug("Found {} routes for application", response.getRoutes().size());

        logger.debug("Retrieving route details for each route");
        for(Route route : response.getRoutes()) {
            logger.trace("Fetching details for route: {}", route.getSignature());
            RouteDetailsResponse routeDetailsResponse = sdkExtension.getRouteDetails(orgID, application.get().getAppId(), route.getRouteHash());
            route.setRouteDetailsResponse(routeDetailsResponse);
        }

        logger.info("Successfully retrieved route coverage for application: {}", app_name);
        return response;
    }

    @Tool(name = "get_application_route_coverage_by_app_id", description = "takes a application id and return the route coverage data for that application. " +
            "If a route/endpoint is DISCOVERED, it means it has been found by Assess but that route has had no inbound http requests. If it is EXERCISED, it means it has had atleast one inbound http request to that route/endpoint.")
    public RouteCoverageResponse getRouteCoverageByAppID(String app_id) throws IOException {
        logger.info("Retrieving route coverage for application by ID: {}", app_id);
        ContrastSDK contrastSDK = SDKHelper.getSDK(hostName, apiKey, serviceKey, userName,httpProxyHost, httpProxyPort);
        SDKExtension sdkExtension = new SDKExtension(contrastSDK);

        logger.debug("Fetching route coverage data for application ID: {}", app_id);
        RouteCoverageResponse response = sdkExtension.getRouteCoverage(orgID, app_id, null);
        logger.debug("Found {} routes for application", response.getRoutes().size());

        logger.debug("Retrieving route details for each route");
        for(Route route : response.getRoutes()) {
            logger.trace("Fetching details for route: {}", route.getSignature());
            RouteDetailsResponse routeDetailsResponse = sdkExtension.getRouteDetails(orgID, app_id, route.getRouteHash());
            route.setRouteDetailsResponse(routeDetailsResponse);
        }

        logger.info("Successfully retrieved route coverage for application ID: {}", app_id);
        return response;
    }

    @Tool(name = "get_application_route_coverage_by_app_name_and_session_metadata", description = "takes a application name and return the route coverage data for that application for the specified session metadata name and value. " +
            "If a route/endpoint is DISCOVERED, it means it has been found by Assess but that route has had no inbound http requests. If it is EXERCISED, it means it has had at least one inbound http request to that route/endpoint.")
    public RouteCoverageResponse getRouteCoverageByAppNameAndSessionMetadata(String app_name, String session_Metadata_Name, String session_Metadata_Value) throws IOException {
        logger.info("Retrieving route coverage for application by Name: {}", app_name);
        ContrastSDK contrastSDK = SDKHelper.getSDK(hostName, apiKey, serviceKey, userName,httpProxyHost, httpProxyPort);
        logger.debug("Searching for application ID matching name: {}", app_name);

        Optional<Application> application = SDKHelper.getApplicationByName(app_name, orgID, contrastSDK);
        if (!application.isPresent()) {
            logger.error("Application not found: {}", app_name);
            throw new IOException("Application not found: " + app_name);
        }
        return getRouteCoverageByAppIDAndSessionMetadata(application.get().getAppId(), session_Metadata_Name, session_Metadata_Value);
    }

    @Tool(name = "get_application_route_coverage_by_app_id_and_session_metadata", description = "takes a application id and return the route coverage data for that application for the specified session metadata name and value. " +
            "If a route/endpoint is DISCOVERED, it means it has been found by Assess but that route has had no inbound http requests. If it is EXERCISED, it means it has had at least one inbound http request to that route/endpoint.")
    public RouteCoverageResponse getRouteCoverageByAppIDAndSessionMetadata(String app_id, String session_Metadata_Name, String session_Metadata_Value) throws IOException {
        logger.info("Retrieving route coverage for application by ID: {}", app_id);
        ContrastSDK contrastSDK = SDKHelper.getSDK(hostName, apiKey, serviceKey, userName,httpProxyHost, httpProxyPort);
        SDKExtension sdkExtension = new SDKExtension(contrastSDK);
        RouteCoverageBySessionIDAndMetadataRequestExtended requestExtended = new RouteCoverageBySessionIDAndMetadataRequestExtended();
        RouteCoverageMetadataLabelValues metadataLabelValue = new RouteCoverageMetadataLabelValues();
        metadataLabelValue.setLabel(session_Metadata_Name);
        metadataLabelValue.getValues().add(String.valueOf(session_Metadata_Value));
        requestExtended.getValues().add(metadataLabelValue);
        logger.debug("Fetching route coverage data for application ID: {}", app_id);
        RouteCoverageResponse response = sdkExtension.getRouteCoverage(orgID, app_id, requestExtended);
        logger.debug("Found {} routes for application", response.getRoutes().size());

        logger.debug("Retrieving route details for each route");
        for(Route route : response.getRoutes()) {
            logger.trace("Fetching details for route: {}", route.getSignature());
            RouteDetailsResponse routeDetailsResponse = sdkExtension.getRouteDetails(orgID, app_id, route.getRouteHash());
            route.setRouteDetailsResponse(routeDetailsResponse);
        }

        logger.info("Successfully retrieved route coverage for application ID: {}", app_id);
        return response;
    }

    @Tool(name = "get_application_route_coverage_by_app_name_latest_session", description = "takes a application name and return the route coverage data for that application from the latest session. " +
            "If a route/endpoint is DISCOVERED, it means it has been found by Assess but that route has had no inbound http requests. If it is EXERCISED, it means it has had atleast one inbound http request to that route/endpoint.")
    public RouteCoverageResponse getRouteCoverageByAppNameLatestSession(String app_name) throws IOException {
        logger.info("Retrieving route coverage for application by Name: {}", app_name);
        ContrastSDK contrastSDK = SDKHelper.getSDK(hostName, apiKey, serviceKey, userName, httpProxyHost, httpProxyPort);
        Optional<Application> application = SDKHelper.getApplicationByName(app_name, orgID, contrastSDK);
        if (application.isEmpty()) {
            logger.error("Application not found: {}", app_name);
            throw new IOException("Application not found: " + app_name);
        }
        return getRouteCoverageByAppIDLatestSession(application.get().getAppId());
    }


    @Tool(name = "get_application_route_coverage_by_app_id_latest_session", description = "takes a application id and return the route coverage data for that application from the latest session. " +
            "If a route/endpoint is DISCOVERED, it means it has been found by Assess but that route has had no inbound http requests. If it is EXERCISED, it means it has had atleast one inbound http request to that route/endpoint.")
    public RouteCoverageResponse getRouteCoverageByAppIDLatestSession(String app_id) throws IOException {
        logger.info("Retrieving route coverage for application by ID: {}", app_id);
        ContrastSDK contrastSDK = SDKHelper.getSDK(hostName, apiKey, serviceKey, userName,httpProxyHost, httpProxyPort);
        SDKExtension sdkExtension = new SDKExtension(contrastSDK);
        SDKExtension extension = new SDKExtension(contrastSDK);
        SessionMetadataResponse latest = extension.getLatestSessionMetadata(orgID,app_id);
        if (latest == null || latest.getAgentSession() == null) {
            logger.error("No session metadata found for application ID: {}", app_id);
            RouteCoverageResponse noRouteCoverageResponse = new RouteCoverageResponse();
            noRouteCoverageResponse.setSuccess(Boolean.FALSE);
            logger.debug("No Agent session found in latest session metadata response for application ID: {}", app_id);
            return noRouteCoverageResponse; // Return empty response if no session metadata found
        }
        RouteCoverageBySessionIDAndMetadataRequestExtended requestExtended = new RouteCoverageBySessionIDAndMetadataRequestExtended();
        requestExtended.setSessionId(latest.getAgentSession().getAgentSessionId());
        logger.debug("Fetching route coverage data for application ID: {}", app_id);
        RouteCoverageResponse response = sdkExtension.getRouteCoverage(orgID, app_id, requestExtended);
        logger.debug("Found {} routes for application", response.getRoutes().size());

        logger.debug("Retrieving route details for each route");
        for(Route route : response.getRoutes()) {
            logger.trace("Fetching details for route: {}", route.getSignature());
            RouteDetailsResponse routeDetailsResponse = sdkExtension.getRouteDetails(orgID, app_id, route.getRouteHash());
            route.setRouteDetailsResponse(routeDetailsResponse);
        }

        logger.info("Successfully retrieved route coverage for application ID: {}", app_id);
        return response;
    }






}
