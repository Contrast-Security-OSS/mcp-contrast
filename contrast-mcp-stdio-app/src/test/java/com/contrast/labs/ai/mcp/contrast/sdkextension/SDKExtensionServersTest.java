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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.contrast.labs.ai.mcp.contrast.sdkextension.data.server.ServerFilterBody;
import com.contrastsecurity.http.HttpMethod;
import com.contrastsecurity.http.MediaType;
import com.contrastsecurity.sdk.ContrastSDK;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SDKExtensionServersTest {

  private ContrastSDK sdk;
  private SDKExtension sdkExtension;

  @BeforeEach
  void setUp() {
    sdk = mock();
    sdkExtension = new SDKExtension(sdk);
  }

  @Test
  void getServersFiltered_should_request_only_numApps_expand_when_applications_not_requested()
      throws Exception {
    stubResponse(emptySuccess());

    sdkExtension.getServersFiltered("org-123", filterBody(), 50, 0, "-lastActivity", false);

    var urlCaptor = ArgumentCaptor.forClass(String.class);
    verify(sdk)
        .makeRequestWithBody(
            eq(HttpMethod.POST), urlCaptor.capture(), anyString(), eq(MediaType.JSON));
    assertThat(urlCaptor.getValue())
        .contains(
            "/ng/org-123/servers/filter",
            "expand=num_apps",
            "includeArchived=false",
            "limit=50",
            "offset=0",
            "sort=-lastActivity")
        .doesNotContain("expand=applications", "num_apps%2Capplications");
  }

  @Test
  void getServersFiltered_should_request_only_applications_expand_when_applications_requested()
      throws Exception {
    stubResponse(emptySuccess());

    sdkExtension.getServersFiltered("org-123", filterBody(), 25, 0, "serverName", true);

    var urlCaptor = ArgumentCaptor.forClass(String.class);
    verify(sdk)
        .makeRequestWithBody(
            eq(HttpMethod.POST), urlCaptor.capture(), anyString(), eq(MediaType.JSON));
    assertThat(urlCaptor.getValue())
        .contains("expand=applications", "limit=25", "sort=serverName")
        .doesNotContain("num_apps");
  }

  @Test
  void getServersFiltered_should_serialize_wire_spellings_and_case_sensitive_none_sentinel()
      throws Exception {
    stubResponse(emptySuccess());
    var body =
        ServerFilterBody.builder()
            .applicationsIds(List.of("None"))
            .serverEnvironments(List.of("PRODUCTION"))
            .q("checkout")
            .quickFilter("UNPROTECTED")
            .build();

    sdkExtension.getServersFiltered("org-123", body, 50, 0, "-lastActivity", false);

    var bodyCaptor = ArgumentCaptor.forClass(String.class);
    verify(sdk)
        .makeRequestWithBody(
            eq(HttpMethod.POST), anyString(), bodyCaptor.capture(), eq(MediaType.JSON));
    assertThat(bodyCaptor.getValue())
        .contains(
            "\"applicationsIds\":[\"None\"]",
            "\"serverEnvironments\":[\"PRODUCTION\"]",
            "\"q\":\"checkout\"",
            "\"quickFilter\":\"UNPROTECTED\"")
        .doesNotContain("applicationIds");
  }

  @Test
  void getServersFiltered_should_recover_total_from_first_page_when_later_page_is_empty()
      throws Exception {
    when(sdk.makeRequestWithBody(any(), anyString(), anyString(), any()))
        .thenReturn(stream(emptySuccess()), stream(firstPageSuccess()));

    var response =
        sdkExtension.getServersFiltered("org-123", filterBody(), 50, 100, "-lastActivity", false);

    var urlCaptor = ArgumentCaptor.forClass(String.class);
    verify(sdk, times(2))
        .makeRequestWithBody(
            eq(HttpMethod.POST), urlCaptor.capture(), anyString(), eq(MediaType.JSON));
    assertThat(response.getServers()).isEmpty();
    assertThat(response.getCount()).isEqualTo(7L);
    assertThat(urlCaptor.getAllValues().getFirst())
        .contains("limit=50", "offset=100", "expand=num_apps");
    assertThat(urlCaptor.getAllValues().get(1))
        .contains("limit=1", "offset=0")
        .doesNotContain("expand=");
  }

  @Test
  void getServersFiltered_should_not_issue_fallback_for_empty_first_page() throws Exception {
    stubResponse(emptySuccess());

    var response =
        sdkExtension.getServersFiltered("org-123", filterBody(), 50, 0, "-lastActivity", false);

    assertThat(response.getCount()).isZero();
    verify(sdk)
        .makeRequestWithBody(eq(HttpMethod.POST), anyString(), anyString(), eq(MediaType.JSON));
  }

  @Test
  void getServersFiltered_should_reject_unsuccessful_or_null_envelopes() throws Exception {
    when(sdk.makeRequestWithBody(any(), anyString(), anyString(), any()))
        .thenReturn(stream("{\"success\":false,\"messages\":[\"secret\"]}"), stream("null"));

    assertThatThrownBy(
            () ->
                sdkExtension.getServersFiltered(
                    "org-123", filterBody(), 50, 0, "-lastActivity", false))
        .isInstanceOf(IOException.class)
        .hasMessage("Invalid server response envelope");
    assertThatThrownBy(
            () ->
                sdkExtension.getServersFiltered(
                    "org-123", filterBody(), 50, 0, "-lastActivity", false))
        .isInstanceOf(IOException.class)
        .hasMessage("Invalid server response envelope");
  }

  @Test
  void getServersFiltered_should_reject_malformed_json() throws Exception {
    stubResponse("{");

    assertThatThrownBy(
            () ->
                sdkExtension.getServersFiltered(
                    "org-123", filterBody(), 50, 0, "-lastActivity", false))
        .isInstanceOf(RuntimeException.class);
  }

  private void stubResponse(String json) throws Exception {
    when(sdk.makeRequestWithBody(any(), anyString(), anyString(), any())).thenReturn(stream(json));
  }

  private static ByteArrayInputStream stream(String json) {
    return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
  }

  private static ServerFilterBody filterBody() {
    return ServerFilterBody.builder().build();
  }

  private static String emptySuccess() {
    return """
    {"success":true,"count":0,"servers":[]}
    """;
  }

  private static String firstPageSuccess() {
    return """
    {"success":true,"count":7,"servers":[{"server_id":1,"name":"server-1"}]}
    """;
  }
}
