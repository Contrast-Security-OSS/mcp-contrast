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

class SearchVulnerabilitiesToolTest {

  private SearchVulnerabilitiesTool tool;
  private VulnerabilityMapper mapper;
  private ContrastSDKFactory sdkFactory;
  private ContrastSDK sdk;

  @BeforeEach
  void setUp() {
    mapper = mock();
    sdk = mock();
    sdkFactory = mock();

    when(sdkFactory.getSDK()).thenReturn(sdk);
    when(sdkFactory.getOrgId()).thenReturn("test-org-id");

    tool = new SearchVulnerabilitiesTool(mapper);
    ReflectionTestUtils.setField(tool, "sdkFactory", sdkFactory);
  }

  /**
   * Mocks the ContrastSDK to return the given JSON response when makeRequestWithBody is called for
   * the orgtraces endpoint.
   */
  private void mockSdkWithTracesResponse(String jsonResponse) throws Exception {
    when(sdk.makeRequestWithBody(
            eq(HttpMethod.POST), contains("/orgtraces/filter"), anyString(), eq(MediaType.JSON)))
        .thenReturn(new ByteArrayInputStream(jsonResponse.getBytes(StandardCharsets.UTF_8)));
  }

  // -- Validation tests --

  @Test
  void searchVulnerabilities_should_return_validation_error_without_sdk_call() {
    var result =
        tool.searchVulnerabilities(1, 10, "INVALID_SEVERITY", null, null, null, null, null, null);

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors()).anyMatch(e -> e.contains("Invalid severities"));

