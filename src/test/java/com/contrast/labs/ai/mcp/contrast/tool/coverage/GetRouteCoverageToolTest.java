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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.contrast.labs.ai.mcp.contrast.config.ContrastSDKFactory;
import com.contrast.labs.ai.mcp.contrast.mapper.RouteMapper;
import com.contrast.labs.ai.mcp.contrast.sdkextension.SDKExtension;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.routecoverage.Observation;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.routecoverage.Route;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.routecoverage.RouteCoverageBySessionIDAndMetadataRequestExtended;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.routecoverage.RouteCoverageResponse;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.sessionmetadata.AgentSession;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.sessionmetadata.SessionMetadataResponse;
import com.contrastsecurity.sdk.ContrastSDK;
import java.util.ArrayList;
import java.util.List;
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
  private RouteMapper routeMapper;

  @BeforeEach
  void setUp() {
    sdk = mock();
    sdkFactory = mock();
    sdkExtension = mock();
    routeMapper = new RouteMapper();

    when(sdkFactory.getSDK()).thenReturn(sdk);
    when(sdkFactory.getOrgId()).thenReturn(ORG_ID);

    tool = new GetRouteCoverageTool(routeMapper);
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

    try (var mockedConstruction =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              when(mock.getRouteCoverage(eq(ORG_ID), eq(VALID_APP_ID), isNull()))
                  .thenReturn(mockResponse);
            })) {

      var result = tool.getRouteCoverage(VALID_APP_ID, null, null, null);

      assertThat(result.isSuccess()).isTrue();
      assertThat(result.found()).isTrue();
      assertThat(result.data()).isNotNull();
      assertThat(result.data().routes()).hasSize(2);

      // Observations are included inline via expand=observations
      var constructedMock = mockedConstruction.constructed().get(0);
      verify(constructedMock).getRouteCoverage(eq(ORG_ID), eq(VALID_APP_ID), isNull());
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
      assertThat(result.data().routes()).isEmpty();
    }
  }

  // ========== Session metadata filter tests ==========

  @Test
  void getRouteCoverage_should_filter_by_session_metadata() throws Exception {
    var mockResponse = createMockRouteCoverageResponse(1);

    try (var mockedConstruction =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              when(mock.getRouteCoverage(
                      eq(ORG_ID),
                      eq(VALID_APP_ID),
                      any(RouteCoverageBySessionIDAndMetadataRequestExtended.class)))
                  .thenReturn(mockResponse);
            })) {

      var result = tool.getRouteCoverage(VALID_APP_ID, METADATA_NAME, METADATA_VALUE, null);

      assertThat(result.isSuccess()).isTrue();
      assertThat(result.data()).isNotNull();
      assertThat(result.data().routes()).hasSize(1);

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
            })) {

      var result = tool.getRouteCoverage(VALID_APP_ID, null, null, true);

      assertThat(result.isSuccess()).isTrue();
      assertThat(result.data()).isNotNull();
      assertThat(result.data().routes()).hasSize(1);

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
      assertThat(result.found()).isFalse(); // BaseSingleTool treats success=false as not found

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
      assertThat(result.data().routes()).isEmpty();
    }
  }

  // ========== Precedence warning ==========

  @Test
  void getRouteCoverage_should_add_warning_when_precedence_applies() throws Exception {
    var sessionResponse = createMockSessionMetadataResponse();
    var mockResponse = createMockRouteCoverageResponse(1);

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
            })) {

      var result = tool.getRouteCoverage(VALID_APP_ID, METADATA_NAME, METADATA_VALUE, true);

      assertThat(result.isSuccess()).isTrue();
      assertThat(result.warnings()).anyMatch(w -> w.contains("useLatestSession takes precedence"));
    }
  }

  // ========== Observations are included inline ==========

  @Test
  void getRouteCoverage_should_return_observations_inline() throws Exception {
    var mockResponse = createMockRouteCoverageResponse(3);

    try (var mockedConstruction =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              when(mock.getRouteCoverage(eq(ORG_ID), eq(VALID_APP_ID), isNull()))
                  .thenReturn(mockResponse);
            })) {

      var result = tool.getRouteCoverage(VALID_APP_ID, null, null, null);

      assertThat(result.isSuccess()).isTrue();
      assertThat(result.data().routes()).hasSize(3);

      // Verify observations are included inline (from expand=observations)
      for (var route : result.data().routes()) {
        assertThat(route.observations()).isNotNull();
        assertThat(route.observations()).hasSize(1);
        assertThat(route.totalObservations()).isEqualTo(1L);
      }
    }
  }

  // ========== Light response transformation tests ==========

  @Test
  void getRouteCoverage_should_return_light_response_with_aggregate_statistics() throws Exception {
    var mockResponse = createMockRouteCoverageResponse(4);
    // Set status on routes: 2 exercised (even indices), 2 discovered (odd indices)
    mockResponse.getRoutes().get(0).setStatus("EXERCISED");
    mockResponse.getRoutes().get(1).setStatus("DISCOVERED");
    mockResponse.getRoutes().get(2).setStatus("EXERCISED");
    mockResponse.getRoutes().get(3).setStatus("DISCOVERED");
    // Add vulnerability counts
    mockResponse.getRoutes().get(0).setVulnerabilities(2);
    mockResponse.getRoutes().get(0).setCriticalVulnerabilities(1);
    mockResponse.getRoutes().get(1).setVulnerabilities(3);
    mockResponse.getRoutes().get(1).setCriticalVulnerabilities(2);

    try (var mockedConstruction =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              when(mock.getRouteCoverage(eq(ORG_ID), eq(VALID_APP_ID), isNull()))
                  .thenReturn(mockResponse);
            })) {

      var result = tool.getRouteCoverage(VALID_APP_ID, null, null, null);

      assertThat(result.isSuccess()).isTrue();
      var lightResponse = result.data();

      // Verify aggregate statistics are computed
      assertThat(lightResponse.totalRoutes()).isEqualTo(4);
      assertThat(lightResponse.exercisedCount()).isEqualTo(2);
      assertThat(lightResponse.discoveredCount()).isEqualTo(2);
      assertThat(lightResponse.coveragePercent()).isEqualTo(50.0);
      assertThat(lightResponse.totalVulnerabilities()).isEqualTo(5);
      assertThat(lightResponse.totalCriticalVulnerabilities()).isEqualTo(3);

      // Verify routes are transformed to light format
      assertThat(lightResponse.routes()).hasSize(4);
      var firstRoute = lightResponse.routes().get(0);
      assertThat(firstRoute.signature()).isEqualTo("GET /api/endpoint0");
      assertThat(firstRoute.routeHash()).isEqualTo(ROUTE_HASH + "-0");
      assertThat(firstRoute.status()).isEqualTo("EXERCISED");
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

      // Include observations inline (simulating expand=observations response)
      var observation = new Observation();
      observation.setVerb("GET");
      observation.setUrl("/api/endpoint" + i);
      route.setObservations(List.of(observation));
      route.setTotalObservations(1L);

      routes.add(route);
    }

    response.setRoutes(routes);
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
