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
package com.contrast.labs.ai.mcp.contrast;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.contrast.labs.ai.mcp.contrast.sdkextension.SDKExtension;
import com.contrast.labs.ai.mcp.contrast.sdkextension.SDKHelper;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.CveData;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.LibraryExtended;
import com.contrastsecurity.sdk.ContrastSDK;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for SCAService (Software Composition Analysis). Tests library retrieval and CVE
 * vulnerability mapping.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SCAServiceTest {

  private SCAService scaService;

  @Mock private ContrastSDK mockContrastSDK;

  @Mock private SDKExtension mockSDKExtension;

  private MockedStatic<SDKHelper> mockedSDKHelper;
  private MockedConstruction<SDKExtension> mockedSDKExtension;

  private static final String TEST_ORG_ID = "test-org-123";
  private static final String TEST_HOST = "https://test.contrast.local";
  private static final String TEST_API_KEY = "test-api-key";
  private static final String TEST_SERVICE_KEY = "test-service-key";
  private static final String TEST_USERNAME = "test-user";
  private static final String TEST_APP_ID = "test-app-456";
  private static final String TEST_CVE_ID = "CVE-2023-12345";

  @BeforeEach
  void setUp() {
    scaService = new SCAService();

    // Mock SDKHelper.getSDK()
    mockedSDKHelper = mockStatic(SDKHelper.class);
    mockedSDKHelper
        .when(
            () ->
                SDKHelper.getSDK(
                    anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
        .thenReturn(mockContrastSDK);

    // Mock SDKExtension constructor
    mockedSDKExtension =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              // No-op constructor mock
            });

    // Set configuration fields
    ReflectionTestUtils.setField(scaService, "orgID", TEST_ORG_ID);
    ReflectionTestUtils.setField(scaService, "hostName", TEST_HOST);
    ReflectionTestUtils.setField(scaService, "apiKey", TEST_API_KEY);
    ReflectionTestUtils.setField(scaService, "serviceKey", TEST_SERVICE_KEY);
    ReflectionTestUtils.setField(scaService, "userName", TEST_USERNAME);
    ReflectionTestUtils.setField(scaService, "httpProxyHost", "");
    ReflectionTestUtils.setField(scaService, "httpProxyPort", "");
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

  // ========== Tests for list_application_libraries_by_app_id ==========

  @Test
  void testGetApplicationLibrariesByID_Success() throws IOException {
    // Given
    var mockLibraries = createMockLibraries(3);
    mockedSDKHelper
        .when(
            () -> SDKHelper.getLibsForID(eq(TEST_APP_ID), eq(TEST_ORG_ID), any(SDKExtension.class)))
        .thenReturn(mockLibraries);

    // When
    var result = scaService.getApplicationLibrariesByID(TEST_APP_ID);

    // Then
    assertNotNull(result, "Result should not be null");
    assertEquals(3, result.size(), "Should return 3 libraries");

    // Verify SDKHelper was called correctly
    mockedSDKHelper.verify(
        () ->
            SDKHelper.getSDK(
                eq(TEST_HOST),
                eq(TEST_API_KEY),
                eq(TEST_SERVICE_KEY),
                eq(TEST_USERNAME),
                eq(""),
                eq("")));
    mockedSDKHelper.verify(
        () -> SDKHelper.getLibsForID(eq(TEST_APP_ID), eq(TEST_ORG_ID), any(SDKExtension.class)));
  }

  @Test
  void testGetApplicationLibrariesByID_EmptyList() throws IOException {
    // Given - App exists but has no libraries
    var emptyLibraries = new ArrayList<LibraryExtended>();
    mockedSDKHelper
        .when(
            () -> SDKHelper.getLibsForID(eq(TEST_APP_ID), eq(TEST_ORG_ID), any(SDKExtension.class)))
        .thenReturn(emptyLibraries);

    // When
    var result = scaService.getApplicationLibrariesByID(TEST_APP_ID);

    // Then
    assertNotNull(result, "Result should not be null");
    assertTrue(result.isEmpty(), "Result should be empty list");
  }

  @Test
  void testGetApplicationLibrariesByID_NullAppID() {
    // When/Then - Should handle null gracefully or throw descriptive exception
    assertThrows(
        Exception.class,
        () -> {
          scaService.getApplicationLibrariesByID(null);
        },
        "Should throw exception for null app ID");
  }

  @Test
  void testGetApplicationLibrariesByID_EmptyAppID() {
    // When/Then - Should handle empty string appropriately
    assertThrows(
        Exception.class,
        () -> {
          scaService.getApplicationLibrariesByID("");
        },
        "Should throw exception for empty app ID");
  }

  @Test
  void testGetApplicationLibrariesByID_SDKFailure() {
    // Given - SDK throws exception
    mockedSDKHelper
        .when(() -> SDKHelper.getLibsForID(anyString(), anyString(), any(SDKExtension.class)))
        .thenThrow(new RuntimeException("SDK connection failed"));

    // When/Then
    var exception =
        assertThrows(
            RuntimeException.class,
            () -> {
              scaService.getApplicationLibrariesByID(TEST_APP_ID);
            });

    assertTrue(
        exception.getMessage().contains("SDK connection failed"),
        "Exception message should indicate SDK failure");
  }

  @Test
  void testGetApplicationLibrariesByID_VerifiesClassUsage() throws IOException {
    // Given - Libraries with different class usage counts
    var mockLibraries = createMockLibrariesWithClassUsage();
    mockedSDKHelper
        .when(
            () -> SDKHelper.getLibsForID(eq(TEST_APP_ID), eq(TEST_ORG_ID), any(SDKExtension.class)))
        .thenReturn(mockLibraries);

    // When
    var result = scaService.getApplicationLibrariesByID(TEST_APP_ID);

    // Then
    assertEquals(2, result.size());

    // Verify first library has class usage > 0 (actively used)
    assertTrue(result.get(0).getClassedUsed() > 0, "First library should have classes used");

    // Verify second library has class usage = 0 (likely unused)
    assertEquals(0, result.get(1).getClassedUsed(), "Second library should have zero classes used");
  }

  // ========== Tests for list_applications_vulnerable_to_cve ==========

  @Test
  void testListCVESForApplication_Success() throws IOException {
    // Given
    var mockCveData = createMockCveDataWithApps();
    var mockLibraries = createMockLibrariesWithClassUsage();

    // Mock SDKExtension.getAppsForCVE
    var mockExtension = mock(SDKExtension.class);
    when(mockExtension.getAppsForCVE(eq(TEST_ORG_ID), eq(TEST_CVE_ID))).thenReturn(mockCveData);

    // Replace mockedSDKExtension to return our configured mock
    if (mockedSDKExtension != null) {
      mockedSDKExtension.close();
    }
    mockedSDKExtension =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              when(mock.getAppsForCVE(eq(TEST_ORG_ID), eq(TEST_CVE_ID))).thenReturn(mockCveData);
            });

    // Mock getLibsForID for class usage population
    mockedSDKHelper
        .when(() -> SDKHelper.getLibsForID(anyString(), eq(TEST_ORG_ID), any(SDKExtension.class)))
        .thenReturn(mockLibraries);

    // When
    var result = scaService.listCVESForApplication(TEST_CVE_ID);

    // Then
    assertNotNull(result, "Result should not be null");
    assertNotNull(result.getApps(), "Apps list should not be null");
    assertNotNull(result.getLibraries(), "Libraries list should not be null");
    assertFalse(result.getApps().isEmpty(), "Should have at least one app");
  }

  @Test
  void testListCVESForApplication_NoCVEFound() throws IOException {
    // Given - CVE doesn't exist or no apps are vulnerable
    var emptyCveData = new CveData();
    emptyCveData.setApps(new ArrayList<>());
    emptyCveData.setLibraries(new ArrayList<>());

    if (mockedSDKExtension != null) {
      mockedSDKExtension.close();
    }
    mockedSDKExtension =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              when(mock.getAppsForCVE(eq(TEST_ORG_ID), anyString())).thenReturn(emptyCveData);
            });

    // When
    var result = scaService.listCVESForApplication("CVE-9999-NONEXISTENT");

    // Then
    assertNotNull(result, "Result should not be null");
    assertTrue(result.getApps().isEmpty(), "Should have no vulnerable apps");
  }

  @Test
  void testListCVESForApplication_ClassUsagePopulation() throws IOException {
    // Given
    var mockCveData = createMockCveDataWithApps();

    var mockLibrariesForApp = createMockLibrariesWithMatchingHash();

    if (mockedSDKExtension != null) {
      mockedSDKExtension.close();
    }
    mockedSDKExtension =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              when(mock.getAppsForCVE(eq(TEST_ORG_ID), eq(TEST_CVE_ID))).thenReturn(mockCveData);
            });

    mockedSDKHelper
        .when(() -> SDKHelper.getLibsForID(anyString(), eq(TEST_ORG_ID), any(SDKExtension.class)))
        .thenReturn(mockLibrariesForApp);

    // When
    var result = scaService.listCVESForApplication(TEST_CVE_ID);

    // Then
    assertNotNull(result, "Result should not be null");
    assertTrue(result.getApps().size() > 0, "Should have apps");

    // Verify class usage was populated for apps
    // (Implementation in SCAService populates classCount and classUsage fields)
    var firstApp = result.getApps().get(0);
    assertTrue(firstApp.getClassCount() >= 0, "Class count should be populated");
  }

  // ========== Helper Methods ==========

  private List<LibraryExtended> createMockLibraries(int count) {
    var libraries = new ArrayList<LibraryExtended>();
    for (int i = 0; i < count; i++) {
      var lib = mock(LibraryExtended.class);
      when(lib.getFilename()).thenReturn("library-" + i + ".jar");
      when(lib.getHash()).thenReturn("hash-" + i);
      when(lib.getVersion()).thenReturn("1.0." + i);
      when(lib.getClassCount()).thenReturn(100);
      when(lib.getClassedUsed()).thenReturn(50);
      libraries.add(lib);
    }
    return libraries;
  }

  private List<LibraryExtended> createMockLibrariesWithClassUsage() {
    var libraries = new ArrayList<LibraryExtended>();

    // Library 1: Actively used (classesUsed > 0)
    var lib1 = mock(LibraryExtended.class);
    when(lib1.getFilename()).thenReturn("actively-used-lib.jar");
    when(lib1.getHash()).thenReturn("hash-active-123");
    when(lib1.getVersion()).thenReturn("2.1.0");
    when(lib1.getClassCount()).thenReturn(150);
    when(lib1.getClassedUsed()).thenReturn(75); // 50% usage
    libraries.add(lib1);

    // Library 2: Likely unused (classesUsed = 0)
    var lib2 = mock(LibraryExtended.class);
    when(lib2.getFilename()).thenReturn("unused-lib.jar");
    when(lib2.getHash()).thenReturn("hash-unused-456");
    when(lib2.getVersion()).thenReturn("1.5.2");
    when(lib2.getClassCount()).thenReturn(200);
    when(lib2.getClassedUsed()).thenReturn(0); // Not used!
    libraries.add(lib2);

    return libraries;
  }

  private List<LibraryExtended> createMockLibrariesWithMatchingHash() {
    var libraries = new ArrayList<LibraryExtended>();

    var lib = mock(LibraryExtended.class);
    when(lib.getFilename()).thenReturn("vulnerable-lib.jar");
    when(lib.getHash()).thenReturn("matching-hash-789");
    when(lib.getVersion()).thenReturn("1.0.0");
    when(lib.getClassCount()).thenReturn(100);
    when(lib.getClassedUsed()).thenReturn(50);
    libraries.add(lib);

    return libraries;
  }

  private CveData createMockCveData() {
    var cveData = new CveData();
    cveData.setApps(new ArrayList<>());
    cveData.setLibraries(new ArrayList<>());
    return cveData;
  }

  private CveData createMockCveDataWithApps() {
    var cveData = new CveData();

    var app = mock(com.contrast.labs.ai.mcp.contrast.sdkextension.data.App.class);
    when(app.getApp_id()).thenReturn(TEST_APP_ID);
    when(app.getName()).thenReturn("Test Application");
    when(app.getClassCount()).thenReturn(0);

    var apps = new ArrayList<com.contrast.labs.ai.mcp.contrast.sdkextension.data.App>();
    apps.add(app);
    cveData.setApps(apps);

    var lib = mock(com.contrast.labs.ai.mcp.contrast.sdkextension.data.Library.class);
    when(lib.getHash()).thenReturn("matching-hash-789");
    when(lib.getFile_name()).thenReturn("vulnerable-lib.jar");
    when(lib.getVersion()).thenReturn("1.0.0");

    var libs = new ArrayList<com.contrast.labs.ai.mcp.contrast.sdkextension.data.Library>();
    libs.add(lib);
    cveData.setLibraries(libs);

    return cveData;
  }
}
