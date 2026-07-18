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
package com.contrast.labs.ai.mcp.contrast.tool.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.contrast.labs.ai.mcp.contrast.client.SdkApiClient;
import com.contrast.labs.ai.mcp.contrast.config.ContrastSDKFactory;
import com.contrast.labs.ai.mcp.contrast.config.SDKExtensionFactory;
import com.contrast.labs.ai.mcp.contrast.sdkextension.SDKExtension;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.server.ServerDetail;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.server.ServerFilterBody;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.server.ServersResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SearchServersLocalParityTest {

  private static final String TEST_ORG_ID = "local-org-id-should-not-serialize";

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Mock private ContrastSDKFactory sdkFactory;
  @Mock private SDKExtensionFactory sdkExtensionFactory;
  @Mock private SDKExtension sdkExtension;

  private SearchServersTool tool;

  @BeforeEach
  void setUp() {
    var sdkApiClient = new SdkApiClient(sdkFactory, sdkExtensionFactory);
    tool = new SearchServersTool(sdkApiClient);
    when(sdkFactory.getOrgId()).thenReturn(TEST_ORG_ID);
    when(sdkExtensionFactory.getSDKExtension()).thenReturn(sdkExtension);
  }

  @Test
  void searchServers_should_preserve_local_stdio_response_through_sdk_api_client()
      throws Exception {
    var server = new ServerDetail();
    server.setServerId(42L);
    server.setName("prod-1");
    server.setLatestAgentVersion("NO VERSION AVAILABLE");
    server.setDefend(null);
    server.setApplicationCount(0L);
    server.setTags(List.of("blue"));
    var response = new ServersResponse();
    response.setSuccess(true);
    response.setServers(List.of(server));
    response.setCount(3L);
    when(sdkExtension.getServersFiltered(
            eq(TEST_ORG_ID), any(), eq(25), eq(50), eq("-version"), eq(false)))
        .thenReturn(response);

    var result =
        tool.searchServers(
            3,
            25,
            "prod",
            "PRODUCTION",
            "ONLINE",
            "INFO",
            "blue",
            null,
            false,
            "5.0.0",
            false,
            "agentVersion,DESC");
    var responseJson = objectMapper.writeValueAsString(result);

    var filterCaptor = ArgumentCaptor.forClass(ServerFilterBody.class);
    verify(sdkExtension)
        .getServersFiltered(
            eq(TEST_ORG_ID), filterCaptor.capture(), eq(25), eq(50), eq("-version"), eq(false));
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.items())
        .singleElement()
        .satisfies(
            item -> {
              assertThat(item.serverId()).isEqualTo(42L);
              assertThat(item.protectEnabled()).isNull();
              assertThat(item.agentOutOfDate()).isNull();
              assertThat(item.tags()).containsExactly("blue");
            });
    assertThat(result.totalItems()).isEqualTo(3);
    assertThat(filterCaptor.getValue().getQuickFilter()).isEqualTo("ONLINE");
    assertThat(filterCaptor.getValue().getServerEnvironments()).containsExactly("PRODUCTION");
    assertThat(responseJson).contains("prod-1").doesNotContain(TEST_ORG_ID);
  }
}