    // SDK should NOT be called when validation fails
    verifyNoInteractions(sdk);
    verifyNoInteractions(mapper);
  }

  @Test
  void searchVulnerabilities_should_collect_multiple_validation_errors() {
    var result =
        tool.searchVulnerabilities(
            1, 10, "INVALID", "BadStatus", null, "FAKE_ENV", null, null, null);

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors()).hasSizeGreaterThan(1);
    verifyNoInteractions(sdk);
  }

  // -- Success tests (refactored to mock at SDK boundary) --

  @Test
  void searchVulnerabilities_should_return_mapped_results_on_success() throws Exception {
    var json = TracesJsonFixture.singleTrace("trace-123", "SQL Injection", "CRITICAL");
    mockSdkWithTracesResponse(json);

    when(mapper.toVulnLight(any(Trace.class))).thenReturn(mock(VulnLight.class));

    var result = tool.searchVulnerabilities(1, 10, "CRITICAL", null, null, null, null, null, null);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.items()).hasSize(1);
    assertThat(result.totalItems()).isEqualTo(1);
    verify(mapper).toVulnLight(any(Trace.class));
  }

  @Test
  void searchVulnerabilities_should_add_warning_when_api_returns_null() throws Exception {
    mockSdkWithTracesResponse(TracesJsonFixture.nullTraces());

    var result = tool.searchVulnerabilities(1, 10, null, null, null, null, null, null, null);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.items()).isEmpty();
    assertThat(result.warnings()).anyMatch(w -> w.contains("API returned no trace data"));
  }

  @Test
  void searchVulnerabilities_should_include_default_status_warning() throws Exception {
    mockSdkWithTracesResponse(TracesJsonFixture.emptyTraces());

    var result = tool.searchVulnerabilities(1, 10, null, null, null, null, null, null, null);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.warnings()).anyMatch(w -> w.contains("excluding Fixed and Remediated"));
  }

  @Test
  void searchVulnerabilities_should_propagate_pagination_warnings() throws Exception {
    mockSdkWithTracesResponse(TracesJsonFixture.emptyTraces());

    // Invalid page number
    var result = tool.searchVulnerabilities(-1, 10, null, null, null, null, null, null, null);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.warnings()).anyMatch(w -> w.contains("Invalid page number"));
  }

  @Test
  void searchVulnerabilities_should_include_date_warning_when_dates_specified() throws Exception {
    mockSdkWithTracesResponse(TracesJsonFixture.emptyTraces());

    var result =
        tool.searchVulnerabilities(1, 10, null, null, null, null, "2025-01-01", null, null);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.warnings()).anyMatch(w -> w.contains("LAST ACTIVITY DATE"));
  }

  // -- Filter contract verification tests (Khorikov-compliant communication-based testing) --
  // These verify the contract with the external system (ContrastSDK HTTP API)

  @Test
  void searchVulnerabilities_should_pass_severity_filter_to_sdk() throws Exception {
    mockSdkWithTracesResponse(TracesJsonFixture.emptyTraces());

    tool.searchVulnerabilities(1, 10, "CRITICAL,HIGH", null, null, null, null, null, null);

    var bodyCaptor = ArgumentCaptor.forClass(String.class);
    verify(sdk).makeRequestWithBody(eq(HttpMethod.POST), anyString(), bodyCaptor.capture(), any());
    assertThat(bodyCaptor.getValue()).contains("CRITICAL").contains("HIGH");
  }

  @Test
  void searchVulnerabilities_should_pass_environment_filter_to_sdk() throws Exception {
    mockSdkWithTracesResponse(TracesJsonFixture.emptyTraces());

    tool.searchVulnerabilities(1, 10, null, null, null, "PRODUCTION", null, null, null);

    var bodyCaptor = ArgumentCaptor.forClass(String.class);
    verify(sdk).makeRequestWithBody(eq(HttpMethod.POST), anyString(), bodyCaptor.capture(), any());
    assertThat(bodyCaptor.getValue()).contains("PRODUCTION");
  }

  @Test
  void searchVulnerabilities_should_pass_date_filters_to_sdk() throws Exception {
    mockSdkWithTracesResponse(TracesJsonFixture.emptyTraces());

    tool.searchVulnerabilities(1, 10, null, null, null, null, "2025-01-01", "2025-01-31", null);

    var bodyCaptor = ArgumentCaptor.forClass(String.class);
    verify(sdk).makeRequestWithBody(eq(HttpMethod.POST), anyString(), bodyCaptor.capture(), any());
    assertThat(bodyCaptor.getValue()).contains("startDate").contains("endDate");
  }

  @Test
  void searchVulnerabilities_should_pass_vuln_tags_filter_to_sdk() throws Exception {
    mockSdkWithTracesResponse(TracesJsonFixture.emptyTraces());

    tool.searchVulnerabilities(1, 10, null, null, null, null, null, null, "reviewed,urgent");

    var bodyCaptor = ArgumentCaptor.forClass(String.class);
    verify(sdk).makeRequestWithBody(eq(HttpMethod.POST), anyString(), bodyCaptor.capture(), any());
    assertThat(bodyCaptor.getValue()).contains("reviewed").contains("urgent");
  }

  @Test
  void searchVulnerabilities_should_pass_vuln_types_filter_to_sdk() throws Exception {
    mockSdkWithTracesResponse(TracesJsonFixture.emptyTraces());

    tool.searchVulnerabilities(1, 10, null, null, "sql-injection,xss", null, null, null, null);

    var bodyCaptor = ArgumentCaptor.forClass(String.class);
    verify(sdk).makeRequestWithBody(eq(HttpMethod.POST), anyString(), bodyCaptor.capture(), any());
    assertThat(bodyCaptor.getValue()).contains("sql-injection").contains("xss");
  }

  @Test
  void searchVulnerabilities_should_pass_pagination_params_in_url() throws Exception {
    mockSdkWithTracesResponse(TracesJsonFixture.emptyTraces());

    tool.searchVulnerabilities(3, 25, null, null, null, null, null, null, null);

    var urlCaptor = ArgumentCaptor.forClass(String.class);
    verify(sdk).makeRequestWithBody(any(), urlCaptor.capture(), anyString(), any());
    assertThat(urlCaptor.getValue()).contains("limit=25").contains("offset=50");
  }
}
