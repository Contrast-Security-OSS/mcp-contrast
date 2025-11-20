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

import com.contrast.labs.ai.mcp.contrast.sdkextension.SDKHelper;
import com.contrastsecurity.sdk.scan.Project;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SastService {
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
      name = "get_scan_project",
      description = "takes a scan project name and returns the project details")
  public Project getScanProject(String projectName) throws IOException {
    log.info("Retrieving scan project details for project: {}", projectName);
    var contrastSDK =
        SDKHelper.getSDK(hostName, apiKey, serviceKey, userName, httpProxyHost, httpProxyPort);
    log.debug("ContrastSDK initialized with host: {}", hostName);

    try {
      var project =
          contrastSDK
              .scan(orgID)
              .projects()
              .findByName(projectName)
              .orElseThrow(() -> new IOException("Project not found"));
      log.info("Successfully found project: {}", projectName);
      return project;
    } catch (IOException e) {
      log.error("Failed to find project {}: {}", projectName, e.getMessage());
      throw e;
    }
  }

  @Tool(
      name = "get_scan_results",
      description = "takes a scan project name and returns the latest results in Sarif format")
  public String getLatestScanResult(String projectName) throws IOException {
    log.info("Retrieving latest scan results in SARIF format for project: {}", projectName);
    var contrastSDK =
        SDKHelper.getSDK(hostName, apiKey, serviceKey, userName, httpProxyHost, httpProxyPort);
    log.debug("ContrastSDK initialized with host: {}", hostName);

    try {
      var project =
          contrastSDK
              .scan(orgID)
              .projects()
              .findByName(projectName)
              .orElseThrow(() -> new IOException("Project not found"));
      log.debug("Found project with id: {}", project.id());

      // Check if project has any completed scans
      if (project.lastScanId() == null) {
        var errorMsg =
            String.format(
                "No scan results available for project: %s. Project exists but has no completed"
                    + " scans.",
                projectName);
        log.warn(errorMsg);
        throw new IOException(errorMsg);
      }

      var scans = contrastSDK.scan(orgID).scans(project.id());
      log.debug("Retrieved scans for project, last scan id: {}", project.lastScanId());

      var scan = scans.get(project.lastScanId());
      if (scan == null) {
        var errorMsg =
            String.format(
                "No scan results available for project: %s. Scan ID %s not found.",
                projectName, project.lastScanId());
        log.warn(errorMsg);
        throw new IOException(errorMsg);
      }
      log.debug("Retrieved scan with id: {}", project.lastScanId());

      try (InputStream sarifStream = scan.sarif();
          BufferedReader reader = new BufferedReader(new InputStreamReader(sarifStream))) {
        var result = reader.lines().collect(Collectors.joining(System.lineSeparator()));
        log.info("Successfully retrieved SARIF data for project: {}", projectName);
        return result;
      }
    } catch (IOException e) {
      log.error("Error retrieving SARIF data for project {}: {}", projectName, e.getMessage());
      throw e;
    }
  }
}
