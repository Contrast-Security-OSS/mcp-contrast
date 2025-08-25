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

import com.contrast.labs.ai.mcp.contrast.sdkexstension.SDKHelper;
import com.contrastsecurity.sdk.ContrastSDK;
import com.contrastsecurity.sdk.scan.Project;
import com.contrastsecurity.sdk.scan.Scan;
import com.contrastsecurity.sdk.scan.Scans;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

@Service
public class SastService {

    private static final Logger logger = LoggerFactory.getLogger(SastService.class);

    //This is the accepted risk tolerance level for running ADR operations. Default is ACCEPT_NO_RISK (0)
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


    @Tool(name = "list_Scan_Project", description = "takes a scan project name and returns the project details")
    public Project getScanProject(String projectName) throws IOException {
        acceptedRiskTolerance = RiskLevel.fromString(acceptedRiskToleranceStr).getValue();        
        logger.info("Risk level: low, and your accepted risk tolerance is set to: {}", acceptedRiskTolerance);
        if (acceptedRiskTolerance >= RiskLevel.LOW.getValue()) {
            logger.info("Retrieving scan project details for project: {}", projectName);
            ContrastSDK contrastSDK = SDKHelper.getSDK(hostName, apiKey, serviceKey, userName,httpProxyHost, httpProxyPort);
            logger.debug("ContrastSDK initialized with host: {}", hostName);
            
            try {
                Project project = contrastSDK.scan(orgID).projects().findByName(projectName)
                    .orElseThrow(() -> new IOException("Project not found"));
                logger.info("Successfully found project: {}", projectName);
                return project;
            } catch (IOException e) {
                logger.error("Failed to find project {}: {}", projectName, e.getMessage());
                throw e;
            }
        } else {
            logger.error("Your accepted Risk tolerance is too low to perform this operation. This risk level is: LOW, with your accepted risk tolerance set at: {}", acceptedRiskTolerance);
            throw new IOException("Accepted Risk Tolerance is too low to perform this operation. Required: LOW or higher.");
        }
    }

    @Tool(name = "list_Scan_Results", description = "takes a scan project name and returns the latest results in Sarif format")
    public String getLatestScanResult(String projectName) throws IOException {
        acceptedRiskTolerance = RiskLevel.fromString(acceptedRiskToleranceStr).getValue();        
        logger.info("Risk level: high, and your accepted risk tolerance is set to: {}", acceptedRiskTolerance);
        if (acceptedRiskTolerance >= RiskLevel.HIGH.getValue()) {
            logger.info("Retrieving latest scan results in SARIF format for project: {}", projectName);
            ContrastSDK contrastSDK = SDKHelper.getSDK(hostName, apiKey, serviceKey, userName,httpProxyHost, httpProxyPort);
            logger.debug("ContrastSDK initialized with host: {}", hostName);

            try {
                Project project = contrastSDK.scan(orgID).projects().findByName(projectName)
                    .orElseThrow(() -> new IOException("Project not found"));
                logger.debug("Found project with id: {}", project.id());
                
                Scans scans = contrastSDK.scan(orgID).scans(project.id());
                logger.debug("Retrieved scans for project, last scan id: {}", project.lastScanId());
                
                Scan scan = scans.get(project.lastScanId());
                logger.debug("Retrieved scan with id: {}", project.lastScanId());

                try (InputStream sarifStream = scan.sarif();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(sarifStream))) {
                    String result = reader.lines().collect(Collectors.joining(System.lineSeparator()));
                    logger.info("Successfully retrieved SARIF data for project: {}", projectName);
                    return result;
                }
            } catch (IOException e) {
                logger.error("Error retrieving SARIF data for project {}: {}", projectName, e.getMessage());
                throw e;
            }        
        } else {
            logger.error("Your accepted Risk tolerance is too low to perform this operation. This risk level is: HIGH, with your accepted risk tolerance set at: {}", acceptedRiskTolerance);
            throw new IOException("Accepted Risk Tolerance is too low to perform this operation. Required: HIGH or higher.");
        }
    }
}
