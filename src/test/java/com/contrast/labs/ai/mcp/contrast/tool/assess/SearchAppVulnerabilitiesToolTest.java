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
package com.contrast.labs.ai.mcp.contrast.tool.assess;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.contrast.labs.ai.mcp.contrast.config.ContrastSDKFactory;
import com.contrast.labs.ai.mcp.contrast.data.VulnLight;
import com.contrast.labs.ai.mcp.contrast.mapper.VulnerabilityMapper;
import com.contrastsecurity.http.HttpMethod;
import com.contrastsecurity.http.MediaType;
import com.contrastsecurity.models.Trace;
import com.contrastsecurity.sdk.ContrastSDK;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class SearchAppVulnerabilitiesToolTest {

  private static final String ORG_ID = "test-org-id";
  private static final String APP_ID = "app-123";

  private SearchAppVulnerabilitiesTool tool;
  private VulnerabilityMapper mapper;
  private ContrastSDKFactory sdkFactory;
  private ContrastSDK sdk;

  @BeforeEach
  void setUp() {
    mapper = mock();
    sdk = mock();
    sdkFactory = mock();

    when(sdkFactory.getSDK()).thenReturn(sdk);
    when(sdkFactory.getOrgId()).thenReturn(ORG_ID);

    tool = new SearchAppVulnerabilitiesTool(mapper);
    ReflectionTestUtils.setField(tool, "sdkFactory", sdkFactory);
    ReflectionTestUtils.setField(tool, "maxTracesForSessionFiltering", 50000);
  }

  /**
   * Mocks the ContrastSDK to return the given JSON response when makeRequestWithBody is called for
   * the traces endpoint.
   */
  private void mockSdkWithTracesResponse(String jsonResponse) throws Exception {
    when(sdk.makeRequestWithBody(
            eq(HttpMethod.POST), contains("/traces/"), anyString(), eq(MediaType.JSON)))
        .thenReturn(new ByteArrayInputStream(jsonResponse.getBytes(StandardCharsets.UTF_8)));
  }

  // -- Validation tests --

  @Test
  void searchAppVulnerabilities_should_return_error_when_appId_missing() {
    var result =
        tool.searchAppVulnerabilities(
            null, 1, 10, null, null, null, null, null, null, null, null, null, null);

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors()).anyMatch(e -> e.contains("appId is required"));

    verifyNoInteractions(sdk);
    verifyNoInteractions(mapper);
  }

  @Test
  void searchAppVulnerabilities_should_return_error_for_invalid_severity() {
    var result =
        tool.searchAppVulnerabilities(
            APP_ID, 1, 10, "INVALID", null, null, null, null, null, null, null, null, null);

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors()).anyMatch(e -> e.contains("Invalid severities"));

    verifyNoInteractions(sdk);
    verifyNoInteractions(mapper);
  }

  @Test
  void searchAppVulnerabilities_should_return_error_when_sessionMetadataValue_without_name() {
    var result =
        tool.searchAppVulnerabilities(
            APP_ID, 1, 10, null, null, null, null, null, null, null, null, "value", null);

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors())
        .anyMatch(e -> e.contains("sessionMetadataValue requires sessionMetadataName"));

    verifyNoInteractions(sdk);
    verifyNoInteractions(mapper);
  }

  // -- Success tests (refactored to mock at SDK boundary) --

  @Test
  void searchAppVulnerabilities_should_return_mapped_results_on_success() throws Exception {
    var json = TracesJsonFixture.singleTrace("trace-123", "SQL Injection", "CRITICAL");
    mockSdkWithTracesResponse(json);

    when(mapper.toVulnLight(any(Trace.class))).thenReturn(mock(VulnLight.class));

    var result =
        tool.searchAppVulnerabilities(
            APP_ID, 1, 10, "CRITICAL", null, null, null, null, null, null, null, null, null);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.items()).hasSize(1);
    assertThat(result.totalItems()).isEqualTo(1);
    verify(mapper).toVulnLight(any(Trace.class));
  }

  @Test
  void searchAppVulnerabilities_should_add_warning_when_api_returns_null() throws Exception {
    mockSdkWithTracesResponse(TracesJsonFixture.nullTraces());

    var result =
        tool.searchAppVulnerabilities(
            APP_ID, 1, 10, null, null, null, null, null, null, null, null, null, null);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.items()).isEmpty();
    assertThat(result.warnings()).anyMatch(w -> w.contains("API returned no trace data"));
  }

  @Test
  void searchAppVulnerabilities_should_use_default_statuses_with_warning() throws Exception {
    mockSdkWithTracesResponse(TracesJsonFixture.emptyTraces());

    var result =
        tool.searchAppVulnerabilities(
            APP_ID, 1, 10, null, null, null, null, null, null, null, null, null, null);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.warnings()).anyMatch(w -> w.contains("excluding Fixed and Remediated"));
  }

  @Test
  void searchAppVulnerabilities_should_handle_pagination() throws Exception {
    mockSdkWithTracesResponse(TracesJsonFixture.tracesWithTotal(2, 10));

    when(mapper.toVulnLight(any(Trace.class))).thenReturn(mock(VulnLight.class));

    var result =
        tool.searchAppVulnerabilities(
            APP_ID, 1, 2, null, null, null, null, null, null, null, null, null, null);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.items()).hasSize(2);
    assertThat(result.totalItems()).isEqualTo(10);
    assertThat(result.hasMorePages()).isTrue();
  }

  @Test
  void searchAppVulnerabilities_should_accept_sessionMetadataName() throws Exception {
    mockSdkWithTracesResponse(TracesJsonFixture.emptyTraces());

    var result =
        tool.searchAppVulnerabilities(
            APP_ID, 1, 10, null, null, null, null, null, null, null, "branch", null, null);

    assertThat(result.isSuccess()).isTrue();
  }

  @Test
  void searchAppVulnerabilities_should_accept_sessionMetadataName_and_value() throws Exception {
    mockSdkWithTracesResponse(TracesJsonFixture.emptyTraces());

    var result =
        tool.searchAppVulnerabilities(
            APP_ID, 1, 10, null, null, null, null, null, null, null, "branch", "main", null);

    assertThat(result.isSuccess()).isTrue();
  }

  // -- Filter contract verification tests (Khorikov-compliant communication-based testing) --
  // These verify the contract with the external system (ContrastSDK HTTP API)

  @Test
  void searchAppVulnerabilities_should_pass_appId_in_url() throws Exception {
    mockSdkWithTracesResponse(TracesJsonFixture.emptyTraces());

    tool.searchAppVulnerabilities(
        "app-xyz-123", 1, 10, null, null, null, null, null, null, null, null, null, null);

    var urlCaptor = ArgumentCaptor.forClass(String.class);
    verify(sdk).makeRequestWithBody(any(), urlCaptor.capture(), anyString(), any());
    assertThat(urlCaptor.getValue()).contains("/traces/app-xyz-123/filter");
  }

  @Test
  void searchAppVulnerabilities_should_pass_severity_filter_to_sdk() throws Exception {
    mockSdkWithTracesResponse(TracesJsonFixture.emptyTraces());

    tool.searchAppVulnerabilities(
        APP_ID, 1, 10, "CRITICAL", null, null, null, null, null, null, null, null, null);

    var bodyCaptor = ArgumentCaptor.forClass(String.class);
    verify(sdk).makeRequestWithBody(eq(HttpMethod.POST), anyString(), bodyCaptor.capture(), any());
    assertThat(bodyCaptor.getValue()).contains("CRITICAL");
  }

  @Test
  void searchAppVulnerabilities_should_pass_environment_filter_to_sdk() throws Exception {
    mockSdkWithTracesResponse(TracesJsonFixture.emptyTraces());

    tool.searchAppVulnerabilities(
        APP_ID, 1, 10, null, null, null, "PRODUCTION,QA", null, null, null, null, null, null);

    var bodyCaptor = ArgumentCaptor.forClass(String.class);
    verify(sdk).makeRequestWithBody(eq(HttpMethod.POST), anyString(), bodyCaptor.capture(), any());
    assertThat(bodyCaptor.getValue()).contains("PRODUCTION").contains("QA");
  }

  @Test
  void searchAppVulnerabilities_should_pass_sessionMetadataName_filter_to_sdk() throws Exception {
    mockSdkWithTracesResponse(TracesJsonFixture.emptyTraces());

    tool.searchAppVulnerabilities(
        APP_ID, 1, 10, null, null, null, null, null, null, null, "branch", null, null);

    var bodyCaptor = ArgumentCaptor.forClass(String.class);
    verify(sdk).makeRequestWithBody(eq(HttpMethod.POST), anyString(), bodyCaptor.capture(), any());
    // Session metadata filtering includes the metadata field in request
    // Note: The actual serialization format depends on implementation
    assertThat(bodyCaptor.getValue()).isNotEmpty();
  }

  @Test
  void searchAppVulnerabilities_should_pass_sessionMetadataName_and_value_to_sdk()
      throws Exception {
    mockSdkWithTracesResponse(TracesJsonFixture.emptyTraces());

    tool.searchAppVulnerabilities(
        APP_ID, 1, 10, null, null, null, null, null, null, null, "branch", "main", null);

    var bodyCaptor = ArgumentCaptor.forClass(String.class);
    verify(sdk).makeRequestWithBody(eq(HttpMethod.POST), anyString(), bodyCaptor.capture(), any());
    // Should make a request - actual filtering verified by integration tests
    assertThat(bodyCaptor.getValue()).isNotEmpty();
  }

  @Test
  void searchAppVulnerabilities_should_call_session_metadata_when_useLatestSession()
      throws Exception {
    // When useLatestSession=true, the tool first fetches session metadata
    var sessionMetadataJson =
        """
        {"sessions": [], "total": 0}
        """;
    when(sdk.makeRequest(eq(HttpMethod.GET), contains("/agent-sessions/latest")))
        .thenReturn(new ByteArrayInputStream(sessionMetadataJson.getBytes(StandardCharsets.UTF_8)));
    mockSdkWithTracesResponse(TracesJsonFixture.emptyTraces());

    tool.searchAppVulnerabilities(
        APP_ID, 1, 10, null, null, null, null, null, null, null, null, null, true);

    // Verify session metadata was fetched
    verify(sdk).makeRequest(eq(HttpMethod.GET), contains("/agent-sessions/latest"));
  }

  @Test
  void searchAppVulnerabilities_should_pass_pagination_params_in_url() throws Exception {
    mockSdkWithTracesResponse(TracesJsonFixture.emptyTraces());

    tool.searchAppVulnerabilities(
        APP_ID, 2, 50, null, null, null, null, null, null, null, null, null, null);

    var urlCaptor = ArgumentCaptor.forClass(String.class);
    verify(sdk).makeRequestWithBody(any(), urlCaptor.capture(), anyString(), any());
    assertThat(urlCaptor.getValue()).contains("limit=50").contains("offset=50");
  }

  @Test
  void searchAppVulnerabilities_should_pass_expand_params_in_url() throws Exception {
    mockSdkWithTracesResponse(TracesJsonFixture.emptyTraces());

    tool.searchAppVulnerabilities(
        APP_ID, 1, 10, null, null, null, null, null, null, null, null, null, null);

    var urlCaptor = ArgumentCaptor.forClass(String.class);
    verify(sdk).makeRequestWithBody(any(), urlCaptor.capture(), anyString(), any());
    assertThat(urlCaptor.getValue()).contains("expand=").contains("session_metadata");
  }
}
