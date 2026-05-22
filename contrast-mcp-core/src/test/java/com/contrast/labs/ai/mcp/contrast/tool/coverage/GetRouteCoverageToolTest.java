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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.contrast.labs.ai.mcp.contrast.client.ContrastApiClient;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.routecoverage.Observation;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.routecoverage.Route;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.routecoverage.RouteCoverageBySessionIDAndMetadataRequestExtended;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.routecoverage.RouteCoverageResponse;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.sessionmetadata.AgentSession;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.sessionmetadata.SessionMetadataResponse;
import com.contrastsecurity.exceptions.HttpResponseException;
import com.contrastsecurity.models.RouteCoverageBySessionIDAndMetadataRequest;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;

class GetRouteCoverageToolTest {

  private static final String VALID_APP_ID = "app-123";
  private static final String METADATA_NAME = "branch";
  private static final String METADATA_VALUE = "main";
  private static final String SESSION_ID = "session-456";
  private static final String ROUTE_HASH = "route-hash-789";
  private static final String SECRET_BODY = "token=raw-token-value&apiKey=secret";

  private ContrastApiClient contrastApiClient;
  private GetRouteCoverageTool tool;

  @BeforeEach
  void setUp() {
    contrastApiClient = mock();
    tool = new GetRouteCoverageTool(contrastApiClient, new RouteMapper());
  }

