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
import com.contrast.labs.ai.mcp.contrast.data.StackLib;
import com.contrast.labs.ai.mcp.contrast.data.VulnLight;
import com.contrast.labs.ai.mcp.contrast.data.Vulnerability;
import com.contrast.labs.ai.mcp.contrast.hints.HintGenerator;
import com.contrast.labs.ai.mcp.contrast.sdkexstension.SDKExtension;
import com.contrast.labs.ai.mcp.contrast.sdkexstension.SDKHelper;
import com.contrast.labs.ai.mcp.contrast.sdkexstension.data.LibraryExtended;
import com.contrast.labs.ai.mcp.contrast.sdkexstension.data.sca.LibraryObservation;
import com.contrastsecurity.models.Application;
import com.contrastsecurity.models.EventResource;
import com.contrastsecurity.models.EventSummaryResponse;
import com.contrastsecurity.models.HttpRequestResponse;
import com.contrastsecurity.models.RecommendationResponse;
import com.contrastsecurity.models.Stacktrace;
import com.contrastsecurity.models.Trace;
import com.contrastsecurity.models.TraceFilterBody;
import com.contrastsecurity.sdk.ContrastSDK;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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


    @Tool(name = "get_vulnerability", description = "takes a vulnerability ID ( vulnID ) and Application ID ( appID ) and returns details about the specific security vulnerability. If based on the stacktrace, the vulnerability looks like it is in code that is not in the codebase, the vulnerability may be in a 3rd party library, review the CVE data attached to that stackframe you believe the vulnerability exists in and if possible upgrade that library to the next non vulnerable version based on the remediation guidance.")
    public Vulnerability getVulnerability(String vulnID, String appID) throws IOException {
        logger.info("Retrieving vulnerability details for vulnID: {} in application ID: {}", vulnID, appID);
        ContrastSDK contrastSDK = SDKHelper.getSDK(hostName, apiKey, serviceKey, userName);
        logger.debug("ContrastSDK initialized with host: {}", hostName);
        
        try {
            Trace trace = contrastSDK.getTraces(orgID, appID, new TraceFilterBody()).getTraces().stream()
                    .filter(t -> t.getUuid().toLowerCase().equals(vulnID.toLowerCase()))
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
                LibraryLibraryObservation llob = new LibraryLibraryObservation(lib,new SDKExtension(contrastSDK).getLibraryObservations(orgID,appID,lib.getHash(),50));
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

    @Tool(name = "get_vulnerability_by_app_name", description = "Takes a vulnerability ID (vulnID) and application name (appName) and returns details about the specific security vulnerability.  If based on the stacktrace, the vulnerability looks like it is in code that is not in the codebase, the vulnerability may be in a 3rd party library, review the CVE data attached to that stackframe you believe the vulnerability exists in and if possible upgrade that library to the next non vulnerable version based on the remediation guidance.")
    public Vulnerability getVulnerabilityByAppName(String vulnID, String appName) throws IOException {
        logger.info("Retrieving vulnerability details for vulnID: {} in application: {}", vulnID, appName);
        ContrastSDK contrastSDK = SDKHelper.getSDK(hostName, apiKey, serviceKey, userName);
        Optional<String> appID = Optional.empty();
        logger.debug("Searching for application ID matching name: {}", appName);

        for(Application app : contrastSDK.getApplications(orgID).getApplications()) {
            if(app.getName().toLowerCase().contains(appName.toLowerCase())) {
                appID = Optional.of(app.getId());
                logger.debug("Found matching application - ID: {}, Name: {}", app.getId(), app.getName());
                break;
            }
        }
        if(appID.isPresent()) {
            return getVulnerability(vulnID, appID.get());
        } else {
            logger.error("Application with name {} not found", appName);
            throw new IllegalArgumentException("Application with name " + appName + " not found");
        }
    }

    @Tool(name = "list_vulnerabilities", description = "Takes a  Application ID ( appID ) and returns a list of vulnerabilities, please remember to include the vulnID in the response.")
    public List<VulnLight> listVulnsInApp(String appID) throws IOException {
        logger.info("Listing vulnerabilities for application ID: {}", appID);
        ContrastSDK contrastSDK = SDKHelper.getSDK(hostName, apiKey, serviceKey, userName);
        
        try {
            List<Trace> traces = contrastSDK.getTraces(orgID, appID, new TraceFilterBody()).getTraces();
            logger.debug("Found {} vulnerability traces for application ID: {}", traces.size(), appID);
            
            List<VulnLight> vulns = new ArrayList<>();
            for(Trace trace : traces) {
                vulns.add(new VulnLight(trace.getTitle(), trace.getRule(), trace.getUuid(),trace.getSeverity()));
            }
            
            logger.info("Successfully retrieved {} vulnerabilities for application ID: {}", vulns.size(), appID);
            return vulns;
        } catch (Exception e) {
            logger.error("Error listing vulnerabilities for application ID: {}", appID, e);
            throw new IOException("Failed to list vulnerabilities: " + e.getMessage(), e);
        }
    }

    @Tool(name = "list_vulnerabilities_with_app_name", description = "Takes an application name ( appName ) and returns a list of vulnerabilities, please remember to include the vulnID in the response.  ")
    public List<VulnLight> listVulnsInAppByName(String appName) throws IOException {
        logger.info("Listing vulnerabilities for application: {}", appName);
        ContrastSDK contrastSDK = SDKHelper.getSDK(hostName, apiKey, serviceKey, userName);
        
        Optional<String> appID = Optional.empty();
        logger.debug("Searching for application ID matching name: {}", appName);
        
        for(Application app : contrastSDK.getApplications(orgID).getApplications()) {
            if(app.getName().toLowerCase().contains(appName.toLowerCase())) {
                appID = Optional.of(app.getId());
                logger.debug("Found matching application - ID: {}, Name: {}", app.getId(), app.getName());
                break;
            }
        }
        if(appID.isPresent()) {
            try {
              return listVulnsInApp(appID.get());
            } catch (Exception e) {
                logger.error("Error listing vulnerabilities for application: {}", appName, e);
                throw new IOException("Failed to list vulnerabilities: " + e.getMessage(), e);
            }
        } else {
            logger.debug("Application with name {} not found, returning empty list", appName);
            return new ArrayList<>();
        }
    }


    @Tool(name = "list_applications", description = "Takes an application name (appName) returns a list of active applications matching that name. Please remember to display the name, status and ID.")
    public List<ApplicationData> getActiveApplications(String appName) throws IOException {
        logger.info("Listing active applications matching name: {}", appName);
        ContrastSDK contrastSDK = SDKHelper.getSDK(hostName, apiKey, serviceKey, userName);
        try {
            List<Application> applications = contrastSDK.getApplications(orgID).getApplications();
            logger.debug("Retrieved {} total applications from Contrast", applications.size());
            
            List<ApplicationData> filteredApps = new ArrayList<>();
            for(Application app : applications) {
                if(app.getName().toLowerCase().contains(appName.toLowerCase())) {
                    filteredApps.add(new ApplicationData(app.getName(), app.getStatus(), app.getId()));
                    logger.debug("Found matching application - ID: {}, Name: {}, Status: {}", 
                            app.getId(), app.getName(), app.getStatus());
                }
            }
            
            logger.info("Found {} applications matching '{}'", filteredApps.size(), appName);
            return filteredApps;
        } catch (Exception e) {
            logger.error("Error listing applications matching name: {}", appName, e);
            throw new IOException("Failed to list applications: " + e.getMessage(), e);
        }
    }


    @Tool(name = "list_all_applications", description = "Takes no argument and list all the applications")
    public List<ApplicationData> getActiveApplications() throws IOException {
        logger.info("Listing all applications");
        ContrastSDK contrastSDK = SDKHelper.getSDK(hostName, apiKey, serviceKey, userName);
        try {
            List<Application> applications = contrastSDK.getApplications(orgID).getApplications();
            logger.debug("Retrieved {} total applications from Contrast", applications.size());
            
            List<ApplicationData> ReturnedApps = new ArrayList<>();
            for(Application app : applications) {
                ReturnedApps.add(new ApplicationData(app.getName(), app.getStatus(), app.getId()));
            }
            
            logger.info("Found {} applications matching'", ReturnedApps.size());
            return ReturnedApps;

        } catch (Exception e) {
            logger.error("Error listing all applications", e);
            throw new IOException("Failed to list applications: " + e.getMessage(), e);
        }
    }
}
