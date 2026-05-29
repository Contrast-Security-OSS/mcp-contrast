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
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.CveData;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.Library;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.LibraryExtended;
import com.contrastsecurity.exceptions.HttpResponseException;
import com.contrastsecurity.exceptions.ResourceNotFoundException;
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
}
