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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.contrast.labs.ai.mcp.contrast.client.SdkApiClient;
import com.contrast.labs.ai.mcp.contrast.config.ContrastSDKFactory;
import com.contrast.labs.ai.mcp.contrast.config.SDKExtensionFactory;
import com.contrast.labs.ai.mcp.contrast.sdkextension.SDKExtension;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.routecoverage.Route;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.routecoverage.RouteCoverageResponse;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.sessionmetadata.AgentSession;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.sessionmetadata.SessionMetadataResponse;
import com.contrastsecurity.models.RouteCoverageBySessionIDAndMetadataRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetRouteCoverageLocalParityTest {

  private static final String TEST_ORG_ID = "local-org-id-should-not-serialize";
  private static final String TEST_APP_ID = "app-123";
  private static final String TEST_SESSION_ID = "session-456";

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Mock private ContrastSDKFactory sdkFactory;
  @Mock private SDKExtensionFactory sdkExtensionFactory;
  @Mock private SDKExtension sdkExtension;

  private GetRouteCoverageTool tool;

  @BeforeEach
  void setUp() {
    var sdkApiClient = new SdkApiClient(sdkFactory, sdkExtensionFactory);
    tool = new GetRouteCoverageTool(sdkApiClient, new RouteMapper());
    when(sdkFactory.getOrgId()).thenReturn(TEST_ORG_ID);
    when(sdkExtensionFactory.getSDKExtension()).thenReturn(sdkExtension);
  }

  @Test
  void getRouteCoverage_should_preserve_local_stdio_response_through_sdk_api_client()
      throws Exception {
    var response = routeCoverageResponse("GET /api/orders");
    when(sdkExtension.getRouteCoverage(eq(TEST_ORG_ID), eq(TEST_APP_ID), isNull()))
        .thenReturn(response);

    var result = tool.getRouteCoverage(TEST_APP_ID, null, null, null);
    var responseJson = objectMapper.writeValueAsString(result);

    verify(sdkExtension).getRouteCoverage(eq(TEST_ORG_ID), eq(TEST_APP_ID), isNull());
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.data().routes())
        .singleElement()
        .satisfies(route -> assertThat(route.signature()).isEqualTo("GET /api/orders"));
    assertThat(responseJson).contains("GET /api/orders").doesNotContain(TEST_ORG_ID);
  }

  @Test
  void getRouteCoverage_should_preserve_latest_session_local_sdk_calls() throws Exception {
    var sessionResponse = new SessionMetadataResponse();
    var session = new AgentSession();
    session.setAgentSessionId(TEST_SESSION_ID);
    sessionResponse.setAgentSession(session);
    when(sdkExtension.getLatestSessionMetadata(TEST_ORG_ID, TEST_APP_ID))
        .thenReturn(sessionResponse);
    when(sdkExtension.getRouteCoverage(eq(TEST_ORG_ID), eq(TEST_APP_ID), any()))
        .thenReturn(routeCoverageResponse("GET /api/latest"));

    var result = tool.getRouteCoverage(TEST_APP_ID, null, null, true);

    var requestCaptor = ArgumentCaptor.forClass(RouteCoverageBySessionIDAndMetadataRequest.class);
    verify(sdkExtension).getLatestSessionMetadata(TEST_ORG_ID, TEST_APP_ID);
    verify(sdkExtension)
        .getRouteCoverage(eq(TEST_ORG_ID), eq(TEST_APP_ID), requestCaptor.capture());
    assertThat(result.isSuccess()).isTrue();
    assertThat(requestCaptor.getValue().getSessionID()).isEqualTo(TEST_SESSION_ID);
    assertThat(objectMapper.writeValueAsString(result)).doesNotContain(TEST_ORG_ID);
  }

  private static RouteCoverageResponse routeCoverageResponse(String signature) {
    var route = new Route();
    route.setSignature(signature);
    route.setRouteHash("route-hash");
    route.setStatus("EXERCISED");

    var response = new RouteCoverageResponse();
    response.setSuccess(true);
    response.setRoutes(List.of(route));
    return response;
  }
}
