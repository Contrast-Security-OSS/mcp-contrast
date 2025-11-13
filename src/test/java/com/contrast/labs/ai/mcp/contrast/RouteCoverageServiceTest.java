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

import com.contrast.labs.ai.mcp.contrast.sdkexstension.SDKExtension;
import com.contrast.labs.ai.mcp.contrast.sdkexstension.SDKHelper;
import com.contrast.labs.ai.mcp.contrast.sdkexstension.data.routecoverage.Route;
import com.contrast.labs.ai.mcp.contrast.sdkexstension.data.routecoverage.RouteCoverageBySessionIDAndMetadataRequestExtended;
import com.contrast.labs.ai.mcp.contrast.sdkexstension.data.routecoverage.RouteCoverageResponse;
import com.contrast.labs.ai.mcp.contrast.sdkexstension.data.routecoverage.RouteDetailsResponse;
import com.contrast.labs.ai.mcp.contrast.sdkexstension.data.sessionmetadata.AgentSession;
import com.contrast.labs.ai.mcp.contrast.sdkexstension.data.sessionmetadata.SessionMetadataResponse;
import com.contrastsecurity.sdk.ContrastSDK;
import java.io.IOException;
import java.util.ArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Comprehensive test suite for RouteCoverageService. Tests the consolidated get_route_coverage
 * method with different parameter combinations (AIML-224).
 *
 * <p>The consolidated method replaces 6 previous methods: - Unfiltered query:
 * getRouteCoverage(appId, null, null, null) - Session metadata filter: getRouteCoverage(appId,
 * name, value, null) - Latest session filter: getRouteCoverage(appId, null, null, true)
 */
@ExtendWith(MockitoExtension.class)
class RouteCoverageServiceTest {

  private RouteCoverageService routeCoverageService;
  private ContrastSDK mockContrastSDK;
  private SDKExtension mockSDKExtension;
  private MockedStatic<SDKHelper> mockedSDKHelper;
  private MockedConstruction<SDKExtension> mockedSDKExtensionConstruction;

  private static final String TEST_ORG_ID = "test-org-123";
  private static final String TEST_HOST = "https://test.contrast.local";
  private static final String TEST_API_KEY = "test-api-key";
  private static final String TEST_SERVICE_KEY = "test-service-key";
  private static final String TEST_USERNAME = "test-user";
  private static final String TEST_APP_ID = "test-app-456";
  private static final String TEST_SESSION_ID = "session-789";
  private static final String TEST_METADATA_NAME = "branch";
  private static final String TEST_METADATA_VALUE = "main";
  private static final String TEST_ROUTE_HASH = "route-hash-123";

  @BeforeEach
  void setUp() throws Exception {
    routeCoverageService = new RouteCoverageService();
    mockContrastSDK = mock(ContrastSDK.class);
    mockSDKExtension = mock(SDKExtension.class);

    // Mock the static SDKHelper.getSDK() method
    mockedSDKHelper = mockStatic(SDKHelper.class);
    mockedSDKHelper
        .when(
            () ->
                SDKHelper.getSDK(
                    anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
        .thenReturn(mockContrastSDK);

    // Mock SDKExtension construction to return our mock
    mockedSDKExtensionConstruction =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              // Configure the mock to behave like mockSDKExtension
              when(mock.getRouteCoverage(anyString(), anyString(), any()))
                  .thenAnswer(
                      invocation ->
                          mockSDKExtension.getRouteCoverage(
                              invocation.getArgument(0),
                              invocation.getArgument(1),
                              invocation.getArgument(2)));
              when(mock.getRouteDetails(anyString(), anyString(), anyString()))
                  .thenAnswer(
                      invocation ->
                          mockSDKExtension.getRouteDetails(
                              invocation.getArgument(0),
                              invocation.getArgument(1),
                              invocation.getArgument(2)));
              when(mock.getLatestSessionMetadata(anyString(), anyString()))
                  .thenAnswer(
                      invocation ->
                          mockSDKExtension.getLatestSessionMetadata(
                              invocation.getArgument(0), invocation.getArgument(1)));
            });

    // Set required configuration fields using reflection
    ReflectionTestUtils.setField(routeCoverageService, "orgID", TEST_ORG_ID);
    ReflectionTestUtils.setField(routeCoverageService, "hostName", TEST_HOST);
    ReflectionTestUtils.setField(routeCoverageService, "apiKey", TEST_API_KEY);
    ReflectionTestUtils.setField(routeCoverageService, "serviceKey", TEST_SERVICE_KEY);
    ReflectionTestUtils.setField(routeCoverageService, "userName", TEST_USERNAME);
    ReflectionTestUtils.setField(routeCoverageService, "httpProxyHost", "");
    ReflectionTestUtils.setField(routeCoverageService, "httpProxyPort", "");
  }

