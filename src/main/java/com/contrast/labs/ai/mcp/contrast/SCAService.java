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

import com.contrast.labs.ai.mcp.contrast.sdkextension.SDKExtension;
import com.contrast.labs.ai.mcp.contrast.sdkextension.SDKHelper;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.App;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.CveData;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.Library;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.LibraryExtended;
import com.contrastsecurity.http.LibraryFilterForm;
import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class SCAService {
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

  @Tool(
      name = "list_application_libraries",
      description =
          "Takes an application ID and returns the libraries used in the application. Use"
              + " search_applications(name=...) to find the application ID from a name. Note: if"
              + " class usage count is 0 the library is unlikely to be used")
  public List<LibraryExtended> getApplicationLibrariesByID(String appID) throws IOException {
    if (!StringUtils.hasText(appID)) {
      throw new IllegalArgumentException("Application ID cannot be null or empty");
    }
    log.info("Retrieving libraries for application id: {}", appID);
    var contrastSDK =
        SDKHelper.getSDK(hostName, apiKey, serviceKey, userName, httpProxyHost, httpProxyPort);
    log.debug("ContrastSDK initialized with host: {}", hostName);

    var extendedSDK = new SDKExtension(contrastSDK);
    return SDKHelper.getLibsForID(appID, orgID, extendedSDK);
  }

  @Tool(
      name = "list_applications_by_cve",
      description =
          "takes a cve id and returns the applications and servers vulnerable to the cve. Please"
              + " note if the application class usage is 0, its unlikely to be vulnerable")
  public CveData listCVESForApplication(String cveid) throws IOException {
    log.info("Retrieving applications vulnerable to CVE: {}", cveid);
    var contrastSDK =
        SDKHelper.getSDK(hostName, apiKey, serviceKey, userName, httpProxyHost, httpProxyPort);

    log.debug("ContrastSDK initialized with host: {}", hostName);
    contrastSDK.getLibrariesWithFilter(orgID, new LibraryFilterForm());
    try {
      var extendedSDK = new SDKExtension(contrastSDK);
      var result = extendedSDK.getAppsForCVE(orgID, cveid);
      log.info(
          "Successfully retrieved data for CVE: {}, found {} vulnerable applications",
          cveid,
          result != null && result.getApps() != null ? result.getApps().size() : 0);
      log.info(result.toString());
      var vulnerableLibs = result.getLibraries();
      for (App app : result.getApps()) {
        var libData = SDKHelper.getLibsForID(app.getApp_id(), orgID, extendedSDK);
        for (LibraryExtended lib : libData) {
          for (Library vulnLib : vulnerableLibs) {
            if (lib.getHash().equals(vulnLib.getHash())) {
              if (lib.getClassedUsed() > 0) {
                app.setClassCount(lib.getClassCount());
                app.setClassUsage(lib.getClassedUsed());
              }
            }
          }
        }
      }
      return result;
    } catch (Exception e) {
      log.error("Error retrieving applications vulnerable to CVE: {}", cveid, e);
      throw new IOException("Failed to retrieve CVE data: " + e.getMessage(), e);
    }
  }
}
