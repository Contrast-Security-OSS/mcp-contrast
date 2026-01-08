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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.contrast.labs.ai.mcp.contrast.config.ContrastSDKFactory;
import com.contrast.labs.ai.mcp.contrast.data.VulnLight;
import com.contrast.labs.ai.mcp.contrast.mapper.VulnerabilityMapper;
import com.contrast.labs.ai.mcp.contrast.sdkextension.SDKExtension;
import com.contrastsecurity.models.Trace;
import com.contrastsecurity.models.TraceFilterBody;
import com.contrastsecurity.models.Traces;
import com.contrastsecurity.sdk.ContrastSDK;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
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
  void searchVulnerabilities_should_return_mapped_results_on_success() throws Exception {
    // Setup mock trace
    var trace = mock(Trace.class);
    var traces = mock(Traces.class);
    when(traces.getTraces()).thenReturn(List.of(trace));
    when(traces.getCount()).thenReturn(1);

    var vulnLight = mock(VulnLight.class);
    when(mapper.toVulnLight(trace)).thenReturn(vulnLight);

    try (MockedConstruction<SDKExtension> mocked =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              when(mock.getTracesInOrg(
                      anyString(), any(TraceFilterBody.class), anyInt(), anyInt(), any()))
                  .thenReturn(traces);
            })) {

      var result =
          tool.searchVulnerabilities(1, 10, "CRITICAL", null, null, null, null, null, null);

      assertThat(result.isSuccess()).isTrue();
      assertThat(result.items()).hasSize(1);
      assertThat(result.totalItems()).isEqualTo(1);
      verify(mapper).toVulnLight(trace);
    }
  }

  @Test
  void searchVulnerabilities_should_add_warning_when_api_returns_null() throws Exception {
    try (MockedConstruction<SDKExtension> mocked =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              when(mock.getTracesInOrg(
                      anyString(), any(TraceFilterBody.class), anyInt(), anyInt(), any()))
                  .thenReturn(null);
            })) {

      var result = tool.searchVulnerabilities(1, 10, null, null, null, null, null, null, null);

      assertThat(result.isSuccess()).isTrue();
      assertThat(result.items()).isEmpty();
      assertThat(result.warnings()).anyMatch(w -> w.contains("API returned no trace data"));
    }
  }

  @Test
  void searchVulnerabilities_should_include_default_status_warning() throws Exception {
    var traces = mock(Traces.class);
    when(traces.getTraces()).thenReturn(List.of());
    when(traces.getCount()).thenReturn(0);

    try (MockedConstruction<SDKExtension> mocked =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              when(mock.getTracesInOrg(
                      anyString(), any(TraceFilterBody.class), anyInt(), anyInt(), any()))
                  .thenReturn(traces);
            })) {

      var result = tool.searchVulnerabilities(1, 10, null, null, null, null, null, null, null);

      assertThat(result.isSuccess()).isTrue();
      // Should have warning about default statuses
      assertThat(result.warnings()).anyMatch(w -> w.contains("excluding Fixed and Remediated"));
    }
  }

  @Test
  void searchVulnerabilities_should_propagate_pagination_warnings() throws Exception {
    var traces = mock(Traces.class);
    when(traces.getTraces()).thenReturn(List.of());
    when(traces.getCount()).thenReturn(0);

    try (MockedConstruction<SDKExtension> mocked =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              when(mock.getTracesInOrg(
                      anyString(), any(TraceFilterBody.class), anyInt(), anyInt(), any()))
                  .thenReturn(traces);
            })) {

      // Invalid page number
      var result = tool.searchVulnerabilities(-1, 10, null, null, null, null, null, null, null);

      assertThat(result.isSuccess()).isTrue();
      assertThat(result.warnings()).anyMatch(w -> w.contains("Invalid page number"));
    }
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

  @Test
  void searchVulnerabilities_should_include_date_warning_when_dates_specified() throws Exception {
    var traces = mock(Traces.class);
    when(traces.getTraces()).thenReturn(List.of());
    when(traces.getCount()).thenReturn(0);

    try (MockedConstruction<SDKExtension> mocked =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              when(mock.getTracesInOrg(
                      anyString(), any(TraceFilterBody.class), anyInt(), anyInt(), any()))
                  .thenReturn(traces);
            })) {

      var result =
          tool.searchVulnerabilities(1, 10, null, null, null, null, "2025-01-01", null, null);

      assertThat(result.isSuccess()).isTrue();
      assertThat(result.warnings()).anyMatch(w -> w.contains("LAST ACTIVITY DATE"));
    }
  }
}