  @AfterEach
  void tearDown() {
    if (mockedSDKHelper != null) {
      mockedSDKHelper.close();
    }
    if (mockedSDKExtensionConstruction != null) {
      mockedSDKExtensionConstruction.close();
    }
  }

  // ========== Helper Methods ==========

  private RouteCoverageResponse createMockRouteCoverageResponse(int routeCount) {
    var response = new RouteCoverageResponse();
    response.setSuccess(true);
    var routes = new ArrayList<Route>();

    for (int i = 0; i < routeCount; i++) {
      var route = new Route();
      route.setSignature("GET /api/endpoint" + i);
      route.setRouteHash(TEST_ROUTE_HASH + "-" + i);
      route.setExercised(
          i % 2 == 0 ? 1L : 0L); // Alternate between exercised (>0) and discovered (0)
      routes.add(route);
    }

    response.setRoutes(routes);
    return response;
  }

  private RouteDetailsResponse createMockRouteDetailsResponse() {
    var response = new RouteDetailsResponse();
    response.setSuccess(true);
    return response;
  }

  private SessionMetadataResponse createMockSessionMetadataResponse() {
    var response = new SessionMetadataResponse();
    var session = new AgentSession();
    session.setAgentSessionId(TEST_SESSION_ID);
    response.setAgentSession(session);
    return response;
  }

  // ========== Test Case 1: Unfiltered Query (all parameters null) ==========

  @Test
  void testGetRouteCoverage_UnfilteredQuery_Success() throws Exception {
    // Arrange
    var mockResponse = createMockRouteCoverageResponse(3);
    when(mockSDKExtension.getRouteCoverage(eq(TEST_ORG_ID), eq(TEST_APP_ID), isNull()))
        .thenReturn(mockResponse);

    when(mockSDKExtension.getRouteDetails(eq(TEST_ORG_ID), eq(TEST_APP_ID), anyString()))
        .thenReturn(createMockRouteDetailsResponse());

    // Act
    var result = routeCoverageService.getRouteCoverage(TEST_APP_ID, null, null, null);

    // Assert
    assertNotNull(result);
    assertTrue(result.isSuccess());
    assertEquals(3, result.getRoutes().size());

    // Verify SDK was called with null metadata (unfiltered query)
    verify(mockSDKExtension).getRouteCoverage(eq(TEST_ORG_ID), eq(TEST_APP_ID), isNull());
    verify(mockSDKExtension, times(3))
        .getRouteDetails(eq(TEST_ORG_ID), eq(TEST_APP_ID), anyString());
  }

  @Test
  void testGetRouteCoverage_UnfilteredQuery_EmptyRoutes() throws Exception {
    // Arrange
    var mockResponse = createMockRouteCoverageResponse(0);
    when(mockSDKExtension.getRouteCoverage(eq(TEST_ORG_ID), eq(TEST_APP_ID), isNull()))
        .thenReturn(mockResponse);

    // Act
    var result = routeCoverageService.getRouteCoverage(TEST_APP_ID, null, null, null);

    // Assert
    assertNotNull(result);
    assertTrue(result.isSuccess());
    assertEquals(0, result.getRoutes().size());

    // Verify no route details calls made when no routes
    verify(mockSDKExtension, never()).getRouteDetails(anyString(), anyString(), anyString());
  }

  // ========== Test Case 2: Session Metadata Filter ==========

  @Test
  void testGetRouteCoverage_SessionMetadataFilter_Success() throws Exception {
    // Arrange
    var mockResponse = createMockRouteCoverageResponse(4);
    when(mockSDKExtension.getRouteCoverage(
            eq(TEST_ORG_ID),
            eq(TEST_APP_ID),
            any(RouteCoverageBySessionIDAndMetadataRequestExtended.class)))
        .thenReturn(mockResponse);

    when(mockSDKExtension.getRouteDetails(eq(TEST_ORG_ID), eq(TEST_APP_ID), anyString()))
        .thenReturn(createMockRouteDetailsResponse());

    // Act
    var result =
        routeCoverageService.getRouteCoverage(
            TEST_APP_ID, TEST_METADATA_NAME, TEST_METADATA_VALUE, null);

    // Assert
    assertNotNull(result);
    assertTrue(result.isSuccess());
    assertEquals(4, result.getRoutes().size());

    // Verify metadata filter structure
    var captor = ArgumentCaptor.forClass(RouteCoverageBySessionIDAndMetadataRequestExtended.class);
    verify(mockSDKExtension).getRouteCoverage(eq(TEST_ORG_ID), eq(TEST_APP_ID), captor.capture());

    var request = captor.getValue();
    assertNotNull(request);
    assertNotNull(request.getValues());
    assertEquals(1, request.getValues().size());

    var metadata = request.getValues().get(0);
    assertEquals(TEST_METADATA_NAME, metadata.getLabel());
    assertEquals(1, metadata.getValues().size());
    assertEquals(TEST_METADATA_VALUE, metadata.getValues().get(0));

    // Verify route details fetched for each route
    verify(mockSDKExtension, times(4))
        .getRouteDetails(eq(TEST_ORG_ID), eq(TEST_APP_ID), anyString());
  }

