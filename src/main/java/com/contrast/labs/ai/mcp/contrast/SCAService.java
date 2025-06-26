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

import com.contrast.labs.ai.mcp.contrast.sdkexstension.SDKExtension;
import com.contrast.labs.ai.mcp.contrast.sdkexstension.SDKHelper;
import com.contrast.labs.ai.mcp.contrast.sdkexstension.data.App;
import com.contrast.labs.ai.mcp.contrast.sdkexstension.data.CveData;
import com.contrast.labs.ai.mcp.contrast.sdkexstension.data.Library;
import com.contrast.labs.ai.mcp.contrast.sdkexstension.data.LibraryExtended;
import com.contrast.labs.ai.mcp.contrast.sdkexstension.data.application.Application;
import com.contrastsecurity.http.LibraryFilterForm;
import com.contrastsecurity.sdk.ContrastSDK;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class SCAService {

    private static final Logger logger = LoggerFactory.getLogger(SCAService.class);


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


    @Tool(name = "list_application_libraries_by_app_id", description = "Takes a application ID and returns the libraries used in the application, note if class usage count is 0 the library is unlikely to be used")
    public List<LibraryExtended> getApplicationLibrariesByID(String appID) throws IOException {
        logger.info("Retrieving libraries for application id: {}", appID);
        ContrastSDK contrastSDK = SDKHelper.getSDK(hostName, apiKey, serviceKey, userName,httpProxyHost, httpProxyPort);
        logger.debug("ContrastSDK initialized with host: {}", hostName);

        SDKExtension extendedSDK = new SDKExtension(contrastSDK);
        return SDKHelper.getLibsForID(appID,orgID, extendedSDK);

    }


    @Tool(name = "list_application_libraries", description = "takes a application name and returns the libraries used in the application, note if class usage count is 0 the library is unlikely to be used")
    public List<LibraryExtended> getApplicationLibraries(String app_name) throws IOException {
        logger.info("Retrieving libraries for application: {}", app_name);
        ContrastSDK contrastSDK = SDKHelper.getSDK(hostName, apiKey, serviceKey, userName,httpProxyHost, httpProxyPort);
        logger.debug("ContrastSDK initialized with host: {}", hostName);
        
        SDKExtension extendedSDK = new SDKExtension(contrastSDK);
        logger.debug("Searching for application ID matching name: {}", app_name);

        Optional<Application> application = SDKHelper.getApplicationByName(app_name, orgID, contrastSDK);
        if(application.isPresent()) {
            return SDKHelper.getLibsForID(application.get().getAppId(),orgID, extendedSDK);
        } else {
            logger.error("Application not found: {}", app_name);
            throw new IOException("Application not found");
        }
    }

    @Tool(name= "list_applications_vulnerable_to_cve", description = "takes a cve id and returns the applications and servers vulnerable to the cve. Please note if the application class usage is 0, its unlikely to be vulnerable")
    public CveData listCVESForApplication(String cveid) throws IOException {
        logger.info("Retrieving applications vulnerable to CVE: {}", cveid);
        ContrastSDK contrastSDK = SDKHelper.getSDK(hostName, apiKey, serviceKey, userName,httpProxyHost, httpProxyPort);

        logger.debug("ContrastSDK initialized with host: {}", hostName);
        contrastSDK.getLibrariesWithFilter(orgID, new LibraryFilterForm());
        try {
            SDKExtension extendedSDK = new SDKExtension(contrastSDK);
            CveData result = extendedSDK.getAppsForCVE(orgID, cveid);
            logger.info("Successfully retrieved data for CVE: {}, found {} vulnerable applications",
                    cveid, result != null && result.getApps() != null ? result.getApps().size() : 0);
            logger.info(result.toString());
            List<Library> vulnerableLibs = result.getLibraries();
            for(App app : result.getApps()) {
                List<LibraryExtended> libData = SDKHelper.getLibsForID(app.getApp_id(), orgID, extendedSDK);
                for(LibraryExtended lib:libData) {
                    for(Library vulnLib:vulnerableLibs) {
                        if(lib.getHash().equals(vulnLib.getHash())) {
                            if(lib.getClassedUsed()>0) {
                                app.setClassCount(lib.getClassCount());
                                app.setClassUsage(lib.getClassedUsed());
                            }
                        }
                    }
                }
            }
            return result;
        } catch (Exception e) {
            logger.error("Error retrieving applications vulnerable to CVE: {}", cveid, e);
            throw new IOException("Failed to retrieve CVE data: " + e.getMessage(), e);
        }
    }




}