  @Test
  void getRouteCoverage_should_return_validation_error_when_app_id_missing() {
    var result = tool.getRouteCoverage(null, null, null, null, null);

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors()).containsExactly("appId is required");
    verifyNoInteractions(contrastApiClient);
  }

  @Test
  void getRouteCoverage_should_route_unfiltered_request_through_contrast_api_client()
      throws Exception {
    var response = createRouteCoverageResponse(2);
    when(contrastApiClient.getRouteCoverage(eq(VALID_APP_ID), isNull())).thenReturn(response);

    var result = tool.getRouteCoverage(VALID_APP_ID, null, null, null, null);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.found()).isTrue();
    assertThat(result.data().routes()).hasSize(2);
    assertThat(result.data().totalRoutes()).isEqualTo(2);
    verify(contrastApiClient).getRouteCoverage(eq(VALID_APP_ID), isNull());
  }

  @Test
  void getRouteCoverage_should_route_session_metadata_filter_through_contrast_api_client()
      throws Exception {
    var response = createRouteCoverageResponse(1);
    when(contrastApiClient.getRouteCoverage(eq(VALID_APP_ID), any())).thenReturn(response);

    var result = tool.getRouteCoverage(VALID_APP_ID, METADATA_NAME, METADATA_VALUE, null, null);

    var requestCaptor = ArgumentCaptor.forClass(RouteCoverageBySessionIDAndMetadataRequest.class);
    verify(contrastApiClient).getRouteCoverage(eq(VALID_APP_ID), requestCaptor.capture());
    var request = requestCaptor.getValue();

    assertThat(result.isSuccess()).isTrue();
    assertThat(request).isInstanceOf(RouteCoverageBySessionIDAndMetadataRequestExtended.class);
    assertThat(request.getValues()).hasSize(1);
    assertThat(request.getValues().get(0).getLabel()).isEqualTo(METADATA_NAME);
    assertThat(request.getValues().get(0).getValues()).containsExactly(METADATA_VALUE);
  }

  @Test
  void getRouteCoverage_should_route_latest_session_filter_through_contrast_api_client()
      throws Exception {
    var response = createRouteCoverageResponse(1);
    when(contrastApiClient.getLatestSessionMetadata(VALID_APP_ID))
        .thenReturn(sessionMetadataResponse());
    when(contrastApiClient.getRouteCoverage(eq(VALID_APP_ID), any())).thenReturn(response);

    var result = tool.getRouteCoverage(VALID_APP_ID, null, null, true, null);

    var requestCaptor = ArgumentCaptor.forClass(RouteCoverageBySessionIDAndMetadataRequest.class);
    verify(contrastApiClient).getLatestSessionMetadata(VALID_APP_ID);
    verify(contrastApiClient).getRouteCoverage(eq(VALID_APP_ID), requestCaptor.capture());
    assertThat(result.isSuccess()).isTrue();
    assertThat(requestCaptor.getValue())
        .isInstanceOf(RouteCoverageBySessionIDAndMetadataRequestExtended.class);
    assertThat(requestCaptor.getValue().getSessionID()).isEqualTo(SESSION_ID);
  }

  @Test
  void getRouteCoverage_should_not_fetch_route_coverage_when_latest_session_missing()
      throws Exception {
    when(contrastApiClient.getLatestSessionMetadata(VALID_APP_ID)).thenReturn(null);

    var result = tool.getRouteCoverage(VALID_APP_ID, null, null, true, null);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.found()).isFalse();
    assertThat(result.warnings()).contains("Session metadata not available for this application");
    verify(contrastApiClient, never()).getRouteCoverage(any(), any());
  }

  @Test
  void getRouteCoverage_should_forward_tool_context_and_preserve_tool_name() throws Exception {
    var response = createRouteCoverageResponse(1);
    var toolContext = new ToolContext(Map.of("requestId", "req-123"));
    var capturedContext = new AtomicReference<ToolContext>();
    tool.setAuthenticationStrategy(
        context -> {
          capturedContext.set(context);
          return () -> {};
        });
    when(contrastApiClient.getRouteCoverage(eq(VALID_APP_ID), isNull())).thenReturn(response);

    var result = tool.getRouteCoverage(VALID_APP_ID, null, null, null, toolContext);
    Method method =
        GetRouteCoverageTool.class.getDeclaredMethod(
            "getRouteCoverage",
            String.class,
            String.class,
            String.class,
            Boolean.class,
            ToolContext.class);

    assertThat(result.isSuccess()).isTrue();
    assertThat(capturedContext.get()).isSameAs(toolContext);
    assertThat(method.getAnnotation(Tool.class).name()).isEqualTo("get_route_coverage");
  }

  @Test
  void getRouteCoverage_should_return_not_found_for_null_route_response() throws Exception {
    when(contrastApiClient.getRouteCoverage(eq(VALID_APP_ID), isNull())).thenReturn(null);

    var result = tool.getRouteCoverage(VALID_APP_ID, null, null, null, null);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.found()).isFalse();
    assertThat(result.data()).isNull();
  }

  @Test
  void getRouteCoverage_should_compute_light_response_aggregates() throws Exception {
    var response = createRouteCoverageResponse(4);
    response.getRoutes().get(0).setStatus("EXERCISED");
    response.getRoutes().get(1).setStatus("DISCOVERED");
    response.getRoutes().get(2).setStatus("EXERCISED");
    response.getRoutes().get(3).setStatus("DISCOVERED");
    response.getRoutes().get(0).setVulnerabilities(2);
    response.getRoutes().get(0).setCriticalVulnerabilities(1);
    response.getRoutes().get(1).setVulnerabilities(3);
    response.getRoutes().get(1).setCriticalVulnerabilities(2);
    when(contrastApiClient.getRouteCoverage(eq(VALID_APP_ID), isNull())).thenReturn(response);

    var result = tool.getRouteCoverage(VALID_APP_ID, null, null, null, null);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.data().totalRoutes()).isEqualTo(4);
    assertThat(result.data().exercisedCount()).isEqualTo(2);
    assertThat(result.data().discoveredCount()).isEqualTo(2);
    assertThat(result.data().coveragePercent()).isEqualTo(50.0);
    assertThat(result.data().totalVulnerabilities()).isEqualTo(5);
    assertThat(result.data().totalCriticalVulnerabilities()).isEqualTo(3);
    assertThat(result.data().routes().get(0).routeHash()).isEqualTo(ROUTE_HASH + "-0");
  }

  @Test
  void getRouteCoverage_should_map_downstream_403_without_exposing_response_body()
      throws Exception {
    when(contrastApiClient.getRouteCoverage(eq(VALID_APP_ID), isNull())).thenThrow(apiFailure(403));

    var result = tool.getRouteCoverage(VALID_APP_ID, null, null, null, null);

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors())
        .containsExactly("Access denied. User lacks permission for this resource.");
    assertThat(result.toString()).doesNotContain(SECRET_BODY, "/ng/org/route");
  }

  @Test
  void getRouteCoverage_should_map_downstream_429_without_exposing_response_body()
      throws Exception {
    when(contrastApiClient.getRouteCoverage(eq(VALID_APP_ID), isNull())).thenThrow(apiFailure(429));

    var result = tool.getRouteCoverage(VALID_APP_ID, null, null, null, null);

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors()).containsExactly("Rate limit exceeded. Retry after a brief pause.");
    assertThat(result.toString()).doesNotContain(SECRET_BODY, "/ng/org/route");
  }

  @Test
  void getRouteCoverage_should_map_downstream_5xx_without_exposing_response_body()
      throws Exception {
    when(contrastApiClient.getRouteCoverage(eq(VALID_APP_ID), isNull())).thenThrow(apiFailure(503));

    var result = tool.getRouteCoverage(VALID_APP_ID, null, null, null, null);

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors())
        .containsExactly(
            "The service returned an error. Narrow filters or reduce page size, then retry.");
    assertThat(result.toString()).doesNotContain(SECRET_BODY, "/ng/org/route");
  }

  private static RouteCoverageResponse createRouteCoverageResponse(int routeCount) {
    var response = new RouteCoverageResponse();
    response.setSuccess(true);
    var routes = new ArrayList<Route>();

    for (int index = 0; index < routeCount; index++) {
      var route = new Route();
      route.setSignature("GET /api/endpoint" + index);
      route.setRouteHash(ROUTE_HASH + "-" + index);
      route.setExercised(index % 2 == 0 ? 1L : 0L);

      var observation = new Observation();
      observation.setVerb("GET");
      observation.setUrl("/api/endpoint" + index);
      route.setObservations(List.of(observation));
      route.setTotalObservations(1L);

      routes.add(route);
    }

    response.setRoutes(routes);
    return response;
  }

  private static SessionMetadataResponse sessionMetadataResponse() {
    var response = new SessionMetadataResponse();
    var session = new AgentSession();
    session.setAgentSessionId(SESSION_ID);
    response.setAgentSession(session);
    return response;
  }

  private static HttpResponseException apiFailure(int status) {
    return new HttpResponseException(
        "Downstream failure", "POST", "/ng/org/route", status, "Downstream failure", SECRET_BODY);
  }
}
