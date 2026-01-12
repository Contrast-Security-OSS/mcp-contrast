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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.contrast.labs.ai.mcp.contrast.AnonymousLibraryExtendedBuilder;
import com.contrast.labs.ai.mcp.contrast.config.ContrastSDKFactory;
import com.contrast.labs.ai.mcp.contrast.sdkextension.SDKExtension;
import com.contrast.labs.ai.mcp.contrast.sdkextension.SDKHelper;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.LibrariesExtended;
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
  private ContrastSDKFactory sdkFactory;
  private ContrastSDK sdk;

  private MockedStatic<SDKHelper> mockedSDKHelper;
  private MockedConstruction<SDKExtension> mockedSDKExtension;

  @BeforeEach
  void setUp() {
    sdk = mock();
    sdkFactory = mock();

    when(sdkFactory.getSDK()).thenReturn(sdk);
    when(sdkFactory.getOrgId()).thenReturn(TEST_ORG_ID);

    tool = new ListApplicationLibrariesTool();
    ReflectionTestUtils.setField(tool, "sdkFactory", sdkFactory);

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
    var result = tool.listApplicationLibraries(null, null, null);

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors()).anyMatch(e -> e.contains("appId") && e.contains("required"));
    verifyNoInteractions(sdk);
  }

  @Test
  void listApplicationLibraries_should_return_validation_error_for_empty_app_id() {
    var result = tool.listApplicationLibraries(null, null, "");

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors()).anyMatch(e -> e.contains("appId") && e.contains("required"));
    verifyNoInteractions(sdk);
  }

  @Test
  void listApplicationLibraries_should_return_libraries_on_success() throws IOException {
    var mockLibraries = createMockLibraries(3);
    var mockResponse = createMockResponse(mockLibraries, 3L);
    mockedSDKHelper
        .when(
            () ->
                SDKHelper.getLibraryPage(
                    eq(TEST_APP_ID), eq(TEST_ORG_ID), any(SDKExtension.class), anyInt(), anyInt()))
        .thenReturn(mockResponse);

    var result = tool.listApplicationLibraries(null, null, TEST_APP_ID);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.items()).hasSize(3);
    assertThat(result.totalItems()).isEqualTo(3);
    assertThat(result.errors()).isEmpty();
  }

  @Test
  void listApplicationLibraries_should_return_empty_list_with_warning_when_no_libraries()
      throws IOException {
    var mockResponse = createMockResponse(new ArrayList<>(), 0L);
    mockedSDKHelper
        .when(
            () ->
                SDKHelper.getLibraryPage(
                    eq(TEST_APP_ID), eq(TEST_ORG_ID), any(SDKExtension.class), anyInt(), anyInt()))
        .thenReturn(mockResponse);

    var result = tool.listApplicationLibraries(null, null, TEST_APP_ID);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.items()).isEmpty();
    assertThat(result.totalItems()).isEqualTo(0);
    assertThat(result.warnings()).anyMatch(w -> w.contains("No libraries found"));
  }

  @Test
  void listApplicationLibraries_should_handle_null_libraries_in_response() throws IOException {
    var mockResponse = new LibrariesExtended();
    mockResponse.setLibraries(null);
    mockResponse.setCount(0L);
    mockedSDKHelper
        .when(
            () ->
                SDKHelper.getLibraryPage(
                    eq(TEST_APP_ID), eq(TEST_ORG_ID), any(SDKExtension.class), anyInt(), anyInt()))
        .thenReturn(mockResponse);

    var result = tool.listApplicationLibraries(null, null, TEST_APP_ID);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.items()).isEmpty();
    assertThat(result.warnings()).anyMatch(w -> w.contains("No libraries found"));
  }

  @Test
  void listApplicationLibraries_should_handle_api_exception() throws IOException {
    mockedSDKHelper
        .when(
            () ->
                SDKHelper.getLibraryPage(any(), any(), any(SDKExtension.class), anyInt(), anyInt()))
        .thenThrow(new IOException("SDK connection failed"));

    var result = tool.listApplicationLibraries(null, null, TEST_APP_ID);

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors()).anyMatch(e -> e.contains("Internal error"));
  }

  @Test
  void listApplicationLibraries_should_paginate_results() throws IOException {
    // Server-side pagination returns only the requested page
    var mockLibraries = createMockLibraries(3);
    var mockResponse = createMockResponse(mockLibraries, 10L); // 10 total items
    mockedSDKHelper
        .when(
            () ->
                SDKHelper.getLibraryPage(
                    eq(TEST_APP_ID), eq(TEST_ORG_ID), any(SDKExtension.class), eq(3), eq(0)))
        .thenReturn(mockResponse);

    var result = tool.listApplicationLibraries(1, 3, TEST_APP_ID);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.items()).hasSize(3);
    assertThat(result.totalItems()).isEqualTo(10);
    assertThat(result.hasMorePages()).isTrue();
    assertThat(result.page()).isEqualTo(1);
    assertThat(result.pageSize()).isEqualTo(3);
  }

  @Test
  void listApplicationLibraries_should_return_second_page() throws IOException {
    // Server-side pagination: page 2 with pageSize 3 means offset=3
    var mockLibraries = createMockLibraries(3);
    var mockResponse = createMockResponse(mockLibraries, 10L);
    mockedSDKHelper
        .when(
            () ->
                SDKHelper.getLibraryPage(
                    eq(TEST_APP_ID), eq(TEST_ORG_ID), any(SDKExtension.class), eq(3), eq(3)))
        .thenReturn(mockResponse);

    var result = tool.listApplicationLibraries(2, 3, TEST_APP_ID);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.items()).hasSize(3);
    assertThat(result.page()).isEqualTo(2);
    assertThat(result.hasMorePages()).isTrue();
  }

  @Test
  void listApplicationLibraries_should_return_last_page_partial() throws IOException {
    // Server-side pagination: page 4 with pageSize 3 means offset=9, API returns 1 item
    var mockLibraries = createMockLibraries(1);
    var mockResponse = createMockResponse(mockLibraries, 10L);
    mockedSDKHelper
        .when(
            () ->
                SDKHelper.getLibraryPage(
                    eq(TEST_APP_ID), eq(TEST_ORG_ID), any(SDKExtension.class), eq(3), eq(9)))
        .thenReturn(mockResponse);

    var result = tool.listApplicationLibraries(4, 3, TEST_APP_ID);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.items()).hasSize(1); // Only 1 item on last page (10 total, page 4 of 3)
    assertThat(result.hasMorePages()).isFalse();
  }

  @Test
  void listApplicationLibraries_should_return_empty_for_page_beyond_results() throws IOException {
    // Server-side pagination: page 10 with pageSize 50 means offset=450, API returns 0 items
    var mockResponse = createMockResponse(new ArrayList<>(), 5L);
    mockedSDKHelper
        .when(
            () ->
                SDKHelper.getLibraryPage(
                    eq(TEST_APP_ID), eq(TEST_ORG_ID), any(SDKExtension.class), eq(50), eq(450)))
        .thenReturn(mockResponse);

    var result = tool.listApplicationLibraries(10, 50, TEST_APP_ID);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.items()).isEmpty();
    assertThat(result.totalItems()).isEqualTo(5);
    assertThat(result.hasMorePages()).isFalse();
  }

  @Test
  void listApplicationLibraries_should_cap_pageSize_at_50_with_warning() throws IOException {
    var mockLibraries = createMockLibraries(50);
    var mockResponse = createMockResponse(mockLibraries, 200L);

    // When pageSize exceeds 50, it should be capped to 50
    mockedSDKHelper
        .when(
            () ->
                SDKHelper.getLibraryPage(
                    eq(TEST_APP_ID), eq(TEST_ORG_ID), any(SDKExtension.class), eq(50), eq(0)))
        .thenReturn(mockResponse);

    // Request pageSize 100, should be capped to 50 with warning
    var result = tool.listApplicationLibraries(1, 100, TEST_APP_ID);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.items()).hasSize(50);
    assertThat(result.totalItems()).isEqualTo(200);
    assertThat(result.pageSize()).isEqualTo(50); // Response shows effective (capped) amount
    assertThat(result.warnings()).anyMatch(w -> w.contains("exceeds maximum 50"));
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

  private LibrariesExtended createMockResponse(List<LibraryExtended> libraries, Long count) {
    var response = new LibrariesExtended();
    response.setLibraries(libraries);
    response.setCount(count);
    return response;
  }
}