  @Test
  void testGetRouteCoverage_SessionMetadataFilter_MultipleRoutes() throws Exception {
    // Arrange
    var mockResponse = createMockRouteCoverageResponse(5);
    when(mockSDKExtension.getRouteCoverage(eq(TEST_ORG_ID), eq(TEST_APP_ID), any()))
        .thenReturn(mockResponse);

    when(mockSDKExtension.getRouteDetails(eq(TEST_ORG_ID), eq(TEST_APP_ID), anyString()))
        .thenReturn(createMockRouteDetailsResponse());

    // Act
    var result =
        routeCoverageService.getRouteCoverage(
            TEST_APP_ID, TEST_METADATA_NAME, TEST_METADATA_VALUE, null);

    // Assert
    assertEquals(5, result.getRoutes().size());

    // Verify route details fetched for each route
    verify(mockSDKExtension, times(5))
        .getRouteDetails(eq(TEST_ORG_ID), eq(TEST_APP_ID), anyString());

    // Verify each route has details attached
    for (var route : result.getRoutes()) {
      assertNotNull(route.getRouteDetailsResponse());
    }
  }

  @Test
  void testGetRouteCoverage_SessionMetadataFilter_MissingValue() throws Exception {
    // Act & Assert
    var exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              routeCoverageService.getRouteCoverage(TEST_APP_ID, TEST_METADATA_NAME, null, null);
            });

    assertTrue(exception.getMessage().contains("sessionMetadataValue is required"));

    // Verify SDK was never called
    verify(mockSDKExtension, never()).getRouteCoverage(anyString(), anyString(), any());
  }

  @Test
  void testGetRouteCoverage_SessionMetadataFilter_EmptyValue() throws Exception {
    // Test validation with empty string for sessionMetadataValue (MCP-3EG)
    // Act & Assert
    var exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              routeCoverageService.getRouteCoverage(TEST_APP_ID, TEST_METADATA_NAME, "", null);
            });

    assertTrue(exception.getMessage().contains("sessionMetadataValue is required"));

    // Verify SDK was never called
    verify(mockSDKExtension, never()).getRouteCoverage(anyString(), anyString(), any());
  }

  // ========== Test Case 3: Latest Session Filter ==========

  @Test
  void testGetRouteCoverage_LatestSessionFilter_Success() throws Exception {
    // Arrange
    var sessionResponse = createMockSessionMetadataResponse();
    when(mockSDKExtension.getLatestSessionMetadata(eq(TEST_ORG_ID), eq(TEST_APP_ID)))
        .thenReturn(sessionResponse);

    var mockResponse = createMockRouteCoverageResponse(2);
    when(mockSDKExtension.getRouteCoverage(
            eq(TEST_ORG_ID),
            eq(TEST_APP_ID),
            any(RouteCoverageBySessionIDAndMetadataRequestExtended.class)))
        .thenReturn(mockResponse);

    when(mockSDKExtension.getRouteDetails(eq(TEST_ORG_ID), eq(TEST_APP_ID), anyString()))
        .thenReturn(createMockRouteDetailsResponse());

    // Act
    var result = routeCoverageService.getRouteCoverage(TEST_APP_ID, null, null, true);

    // Assert
    assertNotNull(result);
    assertTrue(result.isSuccess());
    assertEquals(2, result.getRoutes().size());

    // Verify latest session was fetched
    verify(mockSDKExtension).getLatestSessionMetadata(eq(TEST_ORG_ID), eq(TEST_APP_ID));

    // Verify session ID was used in request
    var captor = ArgumentCaptor.forClass(RouteCoverageBySessionIDAndMetadataRequestExtended.class);
    verify(mockSDKExtension).getRouteCoverage(eq(TEST_ORG_ID), eq(TEST_APP_ID), captor.capture());

    var request = captor.getValue();
    assertNotNull(request);
    // Note: Can't verify sessionId directly as it's protected in base class
    // But we can verify the method was called
  }

  @Test
  void testGetRouteCoverage_LatestSessionFilter_NoSessionMetadata() throws Exception {
    // Arrange - Return null session metadata
    when(mockSDKExtension.getLatestSessionMetadata(eq(TEST_ORG_ID), eq(TEST_APP_ID)))
        .thenReturn(null);

    // Act
    var result = routeCoverageService.getRouteCoverage(TEST_APP_ID, null, null, true);

    // Assert - Should return empty response with success=false
    assertNotNull(result);
    assertFalse(result.isSuccess());

    // Verify route coverage was NOT called
    verify(mockSDKExtension, never()).getRouteCoverage(anyString(), anyString(), any());
  }

  @Test
  void testGetRouteCoverage_LatestSessionFilter_NullAgentSession() throws Exception {
    // Arrange - Return session metadata with null agent session
    var sessionResponse = new SessionMetadataResponse();
    sessionResponse.setAgentSession(null);
    when(mockSDKExtension.getLatestSessionMetadata(eq(TEST_ORG_ID), eq(TEST_APP_ID)))
        .thenReturn(sessionResponse);

    // Act
    var result = routeCoverageService.getRouteCoverage(TEST_APP_ID, null, null, true);

    // Assert - Should return empty response with success=false
    assertNotNull(result);
    assertFalse(result.isSuccess());

    // Verify route coverage was NOT called
    verify(mockSDKExtension, never()).getRouteCoverage(anyString(), anyString(), any());
  }

  // ========== Error Handling Tests ==========

  @Test
  void testGetRouteCoverage_SDKThrowsIOException() throws Exception {
    // Arrange
    when(mockSDKExtension.getRouteCoverage(eq(TEST_ORG_ID), eq(TEST_APP_ID), isNull()))
        .thenThrow(new IOException("API connection failed"));

    // Act & Assert
    var exception =
        assertThrows(
            IOException.class,
            () -> {
              routeCoverageService.getRouteCoverage(TEST_APP_ID, null, null, null);
            });

    assertEquals("API connection failed", exception.getMessage());
  }

  @Test
  void testGetRouteCoverage_RouteDetailsFails() throws Exception {
    // Arrange
    var mockResponse = createMockRouteCoverageResponse(2);
    when(mockSDKExtension.getRouteCoverage(eq(TEST_ORG_ID), eq(TEST_APP_ID), any()))
        .thenReturn(mockResponse);

    // First route succeeds, second route fails
    when(mockSDKExtension.getRouteDetails(eq(TEST_ORG_ID), eq(TEST_APP_ID), anyString()))
        .thenReturn(createMockRouteDetailsResponse())
        .thenThrow(new IOException("Failed to fetch route details"));

    // Act & Assert
    var exception =
        assertThrows(
            IOException.class,
            () -> {
              routeCoverageService.getRouteCoverage(
                  TEST_APP_ID, TEST_METADATA_NAME, TEST_METADATA_VALUE, null);
            });

    assertTrue(exception.getMessage().contains("Failed to fetch route details"));
  }

  @Test
  void testGetRouteCoverage_LatestSessionFetchFails() throws Exception {
    // Arrange
    when(mockSDKExtension.getLatestSessionMetadata(eq(TEST_ORG_ID), eq(TEST_APP_ID)))
        .thenThrow(new IOException("Session metadata API failed"));

    // Act & Assert
    var exception =
        assertThrows(
            IOException.class,
            () -> {
              routeCoverageService.getRouteCoverage(TEST_APP_ID, null, null, true);
            });

    assertTrue(exception.getMessage().contains("Session metadata API failed"));
  }

  // ========== SDK Configuration Tests ==========

  @Test
  void testGetRouteCoverage_UsesCorrectSDKConfiguration() throws Exception {
    // Arrange
    when(mockSDKExtension.getRouteCoverage(anyString(), anyString(), any()))
        .thenReturn(createMockRouteCoverageResponse(0));

    // Act
    routeCoverageService.getRouteCoverage(TEST_APP_ID, null, null, null);

    // Assert - Verify SDKHelper was called with correct configuration
    mockedSDKHelper.verify(
        () ->
            SDKHelper.getSDK(
                eq(TEST_HOST),
                eq(TEST_API_KEY),
                eq(TEST_SERVICE_KEY),
                eq(TEST_USERNAME),
                eq(""), // httpProxyHost
                eq("") // httpProxyPort
                ));
  }

  // ========== Parameter Combination Tests ==========

  @Test
  void testGetRouteCoverage_AllParametersNull() throws Exception {
    // Arrange
    var mockResponse = createMockRouteCoverageResponse(1);
    when(mockSDKExtension.getRouteCoverage(eq(TEST_ORG_ID), eq(TEST_APP_ID), isNull()))
        .thenReturn(mockResponse);
    when(mockSDKExtension.getRouteDetails(any(), any(), any()))
        .thenReturn(createMockRouteDetailsResponse());

    // Act
    var result = routeCoverageService.getRouteCoverage(TEST_APP_ID, null, null, null);

    // Assert
    assertNotNull(result);
    assertTrue(result.isSuccess());
    verify(mockSDKExtension).getRouteCoverage(eq(TEST_ORG_ID), eq(TEST_APP_ID), isNull());
  }

  @Test
  void testGetRouteCoverage_UseLatestSessionFalse() throws Exception {
    // Arrange - useLatestSession=false should behave same as null (no filter)
    var mockResponse = createMockRouteCoverageResponse(1);
    when(mockSDKExtension.getRouteCoverage(eq(TEST_ORG_ID), eq(TEST_APP_ID), isNull()))
        .thenReturn(mockResponse);
    when(mockSDKExtension.getRouteDetails(any(), any(), any()))
        .thenReturn(createMockRouteDetailsResponse());

    // Act
    var result = routeCoverageService.getRouteCoverage(TEST_APP_ID, null, null, false);

    // Assert
    assertNotNull(result);
    assertTrue(result.isSuccess());
    verify(mockSDKExtension).getRouteCoverage(eq(TEST_ORG_ID), eq(TEST_APP_ID), isNull());
    verify(mockSDKExtension, never()).getLatestSessionMetadata(anyString(), anyString());
  }

  @Test
  void testGetRouteCoverage_EmptyStringParameters_TreatedAsNull() throws Exception {
    // Arrange - Empty strings should be treated as null and trigger GET endpoint
    // This fixes bug MCP-OU8 where empty strings were incorrectly treated as valid filters
    var mockResponse = createMockRouteCoverageResponse(2);
    when(mockSDKExtension.getRouteCoverage(eq(TEST_ORG_ID), eq(TEST_APP_ID), isNull()))
        .thenReturn(mockResponse);
    when(mockSDKExtension.getRouteDetails(any(), any(), any()))
        .thenReturn(createMockRouteDetailsResponse());

    // Act - Pass empty strings for sessionMetadataName and sessionMetadataValue
    var result = routeCoverageService.getRouteCoverage(TEST_APP_ID, "", "", false);

    // Assert - Should call SDK with null (unfiltered query), not with empty metadata filter
    assertNotNull(result);
    assertTrue(result.isSuccess());
    assertEquals(2, result.getRoutes().size());

    // Verify SDK was called with null metadata (unfiltered query) - GET endpoint
    verify(mockSDKExtension).getRouteCoverage(eq(TEST_ORG_ID), eq(TEST_APP_ID), isNull());

    // Verify it did NOT try to create a metadata filter request with empty strings
    verify(mockSDKExtension, never())
        .getRouteCoverage(
            eq(TEST_ORG_ID),
            eq(TEST_APP_ID),
            any(RouteCoverageBySessionIDAndMetadataRequestExtended.class));

    // Verify route details were fetched
    verify(mockSDKExtension, times(2))
        .getRouteDetails(eq(TEST_ORG_ID), eq(TEST_APP_ID), anyString());
  }

  @Test
  void testGetRouteCoverage_EmptySessionMetadataNameOnly_TreatedAsNull() throws Exception {
    // Arrange - Empty sessionMetadataName with null value should also trigger unfiltered query
    var mockResponse = createMockRouteCoverageResponse(1);
    when(mockSDKExtension.getRouteCoverage(eq(TEST_ORG_ID), eq(TEST_APP_ID), isNull()))
        .thenReturn(mockResponse);
    when(mockSDKExtension.getRouteDetails(any(), any(), any()))
        .thenReturn(createMockRouteDetailsResponse());

    // Act
    var result = routeCoverageService.getRouteCoverage(TEST_APP_ID, "", null, null);

    // Assert
    assertNotNull(result);
    assertTrue(result.isSuccess());
    verify(mockSDKExtension).getRouteCoverage(eq(TEST_ORG_ID), eq(TEST_APP_ID), isNull());
  }
}
