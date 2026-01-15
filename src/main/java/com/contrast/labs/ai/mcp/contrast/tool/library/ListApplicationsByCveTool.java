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
package com.contrast.labs.ai.mcp.contrast.tool.library;

import com.contrast.labs.ai.mcp.contrast.sdkextension.SDKExtension;
import com.contrast.labs.ai.mcp.contrast.sdkextension.SDKHelper;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.App;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.CveData;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.Library;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.LibraryExtended;
import com.contrast.labs.ai.mcp.contrast.tool.base.SingleTool;
import com.contrast.labs.ai.mcp.contrast.tool.base.SingleToolResponse;
import com.contrast.labs.ai.mcp.contrast.tool.library.params.ListApplicationsByCveParams;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * MCP tool for finding applications affected by a specific CVE. Returns applications and their
 * vulnerable libraries, enriched with class usage data to help identify exploitability.
 *
 * <p>Note: This tool will be ENHANCED to search_applications_by_cve with additional filtering
 * capabilities (environment filter, onlyUsedClasses flag).
 */
@Service
@Slf4j
public class ListApplicationsByCveTool extends SingleTool<ListApplicationsByCveParams, CveData> {

  @Tool(
      name = "list_applications_by_cve",
      description =
          """
          Find applications and libraries affected by a specific CVE.

          Takes a CVE ID (e.g., CVE-2021-44228) and returns:
          - apps: List of applications containing vulnerable libraries
          - libraries: The vulnerable library versions
          - cve: CVE details including severity and description

          For each application, class usage data is populated:
          - classCount: Total classes in the vulnerable library
          - classUsage: Number of classes actually used by the application

          Important: If classUsage is 0, the vulnerable library code is likely NOT
          being executed, significantly reducing exploitability risk. Prioritize
          remediation for applications where classUsage > 0.

          Related tools:
          - list_application_libraries: Get all libraries for a specific application
          - search_applications: Find applications by name, tag, or metadata
          """)
  public SingleToolResponse<CveData> listApplicationsByCve(
      @ToolParam(description = "CVE identifier (e.g., CVE-2021-44228)") String cveId) {
    return executePipeline(() -> ListApplicationsByCveParams.of(cveId));
  }

  @Override
  protected CveData doExecute(ListApplicationsByCveParams params, List<String> warnings)
      throws Exception {
    var sdk = getContrastSDK();
    var orgId = getOrgId();
    var extendedSDK = new SDKExtension(sdk);

    log.debug("Retrieving applications vulnerable to CVE: {}", params.cveId());

    var cveData = extendedSDK.getAppsForCVE(orgId, params.cveId());

    if (cveData == null) {
      return null; // SingleTool converts this to notFound response
    }

    // Handle null lists gracefully
    var vulnerableLibs =
        cveData.getLibraries() != null ? cveData.getLibraries() : Collections.<Library>emptyList();
    var apps = cveData.getApps() != null ? cveData.getApps() : Collections.<App>emptyList();

    if (apps.isEmpty()) {
      warnings.add(
          "No applications found with this CVE. "
              + "The CVE may not affect any libraries in your organization, "
              + "or the CVE ID may be invalid.");
      return cveData;
    }

    log.debug(
        "Found {} applications vulnerable to {}, enriching with class usage data",
        apps.size(),
        params.cveId());

    // Enrich each app with class usage data from the vulnerable library
    enrichAppsWithClassUsage(apps, vulnerableLibs, orgId, extendedSDK, warnings);

    log.info(
        "Successfully retrieved CVE data for {}: {} vulnerable applications",
        params.cveId(),
        apps.size());

    return cveData;
  }

  /**
   * Enriches application data with class usage information from vulnerable libraries. This helps
   * determine exploitability - if classUsage is 0, the vulnerable code is likely not being
   * executed.
   */
  private void enrichAppsWithClassUsage(
      List<App> apps,
      List<Library> vulnerableLibs,
      String orgId,
      SDKExtension extendedSDK,
      List<String> warnings) {

    for (App app : apps) {
      try {
        var appLibraries = SDKHelper.getLibsForID(app.getAppId(), orgId, extendedSDK);

        for (LibraryExtended appLib : appLibraries) {
          for (Library vulnLib : vulnerableLibs) {
            if (appLib.getHash().equals(vulnLib.getHash())) {
              // Only populate if the library is actually being used
              if (appLib.getClassesUsed() > 0) {
                app.setClassCount(appLib.getClassCount());
                app.setClassUsage(appLib.getClassesUsed());
              }
              break;
            }
          }
        }
      } catch (Exception e) {
        log.debug("Could not fetch library data for app {}: {}", app.getAppId(), e.getMessage());
        warnings.add(
            "Could not fetch class usage data for application '"
                + app.getName()
                + "': "
                + e.getMessage());
      }
    }
  }
}
