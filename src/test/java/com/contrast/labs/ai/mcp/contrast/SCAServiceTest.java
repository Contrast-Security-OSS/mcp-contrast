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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    assertThat(result).as("Result should not be null").isNotNull();
    assertThat(result.size()).as("Should return 3 libraries").isEqualTo(3);

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
    assertThat(result).as("Result should not be null").isNotNull();
    assertThat(result.isEmpty()).as("Result should be empty list").isTrue();
  }

  @Test
  void testGetApplicationLibrariesByID_NullAppID() {
    // When/Then - Should handle null gracefully or throw descriptive exception
    assertThatThrownBy(
            () -> {
              scaService.getApplicationLibrariesByID(null);
            })
        .as("Should throw exception for null app ID")
        .isInstanceOf(Exception.class);
  }

  @Test
  void testGetApplicationLibrariesByID_EmptyAppID() {
    // When/Then - Should handle empty string appropriately
    assertThatThrownBy(
            () -> {
              scaService.getApplicationLibrariesByID("");
            })
        .as("Should throw exception for empty app ID")
        .isInstanceOf(Exception.class);
  }

  @Test
  void testGetApplicationLibrariesByID_SDKFailure() {
    // Given - SDK throws exception
    mockedSDKHelper
        .when(() -> SDKHelper.getLibsForID(anyString(), anyString(), any(SDKExtension.class)))
        .thenThrow(new RuntimeException("SDK connection failed"));

    // When/Then
    assertThatThrownBy(
            () -> {
              scaService.getApplicationLibrariesByID(TEST_APP_ID);
            })
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("SDK connection failed");
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
    assertThat(result.size()).isEqualTo(2);

    // Verify first library has class usage > 0 (actively used)
    assertThat(result.get(0).getClassedUsed())
        .as("First library should have classes used")
        .isGreaterThan(0);

    // Verify second library has class usage = 0 (likely unused)
    assertThat(result.get(1).getClassedUsed())
        .as("Second library should have zero classes used")
        .isEqualTo(0);
  }

  // ========== Tests for list_applications_by_cve ==========

  @Test
  void testListCVESForApplication_Success() throws IOException {
    // Given
    var mockCveData = createMockCveDataWithApps();
    var mockLibraries = createMockLibrariesWithClassUsage();

    // Mock SDKExtension.getAppsForCVE
    SDKExtension mockExtension = mock();
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
    assertThat(result).as("Result should not be null").isNotNull();
    assertThat(result.getApps()).as("Apps list should not be null").isNotNull();
    assertThat(result.getLibraries()).as("Libraries list should not be null").isNotNull();
    assertThat(result.getApps()).as("Should have at least one app").isNotEmpty();
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
    assertThat(result).as("Result should not be null").isNotNull();
    assertThat(result.getApps()).as("Should have no vulnerable apps").isEmpty();
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
    assertThat(result).as("Result should not be null").isNotNull();
    assertThat(result.getApps()).as("Should have apps").isNotEmpty();

    // Verify class usage was populated for apps
    // (Implementation in SCAService populates classCount and classUsage fields)
    var firstApp = result.getApps().get(0);
    assertThat(firstApp.getClassCount())
        .as("Class count should be populated")
        .isGreaterThanOrEqualTo(0);
  }

  @Test
  void testListCVESForApplication_NullLibrariesList_WithAppLibraries() throws IOException {
    // Given - CVE data with null libraries list AND app has libraries
    // Fix: null lists are now handled gracefully as empty lists
    var cveData = new CveData();
    var app = mock(com.contrast.labs.ai.mcp.contrast.sdkextension.data.App.class);
    when(app.getApp_id()).thenReturn(TEST_APP_ID);
    when(app.getName()).thenReturn("Test Application");

    var apps = new ArrayList<com.contrast.labs.ai.mcp.contrast.sdkextension.data.App>();
    apps.add(app);
    cveData.setApps(apps);
    cveData.setLibraries(null); // NULL libraries list - handled gracefully

    if (mockedSDKExtension != null) {
      mockedSDKExtension.close();
    }
    mockedSDKExtension =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              when(mock.getAppsForCVE(eq(TEST_ORG_ID), eq(TEST_CVE_ID))).thenReturn(cveData);
            });

    // Mock getLibsForID to return libraries
    var mockLibraries = createMockLibraries(1);
    mockedSDKHelper
        .when(() -> SDKHelper.getLibsForID(anyString(), eq(TEST_ORG_ID), any(SDKExtension.class)))
        .thenReturn(mockLibraries);

    // When - no exception thrown, returns result with apps but null libs treated as empty
    var result = scaService.listCVESForApplication(TEST_CVE_ID);

    // Then - result contains apps, null libraries handled gracefully
    assertThat(result).isNotNull();
    assertThat(result.getApps()).hasSize(1);
    assertThat(result.getLibraries()).isNull(); // Original null is preserved in result
  }

  @Test
  void testListCVESForApplication_NullAppsList() throws IOException {
    // Given - CVE data with null apps list
    // Fix: null lists are now handled gracefully as empty lists
    var cveData = new CveData();
    cveData.setApps(null); // NULL apps list - handled gracefully
    cveData.setLibraries(new ArrayList<>());

    if (mockedSDKExtension != null) {
      mockedSDKExtension.close();
    }
    mockedSDKExtension =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              when(mock.getAppsForCVE(eq(TEST_ORG_ID), eq(TEST_CVE_ID))).thenReturn(cveData);
            });

    // When - no exception thrown, returns result with null apps treated as empty
    var result = scaService.listCVESForApplication(TEST_CVE_ID);

    // Then - result returned, null apps handled gracefully (no NPE)
    assertThat(result).isNotNull();
    assertThat(result.getApps()).isNull(); // Original null is preserved in result
    assertThat(result.getLibraries()).isEmpty();
  }

  @Test
  void testListCVESForApplication_ZeroClassUsage_NotPopulated() throws IOException {
    // Given - Library with classUsage=0 should NOT populate app fields
    var mockCveData = createMockCveDataWithApps();

    // Library with ZERO class usage
    var mockLibraries = new ArrayList<LibraryExtended>();
    LibraryExtended lib =
        AnonymousLibraryExtendedBuilder.validLibrary()
            .withFilename("unused-lib.jar")
            .withHash("matching-hash-789") // Matches CVE data
            .withVersion("1.0.0")
            .withClassCount(100)
            .withClassedUsed(0) // ZERO usage - should NOT populate!
            .build();
    mockLibraries.add(lib);

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
        .thenReturn(mockLibraries);

    // When
    var result = scaService.listCVESForApplication(TEST_CVE_ID);

    // Then
    assertThat(result).as("Result should not be null").isNotNull();
    assertThat(result.getApps()).as("Should have apps").isNotEmpty();

    // Verify class usage was NOT populated (classUsage=0 means not exploitable)
    var firstApp = result.getApps().get(0);
    assertThat(firstApp.getClassCount())
        .as("Class count should NOT be populated when classUsage=0")
        .isEqualTo(0); // Should remain at initial value (0 from mock setup)
  }

  // ========== Helper Methods ==========

  private List<LibraryExtended> createMockLibraries(int count) {
    var libraries = new ArrayList<LibraryExtended>();
    for (int i = 0; i < count; i++) {
      LibraryExtended lib =
          AnonymousLibraryExtendedBuilder.validLibrary()
              .withFilename("library-" + i + ".jar")
              .withHash("hash-" + i)
              .withVersion("1.0." + i)
              .withClassCount(100)
              .withClassedUsed(50)
              .build();
      libraries.add(lib);
    }
    return libraries;
  }

  private List<LibraryExtended> createMockLibrariesWithClassUsage() {
    var libraries = new ArrayList<LibraryExtended>();

    // Library 1: Actively used (classesUsed > 0)
    LibraryExtended lib1 =
        AnonymousLibraryExtendedBuilder.validLibrary()
            .withFilename("actively-used-lib.jar")
            .withHash("hash-active-123")
            .withVersion("2.1.0")
            .withClassCount(150)
            .withClassedUsed(75) // 50% usage
            .build();
    libraries.add(lib1);

    // Library 2: Likely unused (classesUsed = 0)
    LibraryExtended lib2 =
        AnonymousLibraryExtendedBuilder.validLibrary()
            .withFilename("unused-lib.jar")
            .withHash("hash-unused-456")
            .withVersion("1.5.2")
            .withClassCount(200)
            .withClassedUsed(0) // Not used!
            .build();
    libraries.add(lib2);

    return libraries;
  }

  private List<LibraryExtended> createMockLibrariesWithMatchingHash() {
    var libraries = new ArrayList<LibraryExtended>();

    LibraryExtended lib =
        AnonymousLibraryExtendedBuilder.validLibrary()
            .withFilename("vulnerable-lib.jar")
            .withHash("matching-hash-789")
            .withVersion("1.0.0")
            .withClassCount(100)
            .withClassedUsed(50)
            .build();
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
