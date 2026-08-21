/*
 * Copyright 2026 Contrast Security
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
import com.contrast.labs.ai.mcp.contrast.tool.base.NoticeCollector;
import com.contrast.labs.ai.mcp.contrast.tool.base.SingleTool;
import com.contrast.labs.ai.mcp.contrast.tool.base.SingleToolResponse;
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

  private static final int HTTP_INTERNAL_SERVER_ERROR = 500;
  private static final String INTERNAL_SERVER_ERROR_MESSAGE =
      "The service returned an error. This typically happens for CVEs that the SCA library data"
          + " does not recognize. Verify the CVE identifier is correct and retry later if the"
          + " service is failing.";

  private final ContrastApiClient contrastApiClient;

  @Tool(
      name = "list_applications_by_cve",
      description =
          """
          List applications affected by one CVE, with the vulnerable library versions and
          per-application class usage. classUsage 0 or absent means no classes from the vulnerable
          library were seen loaded, so exploitation is unlikely; prioritize applications with
          classUsage above 0. Use list_application_libraries for the reverse direction, all
          libraries of one application. lastSeen and server status reflect last-known agent
          reports and can lag live state; search_servers is fresher for current server state.
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
  protected String mapHttpErrorCode(int code) {
    if (code == HTTP_INTERNAL_SERVER_ERROR) {
      return INTERNAL_SERVER_ERROR_MESSAGE;
    }
    return super.mapHttpErrorCode(code);
  }

  @Override
  protected CveData doExecute(ListApplicationsByCveParams params, NoticeCollector collector)
      throws Exception {

    log.debug("Retrieving applications vulnerable to CVE: {}", params.cveId());

    var cveData = contrastApiClient.getApplicationsByCve(params.cveId());
    if (cveData == null) {
      return null; // SingleTool converts this to notFound response
    }
    applyPreferredCvssSummary(cveData.getCve());
    if (hasOnlyCvssV2(cveData.getCve())) {
      collector.notice(
          "score is omitted for CVEs with only CVSS v2 data; use severity and the cvssv2"
              + " metrics.");
    }
    dedupeServersById(cveData);

    var vulnerableLibs =
        cveData.getLibraries() != null ? cveData.getLibraries() : Collections.<Library>emptyList();
    var apps = cveData.getApps() != null ? cveData.getApps() : Collections.<App>emptyList();

    if (apps.isEmpty()) {
      collector.notice(
          "No applications found with this CVE. "
              + "The CVE may not affect any libraries in your organization, "
              + "or the CVE ID may be invalid.");
      return cveData;
    }

    noticeNeverObservedApps(apps, collector);

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

  // TeamServer sends last_seen 0 for applications that have never reported agent activity, and
  // App.lastSeen is a primitive long, so the zero sentinel always serializes (Jira AIML-1331).
  private static void noticeNeverObservedApps(List<App> apps, NoticeCollector collector) {
    var neverObserved =
        apps.stream().filter(app -> app.getLastSeen() == 0).map(App::getName).toList();
    if (!neverObserved.isEmpty()) {
      collector.notice(
          "lastSeen of 0 means the application has never been observed running, typically a"
              + " static or SCA-only upload: "
              + String.join(", ", neverObserved));
    }
  }

  private static void applyPreferredCvssSummary(Cve cve) {
    if (cve == null) {
      return;
    }
    if (cve.getCvssv3() != null) {
      cve.setScore(cve.getCvssv3().getBaseScore());
      cve.setSeverity(cve.getCvssv3().getSeverity());
    } else if (cve.getCvssv2() != null) {
      cve.setScore(null);
      cve.setSeverity(cve.getCvssv2().getSeverity());
    }
  }

  private static boolean hasOnlyCvssV2(Cve cve) {
    return cve != null && cve.getCvssv3() == null && cve.getCvssv2() != null;
  }

  private void enrichAppsWithClassUsage(
      List<App> apps, List<Library> vulnerableLibs, NoticeCollector collector) {

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
