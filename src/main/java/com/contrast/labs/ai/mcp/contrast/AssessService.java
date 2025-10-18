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


import com.contrast.labs.ai.mcp.contrast.data.*;
import com.contrast.labs.ai.mcp.contrast.hints.HintGenerator;
import com.contrast.labs.ai.mcp.contrast.sdkexstension.SDKExtension;
import com.contrast.labs.ai.mcp.contrast.sdkexstension.SDKHelper;
import com.contrast.labs.ai.mcp.contrast.sdkexstension.data.LibraryExtended;
import com.contrast.labs.ai.mcp.contrast.sdkexstension.data.application.Application;
import com.contrast.labs.ai.mcp.contrast.sdkexstension.data.sca.LibraryObservation;
import com.contrast.labs.ai.mcp.contrast.sdkexstension.data.sessionmetadata.SessionMetadataResponse;
import com.contrast.labs.ai.mcp.contrast.sdkexstension.data.traces.MetadataItem;
import com.contrast.labs.ai.mcp.contrast.sdkexstension.data.traces.SessionMetadata;
import com.contrast.labs.ai.mcp.contrast.sdkexstension.data.traces.TraceExtended;
import com.contrastsecurity.models.*;
import com.contrastsecurity.models.TraceFilterBody;
import com.contrastsecurity.http.TraceFilterForm;
import com.contrastsecurity.http.RuleSeverity;
import com.contrastsecurity.http.ServerEnvironment;
import com.contrastsecurity.sdk.ContrastSDK;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AssessService {

    private static final Logger logger = LoggerFactory.getLogger(AssessService.class);
    

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
    public Vulnerability getVulnerabilityById(String vulnID, String appID) throws IOException {
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
            String hint = HintGenerator.generateVulnerabilityFixHint(trace.getRule());
            logger.info("Successfully retrieved vulnerability details for vulnID: {}", vulnID);
            return new Vulnerability(hint, vulnID, trace.getTitle(), trace.getRule(),
                    recommendationResponse.getRecommendation().getText(), stackLibs, new ArrayList<>(libsToReturn), // Convert Set to List
                    httpRequestText, trace.getStatus(), trace.getFirstTimeSeen(), trace.getLastTimeSeen(), trace.getClosedTime());
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
    public Vulnerability getVulnerability(String vulnID, String app_name) throws IOException {
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
    public List<VulnLight> listVulnsByAppId(String appID) throws IOException {
        logger.info("Listing vulnerabilities for application ID: {}", appID);
        ContrastSDK contrastSDK = SDKHelper.getSDK(hostName, apiKey, serviceKey, userName, httpProxyHost, httpProxyPort);
        try {
            TraceFilterBody traceFilterBody = new TraceFilterBody();
            List<TraceExtended> traces = new SDKExtension(contrastSDK).getTracesExtended(orgID, appID, new TraceFilterBody()).getTraces();
            logger.debug("Found {} vulnerability traces for application ID: {}", traces.size(), appID);

            List<VulnLight> vulns = new ArrayList<>();
            for (TraceExtended trace : traces) {
                vulns.add(new VulnLight(trace.getTitle(), trace.getRule(), trace.getUuid(), trace.getSeverity(),trace.getSessionMetadata(),
                        new Date(trace.getLastTimeSeen()).toString(),trace.getLastTimeSeen(), trace.getStatus(), trace.getFirstTimeSeen(), trace.getClosedTime()));
            }

            logger.info("Successfully retrieved {} vulnerabilities for application ID: {}", vulns.size(), appID);
            return vulns;
        } catch (Exception e) {
            logger.error("Error listing vulnerabilities for application ID: {}", appID, e);
            throw new IOException("Failed to list vulnerabilities: " + e.getMessage(), e);
        }
    }




    @Tool(name = "list_vulnerabilities_by_application_and_session_metadata", description = "Takes an application name ( app_name ) and session metadata in the form of name / value. and returns a list of vulnerabilities matching that application name and session metadata.")
    public List<VulnLight> listVulnsInAppByNameAndSessionMetadata(String app_name, String session_Metadata_Name, String session_Metadata_Value) throws IOException {
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
    public List<VulnLight> listVulnsInAppByNameForLatestSession(String app_name) throws IOException {
        logger.info("Listing vulnerabilities for application: {}", app_name);
        ContrastSDK contrastSDK = SDKHelper.getSDK(hostName, apiKey, serviceKey, userName,httpProxyHost, httpProxyPort);


        logger.debug("Searching for application ID matching name: {}", app_name);
        Optional<Application> application = SDKHelper.getApplicationByName(app_name, orgID, contrastSDK);

        if(application.isPresent()) {
            try {
                SDKExtension extension = new SDKExtension(contrastSDK);
                SessionMetadataResponse latest = extension.getLatestSessionMetadata(orgID,application.get().getAppId());
                com.contrast.labs.ai.mcp.contrast.data.TraceFilterBody tfilter = new com.contrast.labs.ai.mcp.contrast.data.TraceFilterBody();
                if(latest!=null&&latest.getAgentSession()!=null&&latest.getAgentSession().getAgentSessionId()!=null) {
                    tfilter.setAgentSessionId(latest.getAgentSession().getAgentSessionId());
                }
                List<TraceExtended> traces = new SDKExtension(contrastSDK).getTracesExtended(orgID, application.get().getAppId(), tfilter).getTraces();

                List<VulnLight> vulns = new ArrayList<>();
                for (TraceExtended trace : traces) {
                    vulns.add(new VulnLight(trace.getTitle(), trace.getRule(), trace.getUuid(), trace.getSeverity(),trace.getSessionMetadata(),
                            new Date(trace.getLastTimeSeen()).toString(),trace.getLastTimeSeen(), trace.getStatus(), trace.getFirstTimeSeen(), trace.getClosedTime()));
                }
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
    public MetadataFilterResponse listSessionMetadataForApplication(String app_name) throws IOException {
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
    public List<VulnLight> listVulnsInAppByName(String app_name) throws IOException {
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
    public List<ApplicationData> getApplications(String app_name) throws IOException {
        logger.info("Listing active applications matching name: {}", app_name);
        ContrastSDK contrastSDK = SDKHelper.getSDK(hostName, apiKey, serviceKey, userName,httpProxyHost, httpProxyPort);
        try {
            List<Application> applications = SDKHelper.getApplicationsWithCache(orgID, contrastSDK);
            logger.debug("Retrieved {} total applications from Contrast", applications.size());

            List<ApplicationData> filteredApps = new ArrayList<>();
            for(Application app : applications) {
                if(app.getName().toLowerCase().contains(app_name.toLowerCase())) {
                    filteredApps.add(new ApplicationData(app.getName(), app.getStatus(), app.getAppId(), app.getLastSeen(),
                            new Date(app.getLastSeen()).toString(), app.getLanguage(), getMetadataFromApp(app), app.getTags(),app.getTechs()));
                    logger.debug("Found matching application - ID: {}, Name: {}, Status: {}",
                            app.getAppId(), app.getName(), app.getStatus());
                }
            }
            if(filteredApps.isEmpty()) {
                SDKHelper.clearApplicationsCache();
                for(Application app : applications) {
                    if(app.getName().toLowerCase().contains(app_name.toLowerCase())) {
                        filteredApps.add(new ApplicationData(app.getName(), app.getStatus(), app.getAppId(), app.getLastSeen(),
                                new Date(app.getLastSeen()).toString(), app.getLanguage(), getMetadataFromApp(app), app.getTags(),app.getTechs()));
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
    public List<ApplicationData> getAllApplicationsByTag(String tag) throws IOException {
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
    public List<ApplicationData> getApplicationsByMetadata(String metadata_name, String metadata_value) throws IOException {
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
    public List<ApplicationData> getApplicationsByMetadataName(String metadata_name) throws IOException {
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
                        app.getLastSeen(), new Date(app.getLastSeen()).toString(),app.getLanguage(),getMetadataFromApp(app),app.getTags(),
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
            Gets vulnerabilities across all applications with optional filtering.

            Filters (all optional):
            - severities: Filter by severity level(s). Options: CRITICAL, HIGH, MEDIUM, LOW, NOTE.
                          Comma-separated for multiple (e.g., "CRITICAL,HIGH").
                          Default: Returns all severities.
            - statuses: Filter by vulnerability status(es). Options: Reported, Suspicious, Confirmed, Remediated, Fixed.
                        Comma-separated for multiple (e.g., "Reported,Confirmed").
                        Default: Returns Reported, Suspicious, and Confirmed (excludes Fixed and Remediated - focus on actionable items).
            - appId: Filter to a specific application ID.
            - vulnTypes: Filter by vulnerability type/rule name(s). Common types:
                         sql-injection, xss-reflected, xss-stored, path-traversal, cmd-injection,
                         crypto-bad-mac, crypto-bad-ciphers, trust-boundary-violation, xxe,
                         untrusted-deserialization, csrf, ssrf, ldap-injection, xpath-injection.
                         Comma-separated for multiple (e.g., "sql-injection,xss-reflected").
                         For complete list, use list_vulnerability_types tool.
                         Default: Returns all vulnerability types.
            - environments: Filter by server environment(s). Options: DEVELOPMENT, QA, PRODUCTION.
                            Comma-separated for multiple (e.g., "PRODUCTION,QA").
                            Default: Returns all environments.
            - lastSeenAfter: Only include vulnerabilities with activity after this date (ISO format: YYYY-MM-DD or epoch timestamp).
                             IMPORTANT: Filters on LAST ACTIVITY DATE (lastTimeSeen), not discovery date.
            - lastSeenBefore: Only include vulnerabilities with activity before this date (ISO format: YYYY-MM-DD or epoch timestamp).
                              IMPORTANT: Filters on LAST ACTIVITY DATE (lastTimeSeen), not discovery date.
            - vulnTags: Filter by vulnerability-level tag(s). Comma-separated for multiple (e.g., "SmartFix Remediated,reviewed").
                        IMPORTANT: Filters on VULNERABILITY TAGS, not application tags.
                        Use case: Query vulnerabilities with specific tags like "SmartFix Remediated" to find SmartFix-remediated issues.
                        Default: Returns all vulnerability tags.

            Pagination: page (default: 1), pageSize (default: 50, max: 100)

            Examples:
            - Critical vulnerabilities only: severities="CRITICAL"
            - High-priority open issues: severities="CRITICAL,HIGH", statuses="Reported,Confirmed"
            - Production vulnerabilities: environments="PRODUCTION"
            - Recent activity: lastSeenAfter="2025-01-01"
            - Production critical issues with recent activity: environments="PRODUCTION", severities="CRITICAL", lastSeenAfter="2025-01-01"
            - Specific app's SQL injection issues: appId="abc123", vulnTypes="sql-injection"
            - SmartFix remediated vulnerabilities: vulnTags="SmartFix Remediated", statuses="Remediated"
            - Reviewed critical vulnerabilities: vulnTags="reviewed", severities="CRITICAL"

            Returns pagination metadata including totalItems (when available) and hasMorePages.
            Check 'message' field for validation warnings or empty result info.
            """
    )
    public PaginatedResponse<VulnLight> getAllVulnerabilities(
            Integer page,
            Integer pageSize,
            String severities,
            String statuses,
            String appId,
            String vulnTypes,
            String environments,
            String lastSeenAfter,
            String lastSeenBefore,
            String vulnTags
    ) throws IOException {
        logger.info("Listing all vulnerabilities - page: {}, pageSize: {}, filters: severities={}, statuses={}, appId={}, vulnTypes={}, environments={}, lastSeenAfter={}, lastSeenBefore={}, vulnTags={}",
                page, pageSize, severities, statuses, appId, vulnTypes, environments, lastSeenAfter, lastSeenBefore, vulnTags);
        long startTime = System.currentTimeMillis();

        // Validate and clamp pagination parameters
        StringBuilder messageBuilder = new StringBuilder();
        int actualPage = page != null && page > 0 ? page : 1;
        if (page != null && page < 1) {
            logger.warn("Invalid page number {}, clamping to 1", page);
            messageBuilder.append(String.format("Invalid page number %d, using page 1. ", page));
            actualPage = 1;
        }

        int actualPageSize = pageSize != null && pageSize > 0 ? pageSize : 50;
        if (pageSize != null && pageSize < 1) {
            logger.warn("Invalid pageSize {}, using default 50", pageSize);
            messageBuilder.append(String.format("Invalid pageSize %d, using default 50. ", pageSize));
            actualPageSize = 50;
        }
        if (pageSize != null && pageSize > 100) {
            logger.warn("Requested pageSize {} exceeds maximum 100, capping", pageSize);
            messageBuilder.append(String.format("Requested pageSize %d exceeds maximum 100, capped to 100. ", pageSize));
            actualPageSize = 100;
        }

        // Parse filter parameters with validation
        List<String> severityList = FilterHelper.parseCommaSeparatedUpperCase(severities);
        List<String> vulnTypeList = FilterHelper.parseCommaSeparatedLowerCase(vulnTypes);
        List<String> envList = FilterHelper.parseCommaSeparatedUpperCase(environments);
        List<String> vulnTagList = FilterHelper.parseCommaSeparated(vulnTags); // Case-sensitive

        // Parse dates with validation messages
        FilterHelper.ParseResult<Date> startDateResult = FilterHelper.parseDateWithValidation(lastSeenAfter, "lastSeenAfter");
        FilterHelper.ParseResult<Date> endDateResult = FilterHelper.parseDateWithValidation(lastSeenBefore, "lastSeenBefore");

        if (startDateResult.hasValidationMessage()) {
            messageBuilder.append(startDateResult.getValidationMessage()).append(" ");
        }
        if (endDateResult.hasValidationMessage()) {
            messageBuilder.append(endDateResult.getValidationMessage()).append(" ");
        }

        // Handle status filter with smart defaults
        List<String> statusList;
        boolean usingSmartDefaults = false;
        if (statuses == null || statuses.trim().isEmpty()) {
            // Smart defaults: exclude Fixed and Remediated
            statusList = Arrays.asList("Reported", "Suspicious", "Confirmed");
            usingSmartDefaults = true;
            logger.debug("Using smart defaults for status filter: {}", statusList);
        } else {
            statusList = FilterHelper.parseCommaSeparated(statuses);
        }

        ContrastSDK contrastSDK = SDKHelper.getSDK(hostName, apiKey, serviceKey, userName, httpProxyHost, httpProxyPort);

        try {
            // Convert to SDK offset (0-based)
            int offset = (actualPage - 1) * actualPageSize;
            int limit = actualPageSize;

            // Build filter form with all filters
            TraceFilterForm filterForm = new TraceFilterForm();
            filterForm.setLimit(limit);
            filterForm.setOffset(offset);

            // Apply severity filter
            if (severityList != null && !severityList.isEmpty()) {
                EnumSet<RuleSeverity> severitySet = EnumSet.noneOf(RuleSeverity.class);
                for (String sev : severityList) {
                    try {
                        severitySet.add(RuleSeverity.valueOf(sev));
                    } catch (IllegalArgumentException e) {
                        logger.warn("Invalid severity value: {}", sev);
                        messageBuilder.append(String.format("Invalid severity '%s'. Valid: CRITICAL, HIGH, MEDIUM, LOW, NOTE. ", sev));
                    }
                }
                if (!severitySet.isEmpty()) {
                    filterForm.setSeverities(severitySet);
                }
            }

            // Apply status filter
            if (statusList != null && !statusList.isEmpty()) {
                filterForm.setStatus(statusList);
                if (usingSmartDefaults) {
                    messageBuilder.append("Showing actionable vulnerabilities only (excluding Fixed and Remediated). To see all statuses, specify statuses parameter explicitly. ");
                }
            }

            // Apply vulnerability type filter
            if (vulnTypeList != null && !vulnTypeList.isEmpty()) {
                filterForm.setVulnTypes(vulnTypeList);
            }

            // Apply environment filter
            if (envList != null && !envList.isEmpty()) {
                EnumSet<ServerEnvironment> envSet = EnumSet.noneOf(ServerEnvironment.class);
                for (String env : envList) {
                    try {
                        envSet.add(ServerEnvironment.valueOf(env));
                    } catch (IllegalArgumentException e) {
                        logger.warn("Invalid environment value: {}", env);
                        messageBuilder.append(String.format("Invalid environment '%s'. Valid: DEVELOPMENT, QA, PRODUCTION. ", env));
                    }
                }
                if (!envSet.isEmpty()) {
                    filterForm.setEnvironments(envSet);
                }
            }

            // Apply date filters
            if (startDateResult.getValue() != null) {
                filterForm.setStartDate(startDateResult.getValue());
                messageBuilder.append("Time filters apply to LAST ACTIVITY DATE (lastTimeSeen), not discovery date. ");
            }
            if (endDateResult.getValue() != null) {
                filterForm.setEndDate(endDateResult.getValue());
                if (startDateResult.getValue() == null) { // Only add message once
                    messageBuilder.append("Time filters apply to LAST ACTIVITY DATE (lastTimeSeen), not discovery date. ");
                }
            }

            // Apply vulnerability tags filter
            if (vulnTagList != null && !vulnTagList.isEmpty()) {
                filterForm.setFilterTags(vulnTagList);
            }

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
                List<VulnLight> vulnerabilities = new ArrayList<>();

                for (Trace trace : traces.getTraces()) {
                    vulnerabilities.add(new VulnLight(
                        trace.getTitle(),
                        trace.getRule(),
                        trace.getUuid(),
                        trace.getSeverity(),
                        new ArrayList<>(), // Session metadata not available at org level
                        new Date(trace.getLastTimeSeen()).toString(),
                        trace.getLastTimeSeen(),
                        trace.getStatus(),
                        trace.getFirstTimeSeen(),
                        trace.getClosedTime()
                    ));
                }

                // Get totalItems if available from SDK response (don't make extra query)
                Integer totalItems = (traces.getCount() != null) ? traces.getCount() : null;

                // Calculate hasMorePages
                boolean hasMorePages;
                if (totalItems != null) {
                    hasMorePages = (actualPage * actualPageSize) < totalItems;
                } else {
                    // Heuristic: if we got a full page, assume more exist
                    hasMorePages = vulnerabilities.size() == actualPageSize;
                }

                // Handle empty results messaging
                if (vulnerabilities.isEmpty() && actualPage == 1) {
                    messageBuilder.append("No vulnerabilities found. ");
                } else if (vulnerabilities.isEmpty() && actualPage > 1) {
                    if (totalItems != null) {
                        int totalPages = (int) Math.ceil((double) totalItems / actualPageSize);
                        messageBuilder.append(String.format(
                            "Requested page %d exceeds available pages (total: %d). ",
                            actualPage, totalPages
                        ));
                    } else {
                        messageBuilder.append(String.format(
                            "Requested page %d returned no results. ",
                            actualPage
                        ));
                    }
                }

                String message = messageBuilder.length() > 0 ? messageBuilder.toString().trim() : null;

                long duration = System.currentTimeMillis() - startTime;
                logger.info("Retrieved {} vulnerabilities for page {} (pageSize: {}, totalItems: {}, took {} ms)",
                           vulnerabilities.size(), actualPage, actualPageSize, totalItems, duration);

                return new PaginatedResponse<>(
                    vulnerabilities,
                    actualPage,
                    actualPageSize,
                    totalItems,
                    hasMorePages,
                    message
                );

            } else {
                // Fallback to application-by-application approach
                logger.warn("Organization-level API returned no results, using fallback approach");

                List<Application> applications = SDKHelper.getApplicationsWithCache(orgID, contrastSDK);
                List<VulnLight> allVulnerabilities = new ArrayList<>();

                for (Application app : applications) {
                    try {
                        List<VulnLight> appVulns = listVulnsByAppId(app.getAppId());
                        allVulnerabilities.addAll(appVulns);
                    } catch (Exception e) {
                        logger.warn("Failed to get vulnerabilities for application {}: {}",
                                   app.getName(), e.getMessage());
                        // Continue processing other applications even if one fails
                    }
                }

                // Manual pagination for fallback path
                // totalItems is null because we can't efficiently count without fetching all
                Integer totalItems = null;
                int startIdx = offset;
                int endIdx = Math.min(startIdx + limit, allVulnerabilities.size());

                List<VulnLight> paginatedVulns = (startIdx < allVulnerabilities.size())
                    ? allVulnerabilities.subList(startIdx, endIdx)
                    : new ArrayList<>();

                // Heuristic for hasMorePages in fallback mode
                boolean hasMorePages = endIdx < allVulnerabilities.size();

                // Handle empty results messaging
                if (paginatedVulns.isEmpty() && actualPage == 1) {
                    messageBuilder.append("No vulnerabilities found. ");
                } else if (paginatedVulns.isEmpty() && actualPage > 1) {
                    messageBuilder.append(String.format(
                        "Requested page %d returned no results. ",
                        actualPage
                    ));
                }

                String message = messageBuilder.length() > 0 ? messageBuilder.toString().trim() : null;

                long duration = System.currentTimeMillis() - startTime;
                logger.info("Retrieved {} vulnerabilities for page {} using fallback (pageSize: {}, totalFetched: {}, took {} ms)",
                           paginatedVulns.size(), actualPage, actualPageSize, allVulnerabilities.size(), duration);

                return new PaginatedResponse<>(
                    paginatedVulns,
                    actualPage,
                    actualPageSize,
                    totalItems,
                    hasMorePages,
                    message
                );
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
