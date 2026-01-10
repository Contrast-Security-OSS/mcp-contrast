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

import com.contrast.labs.ai.mcp.contrast.sdkextension.data.routecoverage.RouteCoverageBySessionIDAndMetadataRequestExtended;
import com.contrastsecurity.http.HttpMethod;
import com.contrastsecurity.http.MediaType;
import com.contrastsecurity.sdk.ContrastSDK;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
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
  void getRouteCoverage_should_use_post_with_empty_body_when_no_metadata() throws Exception {
    var emptyResponse =
        """
        {"success": true, "routes": []}
        """;
    when(sdk.makeRequestWithBody(any(), any(), any(), any()))
        .thenReturn(new ByteArrayInputStream(emptyResponse.getBytes(StandardCharsets.UTF_8)));

    var result = sdkExtension.getRouteCoverage("org-123", "app-456", null);

    assertThat(result.isSuccess()).isTrue();

    // Verify POST is used (not GET), with expand=observations in URL
    // Empty request serializes to {"metadata":[]} not {}
    verify(sdk)
        .makeRequestWithBody(
            eq(HttpMethod.POST),
            argThat(url -> url.contains("/route/filter") && url.contains("expand=observations")),
            argThat(body -> body.contains("metadata")),
            eq(MediaType.JSON));
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
}
