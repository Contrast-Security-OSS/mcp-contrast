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
import com.contrast.labs.ai.mcp.contrast.data.Vulnerability;
import com.contrast.labs.ai.mcp.contrast.data.VulnLight;
import com.contrast.labs.ai.mcp.contrast.hints.HintGenerator;
import com.contrast.labs.ai.mcp.contrast.mapper.VulnerabilityContext;
import com.contrast.labs.ai.mcp.contrast.mapper.VulnerabilityMapper;
import com.contrast.labs.ai.mcp.contrast.sdkexstension.SDKExtension;
import com.contrast.labs.ai.mcp.contrast.utils.PaginationHandler;
import com.contrast.labs.ai.mcp.contrast.sdkexstension.SDKHelper;
import com.contrast.labs.ai.mcp.contrast.sdkexstension.data.LibraryExtended;
import com.contrast.labs.ai.mcp.contrast.sdkexstension.data.application.Application;
import com.contrast.labs.ai.mcp.contrast.sdkexstension.data.sca.LibraryObservation;
import com.contrast.labs.ai.mcp.contrast.sdkexstension.data.sessionmetadata.SessionMetadataResponse;
import com.contrastsecurity.models.EventResource;
import com.contrastsecurity.models.EventSummaryResponse;
import com.contrastsecurity.models.HttpRequestResponse;
import com.contrastsecurity.models.MetadataFilterResponse;
import com.contrastsecurity.models.MetadataItem;
import com.contrastsecurity.models.RecommendationResponse;
import com.contrastsecurity.models.Rules;
import com.contrastsecurity.models.SessionMetadata;
import com.contrastsecurity.models.Stacktrace;
import com.contrastsecurity.models.Trace;
import com.contrastsecurity.models.TraceFilterBody;
import com.contrastsecurity.models.Traces;
import com.contrastsecurity.http.TraceFilterForm;
import com.contrastsecurity.http.RuleSeverity;
import com.contrastsecurity.http.ServerEnvironment;
import com.contrastsecurity.sdk.ContrastSDK;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AssessService {

    private static final Logger logger = LoggerFactory.getLogger(AssessService.class);

    private final VulnerabilityMapper vulnerabilityMapper;
    private final PaginationHandler paginationHandler;

    public AssessService(VulnerabilityMapper vulnerabilityMapper, PaginationHandler paginationHandler) {
        this.vulnerabilityMapper = vulnerabilityMapper;
        this.paginationHandler = paginationHandler;
    }
    

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



    @Tool(name = "get_vulnerability_by_id", description = "takes a vulnerability ID ( vulnID ) and Application ID ( appID ) and returns details about the specific security vulnerability. If based on the stacktrace, the vulnerability looks like it is in code that is not in the codebase, the vulnerability may be in a 3rd party library, review the CVE data attached to that stackframe you believe the vulnerability exists in and if possible upgrade that library to the next non vulnerable version based on the remediation guidance.")
    public Vulnerability getVulnerabilityById(
            @ToolParam(description = "Vulnerability ID (UUID format)") String vulnID,
            @ToolParam(description = "Application ID") String appID) throws IOException {
        logger.info("Retrieving vulnerability details for vulnID: {} in application ID: {}", vulnID, appID);
        ContrastSDK contrastSDK = SDKHelper.getSDK(hostName, apiKey, serviceKey, userName,httpProxyHost, httpProxyPort);
        logger.debug("ContrastSDK initialized with host: {}", hostName);
        
        try {
            Trace trace = contrastSDK.getTraces(orgID, appID, new TraceFilterBody()).getTraces().stream()
                    .filter(t -> t.getUuid().equalsIgnoreCase(vulnID))
                    .findFirst()
                    .orElseThrow();
            logger.debug("Found trace with title: {} and rule: {}", trace.getTitle(), trace.getRule());
            
            RecommendationResponse recommendationResponse = contrastSDK.getRecommendation(orgID, vulnID);
            HttpRequestResponse requestResponse = contrastSDK.getHttpRequest(orgID, vulnID);
            EventSummaryResponse eventSummaryResponse = contrastSDK.getEventSummary(orgID, vulnID);
            
            Optional<EventResource> triggerEvent = eventSummaryResponse.getEvents().stream()
                    .filter(e -> e.getType().equalsIgnoreCase("trigger"))
                    .findFirst();
            
            List<String> stackTraces = new ArrayList<>();
            if (triggerEvent.isPresent()) {
                List<Stacktrace> sTrace = triggerEvent.get().getEvent().getStacktraces();
                if (sTrace != null) {
                    stackTraces.addAll(sTrace.stream().map(Stacktrace::getDescription).toList());
                    logger.debug("Found {} stack traces for vulnerability", stackTraces.size());
                }
            }
            List<LibraryExtended> libs = SDKHelper.getLibsForID(appID,orgID, new SDKExtension(contrastSDK));
            List<LibraryLibraryObservation> lobs = new ArrayList<>();
            for(LibraryExtended lib : libs) {
                LibraryLibraryObservation llob = new LibraryLibraryObservation(lib, SDKHelper.getLibraryObservationsWithCache(lib.getHash(), appID, orgID, 50,new SDKExtension(contrastSDK)));
                lobs.add(llob);
            }
            List<StackLib> stackLibs = new ArrayList<>();
            Set<LibraryExtended> libsToReturn = new HashSet<>();
            for(String stackTrace : stackTraces) {
                Optional<LibraryLibraryObservation> matchingLlobOpt = findMatchingLibraryData(stackTrace, lobs);
                if (matchingLlobOpt.isPresent()) {
                    LibraryLibraryObservation llob = matchingLlobOpt.get();
                    LibraryExtended library = llob.library();
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
            if( requestResponse.getHttpRequest()!=null) {
                httpRequestText =  requestResponse.getHttpRequest().getText();
            }

            VulnerabilityContext context = VulnerabilityContext.builder()
                .recommendation(recommendationResponse.getRecommendation().getText())
                .stackLibs(stackLibs)
                .libraries(new ArrayList<>(libsToReturn)) // Convert Set to List
                .httpRequest(httpRequestText)
                .build();

            logger.info("Successfully retrieved vulnerability details for vulnID: {}", vulnID);
            return vulnerabilityMapper.toFullVulnerability(trace, context);
        } catch (Exception e) {
            logger.error("Error retrieving vulnerability details for vulnID: {}", vulnID, e);
            throw new IOException("Failed to retrieve vulnerability details: " + e.getMessage(), e);
        }
    }

    private Optional<LibraryLibraryObservation> findMatchingLibraryData(String stackTrace, List<LibraryLibraryObservation> lobs) {
        String lowerStackTrace = stackTrace.toLowerCase();
        for (LibraryLibraryObservation llob : lobs) {
            for (LibraryObservation lob : llob.libraryObservation()) {
                if (lob.getName() != null && lowerStackTrace.startsWith(lob.getName().toLowerCase())) {
                    return Optional.of(llob);
                }
            }
        }
        return Optional.empty();
    }

    @Tool(name = "get_vulnerability", description = "Takes a vulnerability ID (vulnID) and application name (app_name) and returns details about the specific security vulnerability.  If based on the stacktrace, the vulnerability looks like it is in code that is not in the codebase, the vulnerability may be in a 3rd party library, review the CVE data attached to that stackframe you believe the vulnerability exists in and if possible upgrade that library to the next non vulnerable version based on the remediation guidance.")
    public Vulnerability getVulnerability(
            @ToolParam(description = "Vulnerability ID (UUID format)") String vulnID,
            @ToolParam(description = "Application name") String app_name) throws IOException {
        logger.info("Retrieving vulnerability details for vulnID: {} in application: {}", vulnID, app_name);
        ContrastSDK contrastSDK = SDKHelper.getSDK(hostName, apiKey, serviceKey, userName,httpProxyHost, httpProxyPort);
        logger.debug("Searching for application ID matching name: {}", app_name);

        Optional<Application> application = SDKHelper.getApplicationByName(app_name, orgID, contrastSDK);
        if(application.isPresent()) {
            return getVulnerabilityById(vulnID, application.get().getAppId());
        } else {
            logger.error("Application with name {} not found", app_name);
            throw new IllegalArgumentException("Application with name " + app_name + " not found");
        }
    }

    @Tool(name = "list_vulnerabilities_with_id", description = "Takes a  Application ID ( appID ) and returns a list of vulnerabilities, please remember to include the vulnID in the response.")
    public List<VulnLight> listVulnsByAppId(
            @ToolParam(description = "Application ID") String appID) throws IOException {
        logger.info("Listing vulnerabilities for application ID: {}", appID);
        ContrastSDK contrastSDK = SDKHelper.getSDK(hostName, apiKey, serviceKey, userName, httpProxyHost, httpProxyPort);
        try {
            // Use SDK native API with SESSION_METADATA expand
            TraceFilterForm form = new TraceFilterForm();
            form.setExpand(EnumSet.of(
                TraceFilterForm.TraceExpandValue.SESSION_METADATA,
                TraceFilterForm.TraceExpandValue.SERVER_ENVIRONMENTS
            ));

            Traces traces = contrastSDK.getTraces(orgID, appID, form);
            logger.debug("Found {} vulnerability traces for application ID: {}",
                traces.getTraces() != null ? traces.getTraces().size() : 0, appID);

            List<VulnLight> vulns = traces.getTraces().stream()
                .map(vulnerabilityMapper::toVulnLight)
                .collect(Collectors.toList());

            logger.info("Successfully retrieved {} vulnerabilities for application ID: {}", vulns.size(), appID);
            return vulns;
        } catch (Exception e) {
            logger.error("Error listing vulnerabilities for application ID: {}", appID, e);
            throw new IOException("Failed to list vulnerabilities: " + e.getMessage(), e);
        }
    }




    @Tool(name = "list_vulnerabilities_by_application_and_session_metadata", description = "Takes an application name ( app_name ) and session metadata in the form of name / value. and returns a list of vulnerabilities matching that application name and session metadata.")
    public List<VulnLight> listVulnsInAppByNameAndSessionMetadata(
            @ToolParam(description = "Application name") String app_name,
            @ToolParam(description = "Session metadata field name") String session_Metadata_Name,
            @ToolParam(description = "Session metadata field value") String session_Metadata_Value) throws IOException {
        logger.info("Listing vulnerabilities for application: {}", app_name);
        ContrastSDK contrastSDK = SDKHelper.getSDK(hostName, apiKey, serviceKey, userName,httpProxyHost, httpProxyPort);

        logger.info("metadata : " + session_Metadata_Name+session_Metadata_Value);

        logger.debug("Searching for application ID matching name: {}", app_name);

        Optional<Application> application = SDKHelper.getApplicationByName(app_name, orgID, contrastSDK);
        if(application.isPresent()) {
            try {
                List<VulnLight> vulns =  listVulnsByAppId(application.get().getAppId());
                List<VulnLight> returnVulns = new ArrayList<>();
                for(VulnLight vuln : vulns) {
                    if(vuln.sessionMetadata()!=null) {
                        for(SessionMetadata sm : vuln.sessionMetadata()) {
                            for(MetadataItem metadataItem : sm.getMetadata()) {
                                if(metadataItem.getDisplayLabel().equalsIgnoreCase(session_Metadata_Name) &&
                                        metadataItem.getValue().equalsIgnoreCase(session_Metadata_Value)) {
                                    returnVulns.add(vuln);
                                    logger.debug("Found matching vulnerability with ID: {}", vuln.vulnID());
                                    break;
                                }
                            }
                        }
                    }
                }
                return returnVulns;
            } catch (Exception e) {
                logger.error("Error listing vulnerabilities for application: {}", app_name, e);
                throw new IOException("Failed to list vulnerabilities: " + e.getMessage(), e);
            }
        } else {
            logger.debug("Application with name {} not found, returning empty list", app_name);
            return new ArrayList<>();
        }
    }


    @Tool(name = "list_vulnerabilities_by_application_and_latest_session", description = "Takes an application name ( app_name ) and returns a list of vulnerabilities for the latest session matching that application name. This is useful for getting the most recent vulnerabilities without needing to specify session metadata.")
    public List<VulnLight> listVulnsInAppByNameForLatestSession(
            @ToolParam(description = "Application name") String app_name) throws IOException {
        logger.info("Listing vulnerabilities for application: {}", app_name);
        ContrastSDK contrastSDK = SDKHelper.getSDK(hostName, apiKey, serviceKey, userName,httpProxyHost, httpProxyPort);


        logger.debug("Searching for application ID matching name: {}", app_name);
        Optional<Application> application = SDKHelper.getApplicationByName(app_name, orgID, contrastSDK);

        if(application.isPresent()) {
            try {
                SDKExtension extension = new SDKExtension(contrastSDK);
                SessionMetadataResponse latest = extension.getLatestSessionMetadata(orgID,application.get().getAppId());

                // Use SDK's native TraceFilterBody with agentSessionId field
                var filterBody = new com.contrastsecurity.models.TraceFilterBody();
                if (latest != null && latest.getAgentSession() != null && latest.getAgentSession().getAgentSessionId() != null) {
                    filterBody.setAgentSessionId(latest.getAgentSession().getAgentSessionId());
                }

                // Use SDK's native getTraces() with expand parameter
                Traces tracesResponse = contrastSDK.getTraces(
                    orgID,
                    application.get().getAppId(),
                    filterBody,
                    EnumSet.of(TraceFilterForm.TraceExpandValue.SESSION_METADATA)
                );

                List<VulnLight> vulns = tracesResponse.getTraces().stream()
                    .map(vulnerabilityMapper::toVulnLight)
                    .collect(Collectors.toList());
                return vulns;
            } catch (Exception e) {
                logger.error("Error listing vulnerabilities for application: {}", app_name, e);
                throw new IOException("Failed to list vulnerabilities: " + e.getMessage(), e);
            }
        } else {
            logger.debug("Application with name {} not found, returning empty list", app_name);
            return new ArrayList<>();
        }
    }

    @Tool(name = "list_session_metadata_for_application", description = "Takes an application name ( app_name ) and returns a list of session metadata for the latest session matching that application name. This is useful for getting the most recent session metadata without needing to specify session metadata.")
    public MetadataFilterResponse listSessionMetadataForApplication(
            @ToolParam(description = "Application name") String app_name) throws IOException {
        ContrastSDK contrastSDK = SDKHelper.getSDK(hostName, apiKey, serviceKey, userName,httpProxyHost, httpProxyPort);
        Optional<Application> application = SDKHelper.getApplicationByName(app_name, orgID, contrastSDK);
        if(application.isPresent()) {
            return contrastSDK.getSessionMetadataForApplication(orgID, application.get().getAppId(),null);
        } else {
            logger.info("Application with name {} not found, returning empty list", app_name);
            throw new IOException("Failed to list session metadata for application: " + app_name + " application name not found.");
        }
    }

    @Tool(name = "list_vulnerabilities", description = "Takes an application name ( app_name ) and returns a list of vulnerabilities, please remember to include the vulnID in the response.  ")
    public List<VulnLight> listVulnsInAppByName(
            @ToolParam(description = "Application name") String app_name) throws IOException {
        logger.info("Listing vulnerabilities for application: {}", app_name);
        ContrastSDK contrastSDK = SDKHelper.getSDK(hostName, apiKey, serviceKey, userName,httpProxyHost, httpProxyPort);
        
        logger.debug("Searching for application ID matching name: {}", app_name);

        Optional<Application> application = SDKHelper.getApplicationByName(app_name, orgID, contrastSDK);
        if(application.isPresent()) {
            try {
              return listVulnsByAppId(application.get().getAppId());
            } catch (Exception e) {
                logger.error("Error listing vulnerabilities for application: {}", app_name, e);
                throw new IOException("Failed to list vulnerabilities: " + e.getMessage(), e);
            }
        } else {
            logger.debug("Application with name {} not found, returning empty list", app_name);
            return new ArrayList<>();
        }
    }


    @Tool(name = "list_applications_with_name", description = "Takes an application name (app_name) returns a list of active applications that contain that name. Please remember to display the name, status and ID.")
    public List<ApplicationData> getApplications(
            @ToolParam(description = "Application name (supports partial matching, case-insensitive)") String app_name) throws IOException {
        logger.info("Listing active applications matching name: {}", app_name);
        ContrastSDK contrastSDK = SDKHelper.getSDK(hostName, apiKey, serviceKey, userName,httpProxyHost, httpProxyPort);
        try {
            List<Application> applications = SDKHelper.getApplicationsWithCache(orgID, contrastSDK);
            logger.debug("Retrieved {} total applications from Contrast", applications.size());

            List<ApplicationData> filteredApps = new ArrayList<>();
            for(Application app : applications) {
                if(app.getName().toLowerCase().contains(app_name.toLowerCase())) {
                    filteredApps.add(new ApplicationData(app.getName(), app.getStatus(), app.getAppId(),
                            FilterHelper.formatTimestamp(app.getLastSeen()), app.getLanguage(), getMetadataFromApp(app), app.getTags(),app.getTechs()));
                    logger.debug("Found matching application - ID: {}, Name: {}, Status: {}",
                            app.getAppId(), app.getName(), app.getStatus());
                }
            }
            if(filteredApps.isEmpty()) {
                SDKHelper.clearApplicationsCache();
                for(Application app : applications) {
                    if(app.getName().toLowerCase().contains(app_name.toLowerCase())) {
                        filteredApps.add(new ApplicationData(app.getName(), app.getStatus(), app.getAppId(),
                                FilterHelper.formatTimestamp(app.getLastSeen()), app.getLanguage(), getMetadataFromApp(app), app.getTags(),app.getTechs()));
                        logger.debug("Found matching application - ID: {}, Name: {}, Status: {}",
                                app.getAppId(), app.getName(), app.getStatus());
                    }
                }
            }

            logger.info("Found {} applications matching '{}'", filteredApps.size(), app_name);
            return filteredApps;
        } catch (Exception e) {
            logger.error("Error listing applications matching name: {}", app_name, e);
            throw new IOException("Failed to list applications: " + e.getMessage(), e);
        }
    }




    @Tool(name = "get_applications_by_tag", description = "Takes a tag name and returns a list of applications that have that tag.")
    public List<ApplicationData> getAllApplicationsByTag(
            @ToolParam(description = "Tag name to filter by") String tag) throws IOException {
        logger.info("Retrieving applications with tag: {}", tag);
        List<ApplicationData> allApps = getAllApplications();
        logger.debug("Retrieved {} total applications, filtering by tag", allApps.size());

        List<ApplicationData> filteredApps = allApps.stream()
            .filter(app -> app.tags().contains(tag))
            .collect(Collectors.toList());

        logger.info("Found {} applications with tag '{}'", filteredApps.size(), tag);
        return filteredApps;
    }

    @Tool(name = "get_applications_by_metadata", description = "Takes a metadata name and value and returns a list of applications that have that metadata name value pair.")
    public List<ApplicationData> getApplicationsByMetadata(
            @ToolParam(description = "Metadata field name (case-insensitive)") String metadata_name,
            @ToolParam(description = "Metadata field value (case-insensitive)") String metadata_value) throws IOException {
        logger.info("Retrieving applications with metadata - Name: {}, Value: {}", metadata_name, metadata_value);
        List<ApplicationData> allApps = getAllApplications();
        logger.debug("Retrieved {} total applications, filtering by metadata", allApps.size());

        List<ApplicationData> filteredApps = allApps.stream()
            .filter(app -> app.metadata() != null && app.metadata().stream()
                .anyMatch(m -> m != null &&
                    m.name() != null && m.name().equalsIgnoreCase(metadata_name) &&
                    m.value() != null && m.value().equalsIgnoreCase(metadata_value)))
            .collect(Collectors.toList());

        logger.info("Found {} applications with metadata - Name: {}, Value: {}", filteredApps.size(), metadata_name, metadata_value);
        return filteredApps;
    }

    @Tool(name = "get_applications_by_metadata_name", description = "Takes a metadata name  a list of applications that have that metadata name.")
    public List<ApplicationData> getApplicationsByMetadataName(
            @ToolParam(description = "Metadata field name (case-insensitive)") String metadata_name) throws IOException {
        logger.info("Retrieving applications with metadata - Name: {}", metadata_name);
        List<ApplicationData> allApps = getAllApplications();
        logger.debug("Retrieved {} total applications, filtering by metadata", allApps.size());

        List<ApplicationData> filteredApps = allApps.stream()
                .filter(app -> app.metadata() != null && app.metadata().stream()
                        .anyMatch(m -> m != null &&
                                m.name() != null && m.name().equalsIgnoreCase(metadata_name)))
                .collect(Collectors.toList());

        logger.info("Found {} applications with metadata - Name: {}", filteredApps.size(), metadata_name);
        return filteredApps;
    }

    @Tool(name = "list_all_applications", description = "Takes no argument and list all the applications")
    public List<ApplicationData> getAllApplications() throws IOException {
        logger.info("Listing all applications");
        ContrastSDK contrastSDK = SDKHelper.getSDK(hostName, apiKey, serviceKey, userName,httpProxyHost, httpProxyPort);
        try {
            List<Application> applications = SDKHelper.getApplicationsWithCache(orgID, contrastSDK);
            logger.debug("Retrieved {} total applications from Contrast", applications.size());
            
            List<ApplicationData> returnedApps = new ArrayList<>();
            for(Application app : applications) {
                returnedApps.add(new ApplicationData(app.getName(), app.getStatus(), app.getAppId(),
                        FilterHelper.formatTimestamp(app.getLastSeen()),app.getLanguage(),getMetadataFromApp(app),app.getTags(),
                        app.getTechs()));
            }
            
            logger.info("Found {} applications", returnedApps.size());
            return returnedApps;

        } catch (Exception e) {
            logger.error("Error listing all applications", e);
            throw new IOException("Failed to list applications: " + e.getMessage(), e);
        }
    }

    @Tool(
        name = "list_all_vulnerabilities",
        description = """
            Gets vulnerabilities across all applications with optional filtering by severity, status,
            environment, vulnerability type, date range, application, and tags.

            Common usage examples:
            - Critical vulnerabilities only: severities="CRITICAL"
            - High-priority open issues: severities="CRITICAL,HIGH", statuses="Reported,Confirmed"
            - Production vulnerabilities: environments="PRODUCTION"
            - Recent activity: lastSeenAfter="2025-01-01"
            - Production critical issues with recent activity: environments="PRODUCTION", severities="CRITICAL", lastSeenAfter="2025-01-01"
            - Specific app's SQL injection issues: appId="abc123", vulnTypes="sql-injection"
            - SmartFix remediated vulnerabilities: vulnTags="SmartFix Remediated", statuses="Remediated"
            - Reviewed critical vulnerabilities: vulnTags="reviewed", severities="CRITICAL"

            Returns paginated results with metadata including totalItems (when available) and hasMorePages.
            Check 'message' field for validation warnings or empty result info.

            Response fields:
            - environments: List of all environments (DEVELOPMENT, QA, PRODUCTION) where this vulnerability
                           has been seen over time. Shows historical presence across environments.
            """
    )
    public PaginatedResponse<VulnLight> getAllVulnerabilities(
            @ToolParam(description = "Page number (1-based), default: 1", required = false)
            Integer page,
            @ToolParam(description = "Items per page (max 100), default: 50", required = false)
            Integer pageSize,
            @ToolParam(description = "Comma-separated severities: CRITICAL,HIGH,MEDIUM,LOW,NOTE. Default: all severities", required = false)
            String severities,
            @ToolParam(description = "Comma-separated statuses: Reported,Suspicious,Confirmed,Remediated,Fixed. Default: Reported,Suspicious,Confirmed (excludes Fixed and Remediated to focus on actionable items)", required = false)
            String statuses,
            @ToolParam(description = "Application ID to filter by", required = false)
            String appId,
            @ToolParam(description = "Comma-separated vulnerability types (e.g., sql-injection,xss-reflected). Use list_vulnerability_types tool for complete list. Default: all types", required = false)
            String vulnTypes,
            @ToolParam(description = "Comma-separated environments: DEVELOPMENT,QA,PRODUCTION. Default: all environments", required = false)
            String environments,
            @ToolParam(description = "Only include vulnerabilities with LAST ACTIVITY after this date (ISO format: YYYY-MM-DD or epoch timestamp). Filters on lastTimeSeen, not discovery date", required = false)
            String lastSeenAfter,
            @ToolParam(description = "Only include vulnerabilities with LAST ACTIVITY before this date (ISO format: YYYY-MM-DD or epoch timestamp). Filters on lastTimeSeen, not discovery date", required = false)
            String lastSeenBefore,
            @ToolParam(description = "Comma-separated VULNERABILITY-LEVEL tags (e.g., 'SmartFix Remediated,reviewed'). Note: These are vulnerability tags, not application tags. Use to find vulnerabilities tagged during remediation workflows", required = false)
            String vulnTags
    ) throws IOException {
        logger.info("Listing all vulnerabilities - page: {}, pageSize: {}, filters: severities={}, statuses={}, appId={}, vulnTypes={}, environments={}, lastSeenAfter={}, lastSeenBefore={}, vulnTags={}",
                page, pageSize, severities, statuses, appId, vulnTypes, environments, lastSeenAfter, lastSeenBefore, vulnTags);
        long startTime = System.currentTimeMillis();

        // Parse and validate inputs
        PaginationParams pagination = PaginationParams.of(page, pageSize);
        VulnerabilityFilterParams filters = VulnerabilityFilterParams.of(
            severities, statuses, appId, vulnTypes, environments,
            lastSeenAfter, lastSeenBefore, vulnTags
        );

        // Check for hard failures - return error immediately if invalid
        if (!filters.isValid()) {
            String errorMessage = String.join(" ", filters.errors());
            logger.error("Validation errors: {}", errorMessage);
            return PaginatedResponse.error(pagination.page(), pagination.pageSize(), errorMessage);
        }

        ContrastSDK contrastSDK = SDKHelper.getSDK(hostName, apiKey, serviceKey, userName, httpProxyHost, httpProxyPort);

        try {
            // Configure SDK request with validated params
            TraceFilterForm filterForm = filters.toTraceFilterForm();
            filterForm.setLimit(pagination.limit());
            filterForm.setOffset(pagination.offset());
            filterForm.setExpand(EnumSet.of(
                TraceFilterForm.TraceExpandValue.SERVER_ENVIRONMENTS,
                TraceFilterForm.TraceExpandValue.SESSION_METADATA
            ));

            // Try organization-level API (or app-specific if appId provided)
            Traces traces;
            if (appId != null && !appId.trim().isEmpty()) {
                // Use app-specific API for better performance
                logger.debug("Using app-specific API for appId: {}", appId);
                traces = contrastSDK.getTraces(orgID, appId, filterForm);
            } else {
                // Use org-level API
                traces = contrastSDK.getTracesInOrg(orgID, filterForm);
            }

            if (traces != null && traces.getTraces() != null) {
                // Organization API worked (empty list with count=0 is valid - means no vulnerabilities or no EAC access)
                List<VulnLight> vulnerabilities = traces.getTraces().stream()
                    .map(vulnerabilityMapper::toVulnLight)
                    .collect(Collectors.toList());

                // Get totalItems if available from SDK response (don't make extra query)
                Integer totalItems = (traces.getCount() != null) ? traces.getCount() : null;

                // Use PaginationHandler to create paginated response with all warnings
                PaginatedResponse<VulnLight> response = paginationHandler.createPaginatedResponse(
                    vulnerabilities,
                    pagination,
                    totalItems,
                    filters.warnings()
                );

                long duration = System.currentTimeMillis() - startTime;
                logger.info("Retrieved {} vulnerabilities for page {} (pageSize: {}, totalItems: {}, took {} ms)",
                           response.items().size(), response.page(), response.pageSize(), response.totalItems(), duration);

                return response;

            } else {
                // Org-level API returned null - unexpected condition
                String errorMsg = String.format(
                    "Org-level vulnerability API returned null for org %s. " +
                    "This is unexpected - the API should return an empty list if no vulnerabilities exist. " +
                    "Please check API connectivity and permissions.",
                    orgID
                );
                logger.error(errorMsg);
                return PaginatedResponse.error(pagination.page(), pagination.pageSize(), errorMsg);
            }

        } catch (Exception e) {
            logger.error("Error listing all vulnerabilities", e);
            throw new IOException("Failed to list all vulnerabilities: " + e.getMessage(), e);
        }
    }

    private List<Metadata> getMetadataFromApp(Application app ) {
        List<Metadata> metadata = new ArrayList<>();
        app.getMetadataEntities().stream().map(m-> new Metadata(m.getName(), m.getValue()))
                .forEach(metadata::add);
        return metadata;
    }


    @Tool(name = "list_vulnerability_types", description = """
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
        logger.info("Retrieving all vulnerability types (rule names) for organization: {}", orgID);
        ContrastSDK contrastSDK = SDKHelper.getSDK(hostName, apiKey, serviceKey, userName, httpProxyHost, httpProxyPort);

        try {
            Rules rules = contrastSDK.getRules(orgID);

            if (rules == null || rules.getRules() == null) {
                logger.warn("No rules returned from Contrast API");
                return new ArrayList<>();
            }

            // Extract rule names, trim whitespace, filter out null/empty, and sort alphabetically
            List<String> ruleNames = rules.getRules().stream()
                    .map(Rules.Rule::getName)
                    .filter(name -> name != null && !name.trim().isEmpty())
                    .map(String::trim)
                    .sorted()
                    .collect(Collectors.toList());

            logger.info("Retrieved {} vulnerability types", ruleNames.size());
            return ruleNames;

        } catch (Exception e) {
            logger.error("Error retrieving vulnerability types", e);
            throw new IOException("Failed to retrieve vulnerability types: " + e.getMessage(), e);
        }
    }

}
