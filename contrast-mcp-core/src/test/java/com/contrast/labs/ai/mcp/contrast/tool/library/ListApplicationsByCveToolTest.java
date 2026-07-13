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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.contrast.labs.ai.mcp.contrast.client.ContrastApiClient;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.App;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.Cve;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.CveData;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.CvssV3;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.Library;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.LibraryExtended;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.Server;
import com.contrastsecurity.exceptions.HttpResponseException;
import com.contrastsecurity.exceptions.ResourceNotFoundException;
import com.contrastsecurity.sdk.internal.GsonFactory;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;

class ListApplicationsByCveToolTest {

  private static final String APP_ID = "app-123";
  private static final String CVE_ID = "CVE-2021-44228";
  private static final String LIBRARY_HASH = "hash-123";
  private static final String SECRET_BODY = "token=raw-token-value&apiKey=secret";
  private static final String CVSS_V3_CVE_RESPONSE =
      """
      {
        "cve": {
          "id": 12345,
          "name": "CVE-2021-44228",
          "description": "Apache Log4j remote code execution",
          "status": "unseen",
          "cwe": "CWE-502",
          "epssScore": 0.97565,
          "epssPercentile": 0.99996,
          "cisa": true,
          "cvssScoreSource": "NVD",
          "nvdPublished": 1639008000000,
          "nvdModified": 1716336000000,
          "firstSeen": 1640995200000,
          "cvssv2": {
            "severity": "High"
          },
          "cvssv3": {
            "baseScore": 9.3,
            "severity": "Critical"
          }
        },
        "apps": [],
        "libraries": [],
        "servers": []
      }
      """;
  private static final String CVSS_V2_CVE_RESPONSE =
      """
      {
        "cve": {
          "id": 2,
          "name": "CVE-2015-4000",
          "description": "TLS weak Diffie-Hellman key exchange",
          "status": "seen",
          "cvssv2": {
            "accessVector": "NETWORK",
            "accessComplexity": "MEDIUM",
            "authentication": "NONE",
            "confidentialityImpact": "PARTIAL",
            "integrityImpact": "NONE",
            "availabilityImpact": "NONE",
            "severity": "High"
          }
        },
        "apps": [],
        "libraries": [],
        "servers": []
      }
      """;

  private ContrastApiClient contrastApiClient;
  private ListApplicationsByCveTool tool;

  @BeforeEach
  void setUp() {
    contrastApiClient = mock();
    tool = new ListApplicationsByCveTool(contrastApiClient);
  }

