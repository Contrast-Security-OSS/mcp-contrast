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
package com.contrast.labs.ai.mcp.contrast.sdkextension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.contrast.labs.ai.mcp.contrast.sdkextension.data.application.AppMetadataFilter;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.routecoverage.RouteCoverageBySessionIDAndMetadataRequestExtended;
import com.contrastsecurity.http.HttpMethod;
import com.contrastsecurity.http.MediaType;
import com.contrastsecurity.http.TraceFilterForm.TraceExpandValue;
import com.contrastsecurity.models.TraceFilterBody;
import com.contrastsecurity.sdk.ContrastSDK;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SDKExtensionTest {

  private ContrastSDK sdk;
  private SDKExtension sdkExtension;

  @BeforeEach
  void setUp() {
    sdk = mock();
    sdkExtension = new SDKExtension(sdk);
  }

  @Test
  void getRouteCoverage_should_use_get_with_expand_observations_when_no_metadata()
      throws Exception {
    var emptyResponse =
        """
        {"success": true, "routes": []}
        """;
    when(sdk.makeRequest(any(), any()))
        .thenReturn(new ByteArrayInputStream(emptyResponse.getBytes(StandardCharsets.UTF_8)));

    var result = sdkExtension.getRouteCoverage("org-123", "app-456", null);

    assertThat(result.isSuccess()).isTrue();

    // Verify GET is used with expand=observations in URL
    verify(sdk)
        .makeRequest(
            eq(HttpMethod.GET),
            argThat(url -> url.contains("/route?") && url.contains("expand=observations")));
  }

  @Test
  void getRouteCoverage_should_use_post_with_metadata_body_when_metadata_provided()
      throws Exception {
    var emptyResponse =
        """
        {"success": true, "routes": []}
        """;
    when(sdk.makeRequestWithBody(any(), any(), any(), any()))
        .thenReturn(new ByteArrayInputStream(emptyResponse.getBytes(StandardCharsets.UTF_8)));

    var metadata = new RouteCoverageBySessionIDAndMetadataRequestExtended();
    metadata.setSessionId("session-123");

    var result = sdkExtension.getRouteCoverage("org-123", "app-456", metadata);

    assertThat(result.isSuccess()).isTrue();

    // Verify POST is used with metadata in body
    verify(sdk)
        .makeRequestWithBody(
            eq(HttpMethod.POST),
            argThat(url -> url.contains("/route/filter") && url.contains("expand=observations")),
            argThat(body -> body.contains("session-123")),
            eq(MediaType.JSON));
  }

  @Test
  void getApplicationsFiltered_should_call_filter_endpoint_with_params() throws Exception {
    var mockResponse =
        """
        {"applications": [{"name": "WebGoat", "app_id": "abc123"}], "count": 1}
        """;

    when(sdk.makeRequestWithBody(
            eq(HttpMethod.POST),
            argThat(url -> url.contains("/applications/filter")),
            any(),
            eq(MediaType.JSON)))
        .thenReturn(new ByteArrayInputStream(mockResponse.getBytes(StandardCharsets.UTF_8)));

    var result = sdkExtension.getApplicationsFiltered("org-123", "WebGoat", null, null, 50, 0);

    assertThat(result.getApplications()).hasSize(1);
    assertThat(result.getCount()).isEqualTo(1);

    verify(sdk)
        .makeRequestWithBody(
            eq(HttpMethod.POST),
            argThat(
                url ->
                    url.contains("/applications/filter")
                        && url.contains("limit=50")
                        && url.contains("offset=0")),
            argThat(body -> body.contains("WebGoat")),
            eq(MediaType.JSON));
  }

  @Test
  void getApplicationsFiltered_should_include_metadata_filters_in_request() throws Exception {
    var mockResponse =
        """
        {"applications": [], "count": 0}
        """;

    when(sdk.makeRequestWithBody(any(), any(), any(), any()))
        .thenReturn(new ByteArrayInputStream(mockResponse.getBytes(StandardCharsets.UTF_8)));

    var metadataFilters = List.of(new AppMetadataFilter(123L, new String[] {"prod"}));

    sdkExtension.getApplicationsFiltered("org-123", null, null, metadataFilters, 50, 0);

    verify(sdk)
        .makeRequestWithBody(
            eq(HttpMethod.POST),
            argThat(url -> url.contains("/applications/filter")),
            argThat(body -> body.contains("\"fieldID\":123") && body.contains("prod")),
            eq(MediaType.JSON));
  }

  @Test
  void getApplicationMetadataFields_should_return_field_definitions() throws Exception {
    var mockResponse =
        """
        {"fields": [{"fieldId": 123, "displayLabel": "Environment", "agentLabel": "env"}]}
        """;

    when(sdk.makeRequest(eq(HttpMethod.GET), argThat(url -> url.contains("/metadata/fields"))))
        .thenReturn(new ByteArrayInputStream(mockResponse.getBytes(StandardCharsets.UTF_8)));

    var result = sdkExtension.getApplicationMetadataFields("org-123");

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getDisplayLabel()).isEqualTo("Environment");
    assertThat(result.get(0).getFieldId()).isEqualTo(123L);
  }

  @Nested
  class UrlConstructionTests {

    @Test
    void getProtectConfig_should_construct_correct_url() throws Exception {
      var mockResponse =
          """
          {"success": true}
          """;
      when(sdk.makeRequest(any(), any()))
          .thenReturn(new ByteArrayInputStream(mockResponse.getBytes(StandardCharsets.UTF_8)));

      sdkExtension.getProtectConfig("org-123", "app-456");

      verify(sdk)
          .makeRequest(
              eq(HttpMethod.GET),
              argThat(
                  url ->
                      url.equals("/ng/org-123/protection/policy/app-456?expand=skip_links")
                          || (url.contains("/ng/org-123/protection/policy/app-456")
                              && url.contains("expand=skip_links"))));
    }

    @Test
    void getAppsForCVE_should_construct_correct_url() throws Exception {
      var mockResponse =
          """
          {"applications": []}
          """;
      when(sdk.makeRequest(any(), any()))
          .thenReturn(new ByteArrayInputStream(mockResponse.getBytes(StandardCharsets.UTF_8)));

      sdkExtension.getAppsForCVE("org-123", "CVE-2021-44228");

      verify(sdk)
          .makeRequest(eq(HttpMethod.GET), eq("/ng/organizations/org-123/cves/CVE-2021-44228"));
    }

    @Test
    void getLibraryObservations_should_construct_correct_url() throws Exception {
      var mockResponse =
          """
          {"observations": [], "total": 0}
          """;
      when(sdk.makeRequest(any(), any()))
          .thenReturn(new ByteArrayInputStream(mockResponse.getBytes(StandardCharsets.UTF_8)));

      sdkExtension.getLibraryObservations("org-123", "app-456", "lib-789", 25);

      verify(sdk)
          .makeRequest(
              eq(HttpMethod.GET),
              argThat(
                  url ->
                      url.contains(
                              "/ng/organizations/org-123/applications/app-456/libraries/lib-789/reports/library-usage")
                          && url.contains("offset=0")
                          && url.contains("limit=25")
                          && url.contains("sortBy=lastObservedTime")
                          && url.contains("sortDirection=DESC")));
    }

    @Test
    void getApplicationsFiltered_should_construct_correct_url() throws Exception {
      var mockResponse =
          """
          {"applications": [], "count": 0}
          """;
      when(sdk.makeRequestWithBody(any(), any(), any(), any()))
          .thenReturn(new ByteArrayInputStream(mockResponse.getBytes(StandardCharsets.UTF_8)));

      sdkExtension.getApplicationsFiltered("org-123", "test", null, null, 50, 10);

      verify(sdk)
          .makeRequestWithBody(
              eq(HttpMethod.POST),
              argThat(
                  url ->
                      url.contains("/ng/org-123/applications/filter")
                          && url.contains("expand=metadata%2Ctechnologies%2Cskip_links")
                          && url.contains("limit=50")
                          && url.contains("offset=10")
                          && url.contains("sort=-appName")),
              any(),
              eq(MediaType.JSON));
    }

    @Test
    void getApplicationMetadataFields_should_construct_correct_url() throws Exception {
      var mockResponse =
          """
          {"fields": []}
          """;
      when(sdk.makeRequest(any(), any()))
          .thenReturn(new ByteArrayInputStream(mockResponse.getBytes(StandardCharsets.UTF_8)));

      sdkExtension.getApplicationMetadataFields("org-123");

      verify(sdk).makeRequest(eq(HttpMethod.GET), eq("/ng/org-123/metadata/fields"));
    }

    @Test
    void getTraces_paginated_should_construct_correct_url_without_expand() throws Exception {
      var mockResponse =
          """
          {"traces": [], "count": 0}
          """;
      when(sdk.makeRequestWithBody(any(), any(), any(), any()))
          .thenReturn(new ByteArrayInputStream(mockResponse.getBytes(StandardCharsets.UTF_8)));

      var filters = new TraceFilterBody();
      sdkExtension.getTraces("org-123", "app-456", filters, 100, 50, null);

      verify(sdk)
          .makeRequestWithBody(
              eq(HttpMethod.POST),
              argThat(
                  url ->
                      url.contains("/ng/org-123/traces/app-456/filter")
                          && url.contains("limit=100")
                          && url.contains("offset=50")
                          && url.contains("sort=-lastTimeSeen")
                          && !url.contains("expand=")),
              any(),
              eq(MediaType.JSON));
    }

    @Test
    void getTraces_paginated_should_construct_correct_url_with_expand() throws Exception {
      var mockResponse =
          """
          {"traces": [], "count": 0}
          """;
      when(sdk.makeRequestWithBody(any(), any(), any(), any()))
          .thenReturn(new ByteArrayInputStream(mockResponse.getBytes(StandardCharsets.UTF_8)));

      var filters = new TraceFilterBody();
      var expand = EnumSet.of(TraceExpandValue.APPLICATION, TraceExpandValue.SESSION_METADATA);
      sdkExtension.getTraces("org-123", "app-456", filters, 100, 0, expand);

      verify(sdk)
          .makeRequestWithBody(
              eq(HttpMethod.POST),
              argThat(
                  url ->
                      url.contains("/ng/org-123/traces/app-456/filter")
                          && url.contains("limit=100")
                          && url.contains("offset=0")
                          && url.contains("sort=-lastTimeSeen")
                          && url.contains("expand=")
                          && url.contains("application")
                          && url.contains("session_metadata")),
              any(),
              eq(MediaType.JSON));
    }

    @Test
    void getTracesInOrg_should_construct_correct_url_without_expand() throws Exception {
      var mockResponse =
          """
          {"traces": [], "count": 0}
          """;
      when(sdk.makeRequestWithBody(any(), any(), any(), any()))
          .thenReturn(new ByteArrayInputStream(mockResponse.getBytes(StandardCharsets.UTF_8)));

      var filters = new TraceFilterBody();
      sdkExtension.getTracesInOrg("org-123", filters, 50, 25, null);

      verify(sdk)
          .makeRequestWithBody(
              eq(HttpMethod.POST),
              argThat(
                  url ->
                      url.contains("/ng/org-123/orgtraces/filter")
                          && url.contains("limit=50")
                          && url.contains("offset=25")
                          && url.contains("sort=-lastTimeSeen")
                          && !url.contains("expand=")),
              any(),
              eq(MediaType.JSON));
    }

    @Test
    void getTracesInOrg_should_construct_correct_url_with_expand() throws Exception {
      var mockResponse =
          """
          {"traces": [], "count": 0}
          """;
      when(sdk.makeRequestWithBody(any(), any(), any(), any()))
          .thenReturn(new ByteArrayInputStream(mockResponse.getBytes(StandardCharsets.UTF_8)));

      var filters = new TraceFilterBody();
      var expand = EnumSet.of(TraceExpandValue.SERVER_ENVIRONMENTS);
      sdkExtension.getTracesInOrg("org-123", filters, 50, 0, expand);

      verify(sdk)
          .makeRequestWithBody(
              eq(HttpMethod.POST),
              argThat(
                  url ->
                      url.contains("/ng/org-123/orgtraces/filter")
                          && url.contains("expand=server_environments")),
              any(),
              eq(MediaType.JSON));
    }

    @Test
    void getLatestSessionMetadata_should_construct_correct_url() throws Exception {
      var mockResponse =
          """
          {"metadata": {}}
          """;
      when(sdk.makeRequest(any(), any()))
          .thenReturn(new ByteArrayInputStream(mockResponse.getBytes(StandardCharsets.UTF_8)));

      sdkExtension.getLatestSessionMetadata("org-123", "app-456");

      verify(sdk)
          .makeRequest(
              eq(HttpMethod.GET),
              eq("/ng/organizations/org-123/applications/app-456/agent-sessions/latest"));
    }

    @Test
    void getAttacks_should_construct_correct_url_with_defaults() throws Exception {
      var mockResponse =
          """
          {"attacks": [], "count": 0}
          """;
      when(sdk.makeRequestWithBody(any(), any(), any(), any()))
          .thenReturn(new ByteArrayInputStream(mockResponse.getBytes(StandardCharsets.UTF_8)));

      sdkExtension.getAttacks("org-123");

      verify(sdk)
          .makeRequestWithBody(
              eq(HttpMethod.POST),
              argThat(
                  url ->
                      url.contains("/ng/org-123/attacks")
                          && url.contains("expand=skip_links")
                          && url.contains("limit=1000")
                          && url.contains("offset=0")
                          && url.contains("sort=-startTime")),
              any(),
              eq(MediaType.JSON));
    }

    @Test
    void getAttacks_should_construct_correct_url_with_custom_params() throws Exception {
      var mockResponse =
          """
          {"attacks": [], "count": 0}
          """;
      when(sdk.makeRequestWithBody(any(), any(), any(), any()))
          .thenReturn(new ByteArrayInputStream(mockResponse.getBytes(StandardCharsets.UTF_8)));

      sdkExtension.getAttacks("org-123", null, 50, 100, "-endTime");

      verify(sdk)
          .makeRequestWithBody(
              eq(HttpMethod.POST),
              argThat(
                  url ->
                      url.contains("/ng/org-123/attacks")
                          && url.contains("expand=skip_links")
                          && url.contains("limit=50")
                          && url.contains("offset=100")
                          && url.contains("sort=-endTime")),
              any(),
              eq(MediaType.JSON));
    }
  }
}
