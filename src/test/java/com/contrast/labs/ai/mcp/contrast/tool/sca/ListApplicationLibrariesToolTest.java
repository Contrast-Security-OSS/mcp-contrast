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
package com.contrast.labs.ai.mcp.contrast.tool.sca;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.contrast.labs.ai.mcp.contrast.AnonymousLibraryExtendedBuilder;
import com.contrast.labs.ai.mcp.contrast.config.ContrastConfig;
import com.contrast.labs.ai.mcp.contrast.sdkextension.SDKExtension;
import com.contrast.labs.ai.mcp.contrast.sdkextension.SDKHelper;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.LibraryExtended;
import com.contrastsecurity.sdk.ContrastSDK;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

class ListApplicationLibrariesToolTest {

  private static final String TEST_ORG_ID = "test-org-123";
  private static final String TEST_APP_ID = "test-app-456";

  private ListApplicationLibrariesTool tool;
  private ContrastConfig config;
  private ContrastSDK sdk;

  private MockedStatic<SDKHelper> mockedSDKHelper;
  private MockedConstruction<SDKExtension> mockedSDKExtension;

  @BeforeEach
  void setUp() {
    sdk = mock();
    config = mock();

    when(config.getSDK()).thenReturn(sdk);
    when(config.getOrgId()).thenReturn(TEST_ORG_ID);

    tool = new ListApplicationLibrariesTool();
    ReflectionTestUtils.setField(tool, "config", config);

    // Mock SDKHelper static methods
    mockedSDKHelper = mockStatic(SDKHelper.class);

    // Mock SDKExtension constructor
    mockedSDKExtension = org.mockito.Mockito.mockConstruction(SDKExtension.class);
  }

  @AfterEach
  void tearDown() {
    if (mockedSDKHelper != null) {
      mockedSDKHelper.close();
    }
    if (mockedSDKExtension != null) {
      mockedSDKExtension.close();
    }
  }

  @Test
  void listApplicationLibraries_should_return_validation_error_for_missing_app_id() {
    var result = tool.listApplicationLibraries(null);

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors()).anyMatch(e -> e.contains("appId") && e.contains("required"));
    verifyNoInteractions(sdk);
  }

  @Test
  void listApplicationLibraries_should_return_validation_error_for_empty_app_id() {
    var result = tool.listApplicationLibraries("");

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors()).anyMatch(e -> e.contains("appId") && e.contains("required"));
    verifyNoInteractions(sdk);
  }

  @Test
  void listApplicationLibraries_should_return_libraries_on_success() throws IOException {
    var mockLibraries = createMockLibraries(3);
    mockedSDKHelper
        .when(
            () -> SDKHelper.getLibsForID(eq(TEST_APP_ID), eq(TEST_ORG_ID), any(SDKExtension.class)))
        .thenReturn(mockLibraries);

    var result = tool.listApplicationLibraries(TEST_APP_ID);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.found()).isTrue();
    assertThat(result.data()).hasSize(3);
    assertThat(result.errors()).isEmpty();
  }

  @Test
  void listApplicationLibraries_should_return_empty_list_with_warning_when_no_libraries()
      throws IOException {
    mockedSDKHelper
        .when(
            () -> SDKHelper.getLibsForID(eq(TEST_APP_ID), eq(TEST_ORG_ID), any(SDKExtension.class)))
        .thenReturn(new ArrayList<>());

    var result = tool.listApplicationLibraries(TEST_APP_ID);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.data()).isEmpty();
    assertThat(result.warnings()).anyMatch(w -> w.contains("No libraries found"));
  }

  @Test
  void listApplicationLibraries_should_handle_null_response() throws IOException {
    mockedSDKHelper
        .when(
            () -> SDKHelper.getLibsForID(eq(TEST_APP_ID), eq(TEST_ORG_ID), any(SDKExtension.class)))
        .thenReturn(null);

    var result = tool.listApplicationLibraries(TEST_APP_ID);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.data()).isEmpty();
    assertThat(result.warnings()).anyMatch(w -> w.contains("No libraries found"));
  }

  @Test
  void listApplicationLibraries_should_handle_api_exception() throws IOException {
    mockedSDKHelper
        .when(() -> SDKHelper.getLibsForID(any(), any(), any(SDKExtension.class)))
        .thenThrow(new IOException("SDK connection failed"));

    var result = tool.listApplicationLibraries(TEST_APP_ID);

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors()).anyMatch(e -> e.contains("Internal error"));
  }

  private List<LibraryExtended> createMockLibraries(int count) {
    var libraries = new ArrayList<LibraryExtended>();
    for (int i = 0; i < count; i++) {
      var lib =
          AnonymousLibraryExtendedBuilder.validLibrary()
              .withFilename("library-" + i + ".jar")
              .withVersion("1.0." + i)
              .withHash("hash-" + i)
              .withClassCount(100)
              .withClassedUsed(50)
              .build();
      libraries.add(lib);
    }
    return libraries;
  }
}
