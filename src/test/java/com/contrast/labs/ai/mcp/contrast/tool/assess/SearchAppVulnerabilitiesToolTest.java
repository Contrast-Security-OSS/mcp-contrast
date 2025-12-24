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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.contrast.labs.ai.mcp.contrast.config.ContrastConfig;
import com.contrast.labs.ai.mcp.contrast.data.VulnLight;
import com.contrast.labs.ai.mcp.contrast.mapper.VulnerabilityMapper;
import com.contrastsecurity.http.TraceFilterForm;
import com.contrastsecurity.models.Trace;
import com.contrastsecurity.models.Traces;
import com.contrastsecurity.sdk.ContrastSDK;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class SearchAppVulnerabilitiesToolTest {

  private static final String ORG_ID = "test-org-id";
  private static final String APP_ID = "app-123";

  private SearchAppVulnerabilitiesTool tool;
  private VulnerabilityMapper mapper;
  private ContrastConfig config;
  private ContrastSDK sdk;

  @BeforeEach
  void setUp() {
    mapper = mock();
    sdk = mock();
    config = mock();

    when(config.getSDK()).thenReturn(sdk);
    when(config.getOrgId()).thenReturn(ORG_ID);

    tool = new SearchAppVulnerabilitiesTool(mapper);
    ReflectionTestUtils.setField(tool, "config", config);
    ReflectionTestUtils.setField(tool, "maxTracesForSessionFiltering", 50000);
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

  // -- Success tests without session filtering --

  @Test
  void searchAppVulnerabilities_should_return_mapped_results_on_success() throws Exception {
    var trace = mock(Trace.class);
    var traces = mock(Traces.class);
    when(traces.getTraces()).thenReturn(List.of(trace));
    when(traces.getCount()).thenReturn(1);
    when(sdk.getTraces(eq(ORG_ID), eq(APP_ID), any(TraceFilterForm.class))).thenReturn(traces);

    var vulnLight = mock(VulnLight.class);
    when(mapper.toVulnLight(trace)).thenReturn(vulnLight);

    var result =
        tool.searchAppVulnerabilities(
            APP_ID, 1, 10, "CRITICAL", null, null, null, null, null, null, null, null, null);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.items()).hasSize(1);
    assertThat(result.totalItems()).isEqualTo(1);

    verify(sdk).getTraces(eq(ORG_ID), eq(APP_ID), any(TraceFilterForm.class));
    verify(mapper).toVulnLight(trace);
  }

  @Test
  void searchAppVulnerabilities_should_add_warning_when_api_returns_null() throws Exception {
    when(sdk.getTraces(eq(ORG_ID), eq(APP_ID), any(TraceFilterForm.class))).thenReturn(null);

    var result =
        tool.searchAppVulnerabilities(
            APP_ID, 1, 10, null, null, null, null, null, null, null, null, null, null);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.items()).isEmpty();
    assertThat(result.warnings()).anyMatch(w -> w.contains("API returned no trace data"));
  }

  @Test
  void searchAppVulnerabilities_should_use_default_statuses_with_warning() throws Exception {
    var traces = mock(Traces.class);
    when(traces.getTraces()).thenReturn(List.of());
    when(traces.getCount()).thenReturn(0);
    when(sdk.getTraces(eq(ORG_ID), eq(APP_ID), any(TraceFilterForm.class))).thenReturn(traces);

    var result =
        tool.searchAppVulnerabilities(
            APP_ID, 1, 10, null, null, null, null, null, null, null, null, null, null);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.warnings()).anyMatch(w -> w.contains("excluding Fixed and Remediated"));
  }

  @Test
  void searchAppVulnerabilities_should_handle_pagination() throws Exception {
    var trace1 = mock(Trace.class);
    var trace2 = mock(Trace.class);
    var traces = mock(Traces.class);
    when(traces.getTraces()).thenReturn(List.of(trace1, trace2));
    when(traces.getCount()).thenReturn(10); // Total of 10, showing 2
    when(sdk.getTraces(eq(ORG_ID), eq(APP_ID), any(TraceFilterForm.class))).thenReturn(traces);

    var vulnLight1 = mock(VulnLight.class);
    var vulnLight2 = mock(VulnLight.class);
    when(mapper.toVulnLight(trace1)).thenReturn(vulnLight1);
    when(mapper.toVulnLight(trace2)).thenReturn(vulnLight2);

    var result =
        tool.searchAppVulnerabilities(
            APP_ID, 1, 2, null, null, null, null, null, null, null, null, null, null);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.items()).hasSize(2);
    assertThat(result.totalItems()).isEqualTo(10);
    assertThat(result.hasMorePages()).isTrue();
  }

  // -- Tests with session filtering --

  @Test
  void searchAppVulnerabilities_should_accept_sessionMetadataName() throws Exception {
    var trace = mock(Trace.class);
    var traces = mock(Traces.class);
    when(traces.getTraces()).thenReturn(List.of(trace));
    when(traces.getCount()).thenReturn(1);
    when(sdk.getTraces(eq(ORG_ID), eq(APP_ID), any(TraceFilterForm.class))).thenReturn(traces);

    var vulnLight = mock(VulnLight.class);
    when(mapper.toVulnLight(trace)).thenReturn(vulnLight);

    // When session filtering is used, we fetch all pages and filter in-memory
    // For this test, we mock a single page with one trace that matches
    var result =
        tool.searchAppVulnerabilities(
            APP_ID, 1, 10, null, null, null, null, null, null, null, "branch", null, null);

    // Should succeed even with session metadata name
    assertThat(result.isSuccess()).isTrue();
  }

  @Test
  void searchAppVulnerabilities_should_accept_sessionMetadataName_and_value() throws Exception {
    var trace = mock(Trace.class);
    var traces = mock(Traces.class);
    when(traces.getTraces()).thenReturn(List.of(trace));
    when(traces.getCount()).thenReturn(1);
    when(sdk.getTraces(eq(ORG_ID), eq(APP_ID), any(TraceFilterForm.class))).thenReturn(traces);

    var vulnLight = mock(VulnLight.class);
    when(mapper.toVulnLight(trace)).thenReturn(vulnLight);

    var result =
        tool.searchAppVulnerabilities(
            APP_ID, 1, 10, null, null, null, null, null, null, null, "branch", "main", null);

    assertThat(result.isSuccess()).isTrue();
  }
}
