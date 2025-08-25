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
 * distributed under the License is distributed o n an "AS IS" BASIS,
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
import com.contrastsecurity.sdk.ContrastSDK;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AssessService {

    private static final Logger logger = LoggerFactory.getLogger(AssessService.class);
    
    //This is the accepted risk tolerance level for running ADR operations. Default is NO_RISK (0)
    private int acceptedRiskTolerance = 0;

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

    @Value("${accepted.risk.tolerance:${ACCEPTED_RISK_TOLERANCE:}}")
    private String acceptedRiskToleranceStr;

    @Tool(name = "get_vulnerability_by_id", description = "takes a vulnerability ID ( vulnID ) and Application ID ( appID ) and returns details about the specific security vulnerability. If based on the stacktrace, the vulnerability looks like it is in code that is not in the codebase, the vulnerability may be in a 3rd party library, review the CVE data attached to that stackframe you believe the vulnerability exists in and if possible upgrade that library to the next non vulnerable version based on the remediation guidance.")
    public Vulnerability getVulnerabilityById(String vulnID, String appID) throws IOException {
        acceptedRiskTolerance = RiskLevel.fromString(acceptedRiskToleranceStr).getValue();        
        logger.info("Risk level: high, and your accepted risk tolerance is set to: {}", acceptedRiskTolerance);
        if (acceptedRiskTolerance >= RiskLevel.HIGH.getValue()) {

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
                        httpRequestText);
            } catch (Exception e) {
                logger.error("Error retrieving vulnerability details for vulnID: {}", vulnID, e);
                throw new IOException("Failed to retrieve vulnerability details: " + e.getMessage(), e);
            }
        } else {
            logger.error("Your accepted Risk tolerance is too low to perform this operation. This risk level is: HIGH, with your accepted risk tolerance set at: {}", acceptedRiskTolerance);
            throw new IOException("Accepted Risk Tolerance is too low to perform this operation. Required: HIGH or higher.");
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
        acceptedRiskTolerance = RiskLevel.fromString(acceptedRiskToleranceStr).getValue();        
        logger.info("Risk level: high, and your accepted risk tolerance is set to: {}", acceptedRiskTolerance);
        if (acceptedRiskTolerance >= RiskLevel.HIGH.getValue()) {

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
        } else {
            logger.error("Your accepted Risk tolerance is too low to perform this operation. This risk level is: HIGH, with your accepted risk tolerance set at: {}", acceptedRiskTolerance);
            throw new IOException("Accepted Risk Tolerance is too low to perform this operation. Required: HIGH or higher.");
        }
    }

    @Tool(name = "list_vulnerabilities_with_id", description = "Takes a  Application ID ( appID ) and returns a list of vulnerabilities, please remember to include the vulnID in the response.")
    public List<VulnLight> listVulnsByAppId(String appID) throws IOException {
        acceptedRiskTolerance = RiskLevel.fromString(acceptedRiskToleranceStr).getValue();        
        logger.info("Risk level: medium, and your accepted risk tolerance is set to: {}", acceptedRiskTolerance);
        if (acceptedRiskTolerance >= RiskLevel.MEDIUM.getValue()) {

            logger.info("Listing vulnerabilities for application ID: {}", appID);
            ContrastSDK contrastSDK = SDKHelper.getSDK(hostName, apiKey, serviceKey, userName, httpProxyHost, httpProxyPort);
            try {
                List<TraceExtended> traces = new SDKExtension(contrastSDK).getTracesExtended(orgID, appID, new TraceFilterBody()).getTraces();
                logger.debug("Found {} vulnerability traces for application ID: {}", traces.size(), appID);

                List<VulnLight> vulns = new ArrayList<>();
                for (TraceExtended trace : traces) {
                    vulns.add(new VulnLight(trace.getTitle(), trace.getRule(), trace.getUuid(), trace.getSeverity(),trace.getSessionMetadata(),
                            new Date(trace.getLastTimeSeen()).toString(),trace.getLastTimeSeen()));
                }

                logger.info("Successfully retrieved {} vulnerabilities for application ID: {}", vulns.size(), appID);
                return vulns;
            } catch (Exception e) {
                logger.error("Error listing vulnerabilities for application ID: {}", appID, e);
                throw new IOException("Failed to list vulnerabilities: " + e.getMessage(), e);
            }
        } else {
            logger.error("Your accepted Risk tolerance is too low to perform this operation. This risk level is: MEDIUM, with your accepted risk tolerance set at: {}", acceptedRiskTolerance);
            throw new IOException("Accepted Risk Tolerance is too low to perform this operation. Required: MEDIUM or higher.");
        }
    }




    @Tool(name = "list_vulnerabilities_by_application_and_session_metadata", description = "Takes an application name ( app_name ) and session metadata in the form of name / value. and returns a list of vulnerabilities matching that application name and session metadata.")
    public List<VulnLight> listVulnsInAppByNameAndSessionMetadata(String app_name, String session_Metadata_Name, String session_Metadata_Value) throws IOException {
        acceptedRiskTolerance = RiskLevel.fromString(acceptedRiskToleranceStr).getValue();        
        logger.info("Risk level: medium, and your accepted risk tolerance is set to: {}", acceptedRiskTolerance);
        if (acceptedRiskTolerance >= RiskLevel.MEDIUM.getValue()) {

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
        } else {
            logger.error("Your accepted Risk tolerance is too low to perform this operation. This risk level is: MEDIUM, with your accepted risk tolerance set at: {}", acceptedRiskTolerance);
            throw new IOException("Accepted Risk Tolerance is too low to perform this operation. Required: MEDIUM or higher.");
        }
    }


    @Tool(name = "list_vulnerabilities_by_application_and_latest_session", description = "Takes an application name ( app_name ) and returns a list of vulnerabilities for the latest session matching that application name. This is useful for getting the most recent vulnerabilities without needing to specify session metadata.")
    public List<VulnLight> listVulnsInAppByNameForLatestSession(String app_name) throws IOException {
        acceptedRiskTolerance = RiskLevel.fromString(acceptedRiskToleranceStr).getValue();        
        logger.info("Risk level: medium, and your accepted risk tolerance is set to: {}", acceptedRiskTolerance);
        if (acceptedRiskTolerance >= RiskLevel.MEDIUM.getValue()) {

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
                                new Date(trace.getLastTimeSeen()).toString(),trace.getLastTimeSeen()));
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
        } else {
            logger.error("Your accepted Risk tolerance is too low to perform this operation. This risk level is: MEDIUM, with your accepted risk tolerance set at: {}", acceptedRiskTolerance);
            throw new IOException("Accepted Risk Tolerance is too low to perform this operation. Required: MEDIUM or higher.");
        }
    }

    @Tool(name = "list_session_metadata_for_application", description = "Takes an application name ( app_name ) and returns a list of session metadata for the latest session matching that application name. This is useful for getting the most recent session metadata without needing to specify session metadata.")
    public MetadataFilterResponse listSessionMetadataForApplication(String app_name) throws IOException {
        acceptedRiskTolerance = RiskLevel.fromString(acceptedRiskToleranceStr).getValue();        
        logger.info("Risk level: low, and your accepted risk tolerance is set to: {}", acceptedRiskTolerance);
        if (acceptedRiskTolerance >= RiskLevel.LOW.getValue()) {

            ContrastSDK contrastSDK = SDKHelper.getSDK(hostName, apiKey, serviceKey, userName,httpProxyHost, httpProxyPort);
            Optional<Application> application = SDKHelper.getApplicationByName(app_name, orgID, contrastSDK);
            if(application.isPresent()) {
                return contrastSDK.getSessionMetadataForApplication(orgID, application.get().getAppId(),null);
            } else {
                logger.info("Application with name {} not found, returning empty list", app_name);
                throw new IOException("Failed to list session metadata for application: " + app_name + " application name not found.");
            }
        } else {
            logger.error("Your accepted Risk tolerance is too low to perform this operation. This risk level is: LOW, with your accepted risk tolerance set at: {}", acceptedRiskTolerance);
            throw new IOException("Accepted Risk Tolerance is too low to perform this operation. Required: LOW or higher.");
        }
    }

    @Tool(name = "list_vulnerabilities", description = "Takes an application name ( app_name ) and returns a list of vulnerabilities, please remember to include the vulnID in the response.  ")
    public List<VulnLight> listVulnsInAppByName(String app_name) throws IOException {
        acceptedRiskTolerance = RiskLevel.fromString(acceptedRiskToleranceStr).getValue();        
        logger.info("Risk level: medium, and your accepted risk tolerance is set to: {}", acceptedRiskTolerance);
        if (acceptedRiskTolerance >= RiskLevel.MEDIUM.getValue()) {

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
        } else {
            logger.error("Your accepted Risk tolerance is too low to perform this operation. This risk level is: MEDIUM, with your accepted risk tolerance set at: {}", acceptedRiskTolerance);
            throw new IOException("Accepted Risk Tolerance is too low to perform this operation. Required: MEDIUM or higher.");
        }
    }


    @Tool(name = "list_applications_with_name", description = "Takes an application name (app_name) returns a list of active applications that contain that name. Please remember to display the name, status and ID.")
    public List<ApplicationData> getApplications(String app_name) throws IOException {
        acceptedRiskTolerance = RiskLevel.fromString(acceptedRiskToleranceStr).getValue();        
        logger.info("Risk level: low, and your accepted risk tolerance is set to: {}", acceptedRiskTolerance);
        if (acceptedRiskTolerance >= RiskLevel.LOW.getValue()) {

            logger.info("Listing active applications matching name: {}", app_name);
            ContrastSDK contrastSDK = SDKHelper.getSDK(hostName, apiKey, serviceKey, userName,httpProxyHost, httpProxyPort);
            try {
                List<Application> applications = SDKHelper.getApplicationsWithCache(orgID, contrastSDK);
                logger.debug("Retrieved {} total applications from Contrast", applications.size());

                List<ApplicationData> filteredApps = new ArrayList<>();
                for(Application app : applications) {
                    if(app.getName().toLowerCase().contains(app_name.toLowerCase())) {
                        filteredApps.add(new ApplicationData(acceptedRiskToleranceStr, app.getName(), app.getStatus(), app.getAppId(), app.getLastSeen(),
                                new Date(app.getLastSeen()).toString(), app.getLanguage(), getMetadataFromApp(app), app.getTags(),app.getTechs()));
                        logger.debug("Found matching application - ID: {}, Name: {}, Status: {}",
                                app.getAppId(), app.getName(), app.getStatus());
                    }
                }
                if(filteredApps.isEmpty()) {
                    SDKHelper.clearApplicationsCache();
                    for(Application app : applications) {
                        if(app.getName().toLowerCase().contains(app_name.toLowerCase())) {
                            filteredApps.add(new ApplicationData(acceptedRiskToleranceStr, app.getName(), app.getStatus(), app.getAppId(), app.getLastSeen(),
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
        } else {
            logger.error("Your accepted Risk tolerance is too low to perform this operation. This risk level is: LOW, with your accepted risk tolerance set at: {}", acceptedRiskTolerance);
            throw new IOException("Accepted Risk Tolerance is too low to perform this operation. Required: LOW or higher.");
        }
    }




    @Tool(name = "get_applications_by_tag", description = "Takes a tag name and returns a list of applications that have that tag.")
    public List<ApplicationData> getAllApplicationsByTag(String acceptedRiskToleranceStr, String tag) throws IOException {
        acceptedRiskTolerance = RiskLevel.fromString(acceptedRiskToleranceStr).getValue();
        logger.info("Risk level: low, and your accepted risk tolerance is set to: {}", acceptedRiskTolerance);
        if (acceptedRiskTolerance >= RiskLevel.LOW.getValue()) {
      
            logger.info("Retrieving applications with tag: {}", tag);
            List<ApplicationData> allApps = getAllApplications(acceptedRiskToleranceStr);
            logger.debug("Retrieved {} total applications, filtering by tag", allApps.size());

            List<ApplicationData> filteredApps = allApps.stream()
                .filter(app -> app.tags().contains(tag))
                .collect(Collectors.toList());

            logger.info("Found {} applications with tag '{}'", filteredApps.size(), tag);
            return filteredApps;
        } else {
            logger.error("Your accepted Risk tolerance is too low to perform this operation. This risk level is: LOW, with your accepted risk tolerance set at: {}", acceptedRiskTolerance);
            throw new IOException("Accepted Risk Tolerance is too low to perform this operation. Required: LOW or higher.");
        }
    }

    @Tool(name = "get_applications_by_metadata", description = "Takes a metadata name and value and returns a list of applications that have that metadata name value pair.")
    public List<ApplicationData> getApplicationsByMetadata(String acceptedRiskToleranceStr, String metadata_name, String metadata_value) throws IOException {
        acceptedRiskTolerance = RiskLevel.fromString(acceptedRiskToleranceStr).getValue();
        logger.info("Risk level: low, and your accepted risk tolerance is set to: {}", acceptedRiskTolerance);
        if (acceptedRiskTolerance >= RiskLevel.LOW.getValue()) {
        
            logger.info("Retrieving applications with metadata - Name: {}, Value: {}", metadata_name, metadata_value);
            List<ApplicationData> allApps = getAllApplications(acceptedRiskToleranceStr);
            logger.debug("Retrieved {} total applications, filtering by metadata", allApps.size());

            List<ApplicationData> filteredApps = allApps.stream()
                .filter(app -> app.metadata() != null && app.metadata().stream()
                    .anyMatch(m -> m != null &&
                        m.name() != null && m.name().equalsIgnoreCase(metadata_name) &&
                        m.value() != null && m.value().equalsIgnoreCase(metadata_value)))
                .collect(Collectors.toList());

            logger.info("Found {} applications with metadata - Name: {}, Value: {}", filteredApps.size(), metadata_name, metadata_value);
            return filteredApps;
        } else {
            logger.error("Your accepted Risk tolerance is too low to perform this operation. This risk level is: LOW, with your accepted risk tolerance set at: {}", acceptedRiskTolerance);
            throw new IOException("Accepted Risk Tolerance is too low to perform this operation. Required: LOW or higher.");
        }
    }

    @Tool(name = "get_applications_by_metadata_name", description = "Risk Level: LOW, Takes a metadata name  a list of applications that have that metadata name.")
    public List<ApplicationData> getApplicationsByMetadataName(String acceptedRiskToleranceStr, String metadata_name) throws IOException {
        acceptedRiskTolerance = RiskLevel.fromString(acceptedRiskToleranceStr).getValue();
        logger.info("Risk level: low, and your accepted risk tolerance is set to: {}", acceptedRiskTolerance);
        if (acceptedRiskTolerance >= RiskLevel.LOW.getValue()) {
      
            logger.info("Retrieving applications with metadata - Name: {}", metadata_name);
            List<ApplicationData> allApps = getAllApplications(acceptedRiskToleranceStr);
            logger.debug("Retrieved {} total applications, filtering by metadata", allApps.size());

            List<ApplicationData> filteredApps = allApps.stream()
                    .filter(app -> app.metadata() != null && app.metadata().stream()
                            .anyMatch(m -> m != null &&
                                    m.name() != null && m.name().equalsIgnoreCase(metadata_name)))
                    .collect(Collectors.toList());

            logger.info("Found {} applications with metadata - Name: {}", filteredApps.size(), metadata_name);
            return filteredApps;
        } else {
            logger.error("Your accepted Risk tolerance is too low to perform this operation. This risk level is: LOW, with your accepted risk tolerance set at: {}", acceptedRiskTolerance);
            throw new IOException("Accepted Risk Tolerance is too low to perform this operation. Required: LOW or higher.");
        }
    }

    @Tool(name = "list_all_applications", description = "Risk Level: LOW, Takes no argument and list all the applications")
    public List<ApplicationData> getAllApplications(String acceptedRiskToleranceStr) throws IOException {
        acceptedRiskTolerance = RiskLevel.fromString(acceptedRiskToleranceStr).getValue();        
        logger.info("Risk level: low, and your accepted risk tolerance is set to: {}", acceptedRiskTolerance);
        if (acceptedRiskTolerance >= RiskLevel.LOW.getValue()) {
      
            logger.info("Listing all applications");
            ContrastSDK contrastSDK = SDKHelper.getSDK(hostName, apiKey, serviceKey, userName,httpProxyHost, httpProxyPort);
            try {
                List<Application> applications = SDKHelper.getApplicationsWithCache(orgID, contrastSDK);
                logger.debug("Retrieved {} total applications from Contrast", applications.size());
                
                List<ApplicationData> returnedApps = new ArrayList<>();
                for(Application app : applications) {
                    returnedApps.add(new ApplicationData(acceptedRiskToleranceStr, app.getName(), app.getStatus(), app.getAppId(),
                            app.getLastSeen(), new Date(app.getLastSeen()).toString(),app.getLanguage(),getMetadataFromApp(app),app.getTags(),
                            app.getTechs()));
                }
                
                logger.info("Found {} applications", returnedApps.size());
                return returnedApps;

            } catch (Exception e) {
                logger.error("Error listing all applications", e);
                throw new IOException("Failed to list applications: " + e.getMessage(), e);
            }
        } else {
            logger.error("Your accepted Risk tolerance is too low to perform this operation. This risk level is: LOW, with your accepted risk tolerance set at: {}", acceptedRiskTolerance);
            throw new IOException("Accepted Risk Tolerance is too low to perform this operation. Required: LOW or higher.");
        }
    }

    private List<Metadata> getMetadataFromApp(Application app ) {
        List<Metadata> metadata = new ArrayList<>();
        app.getMetadataEntities().stream().map(m-> new Metadata(m.getName(), m.getValue()))
                .forEach(metadata::add);
        return metadata;
    }

}
