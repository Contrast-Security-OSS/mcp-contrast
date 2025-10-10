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
import com.contrast.labs.ai.mcp.contrast.sdkexstension.data.ProtectData;
import com.contrast.labs.ai.mcp.contrast.sdkexstension.data.application.Application;
import com.contrastsecurity.sdk.ContrastSDK;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Optional;

@Service
public class ADRService {

    private static final Logger logger = LoggerFactory.getLogger(ADRService.class);

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

    @Tool(name = "get_ADR_Protect_Rules", description = "takes a application name and returns the protect / adr rules for the application")
    public ProtectData getProtectData(String applicationName) throws IOException {
        acceptedRiskTolerance = RiskLevel.fromString(acceptedRiskToleranceStr).getValue();        
        logger.info("Risk level: medium, and your accepted risk tolerance is set to: {}", acceptedRiskTolerance);
        if (acceptedRiskTolerance >= RiskLevel.MEDIUM.getValue()) {
            logger.info("Starting retrieval of protection rules for application: {}", applicationName);
            long startTime = System.currentTimeMillis();

            try {
                ContrastSDK contrastSDK = SDKHelper.getSDK(hostName, apiKey, serviceKey, userName,httpProxyHost, httpProxyPort);
                logger.debug("ContrastSDK initialized successfully for application: {}", applicationName);

                // Get application ID from name
                logger.debug("Looking up application ID for name: {}", applicationName);
                Optional<Application> app = SDKHelper.getApplicationByName(applicationName, orgID, contrastSDK);
                if (app.isEmpty()) {
                    logger.warn("No application ID found for application: {}", applicationName);
                    return null;
                }
                logger.debug("Found application ID: {} for application: {}", app.get().getAppId(), applicationName);

                ProtectData result = getProtectDataByAppID(app.get().getAppId());
                long duration = System.currentTimeMillis() - startTime;
                logger.info("Completed retrieval of protection rules for application: {} (took {} ms)", applicationName, duration);
                return result;
            } catch (Exception e) {
                long duration = System.currentTimeMillis() - startTime;
                logger.error("Error retrieving protection rules for application: {} (after {} ms): {}",
                        applicationName, duration, e.getMessage(), e);
                throw e;
            }
        } else {
            logger.error("Your accepted Risk tolerance is too low to perform this operation. This risk level is: MEDIUM, with your accepted risk tolerance set at: {}", acceptedRiskTolerance);
            throw new IOException("Accepted Risk Tolerance is too low to perform this operation. Required: MEDIUM or higher.");
        }
    }


    @Tool(name = "get_ADR_Protect_Rules_by_app_id", description = "takes a application ID and returns the protect / adr rules for the application")
    public ProtectData getProtectDataByAppID(String appID) throws IOException {
        acceptedRiskTolerance = RiskLevel.fromString(acceptedRiskToleranceStr).getValue();        
        logger.info("Risk level: medium, and your accepted risk tolerance is set to: {}", acceptedRiskTolerance);
        if (acceptedRiskTolerance >= RiskLevel.MEDIUM.getValue()) {
            if (appID == null || appID.isEmpty()) {
                logger.error("Cannot retrieve protection rules - application ID is null or empty");
                throw new IllegalArgumentException("Application ID cannot be null or empty");
            }

            logger.info("Starting retrieval of protection rules for application ID: {}", appID);
            long startTime = System.currentTimeMillis();

            try {
                // Initialize ContrastSDK
                ContrastSDK contrastSDK = SDKHelper.getSDK(hostName, apiKey, serviceKey, userName,httpProxyHost, httpProxyPort);
                logger.debug("ContrastSDK initialized successfully for application ID: {}", appID);

                // Initialize SDK extension
                SDKExtension extendedSDK = new SDKExtension(contrastSDK);
                logger.debug("SDKExtension initialized successfully for application ID: {}", appID);

                // Get protect configuration
                logger.debug("Retrieving protection configuration for application ID: {}", appID);
                ProtectData protectData = extendedSDK.getProtectConfig(orgID, appID);
                long duration = System.currentTimeMillis() - startTime;

                if (protectData == null) {
                    logger.warn("No protection data returned for application ID: {} (took {} ms)", appID, duration);
                    return null;
                }

                int ruleCount = protectData.getRules() != null ? protectData.getRules().size() : 0;
                logger.info("Successfully retrieved {} protection rules for application ID: {} (took {} ms)",
                        ruleCount, appID, duration);
                return protectData;
            } catch (Exception e) {
                long duration = System.currentTimeMillis() - startTime;
                logger.error("Error retrieving protection rules for application ID: {} (after {} ms): {}",
                        appID, duration, e.getMessage(), e);
                throw e;
            }
        } else {
            logger.error("Your accepted Risk tolerance is too low to perform this operation. This risk level is: MEDIUM, with your accepted risk tolerance set at: {}", acceptedRiskTolerance);
            throw new IOException("Accepted Risk Tolerance is too low to perform this operation. Required: MEDIUM or higher.");
        }
    }

}
