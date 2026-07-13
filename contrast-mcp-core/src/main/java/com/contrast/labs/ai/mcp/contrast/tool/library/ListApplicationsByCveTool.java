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

import com.contrast.labs.ai.mcp.contrast.client.ContrastApiClient;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.App;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.Cve;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.CveData;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.Library;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.LibraryExtended;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.Server;
import com.contrast.labs.ai.mcp.contrast.tool.base.SingleTool;
import com.contrast.labs.ai.mcp.contrast.tool.base.SingleToolResponse;
import com.contrast.labs.ai.mcp.contrast.tool.base.WarningCollector;
import com.contrast.labs.ai.mcp.contrast.tool.library.params.ListApplicationsByCveParams;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * MCP tool for finding applications affected by a specific CVE. Returns applications and their
 * vulnerable libraries, enriched with class usage data to help identify exploitability.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ListApplicationsByCveTool extends SingleTool<ListApplicationsByCveParams, CveData> {

  private final ContrastApiClient contrastApiClient;

  @Tool(
      name = "list_applications_by_cve",
      description =
          """
          Find applications and libraries affected by a specific CVE.

          Takes a CVE ID (e.g., CVE-2021-44228) and returns:
          - apps: List of applications containing vulnerable libraries
          - libraries: The vulnerable library versions
          - cve: CVE details including preferred CVSS score/severity and nested v2/v3 metrics

          For each application, class usage data is populated:
          - classCount: Total classes in the vulnerable library
          - classUsage: Number of classes actually used by the application

          Important: If classUsage is 0, the vulnerable library code is likely NOT
          being executed, significantly reducing exploitability risk. Prioritize
          remediation for applications where classUsage > 0.

          score is absent for v2-only CVEs because TeamServer does not provide a numeric v2
          base score; use severity and cvssv2 metrics in that case.

          Related tools:
          - list_application_libraries: Get all libraries for a specific application
          - search_applications: Find applications by name, tag, or metadata
          """)
  public SingleToolResponse<CveData> listApplicationsByCve(
      @ToolParam(description = "CVE identifier (e.g., CVE-2021-44228)") String cveId,
      ToolContext toolContext) {
    return executePipeline(() -> ListApplicationsByCveParams.of(cveId), toolContext);
  }

  public SingleToolResponse<CveData> listApplicationsByCve(String cveId) {
    return listApplicationsByCve(cveId, null);
  }

  @Override
  protected CveData doExecute(ListApplicationsByCveParams params, WarningCollector collector)
      throws Exception {

    log.debug("Retrieving applications vulnerable to CVE: {}", params.cveId());

    var cveData = contrastApiClient.getApplicationsByCve(params.cveId());
    if (cveData == null) {
      return null; // SingleTool converts this to notFound response
    }
    applyPreferredCvssSummary(cveData.getCve());
    dedupeServersById(cveData);

    var vulnerableLibs =
        cveData.getLibraries() != null ? cveData.getLibraries() : Collections.<Library>emptyList();
    var apps = cveData.getApps() != null ? cveData.getApps() : Collections.<App>emptyList();

    if (apps.isEmpty()) {
      collector.warn(
          "No applications found with this CVE. "
              + "The CVE may not affect any libraries in your organization, "
              + "or the CVE ID may be invalid.");
      return cveData;
    }

    log.debug(
        "Found {} applications vulnerable to {}, enriching with class usage data",
        apps.size(),
        params.cveId());

    var started = Instant.now();
    enrichAppsWithClassUsage(apps, vulnerableLibs, collector);
    log.debug(
        "Built CVE application library enrichment: appCount={}, vulnerableLibraryCount={},"
            + " durationMs={}",
        apps.size(),
        vulnerableLibs.size(),
        Duration.between(started, Instant.now()).toMillis());

    log.info(
        "Successfully retrieved CVE data for {}: {} vulnerable applications",
        params.cveId(),
        apps.size());

    return cveData;
  }

  private static void applyPreferredCvssSummary(Cve cve) {
    if (cve != null && cve.getCvssv3() != null) {
      cve.setScore(cve.getCvssv3().getBaseScore());
      cve.setSeverity(cve.getCvssv3().getSeverity());
    } else if (cve != null && cve.getCvssv2() != null) {
      cve.setSeverity(cve.getCvssv2().getSeverity());
    }
  }

  private void enrichAppsWithClassUsage(
      List<App> apps, List<Library> vulnerableLibs, WarningCollector collector) {

    for (App app : apps) {
      collector.tryRun(
          "Class usage data for application '" + app.getName() + "'",
          () -> {
            var appLibraries = contrastApiClient.getAllLibraries(app.getAppId());
            for (LibraryExtended appLib : appLibraries) {
              for (Library vulnLib : vulnerableLibs) {
                if (Objects.equals(appLib.getHash(), vulnLib.getHash())) {
                  if (appLib.getClassesUsed() > 0) {
                    app.setClassCount(appLib.getClassCount());
                    app.setClassUsage(appLib.getClassesUsed());
                  }
                  break;
                }
              }
            }
          });
    }
  }

  private static void dedupeServersById(CveData cveData) {
    if (cveData.getServers() == null) {
      return;
    }

    var dedupedServers =
        cveData.getServers().stream()
            .collect(
                Collectors.toMap(
                    Server::getServer_id,
                    Function.identity(),
                    (first, duplicate) -> first,
                    LinkedHashMap::new))
            .values()
            .stream()
            .toList();
    cveData.setServers(dedupedServers);
  }
}
