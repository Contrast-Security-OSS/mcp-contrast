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
package com.contrast.labs.ai.mcp.contrast.tool.coverage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.contrast.labs.ai.mcp.contrast.config.ContrastSDKFactory;
import com.contrast.labs.ai.mcp.contrast.sdkextension.SDKExtension;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.routecoverage.Route;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.routecoverage.RouteCoverageBySessionIDAndMetadataRequestExtended;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.routecoverage.RouteCoverageResponse;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.routecoverage.RouteDetailsResponse;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.sessionmetadata.AgentSession;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.sessionmetadata.SessionMetadataResponse;
import com.contrastsecurity.sdk.ContrastSDK;
import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class GetRouteCoverageToolTest {

  private static final String VALID_APP_ID = "app-123";
  private static final String ORG_ID = "test-org-id";
  private static final String METADATA_NAME = "branch";
  private static final String METADATA_VALUE = "main";
  private static final String SESSION_ID = "session-456";
  private static final String ROUTE_HASH = "route-hash-789";

  private GetRouteCoverageTool tool;
  private ContrastSDKFactory sdkFactory;
  private ContrastSDK sdk;
  private SDKExtension sdkExtension;

  @BeforeEach
  void setUp() {
    sdk = mock();
    sdkFactory = mock();
    sdkExtension = mock();

    when(sdkFactory.getSDK()).thenReturn(sdk);
    when(sdkFactory.getOrgId()).thenReturn(ORG_ID);

    tool = new GetRouteCoverageTool();
    ReflectionTestUtils.setField(tool, "sdkFactory", sdkFactory);
  }

  // ========== Validation tests ==========

  @Test
  void getRouteCoverage_should_return_validation_error_when_appId_missing() {
    var result = tool.getRouteCoverage(null, null, null, null);

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors()).anyMatch(e -> e.contains("appId is required"));
  }

  @Test
  void getRouteCoverage_should_return_validation_error_when_metadata_unpaired_name_only() {
    var result = tool.getRouteCoverage(VALID_APP_ID, METADATA_NAME, null, null);

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors()).anyMatch(e -> e.contains("sessionMetadataValue is required"));
  }

  @Test
  void getRouteCoverage_should_return_validation_error_when_metadata_unpaired_value_only() {
    var result = tool.getRouteCoverage(VALID_APP_ID, null, METADATA_VALUE, null);

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors()).anyMatch(e -> e.contains("sessionMetadataName is required"));
  }

  @Test
  void getRouteCoverage_should_collect_multiple_validation_errors() {
    var result = tool.getRouteCoverage(null, METADATA_NAME, null, null);

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors()).hasSize(2);
    assertThat(result.errors())
        .anyMatch(e -> e.contains("appId is required"))
        .anyMatch(e -> e.contains("sessionMetadataValue is required"));
  }

  // ========== Unfiltered query tests ==========

  @Test
  void getRouteCoverage_should_return_data_when_unfiltered() throws Exception {
    var mockResponse = createMockRouteCoverageResponse(2);
    var routeDetails = createMockRouteDetailsResponse();

    try (var mockedConstruction =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              when(mock.getRouteCoverage(eq(ORG_ID), eq(VALID_APP_ID), isNull()))
                  .thenReturn(mockResponse);
              when(mock.getRouteDetails(eq(ORG_ID), eq(VALID_APP_ID), anyString()))
                  .thenReturn(routeDetails);
            })) {

      var result = tool.getRouteCoverage(VALID_APP_ID, null, null, null);

      assertThat(result.isSuccess()).isTrue();
      assertThat(result.found()).isTrue();
      assertThat(result.data()).isNotNull();
      assertThat(result.data().getRoutes()).hasSize(2);

      var constructedMock = mockedConstruction.constructed().get(0);
      verify(constructedMock).getRouteCoverage(eq(ORG_ID), eq(VALID_APP_ID), isNull());
      verify(constructedMock, times(2)).getRouteDetails(eq(ORG_ID), eq(VALID_APP_ID), anyString());
    }
  }

  @Test
  void getRouteCoverage_should_return_empty_routes_when_none_found() throws Exception {
    var mockResponse = createMockRouteCoverageResponse(0);

    try (var mockedConstruction =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              when(mock.getRouteCoverage(eq(ORG_ID), eq(VALID_APP_ID), isNull()))
                  .thenReturn(mockResponse);
            })) {

      var result = tool.getRouteCoverage(VALID_APP_ID, null, null, null);

      assertThat(result.isSuccess()).isTrue();
      assertThat(result.data()).isNotNull();
      assertThat(result.data().getRoutes()).isEmpty();

      var constructedMock = mockedConstruction.constructed().get(0);
      verify(constructedMock, never()).getRouteDetails(anyString(), anyString(), anyString());
    }
  }

  // ========== Session metadata filter tests ==========

  @Test
  void getRouteCoverage_should_filter_by_session_metadata() throws Exception {
    var mockResponse = createMockRouteCoverageResponse(1);
    var routeDetails = createMockRouteDetailsResponse();

    try (var mockedConstruction =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              when(mock.getRouteCoverage(
                      eq(ORG_ID),
                      eq(VALID_APP_ID),
                      any(RouteCoverageBySessionIDAndMetadataRequestExtended.class)))
                  .thenReturn(mockResponse);
              when(mock.getRouteDetails(eq(ORG_ID), eq(VALID_APP_ID), anyString()))
                  .thenReturn(routeDetails);
            })) {

      var result = tool.getRouteCoverage(VALID_APP_ID, METADATA_NAME, METADATA_VALUE, null);

      assertThat(result.isSuccess()).isTrue();
      assertThat(result.data()).isNotNull();
      assertThat(result.data().getRoutes()).hasSize(1);

      var constructedMock = mockedConstruction.constructed().get(0);
      var captor =
          ArgumentCaptor.forClass(RouteCoverageBySessionIDAndMetadataRequestExtended.class);
      verify(constructedMock).getRouteCoverage(eq(ORG_ID), eq(VALID_APP_ID), captor.capture());

      var request = captor.getValue();
      assertThat(request).isNotNull();
      assertThat(request.getValues()).hasSize(1);
      assertThat(request.getValues().get(0).getLabel()).isEqualTo(METADATA_NAME);
      assertThat(request.getValues().get(0).getValues()).contains(METADATA_VALUE);
    }
  }

  // ========== Latest session filter tests ==========

  @Test
  void getRouteCoverage_should_filter_by_latest_session() throws Exception {
    var sessionResponse = createMockSessionMetadataResponse();
    var mockResponse = createMockRouteCoverageResponse(1);
    var routeDetails = createMockRouteDetailsResponse();

    try (var mockedConstruction =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              when(mock.getLatestSessionMetadata(eq(ORG_ID), eq(VALID_APP_ID)))
                  .thenReturn(sessionResponse);
              when(mock.getRouteCoverage(
                      eq(ORG_ID),
                      eq(VALID_APP_ID),
                      any(RouteCoverageBySessionIDAndMetadataRequestExtended.class)))
                  .thenReturn(mockResponse);
              when(mock.getRouteDetails(eq(ORG_ID), eq(VALID_APP_ID), anyString()))
                  .thenReturn(routeDetails);
            })) {

      var result = tool.getRouteCoverage(VALID_APP_ID, null, null, true);

      assertThat(result.isSuccess()).isTrue();
      assertThat(result.data()).isNotNull();
      assertThat(result.data().getRoutes()).hasSize(1);

      var constructedMock = mockedConstruction.constructed().get(0);
      verify(constructedMock).getLatestSessionMetadata(eq(ORG_ID), eq(VALID_APP_ID));
    }
  }

  @Test
  void getRouteCoverage_should_return_empty_when_no_session_metadata() throws Exception {
    try (var mockedConstruction =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              when(mock.getLatestSessionMetadata(eq(ORG_ID), eq(VALID_APP_ID))).thenReturn(null);
            })) {

      var result = tool.getRouteCoverage(VALID_APP_ID, null, null, true);

      assertThat(result.isSuccess()).isTrue();
      assertThat(result.found()).isFalse(); // BaseGetTool treats success=false as not found

      var constructedMock = mockedConstruction.constructed().get(0);
      verify(constructedMock, never()).getRouteCoverage(anyString(), anyString(), any());
    }
  }

  @Test
  void getRouteCoverage_should_return_empty_when_no_agent_session() throws Exception {
    var sessionResponse = new SessionMetadataResponse();
    sessionResponse.setAgentSession(null);

    try (var mockedConstruction =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              when(mock.getLatestSessionMetadata(eq(ORG_ID), eq(VALID_APP_ID)))
                  .thenReturn(sessionResponse);
            })) {

      var result = tool.getRouteCoverage(VALID_APP_ID, null, null, true);

      assertThat(result.isSuccess()).isTrue();
      assertThat(result.found()).isFalse();

      var constructedMock = mockedConstruction.constructed().get(0);
      verify(constructedMock, never()).getRouteCoverage(anyString(), anyString(), any());
    }
  }

  // ========== Null API response handling ==========

  @Test
  void getRouteCoverage_should_handle_null_api_response() throws Exception {
    try (var mockedConstruction =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              when(mock.getRouteCoverage(eq(ORG_ID), eq(VALID_APP_ID), isNull())).thenReturn(null);
            })) {

      var result = tool.getRouteCoverage(VALID_APP_ID, null, null, null);

      assertThat(result.isSuccess()).isTrue();
      assertThat(result.found()).isFalse();
    }
  }

  @Test
  void getRouteCoverage_should_handle_null_routes_list() throws Exception {
    var mockResponse = new RouteCoverageResponse();
    mockResponse.setSuccess(true);
    mockResponse.setRoutes(null);

    try (var mockedConstruction =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              when(mock.getRouteCoverage(eq(ORG_ID), eq(VALID_APP_ID), isNull()))
                  .thenReturn(mockResponse);
            })) {

      var result = tool.getRouteCoverage(VALID_APP_ID, null, null, null);

      assertThat(result.isSuccess()).isTrue();
      assertThat(result.data()).isNotNull();
      assertThat(result.data().getRoutes()).isEmpty();
    }
  }

  // ========== Precedence warning ==========

  @Test
  void getRouteCoverage_should_add_warning_when_precedence_applies() throws Exception {
    var sessionResponse = createMockSessionMetadataResponse();
    var mockResponse = createMockRouteCoverageResponse(1);
    var routeDetails = createMockRouteDetailsResponse();

    try (var mockedConstruction =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              when(mock.getLatestSessionMetadata(eq(ORG_ID), eq(VALID_APP_ID)))
                  .thenReturn(sessionResponse);
              when(mock.getRouteCoverage(
                      eq(ORG_ID),
                      eq(VALID_APP_ID),
                      any(RouteCoverageBySessionIDAndMetadataRequestExtended.class)))
                  .thenReturn(mockResponse);
              when(mock.getRouteDetails(eq(ORG_ID), eq(VALID_APP_ID), anyString()))
                  .thenReturn(routeDetails);
            })) {

      var result = tool.getRouteCoverage(VALID_APP_ID, METADATA_NAME, METADATA_VALUE, true);

      assertThat(result.isSuccess()).isTrue();
      assertThat(result.warnings()).anyMatch(w -> w.contains("useLatestSession takes precedence"));
    }
  }

  // ========== Route details fetching ==========

  @Test
  void getRouteCoverage_should_fetch_route_details_for_each_route() throws Exception {
    var mockResponse = createMockRouteCoverageResponse(3);
    var routeDetails = createMockRouteDetailsResponse();

    try (var mockedConstruction =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              when(mock.getRouteCoverage(eq(ORG_ID), eq(VALID_APP_ID), isNull()))
                  .thenReturn(mockResponse);
              when(mock.getRouteDetails(eq(ORG_ID), eq(VALID_APP_ID), anyString()))
                  .thenReturn(routeDetails);
            })) {

      var result = tool.getRouteCoverage(VALID_APP_ID, null, null, null);

      assertThat(result.isSuccess()).isTrue();
      assertThat(result.data().getRoutes()).hasSize(3);

      // Verify route details were fetched for each route
      for (var route : result.data().getRoutes()) {
        assertThat(route.getRouteDetailsResponse()).isNotNull();
      }

      var constructedMock = mockedConstruction.constructed().get(0);
      verify(constructedMock, times(3)).getRouteDetails(eq(ORG_ID), eq(VALID_APP_ID), anyString());
    }
  }

  // ========== Helper methods ==========

  private RouteCoverageResponse createMockRouteCoverageResponse(int routeCount) {
    var response = new RouteCoverageResponse();
    response.setSuccess(true);
    var routes = new ArrayList<Route>();

    for (int i = 0; i < routeCount; i++) {
      var route = new Route();
      route.setSignature("GET /api/endpoint" + i);
      route.setRouteHash(ROUTE_HASH + "-" + i);
      route.setExercised(i % 2 == 0 ? 1L : 0L);
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
    session.setAgentSessionId(SESSION_ID);
    response.setAgentSession(session);
    return response;
  }
}