  @Test
  void listApplicationsByCve_should_enrich_class_usage_through_contrast_api_client()
      throws Exception {
    var app = app("Orders", APP_ID);
    var cveData = cveData(app, vulnerableLibrary(LIBRARY_HASH));
    var appLibrary = applicationLibrary(LIBRARY_HASH);

    when(contrastApiClient.getApplicationsByCve(eq(CVE_ID))).thenReturn(cveData);
    when(contrastApiClient.getAllLibraries(eq(APP_ID))).thenReturn(List.of(appLibrary));

    var result = tool.listApplicationsByCve(CVE_ID, null);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.found()).isTrue();
    assertThat(result.data()).isSameAs(cveData);
    assertThat(result.data().getApps())
        .singleElement()
        .satisfies(
            enriched -> {
              assertThat(enriched.getClassCount()).isEqualTo(42);
              assertThat(enriched.getClassUsage()).isEqualTo(7);
            });
  }

  @Test
  void listApplicationsByCve_should_return_cvss_v3_score_when_teamserver_nests_cvss_data()
      throws Exception {
    var cveData = GsonFactory.create().fromJson(CVSS_V3_CVE_RESPONSE, CveData.class);
    when(contrastApiClient.getApplicationsByCve(eq(CVE_ID))).thenReturn(cveData);

    var result = tool.listApplicationsByCve(CVE_ID, null);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.data().getCve().getScore()).isEqualTo(9.3);
    assertThat(result.data().getCve().getSeverity()).isEqualTo("Critical");
    assertThat(result.data().getCve())
        .extracting(
            cve -> cve.getId(),
            cve -> cve.getCwe(),
            cve -> cve.getEpssScore(),
            cve -> cve.getEpssPercentile(),
            cve -> cve.getCisa(),
            cve -> cve.getCvssScoreSource(),
            cve -> cve.getNvdPublished(),
            cve -> cve.getNvdModified(),
            cve -> cve.getFirstSeen())
        .containsExactly(
            12345L,
            "CWE-502",
            0.97565,
            0.99996,
            true,
            "NVD",
            1639008000000L,
            1716336000000L,
            1640995200000L);
  }

  @Test
  void listApplicationsByCve_should_fallback_to_cvss_v2_severity_when_cvss_v3_is_absent()
      throws Exception {
    var cveData = GsonFactory.create().fromJson(CVSS_V2_CVE_RESPONSE, CveData.class);
    cveData.getCve().setScore(9.9);
    when(contrastApiClient.getApplicationsByCve(eq("CVE-2015-4000"))).thenReturn(cveData);

    var result = tool.listApplicationsByCve("CVE-2015-4000", null);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.data().getCve().getSeverity()).isEqualTo("High");
    assertThat(result.data().getCve().getScore()).isNull();
    assertThat(result.data().getCve().getCvssv2())
        .extracting(
            cvss -> cvss.getAccessVector(),
            cvss -> cvss.getAccessComplexity(),
            cvss -> cvss.getAuthentication(),
            cvss -> cvss.getConfidentialityImpact(),
            cvss -> cvss.getIntegrityImpact(),
            cvss -> cvss.getAvailabilityImpact())
        .containsExactly("NETWORK", "MEDIUM", "NONE", "PARTIAL", "NONE", "NONE");
  }

  @Test
  void listApplicationsByCve_should_leave_summary_absent_when_cve_has_no_cvss_data()
      throws Exception {
    var cve = new Cve();
    var cveData = cveData(cve);
    when(contrastApiClient.getApplicationsByCve(eq(CVE_ID))).thenReturn(cveData);

    var result = tool.listApplicationsByCve(CVE_ID, null);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.data().getCve()).isSameAs(cve);
    assertThat(result.data().getCve())
        .extracting(Cve::getScore, Cve::getSeverity)
        .containsExactly(null, null);
  }

  @Test
  void listApplicationsByCve_should_leave_severity_absent_when_cvss_v3_severity_is_absent()
      throws Exception {
    var cvssv3 = new CvssV3();
    cvssv3.setBaseScore(7.5);
    var cve = new Cve();
    cve.setCvssv3(cvssv3);
    var cveData = cveData(cve);
    when(contrastApiClient.getApplicationsByCve(eq(CVE_ID))).thenReturn(cveData);

    var result = tool.listApplicationsByCve(CVE_ID, null);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.data().getCve().getScore()).isEqualTo(7.5);
    assertThat(result.data().getCve().getSeverity()).isNull();
  }

  @Test
  void listApplicationsByCve_should_forward_tool_context_and_preserve_tool_name() throws Exception {
    var cveData = cveData(app("Orders", APP_ID), vulnerableLibrary(LIBRARY_HASH));
    var toolContext = new ToolContext(Map.of("requestId", "req-123"));
    var capturedContext = new AtomicReference<ToolContext>();
    tool.setAuthenticationStrategy(
        context -> {
          capturedContext.set(context);
          return () -> {};
        });

    when(contrastApiClient.getApplicationsByCve(eq(CVE_ID))).thenReturn(cveData);
    when(contrastApiClient.getAllLibraries(eq(APP_ID))).thenReturn(List.of());

    var result = tool.listApplicationsByCve(CVE_ID, toolContext);
    Method method =
        ListApplicationsByCveTool.class.getDeclaredMethod(
            "listApplicationsByCve", String.class, ToolContext.class);

    assertThat(result.isSuccess()).isTrue();
    assertThat(capturedContext.get()).isSameAs(toolContext);
    assertThat(method.getAnnotation(Tool.class).name()).isEqualTo("list_applications_by_cve");
  }

  @Test
  void listApplicationsByCve_should_return_validation_error_for_missing_cve_id() {
    var result = tool.listApplicationsByCve(null, null);

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors()).anyMatch(e -> e.contains("cveId") && e.contains("required"));
    verifyNoInteractions(contrastApiClient);
  }

  @Test
  void listApplicationsByCve_should_return_validation_error_for_invalid_cve_format() {
    var result = tool.listApplicationsByCve("not-a-cve", null);

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors()).anyMatch(e -> e.contains("cveId") && e.contains("CVE format"));
    verifyNoInteractions(contrastApiClient);
  }

  @Test
  void listApplicationsByCve_should_return_not_found_for_null_response() throws Exception {
    when(contrastApiClient.getApplicationsByCve(eq(CVE_ID))).thenReturn(null);

    var result = tool.listApplicationsByCve(CVE_ID, null);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.found()).isFalse();
    assertThat(result.data()).isNull();
  }

  @Test
  void listApplicationsByCve_should_warn_for_empty_apps_list() throws Exception {
    var cveData = new CveData();
    cveData.setApps(List.of());
    cveData.setLibraries(List.of());

    when(contrastApiClient.getApplicationsByCve(eq(CVE_ID))).thenReturn(cveData);

    var result = tool.listApplicationsByCve(CVE_ID, null);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.data()).isSameAs(cveData);
    assertThat(result.warnings()).anyMatch(w -> w.contains("No applications found"));
  }

  @Test
  void listApplicationsByCve_should_dedupe_top_level_servers_by_server_id() throws Exception {
    var cveData = cveData(app("Orders", APP_ID), vulnerableLibrary(LIBRARY_HASH));
    cveData.setServers(
        List.of(
            server(92326, "first-server"),
            server(92326, "duplicate-server"),
            server(92327, "another-server")));

    when(contrastApiClient.getApplicationsByCve(eq(CVE_ID))).thenReturn(cveData);
    when(contrastApiClient.getAllLibraries(eq(APP_ID))).thenReturn(List.of());

    var result = tool.listApplicationsByCve(CVE_ID, null);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.data().getServers())
        .extracting(Server::getServer_id)
        .containsExactly(92326, 92327);
    assertThat(result.data().getServers())
        .extracting(Server::getName)
        .containsExactly("first-server", "another-server");
  }

  @Test
  void listApplicationsByCve_should_preserve_empty_top_level_servers_list() throws Exception {
    var cveData = cveData(app("Orders", APP_ID), vulnerableLibrary(LIBRARY_HASH));
    cveData.setServers(List.of());

    when(contrastApiClient.getApplicationsByCve(eq(CVE_ID))).thenReturn(cveData);
    when(contrastApiClient.getAllLibraries(eq(APP_ID))).thenReturn(List.of());

    var result = tool.listApplicationsByCve(CVE_ID, null);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.data().getServers()).isEmpty();
  }

  @Test
  void listApplicationsByCve_should_handle_null_libraries_list_gracefully() throws Exception {
    var app = app("Orders", APP_ID);
    var cveData = new CveData();
    cveData.setApps(List.of(app));
    cveData.setLibraries(null);

    when(contrastApiClient.getApplicationsByCve(eq(CVE_ID))).thenReturn(cveData);
    when(contrastApiClient.getAllLibraries(eq(APP_ID)))
        .thenReturn(List.of(applicationLibrary(LIBRARY_HASH)));

    var result = tool.listApplicationsByCve(CVE_ID, null);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.data().getApps()).containsExactly(app);
  }

  @Test
  void listApplicationsByCve_should_return_not_found_for_unknown_cve() throws Exception {
    when(contrastApiClient.getApplicationsByCve(eq("CVE-2020-99999")))
        .thenThrow(
            new ResourceNotFoundException(
                "CVE not found", "GET", "/api/cve/CVE-2020-99999", "Not Found"));

    var result = tool.listApplicationsByCve("CVE-2020-99999", null);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.found()).isFalse();
    assertThat(result.data()).isNull();
    assertThat(result.warnings()).anyMatch(w -> w.toLowerCase().contains("not found"));
  }

  @Test
  void listApplicationsByCve_should_map_downstream_403_without_exposing_response_body()
      throws Exception {
    when(contrastApiClient.getApplicationsByCve(eq(CVE_ID)))
        .thenThrow(
            new HttpResponseException(
                "Forbidden", "GET", "/ng/org/libraries/cve", 403, "Forbidden", SECRET_BODY));

    var result = tool.listApplicationsByCve(CVE_ID, null);

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors())
        .containsExactly(
            "Access denied or resource not found. Verify credentials and that the resource ID is"
                + " correct.");
    assertThat(result.toString()).doesNotContain(SECRET_BODY, "/ng/org/libraries/cve");
  }

  @Test
  void listApplicationsByCve_should_map_downstream_429_without_exposing_response_body()
      throws Exception {
    when(contrastApiClient.getApplicationsByCve(eq(CVE_ID)))
        .thenThrow(
            new HttpResponseException(
                "Rate limited",
                "GET",
                "/ng/org/libraries/cve",
                429,
                "Too Many Requests",
                SECRET_BODY));

    var result = tool.listApplicationsByCve(CVE_ID, null);

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors()).containsExactly("Rate limit exceeded. Retry after a brief pause.");
    assertThat(result.toString()).doesNotContain(SECRET_BODY, "/ng/org/libraries/cve");
  }

  @Test
  void listApplicationsByCve_should_not_leak_exception_message_when_enrichment_fails()
      throws Exception {
    var cveData = cveData(app("Orders", APP_ID), vulnerableLibrary(LIBRARY_HASH));
    var secretMessage = "secret internal path /api/ng/org/app/libraries";

    when(contrastApiClient.getApplicationsByCve(eq(CVE_ID))).thenReturn(cveData);
    when(contrastApiClient.getAllLibraries(eq(APP_ID))).thenThrow(new IOException(secretMessage));

    var result = tool.listApplicationsByCve(CVE_ID, null);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.warnings()).anyMatch(w -> w.contains("(retrieval error)"));
    assertThat(result.toString()).doesNotContain(secretMessage);
  }

  private static CveData cveData(App app, Library library) {
    var cveData = new CveData();
    cveData.setApps(List.of(app));
    cveData.setLibraries(List.of(library));
    return cveData;
  }

  private static CveData cveData(Cve cve) {
    var cveData = new CveData();
    cveData.setCve(cve);
    cveData.setApps(List.of());
    cveData.setLibraries(List.of());
    return cveData;
  }

  private static App app(String name, String appId) {
    var app = new App();
    app.setName(name);
    app.setAppId(appId);
    return app;
  }

  private static Library vulnerableLibrary(String hash) {
    var library = new Library();
    library.setHash(hash);
    return library;
  }

  private static LibraryExtended applicationLibrary(String hash) {
    var library = new LibraryExtended();
    library.setHash(hash);
    library.setClassCount(42);
    library.setClassesUsed(7);
    return library;
  }

  private static Server server(int serverId) {
    return server(serverId, null);
  }

  private static Server server(int serverId, String name) {
    var server = new Server();
    server.setServer_id(serverId);
    server.setName(name);
    return server;
  }
}
