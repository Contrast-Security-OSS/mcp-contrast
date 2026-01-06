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
package com.contrast.labs.ai.mcp.contrast.tool.applications;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.contrast.labs.ai.mcp.contrast.config.ContrastConfig;
import com.contrastsecurity.models.MetadataFilterResponse;
import com.contrastsecurity.sdk.ContrastSDK;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class GetSessionMetadataToolTest {

  private GetSessionMetadataTool tool;
  private ContrastConfig config;
  private ContrastSDK sdk;

  @BeforeEach
  void setUp() {
    sdk = mock();
    config = mock();

    when(config.createSDK()).thenReturn(sdk);
    when(config.getOrgId()).thenReturn("test-org-id");

    tool = new GetSessionMetadataTool();
    ReflectionTestUtils.setField(tool, "config", config);
  }

  @Test
  void getSessionMetadata_should_return_validation_error_for_missing_app_id() {
    var result = tool.getSessionMetadata(null);

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors()).anyMatch(e -> e.contains("appId") && e.contains("required"));
    verifyNoInteractions(sdk);
  }

  @Test
  void getSessionMetadata_should_return_validation_error_for_empty_app_id() {
    var result = tool.getSessionMetadata("");

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors()).anyMatch(e -> e.contains("appId") && e.contains("required"));
    verifyNoInteractions(sdk);
  }

  @Test
  void getSessionMetadata_should_return_data_on_success() throws Exception {
    var response = mock(MetadataFilterResponse.class);
    when(sdk.getSessionMetadataForApplication(eq("test-org-id"), eq("app-123"), isNull()))
        .thenReturn(response);

    var result = tool.getSessionMetadata("app-123");

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.found()).isTrue();
    assertThat(result.data()).isEqualTo(response);
    assertThat(result.errors()).isEmpty();

    verify(sdk).getSessionMetadataForApplication(eq("test-org-id"), eq("app-123"), isNull());
  }

  @Test
  void getSessionMetadata_should_handle_null_response_gracefully() throws Exception {
    when(sdk.getSessionMetadataForApplication(eq("test-org-id"), eq("app-123"), isNull()))
        .thenReturn(null);

    var result = tool.getSessionMetadata("app-123");

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.found()).isFalse();
    assertThat(result.data()).isNull();
    assertThat(result.warnings()).anyMatch(w -> w.contains("No session metadata found"));
  }

  @Test
  void getSessionMetadata_should_handle_api_exception() throws Exception {
    when(sdk.getSessionMetadataForApplication(any(), any(), any()))
        .thenThrow(new RuntimeException("API error"));

    var result = tool.getSessionMetadata("app-123");

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors()).anyMatch(e -> e.contains("Internal error"));
  }
}
